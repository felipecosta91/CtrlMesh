package com.ctrlmesh.protocolo

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TelaVirtualProtocoloTest {

    private fun paraCelular(m: Mensagem) = Protocolo.decodificar(Protocolo.codificar(m), Papel.CELULAR)
    private fun paraCentral(m: Mensagem) = Protocolo.decodificar(Protocolo.codificar(m), Papel.MULTIMIDIA)

    private fun assertRejeita(m: Mensagem, receptor: Papel) {
        try {
            Protocolo.decodificar(Protocolo.codificar(m), receptor)
            fail("deveria lançar ProtocoloException")
        } catch (_: ProtocoloException) {
        }
    }

    @Test
    fun `CA-095 central recebe o modo do espelhamento`() {
        ModoEspelhamento.entries.forEach { assertEquals(Mensagem.Modo(it), paraCentral(Mensagem.Modo(it))) }
        assertArrayEquals(byteArrayOf(0x04, 1), Protocolo.codificar(Mensagem.Modo(ModoEspelhamento.TELA_VIRTUAL)))
        assertArrayEquals(byteArrayOf(0x04, 2), Protocolo.codificar(Mensagem.Modo(ModoEspelhamento.TELA_FISICA)))
    }

    @Test
    fun `CA-095 aviso de plano B e de ligacao no celular`() {
        assertEquals(11, Aviso.MODO_TELA_DO_CELULAR.bit)
        assertEquals("modo tela do celular — ative a tela horizontal no celular", Aviso.MODO_TELA_DO_CELULAR.texto)
        assertEquals(12, Aviso.LIGACAO_NO_CELULAR.bit)
        val situacao = Mensagem.Situacao(setOf(Aviso.MODO_TELA_DO_CELULAR))
        assertEquals(situacao, paraCentral(situacao))
        assertArrayEquals(byteArrayOf(0x03, 0, 0, 0x08, 0), Protocolo.codificar(situacao))
    }

    @Test
    fun `CA-103 aviso de ligacao no modo tela virtual`() {
        assertEquals("ligação em andamento — atenda no celular", Aviso.LIGACAO_NO_CELULAR.texto)
        assertEquals(Mensagem.Situacao(setOf(Aviso.LIGACAO_NO_CELULAR)), paraCentral(Mensagem.Situacao(setOf(Aviso.LIGACAO_NO_CELULAR))))
    }

    @Test
    fun `CA-097 lista de apps chega a central com pacote, rotulo e icone`() {
        val icone = ByteArray(4_000) { (it % 255).toByte() }
        val apps = listOf(
            AppDoCelular("com.spotify.music", "Spotify", icone),
            AppDoCelular("com.whatsapp", "WhatsApp", byteArrayOf(1, 2, 3)),
            AppDoCelular("br.com.ação", "Ação & Câmera", ByteArray(0)),
        )
        val recebida = paraCentral(Mensagem.ListaDeApps(apps)) as Mensagem.ListaDeApps
        assertEquals(apps.map { it.pacote }, recebida.apps.map { it.pacote })
        assertEquals(apps.map { it.rotulo }, recebida.apps.map { it.rotulo })
        apps.indices.forEach { assertArrayEquals(apps[it].iconePng, recebida.apps[it].iconePng) }
        assertEquals(Mensagem.ListaDeApps(emptyList()), paraCentral(Mensagem.ListaDeApps(emptyList())))
    }

    @Test
    fun `CA-098 abrir app chega ao celular com o pacote`() {
        assertEquals(Mensagem.AbrirApp("com.spotify.music"), paraCelular(Mensagem.AbrirApp("com.spotify.music")))
        val bytes = Protocolo.codificar(Mensagem.AbrirApp("a.b"))
        assertArrayEquals(byteArrayOf(0x41, 0, 3, 'a'.code.toByte(), '.'.code.toByte(), 'b'.code.toByte()), bytes)
    }

    @Test
    fun `CA-099 tela virtual vazia avisa a central para mostrar o inicio`() {
        assertEquals(Mensagem.TelaVirtualVazia, paraCentral(Mensagem.TelaVirtualVazia))
        assertArrayEquals(byteArrayOf(0x42), Protocolo.codificar(Mensagem.TelaVirtualVazia))
    }

    @Test
    fun `CA-096 central informa nova area da imagem`() {
        assertEquals(Mensagem.Area(1160, 720), paraCelular(Mensagem.Area(1160, 720)))
        assertArrayEquals(byteArrayOf(0x05, 0x04, 0x88.toByte(), 0x02, 0xD0.toByte()), Protocolo.codificar(Mensagem.Area(1160, 720)))
    }

    @Test
    fun `CA-071 mensagens novas so valem no sentido do contrato`() {
        assertRejeita(Mensagem.Modo(ModoEspelhamento.TELA_VIRTUAL), Papel.CELULAR)
        assertRejeita(Mensagem.ListaDeApps(emptyList()), Papel.CELULAR)
        assertRejeita(Mensagem.TelaVirtualVazia, Papel.CELULAR)
        assertRejeita(Mensagem.AbrirApp("x"), Papel.MULTIMIDIA)
        assertRejeita(Mensagem.Area(10, 10), Papel.MULTIMIDIA)
    }

    @Test
    fun `RNF-006 lista de apps truncada e rejeitada`() {
        val bytes = Protocolo.codificar(Mensagem.ListaDeApps(listOf(AppDoCelular("a", "A", ByteArray(10)))))
        try {
            Protocolo.decodificar(bytes.copyOf(bytes.size - 3), Papel.MULTIMIDIA)
            fail("deveria lançar ProtocoloException")
        } catch (_: ProtocoloException) {
        }
        assertTrue(bytes[0] == 0x40.toByte())
    }

    @Test
    fun `CA-096 tela virtual horizontal com lado maior de 1280 em area grande`() {
        assertEquals(1280 to 720, TamanhoDaTelaVirtual.calcular(1280, 720))
        assertEquals(1280 to 720, TamanhoDaTelaVirtual.calcular(2560, 1440))
        val (l, a) = TamanhoDaTelaVirtual.calcular(2440, 1600)
        assertEquals(1280, l)
        assertTrue(a in 832..840 && a % 8 == 0)
    }

    @Test
    fun `CA-096 area menor que 1280 - tela virtual do tamanho da area`() {
        assertEquals(1160 to 720, TamanhoDaTelaVirtual.calcular(1160, 720))
        assertEquals(904 to 600, TamanhoDaTelaVirtual.calcular(904, 600))
        val (l, a) = TamanhoDaTelaVirtual.calcular(1021, 597)
        assertTrue(l <= 1021 && a <= 597 && l % 8 == 0 && a % 8 == 0 && l > a)
        assertTrue(l >= 1021 - 8 && a >= 597 - 8)
    }

    @Test
    fun `CA-096 densidade definida pelo tamanho da tela virtual`() {
        assertEquals(213, TamanhoDaTelaVirtual.densidade(1280))
        assertEquals(160, TamanhoDaTelaVirtual.densidade(960))
        assertEquals(120, TamanhoDaTelaVirtual.densidade(400))
    }
}
