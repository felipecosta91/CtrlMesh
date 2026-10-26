package com.ctrlmesh.central

import com.ctrlmesh.MapeadorDeToque
import com.ctrlmesh.protocolo.AcaoToque
import com.ctrlmesh.protocolo.AppDoCelular
import com.ctrlmesh.protocolo.Mensagem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class InicioETelaVirtualTest {

    private fun app(rotulo: String) = AppDoCelular("pkg.${rotulo.length}.$rotulo".filter { it.isLetterOrDigit() || it == '.' }, rotulo, ByteArray(0))

    @Test
    fun `CA-097 inicio do CtrlMesh em ordem alfabetica, sem diferenciar maiusculas e acentos`() {
        val apps = listOf("WhatsApp", "Ágora", "agenda", "Spotify", "Câmera", "calculadora").map(::app)
        assertEquals(
            listOf("agenda", "Ágora", "calculadora", "Câmera", "Spotify", "WhatsApp"),
            InicioDoCtrlMesh.ordenar(apps).map { it.rotulo },
        )
    }

    @Test
    fun `CA-096 CA-112 na tela virtual a imagem ocupa toda a area acima da barra, sem faixas`() {
        val layout = Layout.calcularTelaVirtual(1280, 720, 120)
        assertEquals(Retangulo(0, 600, 1280, 720), layout.barra)
        assertEquals(Retangulo(0, 0, 1280, 600), layout.imagem)
    }

    @Test
    fun `CA-096 toque em qualquer ponto da imagem vai para a tela virtual, sem faixas`() {
        val layout = Layout.calcularTelaVirtual(1280, 720, 120)
        listOf(0f to 0f, 1279f to 599f, 640f to 300f, 0f to 599f, 1279f to 0f).forEach { (x, y) ->
            assertNotNull("($x,$y) está na imagem", MapeadorDeToque.naCentral(layout, x, y))
        }
        assertNull(MapeadorDeToque.naCentral(layout, 640f, 660f))

        val centro = MapeadorDeToque.naCentral(layout, 640f, 300f)!!
        assertEquals(0.5f, centro.x, 0.01f)
        assertEquals(0.5f, centro.y, 0.01f)
    }

    @Test
    fun `CA-096 toque normalizado vira pixel da tela virtual`() {
        val toque = Mensagem.Toque(0, AcaoToque.DESCE, 0.25f, 0.5f, 1160, 720, 0)
        val pixel = MapeadorDeToque.noCelular(toque, 1160, 720)!!
        assertEquals(290f, pixel.first, 1f)
        assertEquals(360f, pixel.second, 1f)
    }
}
