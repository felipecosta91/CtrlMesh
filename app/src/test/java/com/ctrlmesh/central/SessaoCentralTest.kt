package com.ctrlmesh.central

import com.ctrlmesh.central.EstadoCentral.AguardandoCelular
import com.ctrlmesh.central.EstadoCentral.AguardandoConfirmacao
import com.ctrlmesh.central.EstadoCentral.Conectando
import com.ctrlmesh.central.EstadoCentral.DesbloqueieCelular
import com.ctrlmesh.central.EstadoCentral.EmUsoPorOutraCentral
import com.ctrlmesh.central.EstadoCentral.Encerrado
import com.ctrlmesh.central.EstadoCentral.Espelhando
import com.ctrlmesh.central.EstadoCentral.Reconectando
import com.ctrlmesh.central.EventoCentral.CelularFechouConexao
import com.ctrlmesh.central.EventoCentral.ConexaoAberta
import com.ctrlmesh.central.EventoCentral.EncerrarTocado
import com.ctrlmesh.central.EventoCentral.EspelharTocado
import com.ctrlmesh.central.EventoCentral.Queda
import com.ctrlmesh.central.EventoCentral.Recebeu
import com.ctrlmesh.central.EventoCentral.RedeDisponivel
import com.ctrlmesh.central.EventoCentral.RedeIndisponivel
import com.ctrlmesh.central.EventoCentral.Relogio
import com.ctrlmesh.central.EventoCentral.TentarDeNovoTocado
import com.ctrlmesh.protocolo.EstadoRemoto
import com.ctrlmesh.protocolo.Mensagem
import com.ctrlmesh.protocolo.MotivoEncerramento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessaoCentralTest {

    private class Linha(var estado: EstadoCentral = SessaoCentral.inicial) {
        val historico = mutableListOf(estado)
        fun evento(evento: EventoCentral, agoraMs: Long = 0): Linha {
            estado = SessaoCentral.reduzir(estado, evento, agoraMs)
            historico += estado
            return this
        }
        fun celular(estado: EstadoRemoto, agoraMs: Long = 0) = evento(Recebeu(Mensagem.Estado(estado)), agoraMs)
        fun celularEncerrou(motivo: MotivoEncerramento) = evento(Recebeu(Mensagem.Encerrar(motivo)))
    }

    private fun espelhando() = Linha().evento(ConexaoAberta).celular(EstadoRemoto.ESPELHANDO)

    @Test
    fun `CA-078 textos de estado da central`() {
        assertEquals("aguardando o celular", AguardandoCelular.texto)
        assertEquals("conectando", Conectando.texto)
        assertEquals("aguardando confirmação no celular", AguardandoConfirmacao.texto)
        assertEquals("espelhando", Espelhando.texto)
        assertEquals("desbloqueie o celular", DesbloqueieCelular.texto)
        assertEquals("reconectando", Reconectando(0).texto)
        assertEquals("celular em uso por outra central", EmUsoPorOutraCentral.texto)
        assertEquals("conexão perdida", Encerrado(MotivoEncerramento.CONEXAO_PERDIDA).texto)
    }

    @Test
    fun `CA-078 central em escuta tenta conectar e espelha sem nenhum toque`() {
        val linha = Linha()
        assertEquals(AguardandoCelular, linha.estado)
        assertTrue(linha.estado.deveConectar)

        linha.evento(ConexaoAberta)
        assertEquals(Conectando, linha.estado)

        linha.celular(EstadoRemoto.ESPELHANDO)
        assertEquals(Espelhando, linha.estado)
        assertTrue(linha.estado.encaminhaToques)
        assertTrue(linha.estado.tocaSom)
    }

    @Test
    fun `CA-089 nao existe pedido de veiculo parado entre conectar e espelhar`() {
        val linha = espelhando()
        assertEquals(listOf(AguardandoCelular, Conectando, Espelhando), linha.historico)
        assertTrue(linha.historico.none { it.texto.contains("veículo") })
    }

    @Test
    fun `CA-084 sem liberacao permanente a central aguarda confirmacao no celular sem som nem toques`() {
        val linha = Linha().evento(ConexaoAberta).celular(EstadoRemoto.AGUARDANDO_CONFIRMACAO)
        assertEquals(AguardandoConfirmacao, linha.estado)
        assertFalse(linha.estado.encaminhaToques)
        assertFalse(linha.estado.tocaSom)

        linha.celular(EstadoRemoto.ESPELHANDO)
        assertEquals(Espelhando, linha.estado)
    }

    @Test
    fun `CA-021 nao confirmado nao tenta de novo sozinho e tentar de novo reconecta`() {
        val linha = Linha().evento(ConexaoAberta).celular(EstadoRemoto.AGUARDANDO_CONFIRMACAO)
            .celularEncerrou(MotivoEncerramento.NAO_CONFIRMADO_NO_CELULAR)
        assertEquals(Encerrado(MotivoEncerramento.NAO_CONFIRMADO_NO_CELULAR), linha.estado)
        assertEquals("não confirmado no celular", linha.estado.texto)
        assertFalse(linha.estado.deveConectar)

        linha.evento(TentarDeNovoTocado)
        assertTrue(linha.estado.deveConectar)
        linha.evento(ConexaoAberta).celular(EstadoRemoto.AGUARDANDO_CONFIRMACAO)
        assertEquals(AguardandoConfirmacao, linha.estado)
    }

    @Test
    fun `CA-088 celular bloqueado mostra desbloqueie e volta sozinho ao desbloquear`() {
        val linha = espelhando().celular(EstadoRemoto.DESBLOQUEIE)
        assertEquals(DesbloqueieCelular, linha.estado)
        assertEquals("desbloqueie o celular", linha.estado.texto)
        assertFalse(linha.estado.encaminhaToques)

        linha.celular(EstadoRemoto.ESPELHANDO)
        assertEquals(Espelhando, linha.estado)
    }

    @Test
    fun `CA-073 segunda central mostra em uso e espelha sozinha quando liberada`() {
        val linha = Linha().evento(ConexaoAberta).celular(EstadoRemoto.EM_USO_POR_OUTRA_CENTRAL)
        assertEquals(EmUsoPorOutraCentral, linha.estado)
        assertFalse(linha.estado.encaminhaToques)
        assertFalse(linha.estado.tocaSom)

        linha.celular(EstadoRemoto.ESPELHANDO)
        assertEquals(Espelhando, linha.estado)
    }

    @Test
    fun `CA-049 queda leva a reconectando com imagem desatualizada e toques descartados`() {
        val linha = espelhando().evento(Queda, agoraMs = 10_000)
        assertEquals(Reconectando(10_000), linha.estado)
        assertEquals("reconectando", linha.estado.texto)
        assertTrue(linha.estado.imagemDesatualizada)
        assertFalse(linha.estado.encaminhaToques)
        assertTrue(linha.estado.deveConectar)
    }

    @Test
    fun `CA-049 Wi-Fi da central desligado tambem leva a reconectando`() {
        val linha = espelhando().evento(RedeIndisponivel, agoraMs = 5_000)
        assertEquals(Reconectando(5_000), linha.estado)
    }

    @Test
    fun `CA-064 durante reconectando a central nao toca som`() {
        assertFalse(espelhando().evento(Queda).estado.tocaSom)
    }

    @Test
    fun `CA-050 reconexao dentro de 60 segundos continua o espelhamento sem acao`() {
        val linha = espelhando().evento(Queda, agoraMs = 0)
            .evento(Relogio(59_000), agoraMs = 59_000)
        assertEquals(Reconectando(0), linha.estado)

        linha.evento(ConexaoAberta, agoraMs = 59_500)
        assertTrue(linha.estado !is Encerrado)
        linha.celular(EstadoRemoto.ESPELHANDO, agoraMs = 59_600)
        assertEquals(Espelhando, linha.estado)
        assertFalse(linha.estado.imagemDesatualizada)
        assertTrue(linha.estado.tocaSom)
    }

    @Test
    fun `CA-051 60 segundos sem conexao encerra por conexao perdida e recomeca sozinho`() {
        val linha = espelhando().evento(Queda, agoraMs = 1_000)
            .evento(Relogio(61_000), agoraMs = 61_000)
        assertEquals(Encerrado(MotivoEncerramento.CONEXAO_PERDIDA), linha.estado)
        assertEquals("conexão perdida", linha.estado.texto)
        assertTrue(linha.estado.deveConectar)

        linha.evento(ConexaoAberta).celular(EstadoRemoto.ESPELHANDO)
        assertEquals(Espelhando, linha.estado)
    }

    @Test
    fun `CA-091 app fechado no celular encerra sem passar por reconectando`() {
        val linha = espelhando().evento(CelularFechouConexao)
        assertEquals(Encerrado(MotivoEncerramento.APP_FECHADO_OU_DESLIGADO), linha.estado)
        assertEquals("app fechado ou aparelho desligado", linha.estado.texto)
        assertTrue(linha.historico.none { it is Reconectando })
    }

    @Test
    fun `CA-091 celular avisando app fechado tambem encerra sem reconectando`() {
        val linha = espelhando().celularEncerrou(MotivoEncerramento.APP_FECHADO_OU_DESLIGADO)
        assertEquals(Encerrado(MotivoEncerramento.APP_FECHADO_OU_DESLIGADO), linha.estado)
        assertTrue(linha.historico.none { it is Reconectando })
    }

    @Test
    fun `CA-092 encerrado pelo celular corta imagem, som e toques e mostra o motivo`() {
        val linha = espelhando().celularEncerrou(MotivoEncerramento.ENCERRADO_PELO_CELULAR)
        assertEquals("encerrado pelo celular", linha.estado.texto)
        assertFalse(linha.estado.encaminhaToques)
        assertFalse(linha.estado.tocaSom)
    }

    @Test
    fun `CA-092 encerrar na central leva o motivo encerrado pela central`() {
        val linha = espelhando().evento(EncerrarTocado)
        assertEquals(Encerrado(MotivoEncerramento.ENCERRADO_PELA_CENTRAL), linha.estado)
        assertFalse(linha.estado.encaminhaToques)
        assertFalse(linha.estado.tocaSom)
    }

    @Test
    fun `CA-044 encerrar funciona em qualquer estado do espelhamento`() {
        val estados = listOf(
            espelhando(),
            espelhando().celular(EstadoRemoto.DESBLOQUEIE),
            espelhando().evento(Queda),
            Linha().evento(ConexaoAberta).celular(EstadoRemoto.AGUARDANDO_CONFIRMACAO),
        )
        estados.forEach {
            assertEquals(Encerrado(MotivoEncerramento.ENCERRADO_PELA_CENTRAL), it.evento(EncerrarTocado).estado)
        }
    }

    @Test
    fun `CA-047 encerrado pelo dono nao recomeca sozinho na mesma rede`() {
        listOf(
            espelhando().evento(EncerrarTocado),
            espelhando().celularEncerrou(MotivoEncerramento.ENCERRADO_PELO_CELULAR),
        ).forEach { linha ->
            assertFalse(linha.estado.deveConectar)
            linha.evento(Relogio(600_000), agoraMs = 600_000)
            assertFalse(linha.estado.deveConectar)
        }
    }

    @Test
    fun `CA-047 espelhar na central recomeca`() {
        val linha = espelhando().evento(EncerrarTocado).evento(EspelharTocado)
        assertTrue(linha.estado.deveConectar)
        linha.evento(ConexaoAberta).celular(EstadoRemoto.ESPELHANDO)
        assertEquals(Espelhando, linha.estado)
    }

    @Test
    fun `CA-047 central sai da rede e volta e o espelhamento recomeca sozinho`() {
        val linha = espelhando().evento(EncerrarTocado)
            .evento(RedeIndisponivel)
            .evento(RedeDisponivel)
        assertTrue(linha.estado.deveConectar)
        linha.evento(ConexaoAberta).celular(EstadoRemoto.ESPELHANDO)
        assertEquals(Espelhando, linha.estado)
    }

    @Test
    fun `CA-078 sem celular na rede a central volta a aguardar e continua tentando`() {
        val linha = Linha().evento(ConexaoAberta).evento(CelularFechouConexao)
        assertEquals(AguardandoCelular, linha.estado)
        assertTrue(linha.estado.deveConectar)
    }
}
