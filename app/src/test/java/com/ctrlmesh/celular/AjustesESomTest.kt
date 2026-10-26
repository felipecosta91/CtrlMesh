package com.ctrlmesh.celular

import com.ctrlmesh.protocolo.Aviso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AjustesESomTest {

    private class AjustesFalsos : AjustesDoCelular {
        override var rotacao = 0
        override var giroAutomatico = false
        override var brilho = 178
        override var brilhoAutomatico = false
        override var tempoDeBloqueioMs = 30_000
        override var volumeMidia = 9
        override var volumeToque = 12
        override var volumeAlarme = 12
    }

    private class ArmazenamentoEmMemoria : ArmazenamentoDeRestauracao {
        var salvo: AjustesSalvos? = null
        override fun salvar(ajustes: AjustesSalvos) { salvo = ajustes }
        override fun ler() = salvo
        override fun apagar() { salvo = null }
    }

    private val ajustes = AjustesFalsos()
    private val armazenamento = ArmazenamentoEmMemoria()
    private val restauracao = RestauracaoDoCelular(ajustes, armazenamento)

    private fun estaEmPaisagem() = !ajustes.giroAutomatico && (ajustes.rotacao == 1 || ajustes.rotacao == 3)

    @Test
    fun `CA-026 com autorizacao de girar o celular vai para paisagem ao comecar`() {
        restauracao.aplicar(podeGirar = true, silenciarMidia = true)
        assertTrue(estaEmPaisagem())
    }

    @Test
    fun `CA-029 sem autorizacao de girar a orientacao do celular nao muda`() {
        ajustes.giroAutomatico = true
        restauracao.aplicar(podeGirar = false, silenciarMidia = true)
        assertEquals("os demais ajustes são aplicados", 0, ajustes.volumeMidia)
        assertEquals(0, ajustes.rotacao)
        assertTrue(ajustes.giroAutomatico)
    }

    @Test
    fun `CA-028 ao encerrar volta a retrato com giro automatico desligado`() {
        restauracao.aplicar(podeGirar = true, silenciarMidia = true)
        assertTrue(estaEmPaisagem())
        restauracao.restaurar()
        assertEquals(0, ajustes.rotacao)
        assertFalse(ajustes.giroAutomatico)
    }

    @Test
    fun `CA-028 giro automatico ligado antes volta ligado`() {
        ajustes.giroAutomatico = true
        restauracao.aplicar(podeGirar = true, silenciarMidia = true)
        assertFalse(ajustes.giroAutomatico)
        restauracao.restaurar()
        assertTrue(ajustes.giroAutomatico)
    }

    @Test
    fun `CA-028 app fechado durante o espelhamento - orientacao volta quando o CtrlMesh executa de novo`() {
        restauracao.aplicar(podeGirar = true, silenciarMidia = true)
        assertTrue(estaEmPaisagem())

        val novoProcesso = RestauracaoDoCelular(ajustes, armazenamento)
        novoProcesso.restaurar()

        assertEquals(0, ajustes.rotacao)
        assertFalse(ajustes.giroAutomatico)
        assertNull(armazenamento.ler())
    }

    @Test
    fun `CA-111 brilho do dono continua em 70 por cento e bloqueio volta a 30 s ao encerrar`() {
        restauracao.aplicar(podeGirar = true, silenciarMidia = true)
        assertEquals("o CtrlMesh nunca muda o brilho", 178, ajustes.brilho)
        assertTrue("os demais ajustes são aplicados", estaEmPaisagem())

        restauracao.restaurar()
        assertEquals(178, ajustes.brilho)
        assertEquals(30_000, ajustes.tempoDeBloqueioMs)
    }

    @Test
    fun `CA-111 brilho automatico do dono nao e desligado`() {
        ajustes.brilhoAutomatico = true
        restauracao.aplicar(podeGirar = true, silenciarMidia = true)
        assertTrue(ajustes.brilhoAutomatico)
        assertEquals(0, ajustes.volumeMidia)
        restauracao.restaurar()
        assertTrue(ajustes.brilhoAutomatico)
    }

    @Test
    fun `CA-057 so o volume de midia e silenciado e volta a 60 por cento`() {
        restauracao.aplicar(podeGirar = true, silenciarMidia = true)
        assertEquals(0, ajustes.volumeMidia)
        assertEquals(12, ajustes.volumeToque)
        assertEquals(12, ajustes.volumeAlarme)

        restauracao.restaurar()
        assertEquals(9, ajustes.volumeMidia)
    }

    @Test
    fun `CA-057 app fechado durante o espelhamento - volume volta quando o CtrlMesh executa de novo`() {
        restauracao.aplicar(podeGirar = true, silenciarMidia = true)
        assertEquals(0, ajustes.volumeMidia)
        RestauracaoDoCelular(ajustes, armazenamento).restaurar()
        assertEquals(9, ajustes.volumeMidia)
        assertEquals(178, ajustes.brilho)
    }

    @Test
    fun `CA-050 aplicar de novo numa retomada nao perde os valores originais`() {
        restauracao.aplicar(podeGirar = true, silenciarMidia = true)
        restauracao.aplicar(podeGirar = true, silenciarMidia = true)
        RestauracaoDoCelular(ajustes, armazenamento).aplicar(podeGirar = true, silenciarMidia = true)
        assertEquals(0, ajustes.volumeMidia)
        assertTrue(estaEmPaisagem())

        restauracao.restaurar()
        assertEquals(9, ajustes.volumeMidia)
        assertEquals(178, ajustes.brilho)
        assertEquals(0, ajustes.rotacao)
    }

    @Test
    fun `CA-092 restaurar sem espelhamento anterior nao mexe em nada`() {
        ajustes.volumeMidia = 4
        ajustes.brilho = 90
        restauracao.restaurar()
        assertEquals(4, ajustes.volumeMidia)
        assertEquals(90, ajustes.brilho)
    }

    @Test
    fun `CA-059 sem autorizacao de audio o volume do celular nao muda`() {
        val plano = PlanoDeAudio.decidir(podeGravarAudio = false, silenciarZeraSomEnviado = false)
        restauracao.aplicar(podeGirar = true, silenciarMidia = plano.silenciarCelular)
        assertEquals(9, ajustes.volumeMidia)
    }

    @Test
    fun `CA-056 com autorizacao de audio o som vai para a central e o celular e silenciado`() {
        val plano = PlanoDeAudio.decidir(podeGravarAudio = true, silenciarZeraSomEnviado = false)
        assertTrue(plano.enviarSom)
        assertTrue(plano.silenciarCelular)
        assertTrue(plano.avisos.isEmpty())
    }

    @Test
    fun `CA-059 sem autorizacao de audio, sem som, sem silenciar e com aviso`() {
        val plano = PlanoDeAudio.decidir(podeGravarAudio = false, silenciarZeraSomEnviado = false)
        assertFalse(plano.enviarSom)
        assertFalse(plano.silenciarCelular)
        assertEquals(setOf(Aviso.FALTA_GRAVAR_AUDIO), plano.avisos)
        assertEquals("sem som: autorização de áudio não concedida no celular", Aviso.FALTA_GRAVAR_AUDIO.texto)
    }

    @Test
    fun `CA-093 celular que zera o som enviado ao silenciar nao e silenciado e a central avisa`() {
        val plano = PlanoDeAudio.decidir(podeGravarAudio = true, silenciarZeraSomEnviado = true)
        assertTrue(plano.enviarSom)
        assertFalse(plano.silenciarCelular)
        assertEquals(setOf(Aviso.NAO_SILENCIA), plano.avisos)
        assertEquals("este celular não permite silenciar sem cortar o som da central", Aviso.NAO_SILENCIA.texto)
    }
}
