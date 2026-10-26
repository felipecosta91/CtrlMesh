package com.ctrlmesh

import com.ctrlmesh.central.Layout
import com.ctrlmesh.central.PontoNormalizado
import com.ctrlmesh.protocolo.AcaoToque
import com.ctrlmesh.protocolo.Mensagem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MapeadorDeToqueTest {

    private val delta = 0.01f

    private fun assertPonto(esperadoX: Float, esperadoY: Float, ponto: PontoNormalizado?) {
        assertNotNull(ponto)
        assertEquals(esperadoX, ponto!!.x, delta)
        assertEquals(esperadoY, ponto.y, delta)
    }

    private fun toque(x: Float, y: Float, largura: Int, altura: Int) =
        Mensagem.Toque(0, AcaoToque.DESCE, x, y, largura, altura, 0)

    @Test
    fun `CA-035 toque na imagem vai para o ponto correspondente do celular`() {
        val layout = Layout.calcular(1280, 720, 2400, 1080, 120)
        val img = layout.imagem

        assertPonto(0.5f, 0.5f, MapeadorDeToque.naCentral(layout, (img.esquerda + img.direita) / 2f, (img.topo + img.base) / 2f))
        assertPonto(0f, 0f, MapeadorDeToque.naCentral(layout, img.esquerda.toFloat(), img.topo.toFloat()))
        assertPonto(1f, 1f, MapeadorDeToque.naCentral(layout, img.direita - 0.5f, img.base - 0.5f))
        assertPonto(0.25f, 0.75f, MapeadorDeToque.naCentral(layout, img.esquerda + img.largura * 0.25f, img.topo + img.altura * 0.75f))
    }

    @Test
    fun `CA-035 no celular o ponto normalizado vira pixel da tela atual`() {
        val pixel = MapeadorDeToque.noCelular(toque(0.25f, 0.75f, 2400, 1080), 2400, 1080)
        assertNotNull(pixel)
        assertEquals(600f, pixel!!.first, 1f)
        assertEquals(810f, pixel.second, 1f)
    }

    @Test
    fun `CA-036 toques nas faixas e na barra de baixo nao vao para o celular`() {
        val layout = Layout.calcular(1280, 720, 1080, 2400, 120)
        val img = layout.imagem

        assertNull(MapeadorDeToque.naCentral(layout, img.esquerda - 5f, 360f))
        assertNull(MapeadorDeToque.naCentral(layout, img.direita + 5f, 360f))
        assertNull(MapeadorDeToque.naCentral(layout, 1279f, 719f))
        assertNull(MapeadorDeToque.naCentral(layout, 60f, 360f))
        assertNull(MapeadorDeToque.naCentral(layout, 640f, 700f))
    }

    @Test
    fun `CA-027 com app em retrato o toque usa a imagem em retrato`() {
        val layout = Layout.calcular(1280, 720, 1080, 2400, 120)
        val img = layout.imagem
        val ponto = MapeadorDeToque.naCentral(layout, img.esquerda + img.largura / 2f, img.topo + img.altura / 10f)
        assertPonto(0.5f, 0.1f, ponto)

        val pixel = MapeadorDeToque.noCelular(toque(ponto!!.x, ponto.y, 1080, 2400), 1080, 2400)
        assertEquals(540f, pixel!!.first, 2f)
        assertEquals(240f, pixel.second, 3f)
    }

    @Test
    fun `CA-035 toque feito sobre imagem de orientacao antiga nao cai em lugar errado no celular`() {
        assertNull(MapeadorDeToque.noCelular(toque(0.9f, 0.1f, 2400, 1080), 1080, 2400))
        assertNull(MapeadorDeToque.noCelular(toque(0.9f, 0.1f, 1080, 2400), 2400, 1080))
    }
}
