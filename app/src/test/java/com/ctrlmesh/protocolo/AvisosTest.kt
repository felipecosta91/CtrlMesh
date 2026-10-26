package com.ctrlmesh.protocolo

import org.junit.Assert.assertEquals
import org.junit.Test

class AvisosTest {

    @Test
    fun `CA-085 aviso de captura sem liberacao permanente`() {
        assertEquals(
            "captura sem liberação permanente — refaça a liberação pelo Mac para não precisar tocar em Permitir",
            Aviso.SEM_LIBERACAO_PERMANENTE.texto,
        )
    }

    @Test
    fun `CA-022 aviso de compartilhamento de um app so`() {
        assertEquals("o celular está compartilhando só um app; encerre e escolha a tela inteira", Aviso.COMPARTILHANDO_SO_UM_APP.texto)
    }

    @Test
    fun `CA-060 aviso de ligacao em andamento`() {
        assertEquals("ligação em andamento — o som está no celular", Aviso.LIGACAO_EM_ANDAMENTO.texto)
    }

    @Test
    fun `CA-017 aviso de economia de bateria`() {
        assertEquals("o sistema pode fechar o CtrlMesh e o espelhamento pode não começar sozinho", Aviso.FALTA_BATERIA.texto)
    }

    @Test
    fun `CA-033 aviso de conteudo protegido`() {
        assertEquals("conteúdo protegido pelo app", Aviso.CONTEUDO_PROTEGIDO.texto)
    }

    @Test
    fun `CA-068 bits dos avisos no fio seguem o contrato`() {
        val esperado = listOf(
            Aviso.FALTA_ACESSIBILIDADE, Aviso.FALTA_GIRAR_TELA, Aviso.FALTA_SOBREPOR, Aviso.FALTA_GRAVAR_AUDIO,
            Aviso.FALTA_BATERIA, Aviso.SEM_LIBERACAO_PERMANENTE, Aviso.CONTROLE_BLOQUEADO_PELO_SISTEMA,
            Aviso.COMPARTILHANDO_SO_UM_APP, Aviso.LIGACAO_EM_ANDAMENTO, Aviso.NAO_SILENCIA, Aviso.CONTEUDO_PROTEGIDO,
        )
        esperado.forEachIndexed { bit, aviso -> assertEquals(aviso.name, bit, aviso.bit) }
    }
}
