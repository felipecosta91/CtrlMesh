package com.ctrlmesh.central

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ctrlmesh.CelularFalso
import com.ctrlmesh.EstadoDoAparelho
import com.ctrlmesh.MainActivity
import com.ctrlmesh.VideoDeTeste
import com.ctrlmesh.esperarTexto
import com.ctrlmesh.prepararApp
import com.ctrlmesh.protocolo.AppDoCelular
import com.ctrlmesh.protocolo.Aviso
import com.ctrlmesh.protocolo.EstadoRemoto
import com.ctrlmesh.protocolo.Mensagem
import com.ctrlmesh.protocolo.ModoEspelhamento
import com.ctrlmesh.protocolo.Papel
import com.ctrlmesh.protocolo.TeclaDeSistema
import com.ctrlmesh.shell
import java.io.ByteArrayOutputStream
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Central no modo tela virtual (v3.2, §10) contra o CelularFalso. Rodar só com ANDROID_SERIAL=emulator-5554. */
@RunWith(AndroidJUnit4::class)
class TelaVirtualInstrumentadoTest {

    companion object {
        private lateinit var telaVirtual: VideoDeTeste.Video

        @BeforeClass
        @JvmStatic
        fun preparar() {
            EstadoDoAparelho.salvar()
            telaVirtual = VideoDeTeste.gerar(1280, 720)
        }

        @AfterClass
        @JvmStatic
        fun restaurarAparelho() = EstadoDoAparelho.restaurar()

        private fun icone(cor: Int): ByteArray {
            val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888).apply { eraseColor(cor) }
            return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        }

        private val APPS_FORA_DE_ORDEM = listOf(
            AppDoCelular("com.whatsapp", "WhatsApp", icone(Color.GREEN)),
            AppDoCelular("com.spotify.music", "Spotify", icone(Color.BLACK)),
            AppDoCelular("com.android.camera", "Câmera", icone(Color.GRAY)),
            AppDoCelular("com.agenda", "agenda", icone(Color.BLUE)),
        )
        private val ORDEM_ESPERADA = listOf("com.agenda", "com.android.camera", "com.spotify.music", "com.whatsapp")
    }

    @get:Rule
    val compose = createEmptyComposeRule()

    private var celular: CelularFalso? = null
    private var cenario: ActivityScenario<MainActivity>? = null

    @Before
    fun antes() {
        prepararApp(Papel.MULTIMIDIA, enderecoDoCelular = "127.0.0.1")
        shell("appops set com.ctrlmesh SYSTEM_ALERT_WINDOW allow")
    }

    @After
    fun limpar() {
        cenario?.close()
        celular?.close()
    }

    private fun conectar(
        modo: ModoEspelhamento? = ModoEspelhamento.TELA_VIRTUAL,
        avisos: Set<Aviso> = emptySet(),
        apps: List<AppDoCelular>? = APPS_FORA_DE_ORDEM,
    ): CelularFalso {
        val falso = CelularFalso(EstadoRemoto.ESPELHANDO, avisos, telaVirtual, modo, apps)
        celular = falso
        cenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(15_000) { falso.conexoes.get() >= 1 }
        compose.esperarTexto("estado", "espelhando")
        return falso
    }

    private fun existe(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun esperarInicio() = compose.waitUntil(10_000) { existe("inicio-apps") }

    private fun abrirSpotify(falso: CelularFalso) {
        compose.onNodeWithTag("inicio-apps").performScrollToNode(hasTestTag("app-com.spotify.music"))
        compose.onNodeWithTag("app-com.spotify.music").performClick()
        val abrir = falso.esperar { it is Mensagem.AbrirApp } as Mensagem.AbrirApp?
        assertEquals("com.spotify.music", abrir?.pacote)
        falso.enviarVideo()
        compose.waitUntil(10_000) { existe("imagem") && !existe("inicio-apps") }
    }

    @Test
    fun ca097_inicioDoCtrlMeshMostraOsAppsComIconeENomeEmOrdemAlfabetica() {
        conectar()
        esperarInicio()
        compose.onNodeWithTag("imagem").assertDoesNotExist()
        val posicoes = ORDEM_ESPERADA.map { pacote ->
            compose.onNodeWithTag("inicio-apps").performScrollToNode(hasTestTag("app-$pacote"))
            assertTrue("ícone de $pacote", existe("icone-$pacote"))
            val rotulo = APPS_FORA_DE_ORDEM.first { it.pacote == pacote }.rotulo
            assertTrue(
                "rótulo $rotulo",
                compose.onAllNodes(hasText(rotulo), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty(),
            )
            compose.onNodeWithTag("app-$pacote").getBoundsInRoot()
        }
        val ordemNaTela = ORDEM_ESPERADA.zip(posicoes)
            .sortedWith(compareBy({ it.second.top.value.toInt() / 8 }, { it.second.left.value }))
            .map { it.first }
        assertEquals("ordem alfabética (linha a linha)", ORDEM_ESPERADA, ordemNaTela)
    }

    @Test
    fun ca098_ca096_tocarNumAppEnviaAbrirAppEAImagemDaTelaVirtualOcupaAAreaSemFaixas() {
        val falso = conectar()
        esperarInicio()
        abrirSpotify(falso)

        val raiz = compose.onRoot().getBoundsInRoot()
        val barra = compose.onNodeWithTag("barra").getBoundsInRoot()
        val imagem = compose.onNodeWithTag("imagem").getBoundsInRoot()
        assertEquals(raiz.left.value, imagem.left.value, 1f)
        assertEquals(raiz.right.value, imagem.right.value, 1f)
        assertEquals(raiz.top.value, imagem.top.value, 1f)
        assertEquals(barra.top.value, imagem.bottom.value, 1f)
    }

    @Test
    fun ca099_inicioDaBarraVoltaAoInicioDoCtrlMeshEVoltarERecentesVaoAoCelular() {
        val falso = conectar()
        esperarInicio()
        abrirSpotify(falso)
        falso.recebidas.clear()

        compose.onNodeWithTag("botao-voltar").performClick()
        assertEquals(TeclaDeSistema.VOLTAR, (falso.esperar { it is Mensagem.Tecla } as Mensagem.Tecla?)?.tecla)
        compose.onNodeWithTag("botao-recentes").performClick()
        assertEquals(TeclaDeSistema.RECENTES, (falso.esperar { it is Mensagem.Tecla } as Mensagem.Tecla?)?.tecla)
        assertTrue("Voltar e Recentes não mostram o início sozinhos", !existe("inicio-apps"))

        compose.onNodeWithTag("botao-inicio").performClick()
        esperarInicio()
        assertEquals(TeclaDeSistema.INICIO, (falso.esperar { it is Mensagem.Tecla } as Mensagem.Tecla?)?.tecla)
    }

    @Test
    fun ca099_telaVirtualVaziaMostraOInicioDoCtrlMesh() {
        val falso = conectar()
        esperarInicio()
        abrirSpotify(falso)
        falso.enviar(Mensagem.TelaVirtualVazia)
        esperarInicio()
        compose.esperarTexto("estado", "espelhando")
    }

    @Test
    fun ca095_planoBMostraOAvisoDeModoTelaDoCelularESemInicioDoCtrlMesh() {
        conectar(modo = ModoEspelhamento.TELA_FISICA, avisos = setOf(Aviso.MODO_TELA_DO_CELULAR), apps = null)
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasText(Aviso.MODO_TELA_DO_CELULAR.texto), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitUntil(10_000) { existe("imagem") }
        assertTrue(!existe("inicio-apps"))
    }

    @Test
    fun ca095_celularSemModoTelaVirtualContinuaNoPlanoB() {
        conectar(modo = null, apps = null)
        compose.waitUntil(10_000) { existe("imagem") }
        assertTrue(!existe("inicio-apps"))
    }

    @Test
    fun ca103_ligacaoNoModoTelaVirtualMostraAtendaNoCelularSemEncerrar() {
        conectar(avisos = setOf(Aviso.LIGACAO_NO_CELULAR))
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasText(Aviso.LIGACAO_NO_CELULAR.texto), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("botao-encerrar").assertIsDisplayed()
        compose.esperarTexto("estado", "espelhando")
    }
}
