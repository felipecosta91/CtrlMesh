package com.ctrlmesh.celular

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Display
import androidx.annotation.RequiresApi
import com.ctrlmesh.protocolo.Mensagem
import com.ctrlmesh.protocolo.CsdDoOpus
import com.ctrlmesh.protocolo.PoliticaDeBanda
import com.ctrlmesh.protocolo.TamanhoDoVideo
import java.nio.ByteBuffer
import kotlin.concurrent.thread

/**
 * Captura de tela (H.264) e de som (Opus) do celular sobre uma MediaProjection.
 * Uma VirtualDisplay por projeção (Android 14+ não deixa criar outra); o codificador é trocado por baixo dela.
 */
@RequiresApi(Build.VERSION_CODES.Q)
class Captura(private val context: Context, private val projecao: MediaProjection, aoParar: () -> Unit) {
    private val telas = context.getSystemService(DisplayManager::class.java)
    private val principal = Handler(Looper.getMainLooper())
    private var virtual: VirtualDisplay? = null
    private var area = 0 to 0
    private var saidaDeVideo: ((Mensagem) -> Unit)? = null
    private var tamanhoCodificado = 0 to 0
    private var audio: CapturaDeAudio? = null
    val medidor = com.ctrlmesh.Medidor()
    val medidorDeAudio = com.ctrlmesh.Medidor()
    private val codificador = CodificadorDeVideo(medidor)

    /** Pico do sinal capturado na janela atual (0..32767), zerado a cada leitura. */
    fun picoDeAudio(): Int = audio?.lerEZerarPico() ?: -1

    private val ouvinteDeTela = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) {
            if (displayId == Display.DEFAULT_DISPLAY) aoGirar()
        }
    }

    init {
        projecao.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                TelaDoCelular.conteudoCapturado = null
                aoParar()
            }

            // Android 14+: tamanho real do que está sendo capturado (gira junto com o app da frente).
            override fun onCapturedContentResize(width: Int, height: Int) {
                TelaDoCelular.conteudoCapturado = width to height
                aoGirar()
            }
        }, principal)
        val (largura, altura) = tamanhoDaTela()
        virtual = projecao.createVirtualDisplay(
            "ctrlmesh", largura, altura, context.resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, null, null, principal,
        )
        telas.registerDisplayListener(ouvinteDeTela, principal)
    }

    @Synchronized
    fun transmitirVideo(larguraArea: Int, alturaArea: Int, saida: (Mensagem) -> Unit) {
        pararVideo()
        area = larguraArea to alturaArea
        saidaDeVideo = saida
        iniciarCodificador()
    }

    @Synchronized
    fun pararVideo() {
        saidaDeVideo = null
        virtual?.surface = null
        codificador.parar()
    }

    @SuppressLint("MissingPermission")
    @Synchronized
    fun transmitirAudio(saida: (Mensagem) -> Unit, volume: ControleDeVolumeDoTeste) {
        pararAudio()
        audio = CapturaDeAudio(context, FonteDePcm.daProjecao(projecao), saida, volume, medidorDeAudio)
    }

    @Synchronized
    fun pararAudio() {
        audio?.parar()
        audio = null
    }

    @Synchronized
    fun liberar() {
        telas.unregisterDisplayListener(ouvinteDeTela)
        TelaDoCelular.conteudoCapturado = null
        pararVideo()
        pararAudio()
        virtual?.release()
        virtual = null
        runCatching { projecao.stop() }
    }

    @Synchronized
    private fun aoGirar() {
        val saida = saidaDeVideo ?: return
        if (dimensoes(tamanhoDaTela()) == tamanhoCodificado) return
        transmitirVideo(area.first, area.second, saida) // RN-046: novo VideoConfig + quadro-chave
    }

    /** RNF-016 (contrato §4). */
    private fun dimensoes(tela: Pair<Int, Int>) = TamanhoDoVideo.calcular(tela.first, tela.second, area.first, area.second)

    private fun iniciarCodificador() {
        val saida = saidaDeVideo ?: return
        val (largura, altura) = dimensoes(tamanhoDaTela())
        tamanhoCodificado = largura to altura
        val superficie = codificador.iniciar(largura, altura, saida)
        virtual?.resize(largura, altura, context.resources.displayMetrics.densityDpi)
        virtual?.surface = superficie
    }

    /** Quadro-chave já (ex.: a fila de envio descartou vídeo). */
    fun pedirQuadroChave() = codificador.pedirQuadroChave()

    private fun tamanhoDaTela() = TelaDoCelular.tamanho(context)

}

/** H.264 de baixa latência com superfície de entrada; manda `VideoConfig` + quadros pela [saida]. */
class CodificadorDeVideo(private val medidor: com.ctrlmesh.Medidor) {
    @Volatile private var codificador: MediaCodec? = null

    @Synchronized
    fun iniciar(largura: Int, altura: Int, saida: (Mensagem) -> Unit): android.view.Surface {
        parar()
        val novo = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        // Baixa latência (RNF-001): tempo real, sem quadros B, baseline; se o codificador recusar o perfil, vai sem ele.
        runCatching { novo.configure(formato(largura, altura, baseline = true), null, null, MediaCodec.CONFIGURE_FLAG_ENCODE) }
            .onFailure { novo.configure(formato(largura, altura, baseline = false), null, null, MediaCodec.CONFIGURE_FLAG_ENCODE) }
        val superficie = novo.createInputSurface()
        novo.start()
        codificador = novo
        thread(isDaemon = true, name = "ctrlmesh-codificador") { drenar(novo, largura, altura, saida) }
        return superficie
    }

    @Synchronized
    fun parar() {
        val c = codificador ?: return
        codificador = null
        runCatching { c.stop() }
        runCatching { c.release() }
    }

    fun pedirQuadroChave() {
        runCatching { codificador?.setParameters(Bundle().apply { putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0) }) }
    }

    private fun formato(largura: Int, altura: Int, baseline: Boolean) =
        MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, largura, altura).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, PoliticaDeBanda.alocar(BANDA_KBPS).videoKbps * 1000)
            // VBR: tela parada gasta pouco; o limite de 30 qps evita mandar quadros que a central não consegue desenhar.
            setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)
            setInteger(MediaFormat.KEY_FRAME_RATE, QUADROS_POR_SEGUNDO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) setFloat(MediaFormat.KEY_MAX_FPS_TO_ENCODER, QUADROS_POR_SEGUNDO.toFloat())
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, INTERVALO_DE_QUADRO_CHAVE_S)
            setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 100_000L)
            setInteger(MediaFormat.KEY_PRIORITY, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) setInteger(MediaFormat.KEY_MAX_B_FRAMES, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) setInteger(MediaFormat.KEY_LATENCY, 1)
            if (baseline) setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline)
        }

    private fun drenar(c: MediaCodec, largura: Int, altura: Int, saida: (Mensagem) -> Unit) {
        val info = MediaCodec.BufferInfo()
        try {
            while (codificador === c) {
                val indice = c.dequeueOutputBuffer(info, 100_000)
                if (indice == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    saida(Mensagem.VideoConfig(largura, altura, MediaFormat.MIMETYPE_VIDEO_AVC, Codecs.csd(c.outputFormat)))
                    com.ctrlmesh.registrar("video=${largura}x$altura")
                }
                if (indice < 0) continue
                if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && info.size > 0) {
                    val dados = Codecs.copiar(c.getOutputBuffer(indice), info)
                    saida(Mensagem.VideoQuadro(info.presentationTimeUs, info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0, dados))
                    medidor.contar("codificados")
                }
                c.releaseOutputBuffer(indice, false)
            }
        } catch (_: IllegalStateException) {
            // codificador liberado
        }
    }

    private companion object {
        const val QUADROS_POR_SEGUNDO = 30
        // Menos bitrate e quadro-chave mais espaçado (o dono aceitou menos nitidez por menos atraso).
        const val BANDA_KBPS = 4_000
        const val INTERVALO_DE_QUADRO_CHAVE_S = 2
    }
}

/**
 * Tamanho e orientação reais da tela do celular. `getRealMetrics` num serviço pode vir na orientação da última
 * atividade do app (compatibilidade do Android 15+), então a orientação vem do conteúdo capturado (Android 14+)
 * ou da rotação do display; o tamanho vem das métricas.
 */
object TelaDoCelular {
    @Volatile var conteudoCapturado: Pair<Int, Int>? = null

    fun tamanho(context: Context): Pair<Int, Int> {
        val display = context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
        val metricas = DisplayMetrics()
        @Suppress("DEPRECATION")
        display.getRealMetrics(metricas)
        val maior = maxOf(metricas.widthPixels, metricas.heightPixels)
        val menor = minOf(metricas.widthPixels, metricas.heightPixels)
        val emPaisagem = conteudoCapturado?.let { it.first > it.second } ?: run {
            val naturalEmRetrato = display.mode.physicalHeight >= display.mode.physicalWidth
            val girada = display.rotation == android.view.Surface.ROTATION_90 || display.rotation == android.view.Surface.ROTATION_270
            naturalEmRetrato == girada
        }
        return if (emPaisagem) maior to menor else menor to maior
    }
}

/**
 * Teste do RN-090 feito pela captura: o controlador silencia/devolve o volume de mídia e diz se está silenciado.
 * Conclusão "silenciar corta o som" só quando, com música tocando, o sinal é zero com o volume em 0 **e** volta a ter
 * sinal com o volume devolvido. Vale só para a sessão atual (nada é memorizado).
 */
interface ControleDeVolumeDoTeste {
    fun silenciado(): Boolean
    fun devolverVolume()
    fun silenciarDeNovo()
    fun silenciarCortaOSom()
}

/** De onde vem o PCM 48 kHz estéreo 16 bits: captura da projeção ou o pipe do ajudante (tela virtual). */
interface FonteDePcm {
    fun iniciar(): Boolean
    fun ler(destino: ByteArray): Int
    fun fechar()

    companion object {
        @RequiresApi(Build.VERSION_CODES.Q)
        @SuppressLint("MissingPermission")
        fun daProjecao(projecao: MediaProjection): FonteDePcm = object : FonteDePcm {
            private var gravador: AudioRecord? = null

            override fun iniciar(): Boolean {
                val captura = AudioPlaybackCaptureConfiguration.Builder(projecao)
                    .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                    .addMatchingUsage(AudioAttributes.USAGE_GAME)
                    .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                    .build()
                val minimo = AudioRecord.getMinBufferSize(TAXA_PCM, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT)
                gravador = runCatching {
                    AudioRecord.Builder()
                        .setAudioFormat(
                            AudioFormat.Builder().setSampleRate(TAXA_PCM).setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT).build(),
                        )
                        .setBufferSizeInBytes(maxOf(minimo, QUADRO_PCM * 8))
                        .setAudioPlaybackCaptureConfig(captura)
                        .build()
                }.onFailure { com.ctrlmesh.registrar("audio: captura não abriu: ${it.message}") }.getOrNull()
                val g = gravador ?: return false
                g.startRecording()
                if (g.recordingState != AudioRecord.RECORDSTATE_RECORDING) com.ctrlmesh.registrar("audio: gravação não começou")
                return true
            }

            override fun ler(destino: ByteArray) = gravador?.read(destino, 0, destino.size) ?: -1

            override fun fechar() {
                gravador?.let { runCatching { it.stop() }; it.release() }
                gravador = null
            }
        }

        fun doPipe(entrada: java.io.InputStream, aoFechar: () -> Unit): FonteDePcm = object : FonteDePcm {
            override fun iniciar() = true
            override fun ler(destino: ByteArray): Int {
                var lidos = 0
                while (lidos < destino.size) {
                    val n = entrada.read(destino, lidos, destino.size - lidos)
                    if (n < 0) return if (lidos > 0) lidos else -1
                    lidos += n
                }
                return lidos
            }
            override fun fechar() {
                runCatching { entrada.close() }
                aoFechar()
            }
        }

        const val TAXA_PCM = 48_000
        const val QUADRO_PCM = TAXA_PCM / 50 * 4
    }
}

/** Som do celular codificado em Opus 48 kHz estéreo, quadros de 20 ms (RNF-014). [volume] nulo = sem teste do RN-090. */
@SuppressLint("MissingPermission")
class CapturaDeAudio(
    context: Context,
    private val fonte: FonteDePcm,
    private val saida: (Mensagem) -> Unit,
    private val volume: ControleDeVolumeDoTeste?,
    private val medidor: com.ctrlmesh.Medidor,
) {
    @Volatile private var rodando = true
    @Volatile private var pico = 0
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val codificador: MediaCodec?

    // Teste do RN-090 (ver ControleDeVolumeDoTeste).
    private var zeradoSilenciadoMs = 0
    private var testandoMs = -1
    private var sinalNoTeste = false
    private var esperaAteNovoTesteMs = 0

    init {
        codificador = runCatching {
            MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_OPUS).apply {
                configure(
                    MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS, TAXA, CANAIS).apply {
                        setInteger(MediaFormat.KEY_BIT_RATE, PoliticaDeBanda.AUDIO_KBPS * 1000)
                    },
                    null, null, MediaCodec.CONFIGURE_FLAG_ENCODE,
                )
                start()
            }
        }.onFailure { com.ctrlmesh.registrar("audio: codificador Opus não abriu: ${it.message}") }.getOrNull()
        if (codificador != null) thread(isDaemon = true, name = "ctrlmesh-audio") { laco(codificador) } else fonte.fechar()
    }

    fun lerEZerarPico(): Int = pico.also { pico = 0 }

    private fun laco(c: MediaCodec) {
        val pcm = ByteArray(QUADRO_BYTES)
        val info = MediaCodec.BufferInfo()
        var amostras = 0L
        var configEnviada = false
        var csdDoBloco: ByteArray? = null
        try {
            if (!fonte.iniciar()) return
            while (rodando) {
                val lidos = fonte.ler(pcm)
                if (lidos < 0) break
                if (lidos == 0) {
                    medidor.contar("erros_de_leitura")
                    continue
                }
                medidor.contar("capturados")
                val picoDoQuadro = picoPcm16(pcm, lidos)
                if (picoDoQuadro > pico) pico = picoDoQuadro
                testarSilenciamento(picoDoQuadro)
                val entrada = c.dequeueInputBuffer(10_000)
                if (entrada >= 0) {
                    c.getInputBuffer(entrada)?.apply { clear(); put(pcm, 0, lidos) }
                    c.queueInputBuffer(entrada, 0, lidos, amostras * 1_000_000L / TAXA, 0)
                    amostras += lidos / (2 * CANAIS)
                }
                while (true) {
                    val indice = c.dequeueOutputBuffer(info, 0)
                    if (indice == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) continue
                    if (indice < 0) break
                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        csdDoBloco = Codecs.copiar(c.getOutputBuffer(indice), info)
                    } else if (info.size > 0) {
                        if (!configEnviada) {
                            // csd pode vir no formato de saída ou num bloco de configuração (C2: com marcadores).
                            val doFormato = Codecs.csd(c.outputFormat)
                            val csd = CsdDoOpus.normalizar(doFormato.ifEmpty { listOfNotNull(csdDoBloco) })
                            saida(Mensagem.AudioConfig(TAXA, CANAIS, MediaFormat.MIMETYPE_AUDIO_OPUS, csd))
                            com.ctrlmesh.registrar("audio: config csd=${csd.size} (${csd.joinToString { it.size.toString() }} bytes)")
                            configEnviada = true
                        }
                        saida(Mensagem.AudioQuadro(info.presentationTimeUs, Codecs.copiar(c.getOutputBuffer(indice), info)))
                        medidor.contar("enviados")
                    }
                    c.releaseOutputBuffer(indice, false)
                }
            }
        } catch (e: IllegalStateException) {
            if (rodando) com.ctrlmesh.registrar("audio: parou: ${e.message}")
        } finally {
            fonte.fechar()
            runCatching { c.stop() }
            c.release()
        }
    }

    /** RN-090, por sessão: zero com volume 0 e música tocando → devolve o volume por um instante e vê se o sinal volta. */
    private fun testarSilenciamento(picoDoQuadro: Int) {
        val volume = volume ?: return
        if (esperaAteNovoTesteMs > 0) {
            esperaAteNovoTesteMs -= DURACAO_MS
            return
        }
        if (testandoMs >= 0) {
            testandoMs += DURACAO_MS
            if (picoDoQuadro > LIMIAR_DE_SINAL) sinalNoTeste = true
            if (testandoMs < DURACAO_DO_TESTE_MS) return
            testandoMs = -1
            if (sinalNoTeste) {
                com.ctrlmesh.registrar("audio: silenciar a mídia zera a captura neste celular (RN-090)")
                volume.silenciarCortaOSom()
                esperaAteNovoTesteMs = Int.MAX_VALUE
            } else {
                com.ctrlmesh.registrar("audio: sem sinal mesmo com o volume devolvido; o app tocando pode bloquear a captura")
                volume.silenciarDeNovo()
                esperaAteNovoTesteMs = ESPERA_ENTRE_TESTES_MS
            }
            return
        }
        val zerado = picoDoQuadro <= LIMIAR_DE_SINAL
        zeradoSilenciadoMs = if (zerado && volume.silenciado() && audioManager.isMusicActive) zeradoSilenciadoMs + DURACAO_MS else 0
        if (zeradoSilenciadoMs >= ZERADO_ANTES_DO_TESTE_MS) {
            zeradoSilenciadoMs = 0
            testandoMs = 0
            sinalNoTeste = false
            volume.devolverVolume()
        }
    }

    fun parar() {
        rodando = false
    }

    private companion object {
        const val TAXA = 48_000
        const val CANAIS = 2
        const val DURACAO_MS = 20
        const val QUADRO_BYTES = TAXA / 1000 * DURACAO_MS * CANAIS * 2
        const val LIMIAR_DE_SINAL = 8
        const val ZERADO_ANTES_DO_TESTE_MS = 1_000
        const val DURACAO_DO_TESTE_MS = 1_500
        const val ESPERA_ENTRE_TESTES_MS = 30_000

        fun picoPcm16(pcm: ByteArray, tamanho: Int): Int = picoDe(pcm, tamanho)
    }
}

/** Maior amplitude (0..32767) de PCM 16 bits little-endian: diagnóstico de som "zerado" (log de métricas). */
fun picoDe(pcm: ByteArray, tamanho: Int): Int {
    var maior = 0
    var i = 0
    while (i + 1 < tamanho) {
        val amostra = ((pcm[i + 1].toInt() shl 8) or (pcm[i].toInt() and 0xFF)).toShort().toInt()
        val absoluto = if (amostra < 0) -amostra else amostra
        if (absoluto > maior) maior = absoluto
        i += 2
    }
    return maior
}

/** Utilidades de MediaCodec compartilhadas pela captura física, pela tela virtual e pelo som. */
object Codecs {
    fun csd(formato: MediaFormat): List<ByteArray> =
        (0..2).mapNotNull { i -> formato.getByteBuffer("csd-$i")?.let { b -> ByteArray(b.remaining()).also { b.get(it) } } }

    fun copiar(buffer: ByteBuffer?, info: MediaCodec.BufferInfo): ByteArray {
        val b = buffer ?: return ByteArray(0)
        b.position(info.offset).limit(info.offset + info.size)
        return ByteArray(info.size).also { b.get(it) }
    }
}
