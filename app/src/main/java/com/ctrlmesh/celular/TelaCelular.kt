package com.ctrlmesh.celular

import android.Manifest
import android.app.Activity
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ctrlmesh.DialogoDeConfiguracoes
import com.ctrlmesh.ItemDeAjuste
import com.ctrlmesh.abrirAjuste
import com.ctrlmesh.protocolo.Aviso

@Composable
fun TelaCelular(retomadas: Int, aoRedefinir: () -> Unit) {
    val context = LocalContext.current
    val estado by EstadoDaTelaDoCelular.valor.collectAsState()
    var configuracoes by remember { mutableStateOf(false) }
    // Lidas a cada retomada da tela: o dono concede os ajustes fora do app.
    val acessibilidade = remember(retomadas) { Autorizacoes.acessibilidade(context) }
    val girar = remember(retomadas) { Autorizacoes.girarTela(context) }
    val sobrepor = remember(retomadas) { Autorizacoes.sobrepor(context) }
    val audio = remember(retomadas) { Autorizacoes.gravarAudio(context) }
    val bateria = remember(retomadas) { Autorizacoes.bateria(context) }
    val liberacao = remember(retomadas) { Autorizacoes.liberacaoPermanente(context) }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("CtrlMesh — celular", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Text(estado, Modifier.testTag("estado"), color = Color.White)
        ItemDeAjuste(
            "autorizacao-acessibilidade", "Controle pela central (acessibilidade)", acessibilidade,
            { abrirAjuste(context, Settings.ACTION_ACCESSIBILITY_SETTINGS, comPacote = false) },
            "se o Android bloquear, libere as configurações restritas do CtrlMesh nas informações do app",
        )
        ItemDeAjuste("autorizacao-girar-tela", "Girar a tela", girar, { abrirAjuste(context, Settings.ACTION_MANAGE_WRITE_SETTINGS) })
        ItemDeAjuste("autorizacao-sobrepor", "Aparecer sobre outros apps", sobrepor, { abrirAjuste(context, Settings.ACTION_MANAGE_OVERLAY_PERMISSION) })
        ItemDeAjuste("autorizacao-gravar-audio", "Gravar áudio", audio, {
            (context as? Activity)?.requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        })
        ItemDeAjuste(
            "autorizacao-bateria", "Fora da economia de bateria", bateria,
            { abrirAjuste(context, Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) },
        )
        ItemDeAjuste("liberacao-permanente", "Liberação permanente de captura", liberacao, null, Aviso.SEM_LIBERACAO_PERMANENTE.texto)
        ItemDoShizuku(retomadas)
        if (!bateria) Text(Aviso.FALTA_BATERIA.texto, Modifier.testTag("aviso-bateria"), color = Color(0xFFFFC107))
        Button({ configuracoes = true }, Modifier.testTag("botao-configuracoes").sizeIn(minHeight = 76.dp)) { Text("Configurações") }
    }
    if (configuracoes) DialogoDeConfiguracoes(central = false, aoFechar = { configuracoes = false }, aoRedefinir = aoRedefinir)
}

/** RN-102: estado do Shizuku, passo a passo e pedido de autorização (contrato §7.1, §10.1). */
@Composable
private fun ItemDoShizuku(retomadas: Int) {
    val context = LocalContext.current
    val avisado by EstadoDaTelaDoCelular.shizuku.collectAsState()
    val estado = remember(retomadas, avisado) { com.ctrlmesh.celular.virtual.ClienteDoAjudante(context).estado() }
    androidx.compose.foundation.layout.Row(
        androidx.compose.ui.Modifier.testTag("shizuku").semantics(mergeDescendants = true) {}.padding(vertical = 4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Tela horizontal na central: ${estado.texto}", androidx.compose.ui.Modifier.weight(1f), color = Color.White)
        if (estado == EstadoShizuku.SEM_AUTORIZACAO) {
            Button(
                onClick = { runCatching { rikka.shizuku.Shizuku.requestPermission(com.ctrlmesh.celular.virtual.ClienteDoAjudante.CODIGO_PEDIDO) } },
                modifier = Modifier.testTag("botao-autorizar-shizuku").sizeIn(minHeight = 76.dp),
            ) { Text("Autorizar no Shizuku") }
        }
    }
    if (estado != EstadoShizuku.ATIVO) {
        Text(
            "Para a tela horizontal na central (sem girar o celular): 1. instale o app Shizuku; " +
                "2. ligue o Shizuku pela depuração sem fio (o celular precisa estar num Wi-Fi); 3. autorize o CtrlMesh no Shizuku.",
            Modifier.testTag("shizuku-passo-a-passo"),
            color = Color.LightGray,
        )
    }
}
