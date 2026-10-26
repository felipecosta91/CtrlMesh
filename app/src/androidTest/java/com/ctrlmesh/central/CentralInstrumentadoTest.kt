package com.ctrlmesh.central

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.ctrlmesh.CelularFalso
import com.ctrlmesh.ConfiguracaoDoApp
import com.ctrlmesh.MainActivity
import com.ctrlmesh.VideoDeTeste
import com.ctrlmesh.contexto
import com.ctrlmesh.esperarTexto
import com.ctrlmesh.prepararApp
import com.ctrlmesh.protocolo.AcaoToque
import com.ctrlmesh.protocolo.Aviso
import com.ctrlmesh.protocolo.EstadoRemoto
import com.ctrlmesh.protocolo.Mensagem
import com.ctrlmesh.protocolo.MotivoEncerramento
import com.ctrlmesh.protocolo.Papel
import com.ctrlmesh.protocolo.TeclaDeSistema
import com.ctrlmesh.shell
import com.ctrlmesh.textoDaTag
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/** Central (papel multimídia) contra o CelularFalso em 127.0.0.1. Roda no emulador. */
@RunWith(AndroidJUnit4::class)
class CentralInstrumentadoTest {

    companion object {
        private lateinit var paisagem: VideoDeTeste.Video
        private lateinit var retrato: VideoDeTeste.Video

        @BeforeClass
        @JvmStatic
        fun gerarVideos() {
            com.ctrlmesh.EstadoDoAparelho.salvar()
            paisagem = VideoDeTeste.gerar(1200, 540)
            retrato = VideoDeTeste.gerar(540, 1200)
        }

        @org.junit.AfterClass
        @JvmStatic
        fun restaurarAparelho() = com.ctrlmesh.EstadoDoAparelho.restaurar()
    }

    @get:Rule
    val compose = createEmptyComposeRule()

    private val pacote = "com.ctrlmesh"
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private var celular: CelularFalso? = null
    private var cenario: ActivityScenario<MainActivity>? = null

    @Before
    fun preparar() {
        prepararApp(Papel.MULTIMIDIA, enderecoDoCelular = "127.0.0.1")
        shell("appops set $pacote SYSTEM_ALERT_WINDOW allow")
    }

    @After
    fun limpar() {
        cenario?.close()
        celular?.close()
        device.unfreezeRotation()
    }

    private fun abrir() {
        cenario = ActivityScenario.launch(MainActivity::class.java)
    }

    private fun conectar(
        estado: EstadoRemoto = EstadoRemoto.ESPELHANDO,
        video: VideoDeTeste.Video = paisagem,
        avisos: Set<Aviso> = emptySet(),
    ): CelularFalso {
        val falso = CelularFalso(estado, avisos, video)
        celular = falso
        abrir()
        compose.waitUntil(15_000) { falso.conexoes.get() >= 1 }
        return falso
    }

    private fun espelhando(video: VideoDeTeste.Video = paisagem): CelularFalso {
        val falso = conectar(video = video)
        compose.esperarTexto("estado", "espelhando")
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("imagem").fetchSemanticsNodes().isNotEmpty() }
        falso.recebidas.clear()
        return falso
    }

    private fun assertSemTexto(trecho: String) {
        assertTrue(
            "não deveria existir texto com \"$trecho\"",
            compose.onAllNodes(hasText(trecho, substring = true, ignoreCase = true), useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty(),
        )
    }

    // ---------- Aparelhos, rede e início automático ----------

    @Test
    fun ca003_emAndroidComumOAppAbreEFuncionaComoCentral() {
        abrir()
        compose.esperarTexto("estado", "aguardando o celular")
        compose.onNodeWithTag("barra").assertDoesNotExist()
    }

    @Test
    fun ca078_centralPassaDeAguardandoOCelularAEspelhandoSemNenhumToque() {
        abrir()
        compose.esperarTexto("estado", "aguardando o celular")
        val falso = CelularFalso(EstadoRemoto.ESPELHANDO, video = paisagem).also { celular = it }

        compose.esperarTexto("estado", "espelhando", timeoutMs = 15_000)
        assertEquals(1, falso.conexoes.get())
        val ola = falso.esperar { it is Mensagem.Ola } as Mensagem.Ola?
        assertNotNull("a central se apresenta com Ola", ola)
        assertTrue(ola!!.larguraArea > 0 && ola.alturaArea > 0)
    }

    @Test
    fun ca016_enderecoInformadoNasConfiguracoesValeNestaENasProximasConexoes() {
        prepararApp(Papel.MULTIMIDIA, enderecoDoCelular = null)
        val falso = CelularFalso(EstadoRemoto.ESPELHANDO, video = paisagem).also { celular = it }
        abrir()
        compose.onNodeWithTag("botao-configuracoes").performClick()
        compose.onNodeWithTag("campo-endereco").performTextReplacement("127.0.0.1")
        compose.onNodeWithTag("botao-salvar-endereco").performClick()
        assertEquals("127.0.0.1", ConfiguracaoDoApp(contexto).enderecoDoCelular)
        compose.waitUntil(15_000) { falso.conexoes.get() >= 1 }

        cenario?.close()
        contexto.stopService(android.content.Intent(contexto, com.ctrlmesh.ServicoDeEscuta::class.java))
        abrir()
        compose.waitUntil(15_000) { falso.conexoes.get() >= 2 }
    }

    @Test
    fun ca080_comSobreposicaoACentralVemParaAFrenteSozinhaQuandoOEspelhamentoComeca() {
        abrir()
        compose.esperarTexto("estado", "aguardando o celular")
        device.pressHome()
        device.wait(Until.gone(By.pkg(pacote).depth(0)), 5_000)

        celular = CelularFalso(EstadoRemoto.ESPELHANDO, video = paisagem)

        assertTrue(device.wait(Until.hasObject(By.pkg(pacote).depth(0)), 15_000))
    }

    @Test
    fun ca080_semSobreposicaoApareceANotificacaoToqueParaVerOCelularEElaTrazACentral() {
        shell("appops set $pacote SYSTEM_ALERT_WINDOW deny")
        abrir()
        compose.esperarTexto("estado", "aguardando o celular")
        device.pressHome()
        device.wait(Until.gone(By.pkg(pacote).depth(0)), 5_000)

        celular = CelularFalso(EstadoRemoto.ESPELHANDO, video = paisagem)

        device.openNotification()
        val notificacao = device.wait(Until.findObject(By.textContains("toque para ver o celular")), 15_000)
        assertNotNull(notificacao)
        notificacao.click()
        assertTrue(device.wait(Until.hasObject(By.pkg(pacote).depth(0)), 10_000))
    }

    @Test
    fun ca074_ca071_centralNaoTemPareamentoNemEspelhamentoInverso() {
        espelhando()
        listOf("parear", "pareamento", "centrais autorizadas", "espelhar a central", "espelhar esta central").forEach(::assertSemTexto)
        compose.onNodeWithTag("botao-configuracoes").performClick()
        listOf("parear", "pareamento", "centrais autorizadas", "espelhar a central", "ligar o roteador").forEach(::assertSemTexto)
    }

    @Test
    fun ca089_aImagemApareceSemPedidoDeVeiculoParado() {
        espelhando()
        compose.onNodeWithTag("imagem").assertIsDisplayed()
        assertSemTexto("veículo")
    }

    // ---------- Imagem, barra e controles ----------

    @Test
    fun ca025_imagemEmTelaCheiaPaisagemInteiraESemDistorcaoMesmoComOAparelhoEmRetrato() {
        espelhando()
        device.setOrientationPortrait()
        Thread.sleep(1_500)
        cenario!!.onActivity {
            assertEquals(Configuration.ORIENTATION_LANDSCAPE, it.resources.configuration.orientation)
            assertTrue(
                it.requestedOrientation in setOf(
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
                    ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE,
                ),
            )
        }
        val raiz = compose.onRoot().getBoundsInRoot()
        assertTrue(raiz.width > raiz.height)
        val imagem = compose.onNodeWithTag("imagem").getBoundsInRoot()
        assertTrue(imagem.left >= raiz.left && imagem.right <= raiz.right && imagem.top >= raiz.top && imagem.bottom <= raiz.bottom)
        val proporcao = imagem.width / imagem.height
        assertEquals(1200f / 540f, proporcao, 0.03f)
    }

    private val BOTOES_EM_ORDEM = listOf(
        "botao-voltar", "botao-inicio", "botao-recentes", "botao-menos", "botao-mais", "botao-mudo", "botao-ajustes", "botao-encerrar",
    )

    @Test
    fun ca043_controlesDaBarraTemNoMinimo76Dp() {
        espelhando()
        BOTOES_EM_ORDEM.forEach {
            compose.onNodeWithTag(it).assertIsDisplayed().assertWidthIsAtLeast(76.dp).assertHeightIsAtLeast(76.dp)
        }
    }

    @Test
    fun ca112_barraEmbaixoHorizontalComOsControlesEmOrdemEImagemInteiraLogoAcima() {
        espelhando()
        val raiz = compose.onRoot().getBoundsInRoot()
        val barra = compose.onNodeWithTag("barra").getBoundsInRoot()
        val imagem = compose.onNodeWithTag("imagem").getBoundsInRoot()
        assertEquals(raiz.bottom.value, barra.bottom.value, 1f)
        assertEquals(raiz.left.value, barra.left.value, 1f)
        assertEquals(raiz.right.value, barra.right.value, 1f)
        assertTrue("barra horizontal", barra.width > barra.height)
        assertTrue("barra não cobre a imagem", imagem.bottom <= barra.top + 0.5.dp)
        assertEquals("imagem logo acima da barra", barra.top.value, imagem.bottom.value, 1f)
        val esquerdas = BOTOES_EM_ORDEM.map { tag ->
            val b = compose.onNodeWithTag(tag).getBoundsInRoot()
            assertTrue("$tag dentro da barra", b.top >= barra.top - 0.5.dp && b.bottom <= barra.bottom + 0.5.dp)
            b.left.value
        }
        assertEquals("ordem da esquerda para a direita", esquerdas.sorted(), esquerdas)
        val estado = compose.onNodeWithTag("estado").getBoundsInRoot()
        assertTrue("estado da conexão na barra", estado.top >= barra.top - 0.5.dp && estado.bottom <= barra.bottom + 0.5.dp)
    }

    @Test
    fun ca112_comAppEmRetratoAImagemFicaInteiraAcimaDaBarraComFaixasDosLados() {
        espelhando(retrato)
        val barra = compose.onNodeWithTag("barra").getBoundsInRoot()
        val imagem = compose.onNodeWithTag("imagem").getBoundsInRoot()
        val raiz = compose.onRoot().getBoundsInRoot()
        assertEquals(barra.top.value, imagem.bottom.value, 1f)
        assertEquals(raiz.top.value, imagem.top.value, 1f)
        assertTrue("faixas dos lados", imagem.left > raiz.left + 20.dp && imagem.right < raiz.right - 20.dp)
        assertEquals(540f / 1200f, imagem.width / imagem.height, 0.03f)
    }

    @Test
    fun ca112_ajustesAbreAsConfiguracoesSemEncerrarOEspelhamento() {
        val falso = espelhando()
        compose.onNodeWithTag("botao-ajustes").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("campo-endereco").fetchSemanticsNodes().isNotEmpty() }
        Thread.sleep(1_000)
        assertTrue("não encerra", falso.recebidas.none { it is Mensagem.Encerrar })
        assertEquals(1, falso.conexoes.get())
        compose.esperarTexto("estado", "espelhando")
    }

    @Test
    fun ca035_toqueNoCentroDaImagemChegaAoCelularNoCentroDoQuadro() {
        val falso = espelhando()
        compose.onNodeWithTag("imagem").performTouchInput { click(center) }

        val desce = falso.esperar { it is Mensagem.Toque && it.acao == AcaoToque.DESCE } as Mensagem.Toque?
        val sobe = falso.esperar { it is Mensagem.Toque && it.acao == AcaoToque.SOBE } as Mensagem.Toque?
        assertNotNull(desce)
        assertNotNull(sobe)
        assertEquals(0.5f, desce!!.x, 0.03f)
        assertEquals(0.5f, desce.y, 0.03f)
        assertEquals(1200, desce.larguraQuadro)
        assertEquals(540, desce.alturaQuadro)
    }

    @Test
    fun ca035_deslizarEPincaChegamAoCelularComMovimentoEDoisDedos() {
        val falso = espelhando()
        compose.onNodeWithTag("imagem").performTouchInput { swipeLeft() }
        val deslize = falso.toquesRecebidos(1_500)
        assertTrue(deslize.any { it.acao == AcaoToque.MOVE })
        assertTrue(deslize.first().x > deslize.last().x)

        compose.onNodeWithTag("imagem").performTouchInput {
            pinch(center - Offset(10f, 0f), center - Offset(150f, 0f), center + Offset(10f, 0f), center + Offset(150f, 0f))
        }
        val pinca = falso.toquesRecebidos(1_500)
        assertEquals(2, pinca.map { it.ponteiro }.distinct().size)
    }

    @Test
    fun ca036_toquesNasFaixasNaoChegamAoCelular() {
        val falso = espelhando(retrato)
        val raiz = compose.onRoot().getBoundsInRoot()
        val imagem = compose.onNodeWithTag("imagem").getBoundsInRoot()
        assertTrue("há faixa à direita", raiz.right - imagem.right > 20.dp)
        compose.onRoot().performTouchInput { click(Offset(width - 10f, height / 2f)) }
        assertTrue(falso.toquesRecebidos(1_500).isEmpty())
    }

    @Test
    fun ca037_voltarInicioERecentesDaBarraChegamAoCelular() {
        val falso = espelhando()
        compose.onNodeWithTag("botao-voltar").performClick()
        compose.onNodeWithTag("botao-inicio").performClick()
        compose.onNodeWithTag("botao-recentes").performClick()
        val teclas = listOf(TeclaDeSistema.VOLTAR, TeclaDeSistema.INICIO, TeclaDeSistema.RECENTES).map { esperada ->
            (falso.esperar { it is Mensagem.Tecla } as Mensagem.Tecla?)?.tecla.also { assertEquals(esperada, it) }
        }
        assertEquals(3, teclas.size)
    }

    // ---------- Estados ----------

    @Test
    fun ca084_semLiberacaoACentralMostraToqueEmPermitirNoCelular() {
        conectar(EstadoRemoto.AGUARDANDO_CONFIRMACAO)
        compose.esperarTexto("estado", "aguardando confirmação no celular")
        compose.esperarTexto("instrucao", "toque em Permitir no celular")
        compose.onNodeWithTag("botao-encerrar").assertIsDisplayed()
    }

    @Test
    fun ca021_naoConfirmadoMostraTentarDeNovoNaoRepeteSozinhoETentarDeNovoPedeOutraVez() {
        val falso = conectar(EstadoRemoto.AGUARDANDO_CONFIRMACAO)
        compose.esperarTexto("estado", "aguardando confirmação no celular")
        falso.enviar(Mensagem.Encerrar(MotivoEncerramento.NAO_CONFIRMADO_NO_CELULAR))
        compose.esperarTexto("estado", "não confirmado no celular")
        compose.onNodeWithTag("botao-tentar-de-novo").assertIsDisplayed()

        Thread.sleep(6_000)
        assertEquals("não repete sozinho", 1, falso.conexoes.get())

        compose.onNodeWithTag("botao-tentar-de-novo").performClick()
        compose.waitUntil(10_000) { falso.conexoes.get() == 2 }
        compose.esperarTexto("estado", "aguardando confirmação no celular")
    }

    @Test
    fun ca088_celularBloqueadoMostraDesbloqueieENenhumToqueEEnviado() {
        val falso = espelhando()
        falso.enviar(Mensagem.Estado(EstadoRemoto.DESBLOQUEIE))
        compose.esperarTexto("estado", "desbloqueie o celular")
        compose.onRoot().performTouchInput { click(center) }
        assertTrue(falso.toquesRecebidos(1_500).isEmpty())
        compose.onNodeWithTag("botao-encerrar").assertIsDisplayed()

        falso.enviar(Mensagem.Estado(EstadoRemoto.ESPELHANDO))
        compose.esperarTexto("estado", "espelhando")
    }

    @Test
    fun ca073_segundaCentralMostraCelularEmUsoEEspelhaSozinhaQuandoLiberada() {
        val falso = conectar(EstadoRemoto.EM_USO_POR_OUTRA_CENTRAL)
        compose.esperarTexto("estado", "celular em uso por outra central")
        compose.onNodeWithTag("imagem").assertDoesNotExist()

        falso.enviar(Mensagem.Estado(EstadoRemoto.ESPELHANDO))
        falso.enviarVideo()
        compose.esperarTexto("estado", "espelhando")
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("imagem").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun ca049_ca044_quedaMostraReconectandoComImagemDesatualizadaEncerrarAMaoEToquesDescartados() {
        val falso = espelhando()
        falso.silenciar()
        compose.esperarTexto("estado", "reconectando", timeoutMs = 6_000)
        compose.onNodeWithTag("imagem-desatualizada").assertIsDisplayed()
        compose.onNodeWithTag("botao-encerrar").assertIsDisplayed()
        compose.onNodeWithTag("imagem").performTouchInput { click(center) }
        compose.onNodeWithTag("botao-voltar").performClick()

        falso.voltar()
        compose.esperarTexto("estado", "espelhando", timeoutMs = 15_000)
        Thread.sleep(1_500)
        val depois = mutableListOf<Mensagem>().also { falso.recebidas.drainTo(it) }
        assertTrue("toques do intervalo nunca são enviados", depois.none { it is Mensagem.Toque || it is Mensagem.Tecla })
        compose.onNodeWithTag("imagem-desatualizada").assertDoesNotExist()
    }

    @Test
    fun ca091_appFechadoNoCelularMostraOMotivoSemPassarPorReconectando() {
        val falso = espelhando()
        falso.fecharAbrupto()
        val vistos = mutableSetOf<String?>()
        compose.waitUntil(8_000) {
            compose.textoDaTag("estado").also { vistos += it } == "app fechado ou aparelho desligado"
        }
        assertFalse(vistos.contains("reconectando"))
    }

    @Test
    fun ca092_encerradoPeloCelularImagemSomeToquesParamEMotivoAparece() {
        val falso = espelhando()
        falso.enviar(Mensagem.Encerrar(MotivoEncerramento.ENCERRADO_PELO_CELULAR))
        compose.esperarTexto("estado", "encerrado pelo celular")
        compose.onNodeWithTag("imagem").assertDoesNotExist()
        compose.onRoot().performTouchInput { click(center) }
        assertTrue(falso.toquesRecebidos(1_000).isEmpty())
        assertSemTexto("pareamento")
    }

    @Test
    fun ca092_ca044_encerrarNaCentralComUmToqueAvisaOCelularComOMotivo() {
        val falso = espelhando()
        compose.onNodeWithTag("botao-encerrar").performClick()
        val encerrar = falso.esperar { it is Mensagem.Encerrar } as Mensagem.Encerrar?
        assertEquals(MotivoEncerramento.ENCERRADO_PELA_CENTRAL, encerrar?.motivo)
        compose.esperarTexto("estado", "encerrado pela central")
        compose.onNodeWithTag("imagem").assertDoesNotExist()
    }

    @Test
    fun ca047_encerradoPeloDonoNaoRecomecaSozinhoEEspelharRecomeca() {
        val falso = espelhando()
        compose.onNodeWithTag("botao-encerrar").performClick()
        compose.esperarTexto("estado", "encerrado pela central")
        Thread.sleep(6_000)
        assertEquals(1, falso.conexoes.get())

        compose.onNodeWithTag("botao-espelhar").performClick()
        compose.waitUntil(10_000) { falso.conexoes.get() == 2 }
        compose.esperarTexto("estado", "espelhando")
    }

    // ---------- Avisos e som ----------

    @Test
    fun ca068_centralMostraQueFaltamGirarATelaEGravarAudioNoCelular() {
        conectar(avisos = setOf(Aviso.FALTA_GIRAR_TELA, Aviso.FALTA_GRAVAR_AUDIO))
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasText(Aviso.FALTA_GIRAR_TELA.texto), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodes(hasText(Aviso.FALTA_GRAVAR_AUDIO.texto), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun ca059_ca093_ca085_ca022_ca060_ca033_ca023_ca024_ca029_avisosDoCelularAparecemNaCentral() {
        val avisos = setOf(
            Aviso.FALTA_GRAVAR_AUDIO, Aviso.NAO_SILENCIA, Aviso.SEM_LIBERACAO_PERMANENTE, Aviso.COMPARTILHANDO_SO_UM_APP,
            Aviso.LIGACAO_EM_ANDAMENTO, Aviso.CONTEUDO_PROTEGIDO, Aviso.FALTA_ACESSIBILIDADE,
            Aviso.CONTROLE_BLOQUEADO_PELO_SISTEMA, Aviso.FALTA_GIRAR_TELA,
        )
        conectar(avisos = avisos)
        compose.esperarTexto("estado", "espelhando")
        avisos.forEach { aviso ->
            compose.waitUntil(5_000) {
                compose.onAllNodes(hasText(aviso.texto), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }
        }
    }

    @Test
    fun ca060_avisoDeLigacaoSomeQuandoALigacaoTerminaSemEncerrar() {
        val falso = conectar(avisos = setOf(Aviso.LIGACAO_EM_ANDAMENTO))
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasText(Aviso.LIGACAO_EM_ANDAMENTO.texto), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        falso.enviar(Mensagem.Situacao(emptySet()))
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasText(Aviso.LIGACAO_EM_ANDAMENTO.texto), useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
        }
        compose.esperarTexto("estado", "espelhando")
    }

    @Test
    fun ca061_algunsSonsNaoChegamACentralListaOsSonsNaoEnviaveis() {
        espelhando()
        compose.onNodeWithTag("sons-nao-enviaveis").performClick()
        listOf("ligações", "alarmes", "toques", "notificações", "voz de navegação", "apps que bloqueiam a captura").forEach {
            assertTrue(
                it,
                compose.onAllNodes(hasText(it, substring = true), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty(),
            )
        }
    }

    @Test
    fun ca062_menosMaisEMudoNaoMandamNadaAoCelular() {
        val falso = espelhando()
        compose.onNodeWithTag("botao-menos").performClick()
        compose.onNodeWithTag("botao-mais").performClick()
        compose.onNodeWithTag("botao-mudo").performClick()
        compose.onNodeWithTag("botao-mudo").performClick()
        Thread.sleep(1_000)
        assertTrue(falso.recebidas.none { it is Mensagem.Tecla || it is Mensagem.Toque || it is Mensagem.Encerrar })
        compose.esperarTexto("estado", "espelhando")
    }

    @Test
    fun ca025_imagemChegaEOPrimeiroQuadroEDesenhadoSemDistorcaoDeCorNemTelaPreta() {
        espelhando()
        Thread.sleep(1_000)
        val bounds = compose.onNodeWithTag("imagem").getBoundsInRoot()
        assertTrue(bounds.width > 100.dp && bounds.height > 50.dp)
        val captura = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val densidade = contexto.resources.displayMetrics.density
        val janela = IntArray(2)
        cenario!!.onActivity { it.window.decorView.getLocationOnScreen(janela) }
        val cx = janela[0] + ((bounds.left + bounds.right) / 2).value * densidade
        val cy = janela[1] + ((bounds.top + bounds.bottom) / 2).value * densidade
        val pixel = captura.getPixel(cx.toInt().coerceIn(0, captura.width - 1), cy.toInt().coerceIn(0, captura.height - 1))
        val r = android.graphics.Color.red(pixel)
        val g = android.graphics.Color.green(pixel)
        val b = android.graphics.Color.blue(pixel)
        assertTrue("imagem não está preta (r=$r g=$g b=$b)", r + g + b > 60 && abs(b - r) + abs(g - r) > 40)
    }
}
