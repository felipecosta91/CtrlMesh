package com.ctrlmesh

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ctrlmesh.celular.TelaCelular
import com.ctrlmesh.central.TelaCentral
import com.ctrlmesh.protocolo.Papel

class MainActivity : ComponentActivity() {
    private val config by lazy { ConfiguracaoDoApp(this) }
    private var retomadas by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                App(retomadas)
            }
        }
        ajustarJanela(config.papel)
    }

    override fun onResume() {
        super.onResume()
        visivel = true
        retomadas++
        ServicoDeEscuta.iniciar(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
    }

    override fun onPause() {
        visivel = false
        super.onPause()
    }

    /** RN-024: central sempre em paisagem e em tela cheia; celular com orientação livre. */
    private fun ajustarJanela(papel: Papel?) {
        val central = papel == Papel.MULTIMIDIA
        requestedOrientation = if (central) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.decorView.windowInsetsController?.let {
                if (central) {
                    it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    it.hide(WindowInsets.Type.systemBars())
                } else {
                    it.show(WindowInsets.Type.systemBars())
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun App(retomadas: Int) {
        var papel by remember { mutableStateOf(config.papel) }
        val redefinir = {
            config.redefinir()
            stopService(Intent(this, ServicoDeEscuta::class.java))
            papel = null
            ajustarJanela(null)
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .semantics { testTagsAsResourceId = true },
        ) {
            when (papel) {
                null -> EscolhaDePapel { escolhido ->
                    config.papel = escolhido
                    papel = escolhido
                    ajustarJanela(escolhido)
                    ServicoDeEscuta.iniciar(this@MainActivity)
                }
                Papel.MULTIMIDIA -> TelaCentral(retomadas, redefinir)
                Papel.CELULAR -> TelaCelular(retomadas, redefinir)
            }
        }
    }

    companion object {
        @Volatile var visivel = false
            private set
    }
}

@Composable
private fun EscolhaDePapel(aoEscolher: (Papel) -> Unit) {
    var recusado by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Este aparelho é o celular ou a multimídia?", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Button(
            onClick = { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) aoEscolher(Papel.CELULAR) else recusado = true },
            modifier = Modifier.testTag("papel-celular").sizeIn(minWidth = 220.dp, minHeight = 76.dp),
        ) { Text("Celular") }
        Button(
            onClick = { aoEscolher(Papel.MULTIMIDIA) },
            modifier = Modifier.testTag("papel-multimidia").sizeIn(minWidth = 220.dp, minHeight = 76.dp),
        ) { Text("Multimídia") }
        if (recusado) Text("o papel celular exige Android 10 ou superior", color = Color.White)
    }
}

/** Item de ajuste/autorização: texto com "ok" ou "falta" e atalho quando falta (RN-072, RN-085). */
@Composable
fun ItemDeAjuste(tag: String, rotulo: String, ok: Boolean, abrir: (() -> Unit)?, detalheQuandoFalta: String? = null) {
    Row(
        Modifier.fillMaxWidth().testTag(tag).semantics(mergeDescendants = true) {}.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text("$rotulo: ${if (ok) "ok" else "falta"}", color = Color.White)
            if (!ok && detalheQuandoFalta != null) Text(detalheQuandoFalta, color = Color(0xFFFFC107))
        }
        if (!ok && abrir != null) {
            Button(onClick = abrir, modifier = Modifier.sizeIn(minHeight = 76.dp)) { Text("Abrir ajuste") }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DialogoDeConfiguracoes(central: Boolean, aoFechar: () -> Unit, aoRedefinir: () -> Unit) {
    val activity = androidx.compose.ui.platform.LocalContext.current
    val config = remember { ConfiguracaoDoApp(activity) }
    var endereco by remember { mutableStateOf(config.enderecoDoCelular.orEmpty()) }
    Dialog(onDismissRequest = aoFechar) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(
                Modifier.semantics { testTagsAsResourceId = true }.padding(24.dp).width(420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Configurações", style = MaterialTheme.typography.titleLarge)
                if (central) {
                    OutlinedTextField(
                        value = endereco,
                        onValueChange = { endereco = it },
                        label = { Text("Endereço do celular (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("campo-endereco"),
                    )
                    Button(
                        onClick = { config.enderecoDoCelular = endereco },
                        modifier = Modifier.testTag("botao-salvar-endereco").sizeIn(minHeight = 76.dp),
                    ) { Text("Salvar endereço") }
                }
                Button(
                    onClick = {
                        aoFechar()
                        aoRedefinir()
                    },
                    modifier = Modifier.testTag("botao-redefinir").sizeIn(minHeight = 76.dp),
                ) { Text("Redefinir o CtrlMesh") }
                TextButton(onClick = aoFechar, modifier = Modifier.sizeIn(minHeight = 76.dp)) { Text("Fechar") }
            }
        }
    }
}

fun abrirAjuste(context: android.content.Context, acao: String, comPacote: Boolean = true) {
    val intent = Intent(acao).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (comPacote) intent.data = Uri.parse("package:${context.packageName}")
    runCatching { context.startActivity(intent) }.onFailure {
        runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
