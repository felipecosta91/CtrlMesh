package com.ctrlmesh.protocolo

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ProtocoloTest {

    private fun paraCelular(m: Mensagem) = Protocolo.decodificar(Protocolo.codificar(m), Papel.CELULAR)
    private fun paraCentral(m: Mensagem) = Protocolo.decodificar(Protocolo.codificar(m), Papel.MULTIMIDIA)

    private fun assertRejeita(quadro: ByteArray, receptor: Papel) {
        try {
            Protocolo.decodificar(quadro, receptor)
            fail("deveria lançar ProtocoloException")
        } catch (_: ProtocoloException) {
        }
    }

    @Test
    fun `CA-078 porta e versao fixas do contrato`() {
        assertEquals(47800, Protocolo.PORTA)
        assertEquals(1, Protocolo.VERSAO)
    }

    @Test
    fun `CA-078 central se apresenta com nome e area de imagem`() {
        val ola = Mensagem.Ola("Central do carro", 1160, 720)
        assertEquals(ola, paraCelular(ola))
    }

    @Test
    fun `CA-035 toques de dois dedos chegam ao celular com ponteiro, acao e posicao`() {
        val toques = listOf(
            Mensagem.Toque(0, AcaoToque.DESCE, 0.25f, 0.5f, 2400, 1080, 1_000L),
            Mensagem.Toque(1, AcaoToque.DESCE, 0.75f, 0.5f, 2400, 1080, 1_005L),
            Mensagem.Toque(1, AcaoToque.MOVE, 0.9f, 0.5f, 2400, 1080, 1_020L),
            Mensagem.Toque(0, AcaoToque.SOBE, 0.1f, 0.5f, 2400, 1080, 1_040L),
            Mensagem.Toque(1, AcaoToque.CANCELA, 0.9f, 0.5f, 2400, 1080, 1_041L),
        )
        toques.forEach { assertEquals(it, paraCelular(it)) }
    }

    @Test
    fun `CA-037 Voltar, Inicio e Recentes chegam ao celular como teclas distintas`() {
        TeclaDeSistema.entries.forEach { assertEquals(Mensagem.Tecla(it), paraCelular(Mensagem.Tecla(it))) }
    }

    @Test
    fun `CA-037 bytes das teclas no fio seguem o contrato`() {
        assertArrayEquals(byteArrayOf(0x31, 1), Protocolo.codificar(Mensagem.Tecla(TeclaDeSistema.VOLTAR)))
        assertArrayEquals(byteArrayOf(0x31, 2), Protocolo.codificar(Mensagem.Tecla(TeclaDeSistema.INICIO)))
        assertArrayEquals(byteArrayOf(0x31, 3), Protocolo.codificar(Mensagem.Tecla(TeclaDeSistema.RECENTES)))
    }

    @Test
    fun `CA-092 encerramento leva o motivo ao outro lado nos dois sentidos`() {
        MotivoEncerramento.entries.forEach {
            assertEquals(Mensagem.Encerrar(it), paraCelular(Mensagem.Encerrar(it)))
            assertEquals(Mensagem.Encerrar(it), paraCentral(Mensagem.Encerrar(it)))
        }
        assertArrayEquals(byteArrayOf(0x50, 1), Protocolo.codificar(Mensagem.Encerrar(MotivoEncerramento.ENCERRADO_PELO_CELULAR)))
        assertArrayEquals(byteArrayOf(0x50, 5), Protocolo.codificar(Mensagem.Encerrar(MotivoEncerramento.NAO_CONFIRMADO_NO_CELULAR)))
    }

    @Test
    fun `CA-092 textos dos motivos de encerramento, sem pareamento`() {
        assertEquals("encerrado pelo celular", MotivoEncerramento.ENCERRADO_PELO_CELULAR.texto)
        assertEquals("encerrado pela central", MotivoEncerramento.ENCERRADO_PELA_CENTRAL.texto)
        assertEquals("conexão perdida", MotivoEncerramento.CONEXAO_PERDIDA.texto)
        assertEquals("app fechado ou aparelho desligado", MotivoEncerramento.APP_FECHADO_OU_DESLIGADO.texto)
        assertEquals("não confirmado no celular", MotivoEncerramento.NAO_CONFIRMADO_NO_CELULAR.texto)
        assertTrue(MotivoEncerramento.entries.none { it.texto.contains("pareamento") })
    }

    @Test
    fun `CA-073 central recebe cada estado do celular, inclusive em uso por outra central`() {
        EstadoRemoto.entries.forEach { assertEquals(Mensagem.Estado(it), paraCentral(Mensagem.Estado(it))) }
        assertArrayEquals(byteArrayOf(0x02, 4), Protocolo.codificar(Mensagem.Estado(EstadoRemoto.EM_USO_POR_OUTRA_CENTRAL)))
    }

    @Test
    fun `CA-068 central recebe as autorizacoes que faltam no celular`() {
        val situacao = Mensagem.Situacao(setOf(Aviso.FALTA_GIRAR_TELA, Aviso.FALTA_GRAVAR_AUDIO))
        assertEquals(situacao, paraCentral(situacao))
        assertArrayEquals(byteArrayOf(0x03, 0, 0, 0, 0b1010), Protocolo.codificar(situacao))
    }

    @Test
    fun `CA-068 todos os avisos atravessam a rede e situacao vazia tambem`() {
        assertEquals(Mensagem.Situacao(Aviso.entries.toSet()), paraCentral(Mensagem.Situacao(Aviso.entries.toSet())))
        assertEquals(Mensagem.Situacao(emptySet()), paraCentral(Mensagem.Situacao(emptySet())))
    }

    @Test
    fun `CA-025 video chega a central com dimensoes, codec e quadros intactos`() {
        val sps = byteArrayOf(0, 0, 0, 1, 0x67, 0x42)
        val pps = byteArrayOf(0, 0, 0, 1, 0x68, 0x13)
        val config = paraCentral(Mensagem.VideoConfig(2400, 1080, "video/avc", listOf(sps, pps))) as Mensagem.VideoConfig
        assertEquals(2400, config.largura)
        assertEquals(1080, config.altura)
        assertEquals("video/avc", config.codec)
        assertEquals(2, config.csd.size)
        assertArrayEquals(sps, config.csd[0])
        assertArrayEquals(pps, config.csd[1])

        val dados = ByteArray(200_000) { (it % 251).toByte() }
        val quadro = paraCentral(Mensagem.VideoQuadro(33_333L, true, dados)) as Mensagem.VideoQuadro
        assertEquals(33_333L, quadro.ptsUs)
        assertTrue(quadro.chave)
        assertArrayEquals(dados, quadro.dados)
    }

    @Test
    fun `CA-056 som chega a central com configuracao e quadros intactos`() {
        val csd = listOf(byteArrayOf(1, 2, 3), ByteArray(8), ByteArray(8) { 9 })
        val config = paraCentral(Mensagem.AudioConfig(48_000, 2, "audio/opus", csd)) as Mensagem.AudioConfig
        assertEquals(48_000, config.taxa)
        assertEquals(2, config.canais)
        assertEquals("audio/opus", config.codec)
        csd.indices.forEach { assertArrayEquals(csd[it], config.csd[it]) }

        val dados = ByteArray(320) { it.toByte() }
        val quadro = paraCentral(Mensagem.AudioQuadro(20_000L, dados)) as Mensagem.AudioQuadro
        assertEquals(20_000L, quadro.ptsUs)
        assertArrayEquals(dados, quadro.dados)
    }

    @Test
    fun `CA-049 batimento atravessa nos dois sentidos`() {
        assertEquals(Mensagem.Ping(7), paraCelular(Mensagem.Ping(7)))
        assertEquals(Mensagem.Pong(7), paraCentral(Mensagem.Pong(7)))
    }

    @Test
    fun `CA-071 nao existe sentido inverso - celular rejeita imagem e som, central rejeita toques e teclas`() {
        val dados = byteArrayOf(1)
        assertRejeita(Protocolo.codificar(Mensagem.VideoQuadro(0, true, dados)), Papel.CELULAR)
        assertRejeita(Protocolo.codificar(Mensagem.VideoConfig(10, 10, "video/avc", emptyList())), Papel.CELULAR)
        assertRejeita(Protocolo.codificar(Mensagem.AudioQuadro(0, dados)), Papel.CELULAR)
        assertRejeita(Protocolo.codificar(Mensagem.Estado(EstadoRemoto.ESPELHANDO)), Papel.CELULAR)
        assertRejeita(Protocolo.codificar(Mensagem.Toque(0, AcaoToque.DESCE, 0f, 0f, 10, 10, 0)), Papel.MULTIMIDIA)
        assertRejeita(Protocolo.codificar(Mensagem.Tecla(TeclaDeSistema.INICIO)), Papel.MULTIMIDIA)
        assertRejeita(Protocolo.codificar(Mensagem.Ola("x", 10, 10)), Papel.MULTIMIDIA)
    }

    @Test
    fun `RNF-006 tipo desconhecido e ignorado para compatibilidade futura`() {
        assertEquals(Mensagem.Desconhecida(0x7F), Protocolo.decodificar(byteArrayOf(0x7F, 1, 2, 3), Papel.CELULAR))
    }

    @Test
    fun `RNF-006 quadro truncado, vazio ou com valor fora da tabela e rejeitado`() {
        assertRejeita(ByteArray(0), Papel.CELULAR)
        assertRejeita(byteArrayOf(0x31), Papel.CELULAR)
        assertRejeita(byteArrayOf(0x31, 9), Papel.CELULAR)
        assertRejeita(byteArrayOf(0x50, 0), Papel.MULTIMIDIA)
        assertRejeita(byteArrayOf(0x02, 99), Papel.MULTIMIDIA)
        val toque = Protocolo.codificar(Mensagem.Toque(0, AcaoToque.DESCE, 0.5f, 0.5f, 10, 10, 0))
        assertRejeita(toque.copyOf(toque.size - 1), Papel.CELULAR)
        val config = Protocolo.codificar(Mensagem.VideoConfig(10, 10, "video/avc", listOf(ByteArray(4))))
        assertRejeita(config.copyOf(config.size - 2), Papel.MULTIMIDIA)
    }
}
