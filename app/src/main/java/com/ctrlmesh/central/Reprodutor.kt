package com.ctrlmesh.central

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Build
import android.view.Surface
import com.ctrlmesh.protocolo.Mensagem
import java.nio.ByteBuffer
import kotlin.concurrent.thread

/**
 * Decodifica o H.264 do celular na superfície da tela.
 * A thread de rede só entrega quadros numa fila curta; uma thread própria alimenta o decodificador. Se o
 * decodificador não acompanha, a fila descarta até o próximo quadro-chave (nunca entrega um GOP com buracos).
 * O GOP atual fica guardado para reabrir sem esperar o próximo quadro-chave.
 */
class ReprodutorDeVideo(private val aoDesenhar: () -> Unit) {
    val medidor = com.ctrlmesh.Medidor()
    private var superficie: Surface? = null
    private var config: Mensagem.VideoConfig? = null
    private val gop = ArrayList<Mensagem.VideoQuadro>()
    private var codec: MediaCodec? = null
    private var entrada: java.util.concurrent.ArrayBlockingQueue<Mensagem.VideoQuadro>? = null
    private var aguardandoChave = false
    @Volatile private var ultimoRecebidoUs = 0L

    @Synchronized
    fun definirSuperficie(nova: Surface?) {
        if (nova === superficie && (nova == null || codec != null)) return // surfaceChanged repete a mesma superfície
        superficie = nova
        recriar()
    }

    @Synchronized
    fun configurar(nova: Mensagem.VideoConfig) {
        config = nova
        gop.clear()
        medidor.reiniciarRelogio()
        recriar()
    }

    @Synchronized
    fun quadro(quadro: Mensagem.VideoQuadro) {
        medidor.contar("recebidos")
        medidor.chegou(quadro.ptsUs / 1000, com.ctrlmesh.agoraMs())
        ultimoRecebidoUs = quadro.ptsUs
        if (quadro.chave) {
            gop.clear()
            aguardandoChave = false
        }
        if (gop.isEmpty() && !quadro.chave) return
        if (gop.size < LIMITE_DO_GOP) gop += quadro
        val fila = entrada ?: return
        if (aguardandoChave) {
            medidor.contar("descartados")
            return
        }
        if (quadro.chave) fila.clear() // quadros antigos não servem mais
        if (!fila.offer(quadro)) {
            fila.clear()
            aguardandoChave = true
            medidor.contar("descartados")
        }
    }

    @Synchronized
    fun limpar() {
        config = null
        gop.clear()
        liberar()
    }

    private fun recriar() {
        liberar()
        val s = superficie ?: return
        val c = config ?: return
        if (!s.isValid) return
        val formato = MediaFormat.createVideoFormat(c.codec, c.largura, c.altura)
        c.csd.forEachIndexed { i, dados -> formato.setByteBuffer("csd-$i", ByteBuffer.wrap(dados)) }
        formato.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maxOf(c.largura * c.altura, TAMANHO_MINIMO_DE_ENTRADA))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) formato.setInteger(MediaFormat.KEY_LOW_LATENCY, 1)
        formato.setInteger(MediaFormat.KEY_PRIORITY, 0)
        val novo = runCatching {
            MediaCodec.createDecoderByType(c.codec).apply {
                configure(formato, s, null, 0)
                start()
            }
        }.getOrElse {
            com.ctrlmesh.registrar("decodificador de vídeo não abriu: ${it.message}")
            return
        }
        codec = novo
        val fila = java.util.concurrent.ArrayBlockingQueue<Mensagem.VideoQuadro>(TAMANHO_DA_FILA)
        entrada = fila
        aguardandoChave = false
        gop.forEach { fila.offer(it) || return@forEach }
        thread(isDaemon = true, name = "ctrlmesh-video-entrada") { alimentar(novo, fila) }
        thread(isDaemon = true, name = "ctrlmesh-video-saida") { drenar(novo) }
    }

    private fun alimentar(c: MediaCodec, fila: java.util.concurrent.ArrayBlockingQueue<Mensagem.VideoQuadro>) {
        try {
            while (codec === c) {
                val quadro = fila.poll(ESPERA_MS, java.util.concurrent.TimeUnit.MILLISECONDS) ?: continue
                var indice = -1
                while (indice < 0 && codec === c) indice = c.dequeueInputBuffer(ESPERA_US)
                if (indice < 0) return
                val buffer = c.getInputBuffer(indice) ?: return
                buffer.clear()
                if (quadro.dados.size > buffer.remaining()) {
                    c.queueInputBuffer(indice, 0, 0, quadro.ptsUs, 0)
                    medidor.contar("grande_demais")
                    continue
                }
                buffer.put(quadro.dados)
                c.queueInputBuffer(indice, 0, quadro.dados.size, quadro.ptsUs, if (quadro.chave) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0)
            }
        } catch (e: IllegalStateException) {
            if (codec === c) com.ctrlmesh.registrar("decodificador de vídeo parou na entrada: ${e.message}")
        }
    }

    private fun drenar(c: MediaCodec) {
        val info = MediaCodec.BufferInfo()
        try {
            while (codec === c) {
                val indice = c.dequeueOutputBuffer(info, ESPERA_US)
                if (indice < 0) continue
                // Quadro já velho em relação ao último recebido: decodifica (é referência), mas não desenha.
                val atrasado = ultimoRecebidoUs - info.presentationTimeUs > ATRASO_MAXIMO_US
                c.releaseOutputBuffer(indice, !atrasado)
                if (atrasado) {
                    medidor.contar("nao_desenhados")
                    continue
                }
                medidor.contar("desenhados")
                medidor.concluiu(info.presentationTimeUs / 1000, com.ctrlmesh.agoraMs())
                aoDesenhar()
            }
        } catch (e: IllegalStateException) {
            if (codec === c) com.ctrlmesh.registrar("decodificador de vídeo parou: ${e.message}")
        }
    }

    private fun liberar() {
        val c = codec ?: return
        codec = null
        entrada = null
        runCatching { c.stop() }
        runCatching { c.release() }
    }

    private companion object {
        const val LIMITE_DO_GOP = 300
        const val TAMANHO_DA_FILA = 8
        const val TAMANHO_MINIMO_DE_ENTRADA = 512 * 1024
        const val ESPERA_MS = 100L
        const val ESPERA_US = 10_000L
        const val ATRASO_MAXIMO_US = 250_000L
    }
}

/** Decodifica o Opus do celular e toca como mídia (RN-059, RN-068). */
class ReprodutorDeAudio(context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val atributos = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()
    private val foco = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(atributos).build()
    @Volatile private var codec: MediaCodec? = null
    @Volatile private var faixa: AudioTrack? = null
    @Volatile private var ganho = 1f
    private var config: Mensagem.AudioConfig? = null
    private var ultimaTentativaMs = 0L
    val medidor = com.ctrlmesh.Medidor()
    @Volatile private var pico = 0

    /** Pico do som decodificado na janela (0 = silêncio). */
    fun lerEZerarPico(): Int = pico.also { pico = 0 }

    /** Estado do AudioTrack para o log de desempenho. */
    val estadoDaFaixa: String
        get() = faixa?.let {
            when (it.playState) {
                AudioTrack.PLAYSTATE_PLAYING -> "tocando"
                AudioTrack.PLAYSTATE_PAUSED -> "pausado"
                else -> "parado"
            } + " ganho=$ganho" + if (codec == null) " sem_decodificador" else ""
        } ?: if (codec == null) "sem_config" else "sem_faixa"

    @Synchronized
    fun configurar(config: Mensagem.AudioConfig) {
        liberar()
        this.config = config
        ultimaTentativaMs = android.os.SystemClock.elapsedRealtime()
        val formato = MediaFormat.createAudioFormat(config.codec, config.taxa, config.canais)
        com.ctrlmesh.protocolo.CsdDoOpus.normalizar(config.csd)
            .forEachIndexed { i, dados -> formato.setByteBuffer("csd-$i", ByteBuffer.wrap(dados)) }
        codec = runCatching {
            MediaCodec.createDecoderByType(config.codec).apply { configure(formato, null, null, 0); start() }
        }.onFailure { com.ctrlmesh.registrar("audio: decodificador não abriu: ${it.message}") }.getOrNull()
        val canais = if (config.canais == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
        val minimo = AudioTrack.getMinBufferSize(config.taxa, canais, AudioFormat.ENCODING_PCM_16BIT)
        faixa = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(atributos)
                .setAudioFormat(
                    AudioFormat.Builder().setSampleRate(config.taxa).setChannelMask(canais)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT).build(),
                )
                .setBufferSizeInBytes(minimo * 8)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
                .also { it.setVolume(ganho) }
        }.onFailure { com.ctrlmesh.registrar("audio: AudioTrack não abriu: ${it.message}") }.getOrNull()
    }

    @Synchronized
    fun quadro(quadro: Mensagem.AudioQuadro) {
        medidor.contar("recebidos")
        val c = codec ?: run {
            // O decodificador pode falhar ao abrir (ex.: sistema sobrecarregado); tenta de novo a cada 2 s.
            val cfg = config ?: return
            if (android.os.SystemClock.elapsedRealtime() - ultimaTentativaMs < NOVA_TENTATIVA_MS) return
            configurar(cfg)
            return
        }
        val f = faixa ?: return
        if (f.playState != AudioTrack.PLAYSTATE_PLAYING) {
            audio.requestAudioFocus(foco)
            f.play()
        }
        runCatching {
            // Também drena o que saiu antes (o decodificador pode demorar um quadro).
            val entrada = c.dequeueInputBuffer(ESPERA_US)
            if (entrada >= 0) {
                c.getInputBuffer(entrada)?.apply { clear(); put(quadro.dados) }
                c.queueInputBuffer(entrada, 0, quadro.dados.size, quadro.ptsUs, 0)
            }
            val info = MediaCodec.BufferInfo()
            while (true) {
                val saida = c.dequeueOutputBuffer(info, 0)
                if (saida < 0) break
                val pcm = c.getOutputBuffer(saida)
                if (pcm != null && info.size > 0) {
                    medidor.contar("decodificados")
                    pcm.position(info.offset).limit(info.offset + info.size)
                    val amostras = ByteArray(info.size).also { pcm.duplicate().get(it) }
                    pico = maxOf(pico, com.ctrlmesh.celular.picoDe(amostras, amostras.size))
                    val escritos = f.write(pcm, info.size, AudioTrack.WRITE_NON_BLOCKING)
                    medidor.contar(if (escritos > 0) "tocados" else "nao_tocados")
                }
                c.releaseOutputBuffer(saida, false)
            }
        }
    }

    /** Sem som durante "reconectando"/fora do espelhamento: o que ficou no buffer não toca depois (RN-066). */
    @Synchronized
    fun pausar() {
        faixa?.let { runCatching { it.pause(); it.flush() } }
    }

    @Synchronized
    fun definirGanho(novo: Float) {
        ganho = novo
        faixa?.setVolume(novo)
    }

    @Synchronized
    fun esquecer() {
        liberar()
        config = null
    }

    @Synchronized
    fun liberar() {
        codec?.let { runCatching { it.stop() }; runCatching { it.release() } }
        faixa?.let { runCatching { it.stop() }; runCatching { it.release() } }
        codec = null
        faixa = null
    }

    private companion object {
        const val ESPERA_US = 10_000L
        const val NOVA_TENTATIVA_MS = 2_000L
    }
}
