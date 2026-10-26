package com.ctrlmesh.central

import com.ctrlmesh.protocolo.EstadoRemoto
import com.ctrlmesh.protocolo.Mensagem
import com.ctrlmesh.protocolo.MotivoEncerramento

sealed interface EstadoCentral {
    data object AguardandoCelular : EstadoCentral
    data object Conectando : EstadoCentral
    data object AguardandoConfirmacao : EstadoCentral
    data object Espelhando : EstadoCentral
    data object DesbloqueieCelular : EstadoCentral
    data class Reconectando(val desdeMs: Long) : EstadoCentral
    data object EmUsoPorOutraCentral : EstadoCentral
    data class Encerrado(val motivo: MotivoEncerramento) : EstadoCentral

    val texto: String
        get() = when (this) {
            AguardandoCelular -> "aguardando o celular"
            Conectando -> "conectando"
            AguardandoConfirmacao -> "aguardando confirmação no celular"
            Espelhando -> "espelhando"
            DesbloqueieCelular -> "desbloqueie o celular"
            is Reconectando -> "reconectando"
            EmUsoPorOutraCentral -> "celular em uso por outra central"
            is Encerrado -> motivo.texto
        }

    val deveConectar: Boolean
        get() = when (this) {
            AguardandoCelular, is Reconectando -> true
            is Encerrado -> motivo == MotivoEncerramento.CONEXAO_PERDIDA || motivo == MotivoEncerramento.APP_FECHADO_OU_DESLIGADO
            else -> false
        }

    val encaminhaToques: Boolean get() = this == Espelhando
    val tocaSom: Boolean get() = this == Espelhando
    val imagemDesatualizada: Boolean get() = this is Reconectando

    /** Conectando, AguardandoConfirmacao, Espelhando, DesbloqueieCelular, EmUsoPorOutraCentral. */
    val conectado: Boolean
        get() = this == Conectando || this == AguardandoConfirmacao || this == Espelhando ||
            this == DesbloqueieCelular || this == EmUsoPorOutraCentral
}

sealed interface EventoCentral {
    data object RedeDisponivel : EventoCentral
    data object RedeIndisponivel : EventoCentral
    data object ConexaoAberta : EventoCentral
    data class Recebeu(val mensagem: Mensagem) : EventoCentral
    data object Queda : EventoCentral
    data object CelularFechouConexao : EventoCentral
    data object EncerrarTocado : EventoCentral
    data object EspelharTocado : EventoCentral
    data object TentarDeNovoTocado : EventoCentral
    data class Relogio(val agoraMs: Long) : EventoCentral
}

object SessaoCentral {
    const val JANELA_DE_RECONEXAO_MS = 60_000L
    val inicial: EstadoCentral = EstadoCentral.AguardandoCelular

    fun reduzir(atual: EstadoCentral, evento: EventoCentral, agoraMs: Long): EstadoCentral {
        val ativo = atual.conectado || atual is EstadoCentral.Reconectando
        val emSessao = atual == EstadoCentral.AguardandoConfirmacao || atual == EstadoCentral.Espelhando ||
            atual == EstadoCentral.DesbloqueieCelular
        val procurando = atual == EstadoCentral.Conectando || atual == EstadoCentral.EmUsoPorOutraCentral
        return when (evento) {
            EventoCentral.ConexaoAberta ->
                if (atual == EstadoCentral.AguardandoCelular || atual is EstadoCentral.Encerrado) EstadoCentral.Conectando else atual
            is EventoCentral.Recebeu -> when (val m = evento.mensagem) {
                is Mensagem.Estado -> if (ativo) deRemoto(m.estado) else atual
                is Mensagem.Encerrar -> if (ativo) EstadoCentral.Encerrado(m.motivo) else atual
                else -> atual
            }
            EventoCentral.EncerrarTocado ->
                if (ativo) EstadoCentral.Encerrado(MotivoEncerramento.ENCERRADO_PELA_CENTRAL) else atual
            EventoCentral.CelularFechouConexao -> when {
                emSessao -> EstadoCentral.Encerrado(MotivoEncerramento.APP_FECHADO_OU_DESLIGADO)
                procurando -> EstadoCentral.AguardandoCelular
                else -> atual
            }
            EventoCentral.Queda, EventoCentral.RedeIndisponivel -> when {
                emSessao -> EstadoCentral.Reconectando(agoraMs)
                procurando -> EstadoCentral.AguardandoCelular
                else -> atual
            }
            is EventoCentral.Relogio ->
                if (atual is EstadoCentral.Reconectando && evento.agoraMs - atual.desdeMs >= JANELA_DE_RECONEXAO_MS) {
                    EstadoCentral.Encerrado(MotivoEncerramento.CONEXAO_PERDIDA)
                } else {
                    atual
                }
            EventoCentral.RedeDisponivel, EventoCentral.EspelharTocado, EventoCentral.TentarDeNovoTocado ->
                if (atual is EstadoCentral.Encerrado) EstadoCentral.AguardandoCelular else atual
        }
    }

    private fun deRemoto(estado: EstadoRemoto): EstadoCentral = when (estado) {
        EstadoRemoto.AGUARDANDO_CONFIRMACAO -> EstadoCentral.AguardandoConfirmacao
        EstadoRemoto.ESPELHANDO -> EstadoCentral.Espelhando
        EstadoRemoto.DESBLOQUEIE -> EstadoCentral.DesbloqueieCelular
        EstadoRemoto.EM_USO_POR_OUTRA_CENTRAL -> EstadoCentral.EmUsoPorOutraCentral
    }
}
