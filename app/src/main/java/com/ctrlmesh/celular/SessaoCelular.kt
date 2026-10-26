package com.ctrlmesh.celular

import com.ctrlmesh.protocolo.EstadoRemoto
import com.ctrlmesh.protocolo.Mensagem
import com.ctrlmesh.protocolo.MotivoEncerramento

sealed interface EstadoCelular {
    data object Escuta : EstadoCelular
    data class AguardandoConfirmacao(val desdeMs: Long) : EstadoCelular
    data object Espelhando : EstadoCelular
    data object Bloqueado : EstadoCelular
    data class Reconectando(val desdeMs: Long) : EstadoCelular
    data class Encerrado(val motivo: MotivoEncerramento) : EstadoCelular
}

sealed interface EventoCelular {
    data object CentralConectou : EventoCelular
    data object PermitirTocado : EventoCelular
    data object CelularBloqueado : EventoCelular
    data object CelularDesbloqueado : EventoCelular
    data object Queda : EventoCelular
    data object CentralFechouConexao : EventoCelular
    data object EncerrarTocado : EventoCelular
    data class Recebeu(val mensagem: Mensagem) : EventoCelular
    data class Relogio(val agoraMs: Long) : EventoCelular
}

data class SessaoCelular(
    val estado: EstadoCelular,
    val capturaViva: Boolean,
    val liberacaoPermanente: Boolean,
    val versaoAndroid: Int,
    /** Modo tela virtual (contrato §10.3): sem "Permitir", bloqueio não interrompe, nada é ajustado no celular. */
    val telaVirtual: Boolean = false,
) {
    private val podeCapturar get() = telaVirtual || liberacaoPermanente || capturaViva

    private val ativo: Boolean
        get() = estado is EstadoCelular.AguardandoConfirmacao || estado == EstadoCelular.Espelhando ||
            estado == EstadoCelular.Bloqueado || estado is EstadoCelular.Reconectando

    fun reduzir(evento: EventoCelular, agoraMs: Long): SessaoCelular {
        val e = estado
        return when (evento) {
            EventoCelular.CentralConectou -> when (e) {
                EstadoCelular.Escuta, is EstadoCelular.Encerrado -> iniciarCaptura(agoraMs)
                is EstadoCelular.Reconectando -> copy(estado = EstadoCelular.Espelhando)
                else -> this
            }
            EventoCelular.PermitirTocado ->
                if (e is EstadoCelular.AguardandoConfirmacao) copy(estado = EstadoCelular.Espelhando, capturaViva = true) else this
            EventoCelular.CelularBloqueado ->
                if (e == EstadoCelular.Espelhando && !telaVirtual) {
                    copy(estado = EstadoCelular.Bloqueado, capturaViva = capturaViva && (liberacaoPermanente || versaoAndroid < 35))
                } else {
                    this
                }
            EventoCelular.CelularDesbloqueado -> if (e == EstadoCelular.Bloqueado && !telaVirtual) iniciarCaptura(agoraMs) else this
            EventoCelular.Queda ->
                if (e is EstadoCelular.AguardandoConfirmacao || e == EstadoCelular.Espelhando || e == EstadoCelular.Bloqueado) {
                    copy(estado = EstadoCelular.Reconectando(agoraMs))
                } else {
                    this
                }
            is EventoCelular.Relogio -> when {
                e is EstadoCelular.AguardandoConfirmacao && evento.agoraMs - e.desdeMs >= PRAZO_CONFIRMACAO_MS ->
                    encerrar(MotivoEncerramento.NAO_CONFIRMADO_NO_CELULAR)
                e is EstadoCelular.Reconectando && evento.agoraMs - e.desdeMs >= JANELA_DE_RECONEXAO_MS ->
                    encerrar(MotivoEncerramento.CONEXAO_PERDIDA)
                else -> this
            }
            EventoCelular.EncerrarTocado -> if (ativo) encerrar(MotivoEncerramento.ENCERRADO_PELO_CELULAR) else this
            EventoCelular.CentralFechouConexao -> if (ativo) encerrar(MotivoEncerramento.APP_FECHADO_OU_DESLIGADO) else this
            is EventoCelular.Recebeu -> {
                val m = evento.mensagem
                if (ativo && m is Mensagem.Encerrar) encerrar(m.motivo) else this
            }
        }
    }

    private fun iniciarCaptura(agoraMs: Long) =
        copy(estado = if (podeCapturar) EstadoCelular.Espelhando else EstadoCelular.AguardandoConfirmacao(agoraMs))

    private fun encerrar(motivo: MotivoEncerramento) = copy(estado = EstadoCelular.Encerrado(motivo))

    val estadoRemoto: EstadoRemoto?
        get() = when (estado) {
            is EstadoCelular.AguardandoConfirmacao -> EstadoRemoto.AGUARDANDO_CONFIRMACAO
            EstadoCelular.Espelhando -> EstadoRemoto.ESPELHANDO
            EstadoCelular.Bloqueado -> EstadoRemoto.DESBLOQUEIE
            else -> null
        }

    val mostraIndicador: Boolean
        get() = estado == EstadoCelular.Espelhando || estado == EstadoCelular.Bloqueado || estado is EstadoCelular.Reconectando

    val ajustesAplicados: Boolean get() = mostraIndicador && !telaVirtual
    val enviaSom: Boolean get() = estado == EstadoCelular.Espelhando
    val pedePermitir: Boolean get() = estado is EstadoCelular.AguardandoConfirmacao

    companion object {
        const val PRAZO_CONFIRMACAO_MS = 30_000L
        const val JANELA_DE_RECONEXAO_MS = 60_000L

        fun inicial(liberacaoPermanente: Boolean, versaoAndroid: Int, telaVirtual: Boolean = false) =
            SessaoCelular(EstadoCelular.Escuta, false, liberacaoPermanente, versaoAndroid, telaVirtual)
    }
}
