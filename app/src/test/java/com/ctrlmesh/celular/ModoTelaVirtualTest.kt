package com.ctrlmesh.celular

import com.ctrlmesh.celular.EstadoCelular.Encerrado
import com.ctrlmesh.celular.EstadoCelular.Espelhando
import com.ctrlmesh.celular.EstadoCelular.Reconectando
import com.ctrlmesh.celular.EventoCelular.CelularBloqueado
import com.ctrlmesh.celular.EventoCelular.CelularDesbloqueado
import com.ctrlmesh.celular.EventoCelular.CentralConectou
import com.ctrlmesh.celular.EventoCelular.EncerrarTocado
import com.ctrlmesh.celular.EventoCelular.Queda
import com.ctrlmesh.celular.EventoCelular.Relogio
import com.ctrlmesh.protocolo.Aviso
import com.ctrlmesh.protocolo.ModoEspelhamento
import com.ctrlmesh.protocolo.MotivoEncerramento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModoTelaVirtualTest {

    private fun virtual() = SessaoCelular.inicial(liberacaoPermanente = false, versaoAndroid = 36, telaVirtual = true)

    @Test
    fun `CA-104 estado do Shizuku com os textos do contrato`() {
        assertEquals(EstadoShizuku.ATIVO, EstadoShizuku.de(binderVivo = true, autorizado = true))
        assertEquals(EstadoShizuku.SEM_AUTORIZACAO, EstadoShizuku.de(binderVivo = true, autorizado = false))
        assertEquals(EstadoShizuku.DESLIGADO, EstadoShizuku.de(binderVivo = false, autorizado = true))
        assertEquals(EstadoShizuku.DESLIGADO, EstadoShizuku.de(binderVivo = false, autorizado = false))
        assertEquals("Shizuku ativo", EstadoShizuku.ATIVO.texto)
        assertEquals("Shizuku desligado", EstadoShizuku.DESLIGADO.texto)
        assertEquals("CtrlMesh sem autorização no Shizuku", EstadoShizuku.SEM_AUTORIZACAO.texto)
    }

    @Test
    fun `CA-095 tabela de decisao do modo`() {
        assertEquals(ModoEspelhamento.TELA_VIRTUAL, EscolhaDeModo.escolher(EstadoShizuku.ATIVO))
        assertEquals(ModoEspelhamento.TELA_FISICA, EscolhaDeModo.escolher(EstadoShizuku.DESLIGADO))
        assertEquals(ModoEspelhamento.TELA_FISICA, EscolhaDeModo.escolher(EstadoShizuku.SEM_AUTORIZACAO))
        assertEquals(emptySet<Aviso>(), EscolhaDeModo.avisos(ModoEspelhamento.TELA_VIRTUAL))
        assertEquals(setOf(Aviso.MODO_TELA_DO_CELULAR), EscolhaDeModo.avisos(ModoEspelhamento.TELA_FISICA))
    }

    @Test
    fun `CA-095 Shizuku ligado no meio do plano B so vale na proxima conexao`() {
        val modo = ModoDaSessao()
        assertNull(modo.atual)
        assertEquals(ModoEspelhamento.TELA_FISICA, modo.iniciar(EstadoShizuku.DESLIGADO))
        assertEquals(ModoEspelhamento.TELA_FISICA, modo.iniciar(EstadoShizuku.ATIVO))
        assertEquals(ModoEspelhamento.TELA_FISICA, modo.atual)

        modo.encerrar()
        assertNull(modo.atual)
        assertEquals(ModoEspelhamento.TELA_VIRTUAL, modo.iniciar(EstadoShizuku.ATIVO))
    }

    @Test
    fun `CA-105 depois de reiniciar sem religar o Shizuku o espelhamento usa o plano B`() {
        assertEquals(ModoEspelhamento.TELA_FISICA, ModoDaSessao().iniciar(EstadoShizuku.DESLIGADO))
    }

    @Test
    fun `CA-101 no modo tela virtual o espelhamento comeca sem Permitir e sem liberacao pelo Mac`() {
        val sessao = virtual().reduzir(CentralConectou, 0)
        assertEquals(Espelhando, sessao.estado)
        assertFalse(sessao.pedePermitir)
        assertTrue(sessao.enviaSom)
    }

    @Test
    fun `CA-100 bloquear o celular nao interrompe o modo tela virtual e nada e ajustado na tela fisica`() {
        val sessao = virtual().reduzir(CentralConectou, 0)
        assertFalse("não gira, não mexe em brilho, bloqueio nem volume", sessao.ajustesAplicados)
        val bloqueado = sessao.reduzir(CelularBloqueado, 1_000)
        assertEquals(Espelhando, bloqueado.estado)
        assertEquals(Espelhando, bloqueado.reduzir(CelularDesbloqueado, 2_000).estado)
    }

    @Test
    fun `CA-107 no modo tela virtual o volume do celular nao e mexido`() {
        assertFalse(virtual().reduzir(CentralConectou, 0).ajustesAplicados)
        assertFalse(virtual().reduzir(CentralConectou, 0).reduzir(Queda, 1).ajustesAplicados)
    }

    @Test
    fun `CA-108 queda mantem a sessao virtual e a retomada em 60 s volta sem Permitir`() {
        val reconectando = virtual().reduzir(CentralConectou, 0).reduzir(Queda, 1_000)
        assertEquals(Reconectando(1_000), reconectando.estado)
        assertTrue(reconectando.mostraIndicador)
        assertEquals(Espelhando, reconectando.reduzir(CentralConectou, 50_000).estado)
        assertEquals(
            Encerrado(MotivoEncerramento.CONEXAO_PERDIDA),
            reconectando.reduzir(Relogio(61_000), 61_000).estado,
        )
    }

    @Test
    fun `CA-109 indicador aparece no modo tela virtual e some ao encerrar`() {
        val sessao = virtual().reduzir(CentralConectou, 0)
        assertTrue(sessao.mostraIndicador)
        assertFalse(sessao.reduzir(EncerrarTocado, 1).mostraIndicador)
    }
}
