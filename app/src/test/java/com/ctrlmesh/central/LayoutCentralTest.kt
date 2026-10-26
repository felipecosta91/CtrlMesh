package com.ctrlmesh.central

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutCentralTest {

    private val barra = 120

    private fun assertImagemInteiraSemDistorcao(layout: LayoutCentral, telaL: Int, telaA: Int, imgL: Int, imgA: Int) {
        val imagem = layout.imagem
        val areaA = telaA - layout.barra.altura
        assertTrue("imagem dentro da área acima da barra", imagem.esquerda >= 0 && imagem.topo >= 0 && imagem.direita <= telaL && imagem.base <= areaA)
        val proporcaoOriginal = imgL.toDouble() / imgA
        val proporcaoExibida = imagem.largura.toDouble() / imagem.altura
        assertTrue("proporção preservada", abs(proporcaoOriginal - proporcaoExibida) / proporcaoOriginal < 0.01)
        assertTrue("imagem encosta na largura ou na altura da área", imagem.largura >= telaL - 1 || imagem.altura >= areaA - 1)
    }

    private fun assertBarraEmbaixoSemCobrirImagem(layout: LayoutCentral, telaL: Int, telaA: Int) {
        assertEquals(Retangulo(0, telaA - barra, telaL, telaA), layout.barra)
        assertTrue("barra não cobre a imagem", layout.imagem.base <= layout.barra.topo)
        assertEquals("imagem logo acima da barra", layout.barra.topo, layout.imagem.base)
    }

    @Test
    fun `CA-112 celular em paisagem - barra embaixo na largura toda e imagem inteira logo acima`() {
        val layout = Layout.calcular(1280, 720, 2400, 1080, barra)
        assertBarraEmbaixoSemCobrirImagem(layout, 1280, 720)
        assertImagemInteiraSemDistorcao(layout, 1280, 720, 2400, 1080)
        assertEquals(0, layout.imagem.esquerda)
        assertEquals(1280, layout.imagem.direita)
    }

    @Test
    fun `CA-027 app em retrato aparece inteiro com faixas dos lados, acima da barra`() {
        val layout = Layout.calcular(1280, 720, 1080, 2400, barra)
        assertBarraEmbaixoSemCobrirImagem(layout, 1280, 720)
        assertImagemInteiraSemDistorcao(layout, 1280, 720, 1080, 2400)
        assertEquals(0, layout.imagem.topo)
        val faixaEsquerda = layout.imagem.esquerda
        val faixaDireita = 1280 - layout.imagem.direita
        assertTrue("faixas dos lados", faixaEsquerda > 0)
        assertTrue("imagem centralizada na largura", abs(faixaEsquerda - faixaDireita) <= 1)
    }

    @Test
    fun `CA-029 sem girar a tela a imagem em retrato aparece inteira com faixas`() {
        listOf(1024 to 600, 1920 to 720).forEach { (l, a) ->
            val layout = Layout.calcular(l, a, 1080, 2340, barra)
            assertImagemInteiraSemDistorcao(layout, l, a, 1080, 2340)
            assertTrue(layout.imagem.esquerda > 0)
            assertTrue(layout.imagem.direita < l)
        }
    }

    @Test
    fun `CA-025 em telas de central comuns a imagem fica inteira, sem corte nem distorcao`() {
        val telas = listOf(1024 to 600, 1280 to 720, 1920 to 720, 2560 to 1600)
        val celulares = listOf(2400 to 1080, 1080 to 2400, 1920 to 1080, 2340 to 1080)
        telas.forEach { (tl, ta) ->
            celulares.forEach { (cl, ca) ->
                val layout = Layout.calcular(tl, ta, cl, ca, barra)
                assertBarraEmbaixoSemCobrirImagem(layout, tl, ta)
                assertImagemInteiraSemDistorcao(layout, tl, ta, cl, ca)
            }
        }
    }

    @Test
    fun `CA-027 ao voltar a um app em paisagem a imagem volta a ocupar a area`() {
        val retrato = Layout.calcular(1920, 720, 1080, 2400, barra)
        val paisagem = Layout.calcular(1920, 720, 2400, 1080, barra)
        assertTrue(paisagem.imagem.largura * paisagem.imagem.altura > retrato.imagem.largura * retrato.imagem.altura)
    }
}
