package com.ctrlmesh.protocolo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RedeDegradadaTest {

    @Test
    fun `CA-065 com 1 Mbps o som mantem a qualidade e so o video cai`() {
        val folgada = PoliticaDeBanda.alocar(20_000)
        val limitada = PoliticaDeBanda.alocar(1_000)

        assertEquals(folgada.audioKbps, limitada.audioKbps)
        assertTrue(limitada.audioKbps > 0)
        assertTrue(limitada.videoKbps < folgada.videoKbps)
        assertTrue(limitada.videoKbps > 0)
        assertTrue(limitada.audioKbps + limitada.videoKbps <= 1_000)
    }

    @Test
    fun `CA-052 quanto menor a banda, menor o video, sem passar da banda`() {
        val bandas = listOf(20_000, 8_000, 4_000, 2_000, 1_000)
        val videos = bandas.map { PoliticaDeBanda.alocar(it).videoKbps }
        assertEquals(videos.sortedDescending(), videos)
        bandas.forEach { banda ->
            val alocacao = PoliticaDeBanda.alocar(banda)
            assertTrue(alocacao.audioKbps + alocacao.videoKbps <= banda)
        }
    }

    @Test
    fun `CA-065 fila cheia descarta video e nunca som`() {
        val fila = FilaDeEnvio(limiteBytes = 100_000)
        val sons = (0 until 50).map { Mensagem.AudioQuadro(it * 20_000L, ByteArray(300)) }
        sons.forEachIndexed { i, som ->
            fila.ofertar(Mensagem.VideoQuadro(i * 33_333L, chave = i == 0, dados = ByteArray(20_000)))
            fila.ofertar(som)
        }

        val saida = generateSequence { fila.proxima() }.toList()

        val sonsEntregues = saida.filterIsInstance<Mensagem.AudioQuadro>()
        assertEquals(sons.map { it.ptsUs }, sonsEntregues.map { it.ptsUs })
        assertTrue(saida.filterIsInstance<Mensagem.VideoQuadro>().size < 50)
    }

    @Test
    fun `CA-065 depois de descartar video, o proximo video entregue e quadro-chave`() {
        val fila = FilaDeEnvio(limiteBytes = 50_000)
        fila.ofertar(Mensagem.VideoQuadro(0, chave = true, dados = ByteArray(20_000)))
        (1..10).forEach { fila.ofertar(Mensagem.VideoQuadro(it * 33_333L, chave = false, dados = ByteArray(20_000))) }
        fila.ofertar(Mensagem.VideoQuadro(11 * 33_333L, chave = true, dados = ByteArray(20_000)))
        fila.ofertar(Mensagem.VideoQuadro(12 * 33_333L, chave = false, dados = ByteArray(20_000)))

        val videos = generateSequence { fila.proxima() }.filterIsInstance<Mensagem.VideoQuadro>().toList()

        assertTrue(videos.size < 13)
        var precisaChave = false
        var anterior = -33_333L
        videos.forEach { quadro ->
            val pulou = quadro.ptsUs != anterior + 33_333L
            if (pulou) precisaChave = true
            if (precisaChave) {
                assertTrue("após descarte, o primeiro vídeo precisa ser quadro-chave", quadro.chave)
                precisaChave = false
            }
            anterior = quadro.ptsUs
        }
    }

    @Test
    fun `CA-052 mensagens de controle saem antes de som e video`() {
        val fila = FilaDeEnvio(limiteBytes = 1_000_000)
        fila.ofertar(Mensagem.VideoQuadro(0, true, ByteArray(1_000)))
        fila.ofertar(Mensagem.AudioQuadro(0, ByteArray(100)))
        fila.ofertar(Mensagem.Estado(EstadoRemoto.DESBLOQUEIE))

        assertTrue(fila.proxima() is Mensagem.Estado)
        assertTrue(fila.proxima() is Mensagem.AudioQuadro)
        assertTrue(fila.proxima() is Mensagem.VideoQuadro)
        assertEquals(null, fila.proxima())
    }

    @Test
    fun `CA-049 queda e detectada em ate 5 segundos sem dados e nao antes de 1 segundo`() {
        val detector = DetectorDeQueda(inicioMs = 0)
        assertFalse(detector.caiu(1_000))
        detector.recebeu(1_000)
        assertFalse(detector.caiu(2_000))
        assertFalse(detector.caiu(3_500))
        assertTrue(detector.caiu(6_000))
    }

    @Test
    fun `CA-050 qualquer quadro recebido renova o prazo`() {
        val detector = DetectorDeQueda(inicioMs = 0)
        (1..20).forEach { segundo -> detector.recebeu(segundo * 1_000L) }
        assertFalse(detector.caiu(21_000))
    }
}
