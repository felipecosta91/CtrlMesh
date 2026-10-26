package com.ctrlmesh.central

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import com.ctrlmesh.Conexao
import com.ctrlmesh.ConfiguracaoDoApp
import com.ctrlmesh.Fim
import com.ctrlmesh.MainActivity
import com.ctrlmesh.Notificacoes
import com.ctrlmesh.protocolo.AppDoCelular
import com.ctrlmesh.protocolo.Aviso
import com.ctrlmesh.protocolo.ModoEspelhamento
import com.ctrlmesh.protocolo.TeclaDeSistema
import com.ctrlmesh.protocolo.Mensagem
import com.ctrlmesh.protocolo.MotivoEncerramento
import com.ctrlmesh.protocolo.Protocolo
import com.ctrlmesh.protocolo.ProtocoloException
import com.ctrlmesh.registrar
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.concurrent.thread
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class TelaCentralEstado(
    val estado: EstadoCentral = SessaoCentral.inicial,
    val avisos: Set<Aviso> = emptySet(),
    val video: Pair<Int, Int>? = null,
    val mudo: Boolean = false,
    /** Modo do espelhamento (§10); sem `Modo` recebido, tela do celular. */
    val modo: ModoEspelhamento = ModoEspelhamento.TELA_FISICA,
    val apps: List<AppDoCelular> = emptyList(),
    /** Tela virtual: mostrar o início do CtrlMesh em vez da imagem (RN-095). */
    val mostrarInicio: Boolean = true,
)

/** Papel multimídia: procura o celular, mantém a sessão (contrato §5.1) e toca imagem e som. */
class ControladorCentral(private val context: Context) {
    private val config = ConfiguracaoDoApp(context)
    private val conectividade = context.getSystemService(ConnectivityManager::class.java)
    private val volume = VolumeCentral()
    private val audio = ReprodutorDeAudio(context)
    val video = ReprodutorDeVideo(::desenhou)

    private var estado: EstadoCentral = SessaoCentral.inicial
    private var avisos: Set<Aviso> = emptySet()
    private var dimensoesDoVideo: Pair<Int, Int>? = null
    private var modo = ModoEspelhamento.TELA_FISICA
    private var apps: List<AppDoCelular> = emptyList()
    private var mostrarInicio = true
    @Volatile private var areaInformada: Pair<Int, Int>? = null
    @Volatile private var conexao: Conexao? = null
    @Volatile private var rodando = false
    @Volatile private var redeWifi: Network? = null
    @Volatile private var primeiroQuadroPendente = false
    private val sinal = Object()
    private val principal = android.os.Handler(android.os.Looper.getMainLooper())

    private val callbackDeRede = object : ConnectivityManager.NetworkCallback() {
        // Só a passagem "sem Wi-Fi" <-> "com Wi-Fi" vale como sair/voltar à rede (RN-049); o sistema repete
        // onAvailable para a mesma rede ou para redes extras, e isso não pode recomeçar um espelhamento encerrado.
        private val redes = mutableSetOf<Network>()

        override fun onAvailable(network: Network) {
            val voltou = synchronized(redes) {
                val estavaSemRede = redes.isEmpty()
                redes.add(network)
                estavaSemRede
            }
            redeWifi = network
            if (voltou) evento(EventoCentral.RedeDisponivel)
        }

        override fun onLost(network: Network) {
            val saiu = synchronized(redes) { redes.remove(network) && redes.isEmpty() }
            if (redeWifi == network) redeWifi = synchronized(redes) { redes.firstOrNull() }
            if (saiu) evento(EventoCentral.RedeIndisponivel)
        }
    }

    fun iniciar() {
        rodando = true
        atual = this
        video.definirSuperficie(SuperficieDoVideo.superficie) // a superfície pode ter sido criada antes deste controlador
        publicar()
        registrar("estado=${estado.texto}")
        runCatching {
            conectividade.registerNetworkCallback(
                NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(),
                callbackDeRede,
            )
        }
        thread(isDaemon = true, name = "ctrlmesh-central") { laco() }
    }

    fun parar() {
        rodando = false
        if (atual === this) atual = null
        runCatching { conectividade.unregisterNetworkCallback(callbackDeRede) }
        val c = conexao
        conexao = null
        c?.encerrar(Mensagem.Encerrar(MotivoEncerramento.APP_FECHADO_OU_DESLIGADO))
        video.limpar()
        audio.liberar()
        filaDeVideo.shutdownNow()
        filaDeSom.shutdownNow()
        Notificacoes.cancelarVerCelular(context)
        principal.post { if (atual == null) tela.value = TelaCentralEstado() }
        acordar()
    }

    // ---------- ações da tela ----------

    fun toque(toque: Mensagem.Toque) {
        if (estado.encaminhaToques) conexao?.enviar(toque)
    }

    fun tecla(tecla: Mensagem.Tecla) {
        if (!estado.encaminhaToques) return
        // RN-097: no modo tela virtual, Início mostra o início do CtrlMesh aqui mesmo.
        if (tecla.tecla == TeclaDeSistema.INICIO && modo == ModoEspelhamento.TELA_VIRTUAL) {
            synchronized(this) {
                mostrarInicio = true
                publicar()
            }
        }
        conexao?.enviar(tecla)
    }

    /** Toque num app do início do CtrlMesh (RN-095). */
    @Synchronized
    fun abrirApp(pacote: String) {
        if (!estado.encaminhaToques) return
        conexao?.enviar(Mensagem.AbrirApp(pacote))
        mostrarInicio = false
        publicar()
    }

    /** A área de imagem medida pela tela; se mudou desde o `Ola`, avisa o celular (`Area`, v3.2). */
    fun informarArea(largura: Int, altura: Int) {
        if (largura <= 0 || altura <= 0) return
        val c = conexao ?: return
        if (areaInformada == largura to altura) return
        areaInformada = largura to altura
        c.enviar(Mensagem.Area(largura, altura))
    }

    fun encerrar() = evento(EventoCentral.EncerrarTocado)
    fun espelhar() = evento(EventoCentral.EspelharTocado)
    fun tentarDeNovo() = evento(EventoCentral.TentarDeNovoTocado)

    @Synchronized
    fun mudarVolume(acao: VolumeCentral.() -> Unit) {
        volume.acao()
        audio.definirGanho(volume.ganho)
        registrar("volume nivel=${volume.nivel} mudo=${volume.mudo}")
        publicar()
    }

    // ---------- conexão ----------

    private fun laco() {
        var proximoResumo = SystemClock.elapsedRealtime() + JANELA_DE_MEDICAO_MS
        while (rodando) {
            evento(EventoCentral.Relogio(SystemClock.elapsedRealtime()))
            if (SystemClock.elapsedRealtime() >= proximoResumo) {
                proximoResumo += JANELA_DE_MEDICAO_MS
                conexao?.let {
                    registrar(
                        "metricas central video ${video.medidor.resumo(JANELA_DE_MEDICAO_MS)} rtt_ms=${it.rttMs} " +
                            "audio ${audio.medidor.resumo(JANELA_DE_MEDICAO_MS).substringBefore(" atraso")} pico=${audio.lerEZerarPico()} faixa=${audio.estadoDaFaixa}",
                    )
                }
            }
            if (conexao == null && estado.deveConectar) {
                for (alvo in Descoberta.alvos(config.enderecoDoCelular, gateway())) {
                    if (!rodando || conexao != null || !estado.deveConectar) break
                    tentar(alvo)
                }
                if (conexao == null) dormir(INTERVALO_DE_TENTATIVA_MS) else dormir(TIQUE_MS)
            } else {
                dormir(TIQUE_MS)
            }
        }
    }

    private fun tentar(alvo: String) {
        val socket = Socket()
        val nova = try {
            socket.connect(InetSocketAddress(alvo, Protocolo.PORTA), TEMPO_DE_CONEXAO_MS)
            Conexao.comoCentral(socket)
        } catch (_: ProtocoloException) {
            runCatching { socket.close() }
            registrar("aviso=versão incompatível — atualize o CtrlMesh nos dois aparelhos")
            return
        } catch (_: Exception) {
            runCatching { socket.close() }
            return
        }
        synchronized(this) {
            if (!rodando || !estado.deveConectar) {
                nova.fechar()
                return
            }
            conexao = nova
            avisos = emptySet()
            evento(EventoCentral.ConexaoAberta)
        }
        nova.iniciar(aoReceber = { recebeu(nova, it) }, aoTerminar = { terminou(nova, it) })
        val (largura, altura) = areaDaImagem()
        areaInformada = largura to altura
        nova.enviar(Mensagem.Ola(Build.MODEL ?: "central", largura, altura))
    }

    /**
     * Imagem e som são tratados fora da thread de rede: abrir um decodificador pode levar segundos (no emulador) e a
     * thread de rede parada viraria "queda" (nenhum quadro lido em 3 s).
     */
    private val filaDeVideo = java.util.concurrent.Executors.newSingleThreadExecutor { Thread(it, "ctrlmesh-video").apply { isDaemon = true } }
    private val filaDeSom = java.util.concurrent.Executors.newSingleThreadExecutor { Thread(it, "ctrlmesh-som").apply { isDaemon = true } }

    private fun naImagem(tarefa: () -> Unit) {
        runCatching { filaDeVideo.execute(tarefa) }
    }

    private fun noSom(tarefa: () -> Unit) {
        runCatching { filaDeSom.execute(tarefa) }
    }

    private fun recebeu(origem: Conexao, mensagem: Mensagem) {
        if (conexao !== origem) return
        when (mensagem) {
            is Mensagem.VideoConfig -> {
                synchronized(this) {
                    dimensoesDoVideo = mensagem.largura to mensagem.altura
                    primeiroQuadroPendente = true
                    publicar()
                }
                naImagem { video.configurar(mensagem) }
            }
            is Mensagem.VideoQuadro -> naImagem { video.quadro(mensagem) }
            is Mensagem.AudioConfig -> noSom { audio.configurar(mensagem) }
            is Mensagem.AudioQuadro -> if (estado.tocaSom) noSom { audio.quadro(mensagem) }
            is Mensagem.Situacao -> atualizarAvisos(mensagem.avisos)
            is Mensagem.Modo -> synchronized(this) {
                if (modo != mensagem.modo || estado == EstadoCentral.Conectando) {
                    registrar("modo=${if (mensagem.modo == ModoEspelhamento.TELA_VIRTUAL) "virtual" else "fisica"}")
                }
                modo = mensagem.modo
                publicar()
            }
            is Mensagem.ListaDeApps -> synchronized(this) {
                apps = InicioDoCtrlMesh.ordenar(mensagem.apps)
                publicar()
            }
            Mensagem.TelaVirtualVazia -> synchronized(this) {
                mostrarInicio = true
                publicar()
            }
            else -> evento(EventoCentral.Recebeu(mensagem))
        }
    }

    @Synchronized
    private fun terminou(origem: Conexao, fim: Fim) {
        if (conexao !== origem) return
        conexao = null
        when (fim) {
            Fim.QUEDA -> evento(EventoCentral.Queda)
            Fim.FECHADA_PELO_OUTRO -> evento(EventoCentral.CelularFechouConexao)
            Fim.LOCAL -> Unit
        }
        acordar()
    }

    @Synchronized
    private fun evento(evento: EventoCentral) {
        val antes = estado
        val depois = SessaoCentral.reduzir(antes, evento, SystemClock.elapsedRealtime())
        if (depois == antes) return
        estado = depois
        if (evento == EventoCentral.EncerrarTocado) {
            val c = conexao
            conexao = null
            c?.encerrar(Mensagem.Encerrar(MotivoEncerramento.ENCERRADO_PELA_CENTRAL))
        }
        if (antes.texto != depois.texto) registrar("estado=${depois.texto}")
        when {
            depois is EstadoCentral.Encerrado || depois == EstadoCentral.AguardandoCelular -> {
                fecharConexao()
                naImagem { video.limpar() }
                noSom { audio.esquecer() }
                dimensoesDoVideo = null
                avisos = emptySet()
                modo = ModoEspelhamento.TELA_FISICA
                apps = emptyList()
                mostrarInicio = true
            }
            depois is EstadoCentral.Reconectando -> fecharConexao()
            depois == EstadoCentral.Espelhando && antes != EstadoCentral.Espelhando -> {
                primeiroQuadroPendente = true
                trazerParaFrente()
            }
        }
        if (!depois.tocaSom) noSom { audio.pausar() }
        if (depois != EstadoCentral.Espelhando) Notificacoes.cancelarVerCelular(context)
        publicar()
        acordar()
    }

    private fun fecharConexao() {
        val c = conexao
        conexao = null
        c?.fechar()
    }

    @Synchronized
    private fun atualizarAvisos(novos: Set<Aviso>) {
        (novos - avisos).forEach { registrar("aviso=${it.texto}") }
        avisos = novos
        publicar()
    }

    private fun desenhou() {
        if (primeiroQuadroPendente && estado == EstadoCentral.Espelhando) {
            primeiroQuadroPendente = false
            registrar("primeiro-quadro")
        }
    }

    /** RN-084: com sobreposição a central vem para a frente; sem ela, notificação. */
    private fun trazerParaFrente() {
        if (MainActivity.visivel) return
        if (Settings.canDrawOverlays(context)) {
            val intent = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            runCatching { context.startActivity(intent) }
        } else {
            Notificacoes.verCelular(context)
        }
    }

    private fun publicar() {
        if (atual !== this) return
        val novo = TelaCentralEstado(
            estado, avisos, dimensoesDoVideo.takeIf { mostraImagem(estado) }, volume.mudo, modo, apps, mostrarInicio,
        )
        // A tela só observa estado publicado na thread principal.
        principal.post { if (atual === this) tela.value = novo }
    }

    private fun mostraImagem(e: EstadoCentral) =
        e == EstadoCentral.Espelhando || e == EstadoCentral.DesbloqueieCelular || e is EstadoCentral.Reconectando

    private fun gateway(): String? {
        val rede = redeWifi ?: return null
        val propriedades = conectividade.getLinkProperties(rede) ?: return null
        return propriedades.routes.firstOrNull { it.isDefaultRoute && it.gateway is Inet4Address }?.gateway?.hostAddress
    }

    /** Área de imagem em px reais da tela (as métricas de um serviço podem vir em modo de compatibilidade). */
    private fun areaDaImagem(): Pair<Int, Int> {
        val metricas = android.util.DisplayMetrics()
        @Suppress("DEPRECATION")
        context.getSystemService(android.hardware.display.DisplayManager::class.java)
            .getDisplay(android.view.Display.DEFAULT_DISPLAY).getRealMetrics(metricas)
        val largura = maxOf(metricas.widthPixels, metricas.heightPixels)
        val altura = minOf(metricas.widthPixels, metricas.heightPixels)
        val barra = (ALTURA_DA_BARRA_DP * metricas.density).toInt()
        return largura to (altura - barra).coerceAtLeast(1)
    }

    private fun dormir(ms: Long) = synchronized(sinal) { sinal.wait(ms) }
    private fun acordar() = synchronized(sinal) { sinal.notifyAll() }

    companion object {
        /** Faixa da barra embaixo: alvos de 76 dp + margens (RN-044, RN-110). */
        const val ALTURA_DA_BARRA_DP = 92
        private const val JANELA_DE_MEDICAO_MS = 5_000L
        private const val TEMPO_DE_CONEXAO_MS = 2_000
        private const val INTERVALO_DE_TENTATIVA_MS = 2_000L
        private const val TIQUE_MS = 500L

        private val tela = MutableStateFlow(TelaCentralEstado())
        val estadoDaTela: StateFlow<TelaCentralEstado> = tela

        @Volatile var atual: ControladorCentral? = null
            private set
    }
}
