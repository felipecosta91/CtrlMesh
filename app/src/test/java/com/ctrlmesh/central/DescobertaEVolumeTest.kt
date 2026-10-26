package com.ctrlmesh.central

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DescobertaEVolumeTest {

    @Test
    fun `CA-078 sem endereco informado a central procura o roteador da rede`() {
        assertEquals(listOf("192.168.43.1"), Descoberta.alvos(enderecoInformado = null, gateway = "192.168.43.1"))
        assertEquals(listOf("192.168.43.1"), Descoberta.alvos(enderecoInformado = "  ", gateway = "192.168.43.1"))
    }

    @Test
    fun `CA-016 endereco informado e tentado em toda conexao, antes do roteador`() {
        assertEquals(listOf("10.163.85.12", "10.0.2.2"), Descoberta.alvos("10.163.85.12", "10.0.2.2"))
        assertEquals(listOf("10.163.85.12"), Descoberta.alvos("10.163.85.12", null))
    }

    @Test
    fun `CA-082 com endereco informado a central espelha esse celular primeiro`() {
        assertEquals("192.168.43.77", Descoberta.alvos("192.168.43.77", "192.168.43.1").first())
        assertEquals(listOf("192.168.43.1"), Descoberta.alvos("192.168.43.1", "192.168.43.1"))
    }

    @Test
    fun `CA-078 sem rede e sem endereco nao ha tentativa`() {
        assertTrue(Descoberta.alvos(null, null).isEmpty())
    }

    @Test
    fun `CA-062 menos e mais mudam um nivel, dentro dos limites`() {
        val volume = VolumeCentral(niveis = 10)
        assertEquals(10, volume.nivel)
        assertEquals(1f, volume.ganho, 0.001f)

        volume.mais()
        assertEquals(10, volume.nivel)
        volume.menos()
        assertEquals(9, volume.nivel)
        assertEquals(0.9f, volume.ganho, 0.001f)
        repeat(20) { volume.menos() }
        assertEquals(0, volume.nivel)
        volume.mais()
        assertEquals(1, volume.nivel)
    }

    @Test
    fun `CA-062 mudo silencia e volta no nivel anterior`() {
        val volume = VolumeCentral(niveis = 10)
        repeat(3) { volume.menos() }
        volume.alternarMudo()
        assertTrue(volume.mudo)
        assertEquals(0f, volume.ganho, 0.0f)

        volume.alternarMudo()
        assertFalse(volume.mudo)
        assertEquals(7, volume.nivel)
        assertEquals(0.7f, volume.ganho, 0.001f)
    }
}
