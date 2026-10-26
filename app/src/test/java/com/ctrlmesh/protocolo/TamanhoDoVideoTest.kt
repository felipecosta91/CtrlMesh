package com.ctrlmesh.protocolo

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// RNF-016 não tem CA próprio: lado maior ≤ 1280 px ou a área da central, se menor; proporção do celular preservada.
class TamanhoDoVideoTest {

    private fun assertRegra(telaL: Int, telaA: Int, areaL: Int, areaA: Int) {
        val (l, a) = TamanhoDoVideo.calcular(telaL, telaA, areaL, areaA)
        val contexto = "tela ${telaL}x$telaA, área ${areaL}x$areaA → ${l}x$a"
        assertTrue("lado maior ≤ 1280: $contexto", max(l, a) <= 1280)
        assertTrue("cabe na área da central: $contexto", l <= areaL && a <= areaA)
        assertTrue("não aumenta a tela do celular: $contexto", l <= telaL && a <= telaA)
        assertTrue("múltiplos de 16 (H.264): $contexto", l % 16 == 0 && a % 16 == 0 && l > 0 && a > 0)
        val proporcao = telaL.toDouble() / telaA
        assertTrue("proporção preservada: $contexto", abs(l.toDouble() / a - proporcao) / proporcao < 0.05)
        val escalaMax = min(1.0, min(1280.0 / max(telaL, telaA), min(areaL.toDouble() / telaL, areaA.toDouble() / telaA)))
        assertTrue("não reduz além do necessário: $contexto", max(l, a) >= max(telaL, telaA) * escalaMax - 16)
    }

    @Test
    fun `RNF-016 central grande - lado maior limitado a 1280 px`() {
        assertEquals(1280 to 576, TamanhoDoVideo.calcular(2400, 1080, 2440, 1372))
        assertRegra(2400, 1080, 2440, 1372)
        assertRegra(1920, 1080, 1920, 1200)
        assertRegra(2340, 1080, 1800, 720)
    }

    @Test
    fun `RNF-016 central menor que 1280 - vale a area da central`() {
        assertRegra(2400, 1080, 1024, 600)
        assertRegra(2400, 1080, 904, 600)
        assertRegra(1080, 2400, 1160, 720)
    }

    @Test
    fun `RNF-016 celular em retrato ou com tela pequena`() {
        assertRegra(1080, 2400, 2440, 1600)
        assertRegra(1280, 720, 1920, 1080)
        assertEquals(1280 to 720, TamanhoDoVideo.calcular(1280, 720, 1920, 1080))
    }
}
