package com.ctrlmesh

import android.os.SystemClock
import com.ctrlmesh.protocolo.Aperto
import com.ctrlmesh.protocolo.CanalSeguro
import com.ctrlmesh.protocolo.DetectorDeQueda
import com.ctrlmesh.protocolo.FilaDeEnvio
import com.ctrlmesh.protocolo.Mensagem
import com.ctrlmesh.protocolo.Papel
import com.ctrlmesh.protocolo.Protocolo
import com.ctrlmesh.protocolo.ProtocoloException
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.net.Socket
import java.net.SocketException
import kotlin.concurrent.thread

/** Como a conexão terminou (contrato §2, CA-091). */
enum class Fim { QUEDA, FECHADA_PELO_OUTRO, LOCAL }

/**
 * Uma conexão TCP já com aperto e cifra (contrato §3): leitura, fila de envio, batimento e detecção de queda.
 */
class Conexao private constructor(
    private val socket: Socket,
    private val canal: CanalSeguro,
    private val papel: Papel,
    limiteDaFila: Int,
) {
    private val entrada = DataInputStream(BufferedInputStream(socket.getInputStream()))
    private val saida = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
    private val fila = FilaDeEnvio(limiteDaFila)
    private val detector = DetectorDeQueda(SystemClock.elapsedRealtime())
    private val travaDeEscrita = Any()
    @Volatile private var fim: Fim? = null
    private var aoTerminar: (Fim) -> Unit = {}

    val endereco: String = socket.inetAddress.hostAddress ?: ""
    val aberta: Boolean get() = fim == null

    /** Ida e volta do último Ping (ms), -1 sem medida. */
    @Volatile var rttMs = -1L
        private set
    @Volatile private var ultimoPing = -1
    @Volatile private var ultimoPingMs = 0L

    /** Bytes na fila de envio e vídeos descartados pela fila (medição). */
    val bytesNaFila: Int get() = fila.bytesNaFila
    val descartesDeVideo: Int get() = fila.descartesDeVideo

    /** Chamado quando a fila descarta vídeo (o celular pede um quadro-chave ao codificador). */
    fun aoDescartarVideo(acao: () -> Unit) {
        fila.aoDescartarVideo = acao
    }

    /** Lê uma mensagem direto, antes de [iniciar] (ex.: o `Ola`). */
    fun receber(): Mensagem {
        val tamanho = entrada.readInt()
        if (tamanho < 16 || tamanho > CanalSeguro.TAMANHO_MAXIMO) throw ProtocoloException("quadro com tamanho $tamanho")
        // Lido em pedaços, renovando o prazo da queda: uma mensagem grande (ex.: ListaDeApps com ícones) numa rede
        // lenta pode levar mais de 3 s e não é queda.
        val blob = ByteArray(tamanho)
        var lidos = 0
        while (lidos < tamanho) {
            val n = entrada.read(blob, lidos, minOf(PEDACO_DE_LEITURA, tamanho - lidos))
            if (n < 0) throw EOFException()
            lidos += n
            detector.recebeu(SystemClock.elapsedRealtime())
        }
        return Protocolo.decodificar(canal.decifrar(blob), papel)
    }

    fun iniciar(aoReceber: (Mensagem) -> Unit, aoTerminar: (Fim) -> Unit) {
        this.aoTerminar = aoTerminar
        socket.soTimeout = 0
        detector.recebeu(SystemClock.elapsedRealtime())
        thread(isDaemon = true, name = "ctrlmesh-leitura") {
            try {
                while (fim == null) {
                    when (val m = receber()) {
                        is Mensagem.Ping -> enviar(Mensagem.Pong(m.seq))
                        is Mensagem.Pong -> if (m.seq == ultimoPing) rttMs = android.os.SystemClock.elapsedRealtime() - ultimoPingMs
                        else -> aoReceber(m)
                    }
                }
            } catch (e: Exception) {
                terminar(classificar(e))
            }
        }
        thread(isDaemon = true, name = "ctrlmesh-envio") {
            try {
                while (fim == null) {
                    val m = fila.esperarProxima(ESPERA_MS) ?: continue
                    escrever(m)
                }
            } catch (_: Exception) {
                terminar(Fim.QUEDA)
            }
        }
        thread(isDaemon = true, name = "ctrlmesh-batimento") {
            var seq = 0
            var proximoPing = 0L
            while (fim == null) {
                val agora = SystemClock.elapsedRealtime()
                if (detector.caiu(agora)) terminar(Fim.QUEDA)
                if (agora >= proximoPing) {
                    ultimoPing = seq
                    ultimoPingMs = agora
                    enviar(Mensagem.Ping(seq++))
                    proximoPing = agora + BATIMENTO_MS
                }
                Thread.sleep(VERIFICACAO_MS)
            }
        }
    }

    fun enviar(mensagem: Mensagem) {
        if (fim == null) fila.ofertar(mensagem)
    }

    /** Envia `Encerrar` na hora (sem passar pela fila) e fecha; fora da thread principal (pode vir de um toque). */
    fun encerrar(mensagem: Mensagem.Encerrar) {
        if (fim != null) return
        thread(isDaemon = true, name = "ctrlmesh-encerrar") {
            runCatching { escrever(mensagem) }
            terminar(Fim.LOCAL)
        }
    }

    fun fechar() = terminar(Fim.LOCAL)

    private fun escrever(m: Mensagem) = synchronized(travaDeEscrita) {
        val blob = canal.cifrar(Protocolo.codificar(m))
        saida.writeInt(blob.size)
        saida.write(blob)
        saida.flush()
    }

    private fun terminar(motivo: Fim) {
        synchronized(this) {
            if (fim != null) return
            fim = motivo
        }
        runCatching { socket.close() }
        fila.limpar()
        aoTerminar(motivo)
    }

    private fun classificar(e: Exception): Fim = when {
        e is EOFException -> Fim.FECHADA_PELO_OUTRO
        e is SocketException && e.message.orEmpty().contains("reset", ignoreCase = true) -> Fim.FECHADA_PELO_OUTRO
        else -> Fim.QUEDA
    }

    companion object {
        private const val FILA_DA_CENTRAL = 256 * 1024
        // Celular: fila curta (~0,5 s de vídeo a 4 Mbps) e buffer do socket pequeno, para o atraso não se esconder neles.
        private const val FILA_DO_CELULAR = 256 * 1024
        private const val BUFFER_DE_ENVIO_DO_CELULAR = 64 * 1024
        private const val ESPERA_MS = 500L
        private const val BATIMENTO_MS = 1_000L
        private const val VERIFICACAO_MS = 250L
        private const val TEMPO_DO_APERTO_MS = 5_000
        private const val PEDACO_DE_LEITURA = 16 * 1024

        /** Central: envia o aperto primeiro. Lança ProtocoloException para aperto/versão inválidos. */
        fun comoCentral(socket: Socket): Conexao = apertar(socket, Papel.MULTIMIDIA, centralPrimeiro = true)

        /** Celular: lê o aperto da central e responde. */
        fun comoCelular(socket: Socket): Conexao {
            socket.sendBufferSize = BUFFER_DE_ENVIO_DO_CELULAR
            return apertar(socket, Papel.CELULAR, centralPrimeiro = false)
        }

        private fun apertar(socket: Socket, papel: Papel, centralPrimeiro: Boolean): Conexao {
            socket.soTimeout = TEMPO_DO_APERTO_MS
            socket.tcpNoDelay = true
            val par = CanalSeguro.gerarParDeChaves()
            val meu = Aperto.codificar(CanalSeguro.publicaX962(par))
            val entrada = DataInputStream(socket.getInputStream())
            val saida = socket.getOutputStream()
            if (centralPrimeiro) saida.apply { write(meu); flush() }
            val doOutro = ByteArray(Aperto.TAMANHO).also { entrada.readFully(it) }
            val publica = Aperto.decodificar(doOutro)
            if (!centralPrimeiro) saida.apply { write(meu); flush() }
            val limite = if (papel == Papel.CELULAR) FILA_DO_CELULAR else FILA_DA_CENTRAL
            return Conexao(socket, CanalSeguro.criar(papel, par, publica), papel, limite)
        }
    }
}
