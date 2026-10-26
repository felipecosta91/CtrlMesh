package com.ctrlmesh

import android.app.NotificationManager
import android.app.Notification
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ctrlmesh.protocolo.Aviso
import com.ctrlmesh.protocolo.Papel
import com.ctrlmesh.protocolo.Protocolo
import java.net.InetSocketAddress
import java.net.Socket
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Escolha de papel, ajustes de primeira vez e escuta. Roda no emulador (ou no celular). */
@RunWith(AndroidJUnit4::class)
class PapelEAjustesTest {

    companion object {
        @org.junit.BeforeClass
        @JvmStatic
        fun salvarAparelho() = EstadoDoAparelho.salvar()

        @org.junit.AfterClass
        @JvmStatic
        fun restaurarAparelho() = EstadoDoAparelho.restaurar()
    }

    @get:Rule
    val compose = createEmptyComposeRule()

    private val pacote = "com.ctrlmesh"
    private var cenario: ActivityScenario<MainActivity>? = null
    private var acessibilidadeOriginal = ""

    @After
    fun limpar() {
        cenario?.close()
        contexto.stopService(Intent(contexto, ServicoDeEscuta::class.java))
        shell("appops set $pacote SYSTEM_ALERT_WINDOW allow")
        shell("appops set $pacote WRITE_SETTINGS allow")
        shell("dumpsys deviceidle whitelist +$pacote")
        if (acessibilidadeOriginal.isNotBlank()) shell("settings put secure enabled_accessibility_services $acessibilidadeOriginal")
    }

    private fun abrir() {
        cenario?.close()
        cenario = ActivityScenario.launch(MainActivity::class.java)
    }

    private fun item(tag: String) = compose.onNodeWithTag(tag)

    private fun assertItem(tag: String, situacao: String) {
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasTestTag(tag) and hasText(situacao, substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun assertTemAtalho(tag: String) {
        assertTrue(
            "$tag tem atalho Abrir ajuste",
            compose.onAllNodes(hasText("Abrir ajuste") and hasAnyAncestor(hasTestTag(tag)), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty(),
        )
    }

    private fun existe(texto: String, substring: Boolean = false) =
        compose.onAllNodes(hasText(texto, substring = substring, ignoreCase = true), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun ca072_primeiraAberturaPerguntaOPapelNaoPerguntaDeNovoERedefinirPerguntaOutraVez() {
        prepararApp(papel = null)
        abrir()
        compose.onNodeWithTag("papel-celular").assertIsDisplayed()
        compose.onNodeWithTag("papel-multimidia").assertIsDisplayed().performClick()
        assertEquals(Papel.MULTIMIDIA, ConfiguracaoDoApp(contexto).papel)

        abrir()
        compose.onNodeWithTag("papel-multimidia").assertDoesNotExist()

        compose.onNodeWithTag("botao-configuracoes").performClick()
        compose.onNodeWithTag("botao-redefinir").performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("papel-celular")).fetchSemanticsNodes().isNotEmpty() }
        assertNull(ConfiguracaoDoApp(contexto).papel)
    }

    @Test
    fun ca081_ca011_ca017_primeiraVezNaCentralMostraCadaAjusteQueFaltaComAtalhoESomeQuandoConcedido() {
        prepararApp(Papel.MULTIMIDIA)
        shell("appops set $pacote SYSTEM_ALERT_WINDOW deny")
        shell("dumpsys deviceidle whitelist -$pacote")
        abrir()

        assertItem("ajuste-sobrepor", "falta")
        assertTemAtalho("ajuste-sobrepor")
        assertItem("ajuste-bateria", "falta")
        assertTemAtalho("ajuste-bateria")
        item("ajuste-rede").assertIsDisplayed()
        assertTrue(existe("Salve a rede do celular com conexão automática", substring = true))
        compose.onNodeWithTag("aviso-bateria").assertIsDisplayed()
        assertTrue(existe(Aviso.FALTA_BATERIA.texto))
        assertTrue("não oferece ligar o roteador", !existe("ligar o roteador", substring = true))

        shell("appops set $pacote SYSTEM_ALERT_WINDOW allow")
        shell("dumpsys deviceidle whitelist +$pacote")
        abrir()

        assertItem("ajuste-sobrepor", "ok")
        assertItem("ajuste-bateria", "ok")
        compose.onNodeWithTag("aviso-bateria").assertDoesNotExist()
    }

    @Test
    fun ca068_ca023_celularMostraGirarATelaSobreporEAcessibilidadeFaltandoComAtalho() {
        prepararApp(Papel.CELULAR)
        acessibilidadeOriginal = shell("settings get secure enabled_accessibility_services").trim().takeUnless { it == "null" } ?: ""
        shell("appops set $pacote WRITE_SETTINGS deny")
        shell("appops set $pacote SYSTEM_ALERT_WINDOW deny")
        shell("settings delete secure enabled_accessibility_services")
        abrir()

        listOf("autorizacao-girar-tela", "autorizacao-sobrepor", "autorizacao-acessibilidade").forEach {
            assertItem(it, "falta")
            assertTemAtalho(it)
        }

        shell("appops set $pacote WRITE_SETTINGS allow")
        shell("appops set $pacote SYSTEM_ALERT_WINDOW allow")
        abrir()
        assertItem("autorizacao-girar-tela", "ok")
        assertItem("autorizacao-sobrepor", "ok")
    }

    @Test
    fun ca085_ca086_celularMostraSeALiberacaoPermanenteDeCapturaEstaValendo() {
        prepararApp(Papel.CELULAR)
        shell("appops set $pacote PROJECT_MEDIA default")
        abrir()
        assertItem("liberacao-permanente", "falta")
        assertTrue(existe(Aviso.SEM_LIBERACAO_PERMANENTE.texto))

        shell("appops set $pacote PROJECT_MEDIA allow")
        abrir()
        assertItem("liberacao-permanente", "ok")
        assertTrue(!existe(Aviso.SEM_LIBERACAO_PERMANENTE.texto))
    }

    @Test
    fun ca104_celularSemShizukuMostraOPassoAPassoEOEstadoShizukuDesligado() {
        prepararApp(Papel.CELULAR)
        abrir()
        assertItem("shizuku", "Shizuku desligado")
        compose.onNodeWithTag("shizuku-passo-a-passo").assertExists()
    }

    @Test
    fun ca017_celularSujeitoAEconomiaDeBateriaMostraOAvisoEEleSomeAoCorrigir() {
        prepararApp(Papel.CELULAR)
        shell("dumpsys deviceidle whitelist -$pacote")
        abrir()
        compose.onNodeWithTag("aviso-bateria").assertIsDisplayed()
        assertItem("autorizacao-bateria", "falta")

        shell("dumpsys deviceidle whitelist +$pacote")
        abrir()
        compose.onNodeWithTag("aviso-bateria").assertDoesNotExist()
    }

    @Test
    fun ca074_ca071_celularNaoTemPareamentoNemOpcaoDeEspelharACentral() {
        prepararApp(Papel.CELULAR)
        abrir()
        listOf("parear", "pareamento", "centrais autorizadas", "espelhar a central").forEach {
            assertTrue("não deveria existir \"$it\"", !existe(it, substring = true))
        }
    }

    @Test
    fun ca077_aoLigarOAparelhoSemAbrirOAppOCelularEntraEmEscutaComNotificacaoPermanente() {
        prepararApp(Papel.CELULAR)
        val receptores = contexto.packageManager.queryBroadcastReceivers(
            Intent(Intent.ACTION_BOOT_COMPLETED).setPackage(pacote), 0,
        )
        assertTrue("ReceptorDeInicio registrado para BOOT_COMPLETED", receptores.any { it.activityInfo.name == ReceptorDeInicio::class.java.name })

        ReceptorDeInicio().onReceive(contexto, Intent(Intent.ACTION_BOOT_COMPLETED))

        val notificacoes = contexto.getSystemService(NotificationManager::class.java)
        val limite = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < limite &&
            notificacoes.activeNotifications.none { it.notification.flags and Notification.FLAG_ONGOING_EVENT != 0 }
        ) {
            Thread.sleep(200)
        }
        assertTrue(notificacoes.activeNotifications.any { it.notification.flags and Notification.FLAG_ONGOING_EVENT != 0 })
        assertTrue("escutando na porta do contrato", conectaNaPorta(timeoutMs = 10_000))
    }

    @Test
    fun ca077_aoLigarACentralSemAbrirOAppElaProcuraOCelularSozinha() {
        prepararApp(Papel.MULTIMIDIA, enderecoDoCelular = "127.0.0.1")
        CelularFalso().use { falso ->
            ReceptorDeInicio().onReceive(contexto, Intent(Intent.ACTION_BOOT_COMPLETED))
            val limite = System.currentTimeMillis() + 15_000
            while (System.currentTimeMillis() < limite && falso.conexoes.get() == 0) Thread.sleep(200)
            assertTrue(falso.conexoes.get() >= 1)
        }
    }

    @Test
    fun ca075_conexaoQueNaoVemDoRoteadorDoCelularEFechadaSemNenhumByte() {
        prepararApp(Papel.CELULAR)
        ReceptorDeInicio().onReceive(contexto, Intent(Intent.ACTION_BOOT_COMPLETED))
        assertTrue(conectaNaPorta(timeoutMs = 10_000))

        Socket().use { socket ->
            socket.connect(InetSocketAddress("127.0.0.1", Protocolo.PORTA), 2_000)
            socket.soTimeout = 5_000
            assertEquals("celular fecha sem enviar o aperto", -1, socket.getInputStream().read())
        }
    }

    private fun conectaNaPorta(timeoutMs: Long): Boolean {
        val limite = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < limite) {
            val conectou = runCatching {
                Socket().use { it.connect(InetSocketAddress("127.0.0.1", Protocolo.PORTA), 500) }
            }.isSuccess
            if (conectou) return true
            Thread.sleep(300)
        }
        return false
    }
}
