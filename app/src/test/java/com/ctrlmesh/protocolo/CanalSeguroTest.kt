package com.ctrlmesh.protocolo

import java.security.GeneralSecurityException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Test

// RNF-006 não tem CA próprio: toda comunicação cifrada, chave nova por conexão, sem pareamento.
class CanalSeguroTest {

    private class Conexao {
        val parCentral = CanalSeguro.gerarParDeChaves()
        val parCelular = CanalSeguro.gerarParDeChaves()
        val central = CanalSeguro.criar(Papel.MULTIMIDIA, parCentral, CanalSeguro.publicaX962(parCelular))
        val celular = CanalSeguro.criar(Papel.CELULAR, parCelular, CanalSeguro.publicaX962(parCentral))
    }

    private fun assertFalhaDecifra(bloco: () -> Unit) {
        try {
            bloco()
            fail("deveria lançar GeneralSecurityException")
        } catch (_: GeneralSecurityException) {
        }
    }

    @Test
    fun `RNF-006 aperto leva a chave publica e rejeita outra magica ou versao`() {
        val publica = CanalSeguro.publicaX962(CanalSeguro.gerarParDeChaves())
        assertEquals(65, publica.size)
        val aperto = Aperto.codificar(publica)
        assertEquals(Aperto.TAMANHO, aperto.size)
        assertArrayEquals("CTRLMESH".toByteArray(Charsets.US_ASCII), aperto.copyOfRange(0, 8))
        assertEquals(Protocolo.VERSAO, aperto[8].toInt())
        assertArrayEquals(publica, Aperto.decodificar(aperto))

        val outraVersao = aperto.copyOf().also { it[8] = 2 }
        val outraMagica = aperto.copyOf().also { it[0] = 'X'.code.toByte() }
        listOf(outraVersao, outraMagica, aperto.copyOf(Aperto.TAMANHO - 1)).forEach {
            try {
                Aperto.decodificar(it)
                fail("deveria lançar ProtocoloException")
            } catch (_: ProtocoloException) {
            }
        }
    }

    @Test
    fun `RNF-006 central e celular trocam quadros cifrados nos dois sentidos`() {
        val conexao = Conexao()
        val toque = Protocolo.codificar(Mensagem.Tecla(TeclaDeSistema.INICIO))
        val video = ByteArray(50_000) { it.toByte() }

        val cifradoToque = conexao.central.cifrar(toque)
        assertFalse(cifradoToque.contentEquals(toque))
        assertArrayEquals(toque, conexao.celular.decifrar(cifradoToque))
        assertArrayEquals(video, conexao.central.decifrar(conexao.celular.cifrar(video)))
    }

    @Test
    fun `RNF-006 quadro alterado, repetido ou devolvido ao remetente e rejeitado`() {
        val conexao = Conexao()
        val quadro = Protocolo.codificar(Mensagem.Tecla(TeclaDeSistema.VOLTAR))

        val alterado = conexao.central.cifrar(quadro).also { it[it.size - 1] = (it[it.size - 1].toInt() xor 1).toByte() }
        assertFalhaDecifra { conexao.celular.decifrar(alterado) }

        val outra = Conexao()
        val cifrado = outra.central.cifrar(quadro)
        outra.celular.decifrar(cifrado)
        assertFalhaDecifra { outra.celular.decifrar(cifrado) }

        assertFalhaDecifra { outra.central.decifrar(outra.central.cifrar(quadro)) }
    }

    @Test
    fun `RNF-006 cada conexao usa chave nova`() {
        val primeira = Conexao()
        val segunda = Conexao()
        val quadro = Protocolo.codificar(Mensagem.Tecla(TeclaDeSistema.RECENTES))

        val daPrimeira = primeira.central.cifrar(quadro)
        val daSegunda = segunda.central.cifrar(quadro)
        assertFalse(daPrimeira.contentEquals(daSegunda))
        assertFalhaDecifra { segunda.celular.decifrar(daPrimeira) }
    }
}
