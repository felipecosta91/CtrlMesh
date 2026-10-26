package com.ctrlmesh.celular

import android.app.Activity
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.projection.MediaProjectionManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.ctrlmesh.Conexao
import com.ctrlmesh.ConfiguracaoDoApp
import com.ctrlmesh.Fim
import com.ctrlmesh.Notificacoes
import com.ctrlmesh.ServicoDeEscuta
import com.ctrlmesh.protocolo.Aviso
import com.ctrlmesh.protocolo.EstadoRemoto
import com.ctrlmesh.protocolo.Mensagem
import com.ctrlmesh.protocolo.MotivoEncerramento
import com.ctrlmesh.protocolo.Protocolo
import com.ctrlmesh.registrar
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

/** Papel celular: escuta as centrais, mantém a sessão (contrato §5.2/§5.3), captura e executa os toques. */
@RequiresApi(Build.VERSION_CODES.Q)
class ControladorCelular(private val context: Context, private val servico: ServicoDeEscuta) {
    private class Ligacao(val id: Long, val conexao: Conexao, val ola: Mensagem.Ola) {
        /** Área de imagem da central: a do `Ola`, atualizada por `Area` (v3.2). */
        @Volatile var area = ola.larguraArea to ola.alturaArea
    }

    private val config = ConfiguracaoDoApp(context)
    private val armazenamento = ArmazenamentoAndroid(context)
    private val ajustes = AjustesAndroid(context)
    private val restauracao = RestauracaoDoCelular(ajustes, armazenamento)
    private val fila = FilaDeCentrais()
    private val ligacoes = ConcurrentHashMap<Long, Ligacao>()
    private val proximoId = AtomicLong(1)

    private val medidorDeToques = com.ctrlmesh.Medidor()
    private var sessao = SessaoCelular.inicial(Autorizacoes.liberacaoPermanente(context), Build.VERSION.SDK_INT)
    private var avisos: Set<Aviso> = emptySet()
    private var nomeDaCentral = ""
    private var captura: Captura? = null
    private var pedidoDeCapturaDesdeMs: Long? = null
    private var transmitindoPara: Long? = null
    @Volatile private var servidor: ServerSocket? = null
    @Volatile private var rodando = false
    @Volatile private var interfacesExcluidas: Set<String> = emptySet()

    // ---------- modo tela virtual (contrato §10) ----------
    private val ajudante = com.ctrlmesh.celular.virtual.ClienteDoAjudante(context)
    private val modoDaSessao = ModoDaSessao()
    private var capturaVirtual: com.ctrlmesh.celular.virtual.CapturaVirtual? = null
    @Volatile private var estadoShizuku: EstadoShizuku? = null
    @Volatile private var appNaFrente = false
    private val aoMudarShizuku = rikka.shizuku.Shizuku.OnBinderReceivedListener { atualizarShizuku() }
    private val aoPerderShizuku = rikka.shizuku.Shizuku.OnBinderDeadListener { atualizarShizuku() }
    private val aoAutorizarShizuku = rikka.shizuku.Shizuku.OnRequestPermissionResultListener { _, _ -> atualizarShizuku() }
    private val virtualNaSessao get() = modoDaSessao.atual == com.ctrlmesh.protocolo.ModoEspelhamento.TELA_VIRTUAL

    private val tela = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> evento(EventoCelular.CelularBloqueado)
                Intent.ACTION_USER_PRESENT -> evento(EventoCelular.CelularDesbloqueado)
                Intent.ACTION_SCREEN_ON ->
                    if (!c.getSystemService(KeyguardManager::class.java).isKeyguardLocked) evento(EventoCelular.CelularDesbloqueado)
            }
        }
    }

    private val inicioMs = SystemClock.elapsedRealtime()

    /**
     * Logo depois que o app abre, o binder do Shizuku ainda não chegou (vem pelo ShizukuProvider) e o UserService
     * leva alguns segundos para subir: espera o binder por pouco tempo e, se o Shizuku estiver ativo, o ajudante.
     */
    private fun prepararAjudante() {
        while (ajudante.estado() == EstadoShizuku.DESLIGADO && SystemClock.elapsedRealtime() - inicioMs < ESPERA_BINDER_MS) {
            Thread.sleep(200)
        }
        atualizarShizuku()
        if (estadoShizuku == EstadoShizuku.ATIVO && !ajudante.pronto) {
            registrar("ajudante=preparando")
            ajudante.garantir(ESPERA_AJUDANTE_MS)
        }
    }

    fun iniciar() {
        rodando = true
        atual = this
        restauracao.restaurar() // RN-028, RN-060: encerramento anterior com o app fechado
        registrar("estado=${texto(sessao.estado)}")
        publicar()
        context.registerReceiver(
            tela,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            },
        )
        runCatching {
            rikka.shizuku.Shizuku.addBinderReceivedListenerSticky(aoMudarShizuku)
            rikka.shizuku.Shizuku.addBinderDeadListener(aoPerderShizuku)
            rikka.shizuku.Shizuku.addRequestPermissionResultListener(aoAutorizarShizuku)
        }
        atualizarShizuku()
        thread(isDaemon = true, name = "ctrlmesh-escuta") { escutar() }
        thread(isDaemon = true, name = "ctrlmesh-relogio") {
            var tiques = 0
            while (rodando) {
                evento(EventoCelular.Relogio(SystemClock.elapsedRealtime()))
                atualizarShizuku()
                atualizarAvisos()
                vigiarTelaVirtual()
                if (++tiques % TIQUES_POR_RESUMO == 0) resumirMetricas()
                Thread.sleep(TIQUE_MS)
            }
        }
    }

    @Synchronized
    fun parar() {
        rodando = false
        if (atual === this) atual = null
        runCatching { servidor?.close() }
        runCatching { context.unregisterReceiver(tela) }
        ligacoes.values.forEach { it.conexao.encerrar(Mensagem.Encerrar(MotivoEncerramento.APP_FECHADO_OU_DESLIGADO)) }
        ligacoes.clear()
        captura?.liberar()
        captura = null
        capturaVirtual?.fechar()
        capturaVirtual = null
        runCatching {
            rikka.shizuku.Shizuku.removeBinderReceivedListener(aoMudarShizuku)
            rikka.shizuku.Shizuku.removeBinderDeadListener(aoPerderShizuku)
            rikka.shizuku.Shizuku.removeRequestPermissionResultListener(aoAutorizarShizuku)
        }
        ajudante.desligar()
        manterCpuAcordada(false)
        restauracao.restaurar()
        Notificacoes.cancelarEspelharNaCentral(context)
        Notificacoes.cancelarAtivarTelaHorizontal(context)
    }

    /** Estado do Shizuku (§10.1): log na mudança, ajudante ligado quando ativo, notificação quando desligado (RN-104). */
    fun atualizarShizuku() {
        val novo = ajudante.estado()
        if (novo == estadoShizuku) return
        estadoShizuku = novo
        registrar("shizuku=${when (novo) { EstadoShizuku.ATIVO -> "ativo"; EstadoShizuku.DESLIGADO -> "desligado"; EstadoShizuku.SEM_AUTORIZACAO -> "sem-autorizacao" }}")
        EstadoDaTelaDoCelular.atualizarShizuku(novo)
        if (novo == EstadoShizuku.ATIVO) {
            ajudante.ligar()
            Notificacoes.cancelarAtivarTelaHorizontal(context)
        } else if (novo == EstadoShizuku.DESLIGADO && celularNumWifi()) {
            Notificacoes.ativarTelaHorizontal(context)
        } else {
            Notificacoes.cancelarAtivarTelaHorizontal(context)
        }
        atualizarAvisos()
    }

    private fun celularNumWifi(): Boolean {
        val conectividade = context.getSystemService(ConnectivityManager::class.java)
        val rede = conectividade.activeNetwork ?: return false
        return conectividade.getNetworkCapabilities(rede)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }

    /**
     * Com a tela do celular apagada (modo tela virtual, RN-098), a CPU pode dormir entre um quadro e outro: o
     * batimento atrasa mais de 3 s e a central vê queda. Trava parcial enquanto o espelhamento dura.
     */
    private val cpu by lazy {
        context.getSystemService(android.os.PowerManager::class.java)
            .newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "CtrlMesh:espelhamento").apply { setReferenceCounted(false) }
    }

    @android.annotation.SuppressLint("WakelockTimeout")
    private fun manterCpuAcordada(sim: Boolean) {
        if (sim && !cpu.isHeld) cpu.acquire() else if (!sim && cpu.isHeld) cpu.release()
    }

    /** Sem app na frente da tela virtual → a central mostra o início do CtrlMesh (§10.3). */
    private fun vigiarTelaVirtual() {
        val cv = capturaVirtual ?: return
        if (!virtualNaSessao || sessao.estado != EstadoCelular.Espelhando) return
        val tem = cv.temAppNaFrente()
        if (appNaFrente && !tem) enviarParaAtiva(Mensagem.TelaVirtualVazia)
        appNaFrente = tem
    }

    fun encerrarTocado() = evento(EventoCelular.EncerrarTocado)

    // ---------- rede ----------

    private fun escutar() {
        while (rodando) {
            val s = runCatching { ServerSocket().apply { reuseAddress = true; bind(InetSocketAddress(Protocolo.PORTA)) } }.getOrNull()
            if (s == null) {
                Thread.sleep(ESPERA_PORTA_MS)
                continue
            }
            servidor = s
            if (!rodando) s.close()
            while (rodando && !s.isClosed) {
                val socket = runCatching { s.accept() }.getOrNull() ?: break
                val endereco = RedeDoRoteador.semPrefixoMapeado(socket.inetAddress.hostAddress ?: "")
                val subredes = subredesDoRoteador(socket)
                if (!RedeDoRoteador.aceita(endereco, subredes)) {
                    registrar("recusada=$endereco")
                    registrar("sub-redes do roteador=${subredes.joinToString { "${it.endereco}/${it.prefixo}" }.ifEmpty { "nenhuma" }}")
                    registrar("interfaces de cliente excluídas=${interfacesExcluidas.joinToString().ifEmpty { "nenhuma" }}")
                    runCatching { socket.close() }
                    continue
                }
                thread(isDaemon = true, name = "ctrlmesh-atender") { atender(socket) }
            }
            runCatching { s.close() }
        }
    }

    /**
     * RN-077: sub-redes do roteador (hotspot) do celular = endereços IPv4 do celular que não pertencem a nenhuma rede
     * em que ele é cliente (Wi-Fi de casa, dados, VPN). Duas fontes, porque em alguns aparelhos (ex.: ColorOS/Android 16)
     * a listagem de interfaces não traz o `ap0`:
     *  1. interfaces listadas por NetworkInterface;
     *  2. o endereço local do socket aceito (a interface por onde a central chegou), com o prefixo da interface ou /24.
     */
    private fun subredesDoRoteador(socket: Socket): List<Subrede> {
        val conectividade = context.getSystemService(ConnectivityManager::class.java)
        @Suppress("DEPRECATION")
        val propriedades = conectividade.allNetworks
            .filter { ehRedeDeCliente(conectividade.getNetworkCapabilities(it)) }
            .mapNotNull { conectividade.getLinkProperties(it) }
        val interfacesDeCliente = propriedades.mapNotNull { it.interfaceName }.toSet()
        interfacesExcluidas = interfacesDeCliente
        val enderecosDeCliente = propriedades.flatMap { p -> p.linkAddresses.map { it.address.hostAddress } }.toSet()

        val listadas = runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback && it.name !in interfacesDeCliente }
                .flatMap { i -> i.interfaceAddresses.filter { it.address is Inet4Address }.map { Subrede(it.address.hostAddress!!, it.networkPrefixLength.toInt()) } }
        }.getOrDefault(emptyList())

        val local = socket.localAddress?.hostAddress?.let(RedeDoRoteador::semPrefixoMapeado)
        val pelaConexao = local
            ?.takeIf { it !in enderecosDeCliente && !it.startsWith("127.") && it.count { c -> c == '.' } == 3 }
            ?.let { endereco ->
                val prefixo = runCatching {
                    NetworkInterface.getByInetAddress(socket.localAddress)?.interfaceAddresses
                        ?.firstOrNull { it.address.hostAddress == endereco }?.networkPrefixLength?.toInt()
                }.getOrNull()
                Subrede(endereco, prefixo ?: PREFIXO_PADRAO_DO_ROTEADOR)
            }
        return (listadas + listOfNotNull(pelaConexao)).filter { it.endereco !in enderecosDeCliente }.distinct()
    }

    /** Android 15+: o hotspot (`ap0`) também é uma Network, com NET_CAPABILITY_LOCAL_NETWORK; ele não é rede de cliente. */
    private fun ehRedeDeCliente(capacidades: NetworkCapabilities?): Boolean {
        if (capacidades == null) return true
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM ||
            !capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_LOCAL_NETWORK)
    }

    private fun atender(socket: Socket) {
        val conexao = runCatching { Conexao.comoCelular(socket) }.getOrElse {
            runCatching { socket.close() }
            return
        }
        val ola = runCatching { conexao.receber() as? Mensagem.Ola }.getOrNull()
        if (ola == null || !rodando) {
            conexao.fechar()
            return
        }
        val ligacao = Ligacao(proximoId.getAndIncrement(), conexao, ola)
        ligacoes[ligacao.id] = ligacao
        conexao.aoDescartarVideo { captura?.pedirQuadroChave() }
        conexao.iniciar(aoReceber = { recebeu(ligacao, it) }, aoTerminar = { terminou(ligacao, it) })
        // Espelhamento novo: com o Shizuku ativo, espera o ajudante antes de decidir o modo, para nunca cair no
        // pedido de captura só porque o app acabou de abrir (RN-093, RN-074). A central fica em "conectando".
        if (fila.ativa == null && (sessao.estado == EstadoCelular.Escuta || sessao.estado is EstadoCelular.Encerrado)) {
            prepararAjudante()
        }
        synchronized(this) {
            conexao.enviar(Mensagem.Situacao(avisos))
            val anterior = fila.ativa
            when {
                sessao.estado is EstadoCelular.Reconectando && anterior != null -> {
                    fila.retomar(ligacao.id) // RN-054: qualquer Ola em "reconectando" é a retomada
                    ligacoes.remove(anterior)?.conexao?.fechar()
                    conectou(ligacao)
                }
                fila.chegou(ligacao.id) -> conectou(ligacao)
                else -> conexao.enviar(Mensagem.Estado(EstadoRemoto.EM_USO_POR_OUTRA_CENTRAL))
            }
        }
    }

    @Synchronized
    private fun conectou(ligacao: Ligacao) {
        nomeDaCentral = ligacao.ola.nomeCentral
        transmitindoPara = null
        val modo = escolherModo(ligacao)
        sessao = sessao.copy(
            liberacaoPermanente = Autorizacoes.liberacaoPermanente(context),
            telaVirtual = modo == com.ctrlmesh.protocolo.ModoEspelhamento.TELA_VIRTUAL,
        )
        atualizarAvisos()
        avisos.forEach { registrar("aviso=${it.texto}") }
        ligacao.conexao.enviar(Mensagem.Modo(modo)) // antes de Estado(ESPELHANDO), também na retomada (§10.3)
        evento(EventoCelular.CentralConectou)
        sessao.estadoRemoto?.let { ligacao.conexao.enviar(Mensagem.Estado(it)) }
        atualizarFluxo()
        if (sessao.telaVirtual) {
            appNaFrente = false
            thread(isDaemon = true, name = "ctrlmesh-apps") {
                ligacao.conexao.enviar(Mensagem.ListaDeApps(com.ctrlmesh.celular.virtual.AppsDoCelular.listar(context)))
            }
        }
    }

    /**
     * Modo fixado no começo do espelhamento (§10.2). Na tela virtual, a tela é criada já aqui: se o ajudante falhar,
     * cai para a tela do celular antes de enviar `Modo`.
     */
    private fun escolherModo(ligacao: Ligacao): com.ctrlmesh.protocolo.ModoEspelhamento {
        val novo = modoDaSessao.atual == null
        var modo = modoDaSessao.iniciar(estadoShizuku ?: ajudante.estado())
        if (novo && modo == com.ctrlmesh.protocolo.ModoEspelhamento.TELA_VIRTUAL) {
            val pronto = ajudante.garantir(ESPERA_CURTA_MS) // a espera longa já foi feita em prepararAjudante, fora da trava
            val cv = capturaVirtual ?: com.ctrlmesh.celular.virtual.CapturaVirtual(context, ajudante).also { capturaVirtual = it }
            if (!pronto || !cv.transmitir(ligacao.area.first, ligacao.area.second) { if (fila.ativa == ligacao.id) ligacao.conexao.enviar(it) }) {
                registrar("display_virtual=falhou")
                cv.fechar()
                modoDaSessao.cairParaTelaFisica()
                modo = com.ctrlmesh.protocolo.ModoEspelhamento.TELA_FISICA
            } else {
                transmitindoPara = ligacao.id
                cv.transmitirSom(::enviarParaAtiva)
            }
        }
        if (novo) registrar("modo=${if (modo == com.ctrlmesh.protocolo.ModoEspelhamento.TELA_VIRTUAL) "virtual" else "fisica"}")
        return modo
    }

    private fun recebeu(ligacao: Ligacao, mensagem: Mensagem) {
        if (fila.ativa != ligacao.id) return
        when (mensagem) {
            is Mensagem.Toque -> {
                medidorDeToques.contar("toques")
                medidorDeToques.chegou(mensagem.tempoMs, SystemClock.uptimeMillis())
                if (sessao.estado == EstadoCelular.Espelhando) {
                    val cv = capturaVirtual.takeIf { virtualNaSessao }
                    if (cv != null) cv.toque(mensagem) else ServicoDeControle.instancia?.toque(mensagem)
                }
                medidorDeToques.concluiu(mensagem.tempoMs, SystemClock.uptimeMillis())
            }
            is Mensagem.Tecla -> if (sessao.estado == EstadoCelular.Espelhando) {
                val cv = capturaVirtual.takeIf { virtualNaSessao }
                if (cv != null) cv.tecla(mensagem.tecla) else ServicoDeControle.instancia?.tecla(mensagem.tecla)
            }
            is Mensagem.AbrirApp -> {
                val cv = capturaVirtual.takeIf { virtualNaSessao && sessao.estado == EstadoCelular.Espelhando }
                if (cv != null) thread(isDaemon = true, name = "ctrlmesh-abrir") { cv.abrirApp(mensagem.pacote) }
            }
            is Mensagem.Area -> synchronized(this) {
                if (ligacao.area == mensagem.larguraArea to mensagem.alturaArea) return
                ligacao.area = mensagem.larguraArea to mensagem.alturaArea
                transmitindoPara = null // retransmite com o novo tamanho (tela virtual redimensionada, §10.3)
                atualizarFluxo()
            }
            is Mensagem.Encerrar -> evento(EventoCelular.Recebeu(mensagem))
            else -> Unit
        }
    }

    @Synchronized
    private fun terminou(ligacao: Ligacao, fim: Fim) {
        ligacoes.remove(ligacao.id)
        if (fila.ativa != ligacao.id) {
            fila.saiu(ligacao.id)
            return
        }
        when (fim) {
            Fim.QUEDA -> {
                evento(EventoCelular.Queda)
                // A vaga fica reservada durante a janela de reconexão (decisão do dono, contrato §5.3).
                if (sessao.estado !is EstadoCelular.Reconectando) liberarVaga(ligacao.id)
            }
            Fim.FECHADA_PELO_OUTRO -> {
                evento(EventoCelular.CentralFechouConexao)
                if (fila.ativa == ligacao.id) liberarVaga(ligacao.id)
            }
            Fim.LOCAL -> liberarVaga(ligacao.id)
        }
    }

    private fun liberarVaga(id: Long) {
        val proxima = fila.saiu(id) ?: return
        ligacoes[proxima]?.let(::conectou) ?: liberarVaga(proxima)
    }

    private fun ligacaoAtiva(): Ligacao? = fila.ativa?.let { ligacoes[it] }

    private fun enviarParaAtiva(mensagem: Mensagem) {
        ligacaoAtiva()?.conexao?.enviar(mensagem)
    }

    // ---------- sessão ----------

    @Synchronized
    private fun evento(evento: EventoCelular) {
        val antes = sessao
        val depois = antes.reduzir(evento, SystemClock.elapsedRealtime())
        if (depois == antes) return
        sessao = depois
        if (texto(antes.estado) != texto(depois.estado)) registrar("estado=${texto(depois.estado)}")
        val remoto = depois.estadoRemoto
        if (remoto != null && remoto != antes.estadoRemoto) enviarParaAtiva(Mensagem.Estado(remoto))
        if (antes.capturaViva && !depois.capturaViva && !depois.liberacaoPermanente) liberarCaptura()
        if (depois.estado is EstadoCelular.Encerrado) {
            silenciarCortaOSom = false
            if (antes.estado !is EstadoCelular.Encerrado) {
                capturaVirtual?.fechar()
                modoDaSessao.encerrar()
                sessao = sessao.copy(telaVirtual = false)
            }
        }
        if (depois.ajustesAplicados && !antes.ajustesAplicados) {
            restauracao.aplicar(podeGirar = Autorizacoes.girarTela(context), silenciarMidia = planoDeAudio().silenciarCelular)
        } else if (!depois.ajustesAplicados && antes.ajustesAplicados) {
            restauracao.restaurar()
        }
        if (depois.mostraIndicador != antes.mostraIndicador) {
            atualizarNotificacao()
            if (depois.mostraIndicador) registrar("indicador=$nomeDaCentral")
        }
        atualizarFluxo()
        manterCpuAcordada(depois.mostraIndicador)
        publicar()
        val fim = depois.estado
        if (fim is EstadoCelular.Encerrado && antes.estado !is EstadoCelular.Encerrado) {
            val ativa = ligacaoAtiva()
            val avisarCentral = evento !is EventoCelular.Recebeu && evento != EventoCelular.CentralFechouConexao
            when {
                ativa == null -> fila.ativa?.let(::liberarVaga)
                avisarCentral -> ativa.conexao.encerrar(Mensagem.Encerrar(fim.motivo))
                else -> ativa.conexao.fechar()
            }
        }
    }

    /** Linha de desempenho a cada 5 s, só com uma central ativa. */
    private fun resumirMetricas() {
        val ligacao = ligacaoAtiva() ?: return
        val janela = TIQUE_MS * TIQUES_POR_RESUMO
        val cv = capturaVirtual.takeIf { virtualNaSessao }
        val video = (cv?.medidor ?: captura?.medidor)?.resumo(janela)?.substringBefore(" atraso") ?: "codificados=-"
        val audio = when {
            cv != null -> "audio ${cv.medidorDeAudio.resumo(janela).substringBefore(" atraso")} pico=${cv.picoDeAudio()} modo=virtual"
            captura != null -> "audio ${captura!!.medidorDeAudio.resumo(janela).substringBefore(" atraso")} pico=${captura!!.picoDeAudio()}"
            else -> "audio sem captura"
        }
        registrar(
            "metricas celular video $video $audio volume_midia=${ajustes.volumeMidia} fila_bytes=${ligacao.conexao.bytesNaFila} " +
                "descartes_video_total=${ligacao.conexao.descartesDeVideo} rtt_ms=${ligacao.conexao.rttMs} " +
                medidorDeToques.resumo(janela).replace("atraso_excedente_ms", "toque_ate_gesto_excedente_ms"),
        )
    }

    private fun planoDeAudio() = PlanoDeAudio.decidir(Autorizacoes.gravarAudio(context), silenciarCortaOSom)

    /** Liga/desliga captura e transmissão conforme o estado. */
    private fun atualizarFluxo() {
        if (sessao.telaVirtual) {
            atualizarFluxoVirtual()
            return
        }
        val estado = sessao.estado
        val precisaCaptura = estado == EstadoCelular.Espelhando || sessao.pedePermitir
        if (!precisaCaptura) {
            pedidoDeCapturaDesdeMs = null
            Notificacoes.cancelarEspelharNaCentral(context)
        } else if (captura == null) {
            solicitarCaptura()
        }
        val c = captura
        val ligacao = ligacaoAtiva()
        if (estado == EstadoCelular.Espelhando && ligacao != null && c != null) {
            if (transmitindoPara == ligacao.id) return
            transmitindoPara = ligacao.id
            c.transmitirVideo(ligacao.area.first, ligacao.area.second, ::enviarParaAtiva)
            if (planoDeAudio().enviarSom) {
                c.transmitirAudio(::enviarParaAtiva, testeDeVolume)
            }
        } else if (transmitindoPara != null) {
            transmitindoPara = null
            c?.pararVideo()
            c?.pararAudio()
        }
    }

    private fun atualizarFluxoVirtual() {
        val cv = capturaVirtual ?: return
        val ligacao = ligacaoAtiva()
        if (sessao.estado == EstadoCelular.Espelhando && ligacao != null) {
            if (transmitindoPara == ligacao.id) return
            if (!cv.transmitir(ligacao.area.first, ligacao.area.second, ::enviarParaAtiva)) {
                registrar("display_virtual=falhou")
                return
            }
            transmitindoPara = ligacao.id
            cv.transmitirSom(::enviarParaAtiva)
        } else if (transmitindoPara != null) {
            // "reconectando": tela virtual e apps mantidos; só a transmissão para (§10.3).
            transmitindoPara = null
            cv.pararVideo()
            cv.pararSom()
        }
    }

    /** RN-019: com sobreposição o pedido aparece sozinho; sem ela, notificação. */
    private fun solicitarCaptura() {
        val agora = SystemClock.elapsedRealtime()
        val desde = pedidoDeCapturaDesdeMs
        if (desde != null && agora - desde < REPETIR_PEDIDO_MS) return
        pedidoDeCapturaDesdeMs = agora
        if (Settings.canDrawOverlays(context)) {
            runCatching {
                context.startActivity(Intent(context, AtividadeDeCaptura::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        } else {
            Notificacoes.espelharNaCentral(context)
        }
    }

    @Synchronized
    fun capturaConcedida(resultado: Int, dados: Intent?) {
        pedidoDeCapturaDesdeMs = null
        Notificacoes.cancelarEspelharNaCentral(context)
        val estado = sessao.estado
        if (resultado != Activity.RESULT_OK || dados == null) {
            // Captura recusada = não confirmado (contrato §8).
            if (estado is EstadoCelular.AguardandoConfirmacao) {
                evento(EventoCelular.Relogio(estado.desdeMs + SessaoCelular.PRAZO_CONFIRMACAO_MS))
            }
            return
        }
        if (captura == null) {
            servico.emPrimeiroPlano(notificacao(), capturando = true, comAudio = Autorizacoes.gravarAudio(context))
            val gerenciador = context.getSystemService(MediaProjectionManager::class.java)
            val projecao = runCatching { gerenciador.getMediaProjection(resultado, dados) }.getOrNull() ?: return
            captura = runCatching { Captura(context, projecao, ::capturaParou) }.getOrNull()
        }
        if (sessao.pedePermitir) evento(EventoCelular.PermitirTocado) else atualizarFluxo()
    }

    @Synchronized
    private fun capturaParou() {
        if (captura == null) return
        liberarCaptura()
        atualizarFluxo()
    }

    private fun liberarCaptura() {
        val c = captura ?: return
        captura = null
        transmitindoPara = null
        c.liberar()
        servico.emPrimeiroPlano(notificacao(), capturando = false)
    }

    /** RN-090 nesta sessão: resultado só em memória, zerado a cada espelhamento novo. */
    @Volatile private var silenciarCortaOSom = false

    private val testeDeVolume = object : ControleDeVolumeDoTeste {
        override fun silenciado() = ajustes.volumeMidia == 0
        override fun devolverVolume() {
            armazenamento.ler()?.let { ajustes.volumeMidia = it.volumeMidia }
        }
        override fun silenciarDeNovo() {
            if (!silenciarCortaOSom && planoDeAudio().silenciarCelular) ajustes.volumeMidia = 0
        }
        override fun silenciarCortaOSom() {
            silenciarCortaOSom = true
            devolverVolume()
            atualizarAvisos()
        }
    }

    @Synchronized
    private fun atualizarAvisos() {
        val virtual = virtualNaSessao
        val novos = buildSet {
            // Na tela virtual não há acessibilidade, giro, sobreposição, captura nem volume envolvidos (§10).
            if (!virtual) {
                if (!Autorizacoes.acessibilidade(context)) add(Aviso.FALTA_ACESSIBILIDADE)
                if (!Autorizacoes.girarTela(context)) add(Aviso.FALTA_GIRAR_TELA)
                if (!Autorizacoes.sobrepor(context)) add(Aviso.FALTA_SOBREPOR)
                if (!Autorizacoes.liberacaoPermanente(context)) add(Aviso.SEM_LIBERACAO_PERMANENTE)
                addAll(planoDeAudio().avisos)
            }
            if (!Autorizacoes.bateria(context)) add(Aviso.FALTA_BATERIA)
            val modoDoAudio = context.getSystemService(AudioManager::class.java).mode
            if (modoDoAudio == AudioManager.MODE_IN_CALL || modoDoAudio == AudioManager.MODE_IN_COMMUNICATION) {
                add(if (virtual) Aviso.LIGACAO_NO_CELULAR else Aviso.LIGACAO_EM_ANDAMENTO)
            }
            val estado = estadoShizuku
            val modo = modoDaSessao.atual ?: estado?.let(EscolhaDeModo::escolher)
            if (modo != null) addAll(EscolhaDeModo.avisos(modo))
        }
        if (novos == avisos) return
        (novos - avisos).forEach { registrar("aviso=${it.texto}") }
        avisos = novos
        ligacoes.values.forEach { it.conexao.enviar(Mensagem.Situacao(novos)) }
        publicar()
    }

    private fun atualizarNotificacao() {
        servico.emPrimeiroPlano(notificacao(), capturando = captura != null, comAudio = Autorizacoes.gravarAudio(context))
    }

    private fun notificacao() =
        Notificacoes.servico(context, "em escuta", nomeDaCentral.takeIf { sessao.mostraIndicador }, telaVirtual = virtualNaSessao)

    private fun publicar() {
        val novo = texto(sessao.estado)
        android.os.Handler(android.os.Looper.getMainLooper()).post { EstadoDaTelaDoCelular.atualizar(novo) }
    }

    companion object {
        private const val TIQUE_MS = 1_000L
        private const val ESPERA_AJUDANTE_MS = 25_000L
        private const val ESPERA_BINDER_MS = 6_000L
        private const val ESPERA_CURTA_MS = 2_000L
        private const val TIQUES_POR_RESUMO = 5
        private const val PREFIXO_PADRAO_DO_ROTEADOR = 24
        private const val ESPERA_PORTA_MS = 2_000L
        private const val REPETIR_PEDIDO_MS = 15_000L

        @Volatile var atual: ControladorCelular? = null
            private set

        /** Linha `estado=` do contrato §7.2. */
        fun texto(estado: EstadoCelular): String = when (estado) {
            EstadoCelular.Escuta -> "escuta"
            is EstadoCelular.AguardandoConfirmacao -> "aguardando confirmação"
            EstadoCelular.Espelhando -> "espelhando"
            EstadoCelular.Bloqueado -> "bloqueado"
            is EstadoCelular.Reconectando -> "reconectando"
            is EstadoCelular.Encerrado -> "encerrado motivo=${estado.motivo.texto}"
        }
    }
}

/** Estado mostrado na tela do celular (linha `estado=` sem o prefixo). */
object EstadoDaTelaDoCelular {
    private val fluxo = kotlinx.coroutines.flow.MutableStateFlow("escuta")
    val valor: kotlinx.coroutines.flow.StateFlow<String> = fluxo

    fun atualizar(texto: String) {
        fluxo.value = texto
    }

    private val fluxoShizuku = kotlinx.coroutines.flow.MutableStateFlow<EstadoShizuku?>(null)
    val shizuku: kotlinx.coroutines.flow.StateFlow<EstadoShizuku?> = fluxoShizuku

    fun atualizarShizuku(estado: EstadoShizuku) {
        android.os.Handler(android.os.Looper.getMainLooper()).post { fluxoShizuku.value = estado }
    }
}

/** Pede a captura da tela inteira (RN-021); com a liberação permanente, o sistema responde sem mostrar nada. */
class AtividadeDeCaptura : Activity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) return
        val gerenciador = getSystemService(MediaProjectionManager::class.java)
        val pedido = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            gerenciador.createScreenCaptureIntent(android.media.projection.MediaProjectionConfig.createConfigForDefaultDisplay())
        } else {
            gerenciador.createScreenCaptureIntent()
        }
        @Suppress("DEPRECATION")
        startActivityForResult(pedido, PEDIDO)
    }

    @Deprecated("Activity simples, sem ActivityResult API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ControladorCelular.atual?.capturaConcedida(resultCode, data)
        finish()
    }

    private companion object {
        const val PEDIDO = 1
    }
}
