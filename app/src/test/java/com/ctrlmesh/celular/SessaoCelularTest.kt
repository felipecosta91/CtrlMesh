package com.ctrlmesh.celular

import com.ctrlmesh.celular.EstadoCelular.AguardandoConfirmacao
import com.ctrlmesh.celular.EstadoCelular.Bloqueado
import com.ctrlmesh.celular.EstadoCelular.Encerrado
import com.ctrlmesh.celular.EstadoCelular.Escuta
import com.ctrlmesh.celular.EstadoCelular.Espelhando
import com.ctrlmesh.celular.EstadoCelular.Reconectando
import com.ctrlmesh.celular.EventoCelular.CelularBloqueado
import com.ctrlmesh.celular.EventoCelular.CelularDesbloqueado
import com.ctrlmesh.celular.EventoCelular.CentralConectou
import com.ctrlmesh.celular.EventoCelular.CentralFechouConexao
import com.ctrlmesh.celular.EventoCelular.EncerrarTocado
import com.ctrlmesh.celular.EventoCelular.PermitirTocado
import com.ctrlmesh.celular.EventoCelular.Queda
import com.ctrlmesh.celular.EventoCelular.Recebeu
import com.ctrlmesh.celular.EventoCelular.Relogio
import com.ctrlmesh.protocolo.EstadoRemoto
import com.ctrlmesh.protocolo.Mensagem
import com.ctrlmesh.protocolo.MotivoEncerramento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessaoCelularTest {

    private fun comLiberacao(versao: Int = 35) = SessaoCelular.inicial(liberacaoPermanente = true, versaoAndroid = versao)
    private fun semLiberacao(versao: Int = 34) = SessaoCelular.inicial(liberacaoPermanente = false, versaoAndroid = versao)
    private fun SessaoCelular.com(evento: EventoCelular, agoraMs: Long = 0) = reduzir(evento, agoraMs)

    @Test
    fun `CA-077 celular comeca em escuta, sem indicador nem ajustes`() {
        val sessao = comLiberacao()
        assertEquals(Escuta, sessao.estado)
        assertFalse(sessao.mostraIndicador)
        assertFalse(sessao.ajustesAplicados)
        assertFalse(sessao.enviaSom)
    }

    @Test
    fun `CA-076 com liberacao permanente o espelhamento comeca sem Permitir`() {
        val sessao = comLiberacao().com(CentralConectou)
        assertEquals(Espelhando, sessao.estado)
        assertFalse(sessao.pedePermitir)
        assertEquals(EstadoRemoto.ESPELHANDO, sessao.estadoRemoto)
        assertTrue(sessao.mostraIndicador)
        assertTrue(sessao.ajustesAplicados)
        assertTrue(sessao.enviaSom)
    }

    @Test
    fun `CA-083 com liberacao permanente, reinicio, fechamento e bloqueio nao pedem Permitir`() {
        val depoisDeReiniciar = comLiberacao().com(CentralConectou)
        assertEquals(Espelhando, depoisDeReiniciar.estado)

        val bloqueadoEDesbloqueado = comLiberacao(versao = 35).com(CentralConectou)
            .com(CelularBloqueado).com(CelularDesbloqueado)
        assertEquals(Espelhando, bloqueadoEDesbloqueado.estado)
        assertFalse(bloqueadoEDesbloqueado.pedePermitir)
    }

    @Test
    fun `CA-084 sem liberacao o primeiro espelhamento pede Permitir e a central e avisada`() {
        val sessao = semLiberacao().com(CentralConectou, agoraMs = 1_000)
        assertEquals(AguardandoConfirmacao(1_000), sessao.estado)
        assertTrue(sessao.pedePermitir)
        assertEquals(EstadoRemoto.AGUARDANDO_CONFIRMACAO, sessao.estadoRemoto)

        val confirmada = sessao.com(PermitirTocado, agoraMs = 3_000)
        assertEquals(Espelhando, confirmada.estado)
        assertTrue(confirmada.capturaViva)
    }

    @Test
    fun `CA-084 depois do Permitir, a central sair da rede por mais de 60 s e voltar nao pede de novo`() {
        val perdida = semLiberacao().com(CentralConectou).com(PermitirTocado)
            .com(Queda, agoraMs = 10_000)
            .com(Relogio(71_000), agoraMs = 71_000)
        assertEquals(Encerrado(MotivoEncerramento.CONEXAO_PERDIDA), perdida.estado)
        assertTrue("captura continua viva entre conexões", perdida.capturaViva)

        val deVolta = perdida.com(CentralConectou, agoraMs = 200_000)
        assertEquals(Espelhando, deVolta.estado)
        assertFalse(deVolta.pedePermitir)
    }

    @Test
    fun `CA-084 depois de reiniciar o celular ou matar o app o Permitir e pedido de novo`() {
        val processoNovo = semLiberacao()
        assertFalse(processoNovo.capturaViva)
        assertEquals(AguardandoConfirmacao(0), processoNovo.com(CentralConectou).estado)
    }

    @Test
    fun `CA-084 no Android 15 ou superior bloquear e desbloquear pede Permitir de novo`() {
        val desbloqueado = semLiberacao(versao = 35).com(CentralConectou).com(PermitirTocado)
            .com(CelularBloqueado, agoraMs = 5_000)
            .com(CelularDesbloqueado, agoraMs = 9_000)
        assertEquals(AguardandoConfirmacao(9_000), desbloqueado.estado)
        assertTrue(desbloqueado.pedePermitir)
    }

    @Test
    fun `CA-084 no Android 14 bloquear e desbloquear nao pede Permitir de novo`() {
        val desbloqueado = semLiberacao(versao = 34).com(CentralConectou).com(PermitirTocado)
            .com(CelularBloqueado).com(CelularDesbloqueado)
        assertEquals(Espelhando, desbloqueado.estado)
    }

    @Test
    fun `CA-088 bloqueado avisa a central para desbloquear e mantem indicador e ajustes`() {
        val bloqueado = comLiberacao().com(CentralConectou).com(CelularBloqueado)
        assertEquals(Bloqueado, bloqueado.estado)
        assertEquals(EstadoRemoto.DESBLOQUEIE, bloqueado.estadoRemoto)
        assertTrue(bloqueado.mostraIndicador)
        assertTrue(bloqueado.ajustesAplicados)
    }

    @Test
    fun `CA-021 sem toque em Permitir por 30 s a tentativa e cancelada como nao confirmada`() {
        val aguardando = semLiberacao().com(CentralConectou, agoraMs = 0)
        assertEquals(AguardandoConfirmacao(0), aguardando.com(Relogio(29_000), agoraMs = 29_000).estado)

        val expirada = aguardando.com(Relogio(30_000), agoraMs = 30_000)
        assertEquals(Encerrado(MotivoEncerramento.NAO_CONFIRMADO_NO_CELULAR), expirada.estado)
        assertFalse(expirada.pedePermitir)
    }

    @Test
    fun `CA-090 enquanto espera Permitir nada de som e celular nao e silenciado`() {
        val aguardando = semLiberacao().com(CentralConectou)
        assertFalse(aguardando.enviaSom)
        assertFalse(aguardando.ajustesAplicados)

        val confirmada = aguardando.com(PermitirTocado)
        assertTrue(confirmada.enviaSom)
        assertTrue(confirmada.ajustesAplicados)
    }

    @Test
    fun `CA-045 indicador continua durante reconectando`() {
        val reconectando = comLiberacao().com(CentralConectou).com(Queda, agoraMs = 1_000)
        assertEquals(Reconectando(1_000), reconectando.estado)
        assertTrue(reconectando.mostraIndicador)
    }

    @Test
    fun `CA-028 durante reconectando o giro e os demais ajustes sao mantidos`() {
        assertTrue(comLiberacao().com(CentralConectou).com(Queda).ajustesAplicados)
    }

    @Test
    fun `CA-064 durante reconectando o celular nao envia som e continua silenciado`() {
        val reconectando = comLiberacao().com(CentralConectou).com(Queda)
        assertFalse(reconectando.enviaSom)
        assertTrue(reconectando.ajustesAplicados)
    }

    @Test
    fun `CA-050 central volta em ate 60 s e o espelhamento continua sem Permitir`() {
        val retomada = semLiberacao().com(CentralConectou).com(PermitirTocado)
            .com(Queda, agoraMs = 0)
            .com(Relogio(59_000), agoraMs = 59_000)
            .com(CentralConectou, agoraMs = 59_500)
        assertEquals(Espelhando, retomada.estado)
        assertFalse(retomada.pedePermitir)
        assertTrue(retomada.enviaSom)
    }

    @Test
    fun `CA-051 60 s sem a central encerra por conexao perdida e desfaz ajustes`() {
        val encerrada = comLiberacao().com(CentralConectou)
            .com(Queda, agoraMs = 0)
            .com(Relogio(60_000), agoraMs = 60_000)
        assertEquals(Encerrado(MotivoEncerramento.CONEXAO_PERDIDA), encerrada.estado)
        assertFalse(encerrada.mostraIndicador)
        assertFalse(encerrada.ajustesAplicados)

        assertEquals(Espelhando, encerrada.com(CentralConectou, agoraMs = 90_000).estado)
    }

    @Test
    fun `CA-092 encerrar no celular corta tudo, some o indicador e desfaz ajustes`() {
        val encerrada = comLiberacao().com(CentralConectou).com(EncerrarTocado)
        assertEquals(Encerrado(MotivoEncerramento.ENCERRADO_PELO_CELULAR), encerrada.estado)
        assertFalse(encerrada.enviaSom)
        assertFalse(encerrada.mostraIndicador)
        assertFalse(encerrada.ajustesAplicados)
    }

    @Test
    fun `CA-092 encerrar na central chega ao celular com o motivo`() {
        val encerrada = comLiberacao().com(CentralConectou)
            .com(Recebeu(Mensagem.Encerrar(MotivoEncerramento.ENCERRADO_PELA_CENTRAL)))
        assertEquals(Encerrado(MotivoEncerramento.ENCERRADO_PELA_CENTRAL), encerrada.estado)
        assertFalse(encerrada.ajustesAplicados)
    }

    @Test
    fun `CA-091 app da central fechado encerra sem reconectando`() {
        val encerrada = comLiberacao().com(CentralConectou).com(CentralFechouConexao)
        assertEquals(Encerrado(MotivoEncerramento.APP_FECHADO_OU_DESLIGADO), encerrada.estado)
    }
}
