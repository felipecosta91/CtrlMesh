package com.ctrlmesh

import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.platform.app.InstrumentationRegistry
import com.ctrlmesh.protocolo.Aperto
import com.ctrlmesh.protocolo.CanalSeguro
import com.ctrlmesh.protocolo.EstadoRemoto
import com.ctrlmesh.protocolo.Mensagem
import com.ctrlmesh.protocolo.Papel
import com.ctrlmesh.protocolo.Protocolo
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

val contexto: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

fun shell(comando: String): String {
    val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(comando)
    return android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes().toString(Charsets.UTF_8) }
}

/** Estado limpo: serviço parado, configuração apagada e papel definido. */
fun prepararApp(papel: Papel?, enderecoDoCelular: String? = null) {
    contexto.stopService(Intent(contexto, ServicoDeEscuta::class.java))
    val config = ConfiguracaoDoApp(contexto)
    config.redefinir()
    config.papel = papel
    config.enderecoDoCelular = enderecoDoCelular
    shell("pm grant ${contexto.packageName} android.permission.POST_NOTIFICATIONS")
}

/**
 * Os testes apagam a configuração do app (prepararApp). Salve antes da classe e restaure depois, para o
 * emulador continuar configurado (ex.: multimídia com o endereço real do celular) e em user_rotation original.
 */
object EstadoDoAparelho {
    private var papel: Papel? = null
    private var endereco: String? = null
    private var rotacao: String = "0"

    fun salvar() {
        val config = ConfiguracaoDoApp(contexto)
        papel = config.papel
        endereco = config.enderecoDoCelular
        rotacao = shell("settings get system user_rotation").trim().takeIf { it.toIntOrNull() != null } ?: "0"
    }

    fun restaurar() {
        contexto.stopService(Intent(contexto, ServicoDeEscuta::class.java))
        val config = ConfiguracaoDoApp(contexto)
        config.redefinir()
        config.papel = papel
        config.enderecoDoCelular = endereco
        shell("settings put system user_rotation $rotacao")
        if (papel != null) shell("am start -n ${contexto.packageName}/.MainActivity")
    }
}

fun ComposeTestRule.textoDaTag(tag: String): String? {
    val nos = onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes()
    return nos.firstOrNull()?.config?.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text }
}

fun ComposeTestRule.esperarTexto(tag: String, texto: String, timeoutMs: Long = 10_000) {
    waitUntil(timeoutMs) { textoDaTag(tag) == texto }
}

/** Gera um quadro H.264 real (csd + quadro-chave) no próprio aparelho, para a central decodificar. */
object VideoDeTeste {
    class Video(val config: Mensagem.VideoConfig, val quadroChave: Mensagem.VideoQuadro)

    fun gerar(largura: Int, altura: Int): Video {
        val formato = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, largura, altura).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, 2_000_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        val codificador = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        codificador.configure(formato, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val superficie = codificador.createInputSurface()
        codificador.start()
        val csd = mutableListOf<ByteArray>()
        var chave: Mensagem.VideoQuadro? = null
        val info = MediaCodec.BufferInfo()
        val limite = System.currentTimeMillis() + 10_000
        var quadro = 0
        while (chave == null && System.currentTimeMillis() < limite) {
            val canvas = superficie.lockHardwareCanvas()
            canvas.drawColor(if (quadro++ % 2 == 0) Color.BLUE else Color.GREEN)
            superficie.unlockCanvasAndPost(canvas)
            val indice = codificador.dequeueOutputBuffer(info, 50_000)
            if (indice < 0) continue
            val buffer = codificador.getOutputBuffer(indice)!!
            val dados = ByteArray(info.size).also { buffer.position(info.offset); buffer.get(it) }
            when {
                info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0 -> csd += dados
                info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0 -> chave = Mensagem.VideoQuadro(info.presentationTimeUs, true, dados)
            }
            codificador.releaseOutputBuffer(indice, false)
        }
        codificador.stop()
        codificador.release()
        superficie.release()
        return Video(Mensagem.VideoConfig(largura, altura, "video/avc", csd), checkNotNull(chave) { "codificador não gerou quadro-chave" })
    }
}

/**
 * Celular falso para testar a central sozinha: escuta em 127.0.0.1:47800, faz o aperto e a cifra do contrato
 * e responde como o celular. A central de teste usa `enderecoDoCelular = "127.0.0.1"`.
 */
class CelularFalso(
    var estadoInicial: EstadoRemoto = EstadoRemoto.ESPELHANDO,
    var avisos: Set<com.ctrlmesh.protocolo.Aviso> = emptySet(),
    var video: VideoDeTeste.Video? = null,
    var modo: com.ctrlmesh.protocolo.ModoEspelhamento? = null,
    var apps: List<com.ctrlmesh.protocolo.AppDoCelular>? = null,
) : AutoCloseable {

    val recebidas = LinkedBlockingQueue<Mensagem>()
    val conexoes = AtomicInteger(0)
    @Volatile var aceitando = true
    @Volatile private var silenciado = false
    @Volatile private var atual: Conexao? = null
    private val servidor = ServerSocket().apply {
        reuseAddress = true
        bind(InetSocketAddress(InetAddress.getByName("127.0.0.1"), Protocolo.PORTA))
    }

    init {
        thread(isDaemon = true, name = "celular-falso") {
            while (!servidor.isClosed) {
                val socket = runCatching { servidor.accept() }.getOrNull() ?: break
                if (!aceitando) {
                    socket.close()
                    continue
                }
                runCatching { atual?.socket?.close() }
                val conexao = runCatching { Conexao(socket) }.getOrNull() ?: continue
                atual = conexao
                silenciado = false
                conexao.iniciar()
                // Socket fechado pelo teste (close/fecharAbrupto) durante a abertura: ignora, sem derrubar o processo.
                try {
                    enviarAbertura()
                } catch (_: java.io.IOException) {
                    continue
                }
                conexoes.incrementAndGet()
            }
        }
    }

    private fun enviarAbertura() {
        enviar(Mensagem.Situacao(avisos))
        modo?.let { enviar(Mensagem.Modo(it)) }
        enviar(Mensagem.Estado(estadoInicial))
        apps?.let { enviar(Mensagem.ListaDeApps(it)) }
        // No modo tela virtual, o vídeo do app só vem depois de AbrirApp (o teste chama enviarVideo()).
        if (estadoInicial == EstadoRemoto.ESPELHANDO && modo != com.ctrlmesh.protocolo.ModoEspelhamento.TELA_VIRTUAL) enviarVideo()
    }

    fun enviarVideo() {
        val v = video ?: return
        enviar(v.config)
        enviar(v.quadroChave)
    }

    fun enviar(mensagem: Mensagem) {
        atual?.enviar(mensagem)
    }

    /** Fecha o socket sem `Encerrar`, como o processo do app morto. */
    fun fecharAbrupto() {
        aceitando = false
        runCatching { atual?.socket?.close() }
        atual = null
    }

    /** Para de responder sem fechar o socket, como a rede caindo; novas conexões são recusadas. */
    fun silenciar() {
        aceitando = false
        silenciado = true
    }

    /** Volta a aceitar conexões, como a rede voltando. */
    fun voltar() {
        aceitando = true
    }

    fun esperar(timeoutMs: Long = 5_000, filtro: (Mensagem) -> Boolean): Mensagem? {
        val limite = System.currentTimeMillis() + timeoutMs
        while (true) {
            val resta = limite - System.currentTimeMillis()
            if (resta <= 0) return null
            val m = recebidas.poll(resta, TimeUnit.MILLISECONDS) ?: return null
            if (filtro(m)) return m
        }
    }

    fun toquesRecebidos(duranteMs: Long): List<Mensagem.Toque> {
        Thread.sleep(duranteMs)
        val todas = mutableListOf<Mensagem>()
        recebidas.drainTo(todas)
        return todas.filterIsInstance<Mensagem.Toque>()
    }

    override fun close() {
        runCatching { atual?.socket?.close() }
        runCatching { servidor.close() }
    }

    private inner class Conexao(val socket: Socket) {
        private val entrada = DataInputStream(socket.getInputStream())
        private val saida = DataOutputStream(socket.getOutputStream())
        private val canal: CanalSeguro

        init {
            socket.soTimeout = 0
            val apertoDaCentral = ByteArray(Aperto.TAMANHO).also { entrada.readFully(it) }
            val publicaDaCentral = Aperto.decodificar(apertoDaCentral)
            val par = CanalSeguro.gerarParDeChaves()
            saida.write(Aperto.codificar(CanalSeguro.publicaX962(par)))
            saida.flush()
            canal = CanalSeguro.criar(Papel.CELULAR, par, publicaDaCentral)
        }

        fun iniciar() {
            thread(isDaemon = true, name = "celular-falso-leitura") {
                runCatching {
                    while (!socket.isClosed) {
                        val tamanho = entrada.readInt()
                        val blob = ByteArray(tamanho).also { entrada.readFully(it) }
                        val mensagem = Protocolo.decodificar(canal.decifrar(blob), Papel.CELULAR)
                        if (silenciado) continue
                        when (mensagem) {
                            is Mensagem.Ping -> enviar(Mensagem.Pong(mensagem.seq))
                            is Mensagem.Pong -> Unit
                            else -> recebidas.put(mensagem)
                        }
                    }
                }
            }
            thread(isDaemon = true, name = "celular-falso-batimento") {
                var seq = 0
                while (!socket.isClosed) {
                    if (!silenciado) runCatching { enviar(Mensagem.Ping(seq++)) }
                    Thread.sleep(1_000)
                }
            }
        }

        @Synchronized
        fun enviar(mensagem: Mensagem) {
            val blob = canal.cifrar(Protocolo.codificar(mensagem))
            saida.writeInt(blob.size)
            saida.write(blob)
            saida.flush()
        }
    }
}
