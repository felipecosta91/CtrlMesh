package com.ctrlmesh.celular

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FilaERedeTest {

    private val roteador = listOf(Subrede("192.168.43.1", 24))

    @Test
    fun `CA-073 primeira central espelha e a segunda fica em espera`() {
        val fila = FilaDeCentrais()
        assertTrue(fila.chegou(1))
        assertFalse(fila.chegou(2))
        assertEquals(1L, fila.ativa)
    }

    @Test
    fun `CA-073 quando a primeira sai a segunda espelha sozinha`() {
        val fila = FilaDeCentrais()
        fila.chegou(1)
        fila.chegou(2)
        fila.chegou(3)

        assertEquals(2L, fila.saiu(1))
        assertEquals(2L, fila.ativa)
        assertEquals(3L, fila.saiu(2))
        assertNull(fila.saiu(3))
        assertNull(fila.ativa)
    }

    @Test
    fun `CA-073 central em espera que sai nao muda a ativa`() {
        val fila = FilaDeCentrais()
        fila.chegou(1)
        fila.chegou(2)
        assertNull(fila.saiu(2))
        assertEquals(1L, fila.ativa)
        assertNull(fila.saiu(1))
        assertTrue(fila.chegou(4))
    }

    @Test
    fun `CA-075 central no roteador do proprio celular e aceita`() {
        assertTrue(RedeDoRoteador.aceita("192.168.43.20", roteador))
        assertTrue(RedeDoRoteador.aceita("192.168.43.254", roteador))
        assertTrue(RedeDoRoteador.aceita("10.163.85.40", listOf(Subrede("10.163.85.12", 24))))
    }

    @Test
    fun `CA-075 central em outra rede do celular, como o Wi-Fi de casa, e recusada`() {
        assertFalse(RedeDoRoteador.aceita("192.168.1.50", roteador))
        assertFalse(RedeDoRoteador.aceita("192.168.44.20", roteador))
        assertFalse(RedeDoRoteador.aceita("192.168.1.50", emptyList()))
    }

    @Test
    fun `CA-075 loopback, endereco invalido e IPv6 sao recusados`() {
        assertFalse(RedeDoRoteador.aceita("127.0.0.1", roteador))
        assertFalse(RedeDoRoteador.aceita("127.0.0.1", listOf(Subrede("127.0.0.1", 8))))
        assertFalse(RedeDoRoteador.aceita("nao-e-ip", roteador))
        assertFalse(RedeDoRoteador.aceita("fe80::1", roteador))
    }
}
