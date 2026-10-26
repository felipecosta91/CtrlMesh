package com.ctrlmesh.central

import android.os.SystemClock
import android.provider.Settings
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.ctrlmesh.DialogoDeConfiguracoes
import com.ctrlmesh.ItemDeAjuste
import com.ctrlmesh.MapeadorDeToque
import com.ctrlmesh.abrirAjuste
import com.ctrlmesh.celular.Autorizacoes
import com.ctrlmesh.protocolo.AcaoToque
import com.ctrlmesh.protocolo.Aviso
import com.ctrlmesh.protocolo.Mensagem
import com.ctrlmesh.protocolo.MotivoEncerramento
import com.ctrlmesh.protocolo.TeclaDeSistema
import com.ctrlmesh.registrar

private val FUNDO_DA_BARRA = Color(0xCC101114)
private val SONS_NAO_ENVIAVEIS = listOf("ligações", "alarmes", "toques", "notificações", "voz de navegação", "apps que bloqueiam a captura")

@Composable
fun TelaCentral(retomadas: Int, aoRedefinir: () -> Unit) {
    val tela by ControladorCentral.estadoDaTela.collectAsState()
    var configuracoes by remember { mutableStateOf(false) }
    val estado = tela.estado
    if (estado.conectado || estado is EstadoCentral.Reconectando) {
        Sessao(tela) { configuracoes = true }
    } else {
        Inicio(tela, retomadas) { configuracoes = true }
    }
    if (configuracoes) DialogoDeConfiguracoes(central = true, aoFechar = { configuracoes = false }, aoRedefinir = aoRedefinir)
}

@Composable
private fun Inicio(tela: TelaCentralEstado, retomadas: Int, abrirConfiguracoes: () -> Unit) {
    val context = LocalContext.current
    val estado = tela.estado
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(estado.texto, Modifier.testTag("estado"), color = Color.White, fontSize = 28.sp)
        if (estado is EstadoCentral.Encerrado) {
            if (estado.motivo == MotivoEncerramento.NAO_CONFIRMADO_NO_CELULAR) {
                Button({ ControladorCentral.atual?.tentarDeNovo() }, Modifier.testTag("botao-tentar-de-novo").sizeIn(minWidth = 160.dp, minHeight = 76.dp)) {
                    Text("tentar de novo")
                }
            } else {
                Button({ ControladorCentral.atual?.espelhar() }, Modifier.testTag("botao-espelhar").sizeIn(minWidth = 160.dp, minHeight = 76.dp)) {
                    Text("espelhar")
                }
            }
        }
        Avisos(tela.avisos)
        // Lido a cada retomada da tela, para refletir ajustes concedidos fora do app.
        val sobrepor = remember(retomadas) { Autorizacoes.sobrepor(context) }
        val bateria = remember(retomadas) { Autorizacoes.bateria(context) }
        if (estado != EstadoCentral.Espelhando) {
            ItemDeAjuste("ajuste-sobrepor", "Aparecer sobre outros apps", sobrepor, { abrirAjuste(context, Settings.ACTION_MANAGE_OVERLAY_PERMISSION) })
            ItemDeAjuste(
                "ajuste-bateria", "Fora da economia de bateria", bateria,
                { abrirAjuste(context, Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) },
            )
            Row(
                Modifier.fillMaxWidth().testTag("ajuste-rede"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Salve a rede do celular com conexão automática", Modifier.weight(1f), color = Color.White)
                Button({ abrirAjuste(context, Settings.ACTION_WIFI_SETTINGS, comPacote = false) }, Modifier.sizeIn(minHeight = 76.dp)) {
                    Text("Abrir ajuste")
                }
            }
        }
        if (!bateria) Text(Aviso.FALTA_BATERIA.texto, Modifier.testTag("aviso-bateria"), color = Color(0xFFFFC107))
        Button(abrirConfiguracoes, Modifier.testTag("botao-configuracoes").sizeIn(minWidth = 160.dp, minHeight = 76.dp)) {
            Text("Configurações")
        }
    }
}

@Composable
private fun Sessao(tela: TelaCentralEstado, abrirConfiguracoes: () -> Unit) {
    val densidade = LocalDensity.current
    val view = LocalView.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val alturaBarra = with(densidade) { ControladorCentral.ALTURA_DA_BARRA_DP.dp.roundToPx() }
        val video = tela.video
        // Na primeira medição a área pode vir menor que a barra (ou zero); sem espaço, sem imagem.
        val cabe = constraints.maxHeight > alturaBarra && constraints.maxWidth > 0
        val virtual = tela.modo == com.ctrlmesh.protocolo.ModoEspelhamento.TELA_VIRTUAL
        val inicio = virtual && tela.mostrarInicio && tela.estado != EstadoCentral.AguardandoConfirmacao
        val layout = when {
            !cabe || video == null || inicio -> null
            virtual -> Layout.calcularTelaVirtual(constraints.maxWidth, constraints.maxHeight, alturaBarra)
            else -> Layout.calcular(constraints.maxWidth, constraints.maxHeight, video.first, video.second, alturaBarra)
        }
        val larguraArea = constraints.maxWidth
        val alturaArea = constraints.maxHeight - alturaBarra
        LaunchedEffect(larguraArea, alturaArea) { ControladorCentral.atual?.informarArea(larguraArea, alturaArea) }
        Box(Modifier.fillMaxSize().pointerInput(layout, video) { encaminharToques(layout, video) }) {
            if (inicio) InicioDoCtrlMeshNaTela(tela.apps)
            if (layout != null && !inicio) {
                val imagem = layout.imagem
                LaunchedEffect(imagem) {
                    val tela0 = IntArray(2).also(view::getLocationOnScreen)
                    registrar(
                        "layout imagem=${imagem.esquerda + tela0[0]},${imagem.topo + tela0[1]}," +
                            "${imagem.direita + tela0[0]},${imagem.base + tela0[1]}",
                    )
                }
                AndroidView(
                    factory = { contexto -> SurfaceView(contexto).apply { holder.addCallback(SuperficieDoVideo) } },
                    modifier = Modifier.noRetangulo(imagem).testTag("imagem"),
                )
                if (tela.estado.imagemDesatualizada) {
                    Text(
                        "imagem desatualizada",
                        Modifier.absoluteOffset { IntOffset(imagem.esquerda, imagem.topo) }
                            .background(Color(0xCC000000)).padding(12.dp).testTag("imagem-desatualizada"),
                        color = Color(0xFFFFC107),
                    )
                }
            } else if (!inicio) {
                Box(Modifier.fillMaxSize().padding(bottom = ControladorCentral.ALTURA_DA_BARRA_DP.dp), contentAlignment = Alignment.Center) {
                    if (tela.estado == EstadoCentral.AguardandoConfirmacao) {
                        Text("toque em Permitir no celular", Modifier.testTag("instrucao"), color = Color.White, fontSize = 28.sp)
                    }
                }
            }
            Barra(tela, abrirConfiguracoes, Modifier.align(Alignment.BottomStart))
        }
    }
}

/** Início do CtrlMesh (RN-095): apps do celular em grade, na ordem de [InicioDoCtrlMesh.ordenar]. */
@Composable
private fun InicioDoCtrlMeshNaTela(apps: List<com.ctrlmesh.protocolo.AppDoCelular>) {
    LaunchedEffect(apps.size) { registrar("inicio=${apps.size}") }
    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
        columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(132.dp),
        modifier = Modifier.fillMaxSize().padding(bottom = ControladorCentral.ALTURA_DA_BARRA_DP.dp).testTag("inicio-apps"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(apps.size, key = { apps[it].pacote }) { i ->
            val app = apps[i]
            Column(
                Modifier
                    .testTag("app-${app.pacote}")
                    .sizeIn(minWidth = 76.dp, minHeight = 76.dp)
                    .clickable { ControladorCentral.atual?.abrirApp(app.pacote) }
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val imagem = remember(app.pacote) {
                    android.graphics.BitmapFactory.decodeByteArray(app.iconePng, 0, app.iconePng.size)?.asImageBitmap()
                }
                if (imagem != null) {
                    Image(imagem, app.rotulo, Modifier.size(72.dp).testTag("icone-${app.pacote}"))
                } else {
                    Box(Modifier.size(72.dp).background(Color.DarkGray).testTag("icone-${app.pacote}"))
                }
                Text(app.rotulo, color = Color.White, fontSize = 16.sp, maxLines = 2, textAlign = TextAlign.Center)
            }
        }
    }
}

/** Superfície atual da imagem, guardada fora do controlador: a tela e o serviço têm ciclos de vida diferentes. */
object SuperficieDoVideo : SurfaceHolder.Callback {
    @Volatile var superficie: android.view.Surface? = null
        private set

    override fun surfaceCreated(holder: SurfaceHolder) = Unit
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        superficie = holder.surface
        ControladorCentral.atual?.video?.definirSuperficie(holder.surface)
    }
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        if (superficie === holder.surface) superficie = null
        ControladorCentral.atual?.video?.definirSuperficie(superficie)
    }
}

private fun Modifier.noRetangulo(r: Retangulo) = absoluteOffset { IntOffset(r.esquerda, r.topo) }
    .layout { medida, _ ->
        val largura = r.largura.coerceAtLeast(0)
        val altura = r.altura.coerceAtLeast(0)
        val colocavel = medida.measure(Constraints.fixed(largura, altura))
        layout(largura, altura) { colocavel.place(0, 0) }
    }

/** RN-035/RN-036: só toques que começam na imagem vão ao celular; até 10 dedos. */
private suspend fun PointerInputScope.encaminharToques(layout: LayoutCentral?, quadro: Pair<Int, Int>?) {
    if (layout == null || quadro == null) return
    val img = layout.imagem
    val ponteiros = HashMap<PointerId, Int>()
    fun normalizado(p: Offset) = ((p.x - img.esquerda) / img.largura).coerceIn(0f, 1f) to ((p.y - img.topo) / img.altura).coerceIn(0f, 1f)
    fun enviar(ponteiro: Int, acao: AcaoToque, xy: Pair<Float, Float>) {
        ControladorCentral.atual?.toque(
            Mensagem.Toque(ponteiro, acao, xy.first, xy.second, quadro.first, quadro.second, SystemClock.uptimeMillis()),
        )
    }
    awaitPointerEventScope {
        while (true) {
            val evento = awaitPointerEvent()
            for (mudanca in evento.changes) {
                when {
                    mudanca.changedToDownIgnoreConsumed() -> {
                        val ponto = MapeadorDeToque.naCentral(layout, mudanca.position.x, mudanca.position.y) ?: continue
                        val livre = (0 until MAXIMO_DE_DEDOS).firstOrNull { it !in ponteiros.values } ?: continue
                        ponteiros[mudanca.id] = livre
                        enviar(livre, AcaoToque.DESCE, ponto.x to ponto.y)
                    }
                    mudanca.changedToUpIgnoreConsumed() -> {
                        val ponteiro = ponteiros.remove(mudanca.id) ?: continue
                        enviar(ponteiro, AcaoToque.SOBE, normalizado(mudanca.position))
                    }
                    mudanca.pressed && mudanca.position != mudanca.previousPosition -> {
                        val ponteiro = ponteiros[mudanca.id] ?: continue
                        enviar(ponteiro, AcaoToque.MOVE, normalizado(mudanca.position))
                    }
                }
            }
        }
    }
}

private const val MAXIMO_DE_DEDOS = 10

/**
 * Barra embaixo (RN-109, RN-110): faixa reservada na largura toda, fundo escuro translúcido, botões com símbolo e
 * rótulo curto (alvos ≥ 76 dp) e, à direita, o indicador da conexão (toque abre os ajustes), sons e avisos.
 */
@Composable
private fun Barra(tela: TelaCentralEstado, abrirConfiguracoes: () -> Unit, modifier: Modifier) {
    val controlador = { ControladorCentral.atual }
    var sons by remember { mutableStateOf(false) }
    Row(
        modifier
            .testTag("barra")
            .fillMaxWidth()
            .height(ControladorCentral.ALTURA_DA_BARRA_DP.dp)
            .background(FUNDO_DA_BARRA)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BotaoDaBarra("botao-voltar", "◁", "Voltar") { controlador()?.tecla(Mensagem.Tecla(TeclaDeSistema.VOLTAR)) }
        BotaoDaBarra("botao-inicio", "○", "Início") { controlador()?.tecla(Mensagem.Tecla(TeclaDeSistema.INICIO)) }
        BotaoDaBarra("botao-recentes", "▢", "Recentes") { controlador()?.tecla(Mensagem.Tecla(TeclaDeSistema.RECENTES)) }
        BotaoDaBarra("botao-menos", "−", "Volume") { controlador()?.mudarVolume { menos() } }
        BotaoDaBarra("botao-mais", "+", "Volume") { controlador()?.mudarVolume { mais() } }
        BotaoDaBarra("botao-mudo", if (tela.mudo) "♪" else "×", if (tela.mudo) "Som" else "Mudo") { controlador()?.mudarVolume { alternarMudo() } }
        BotaoDaBarra("botao-ajustes", "⚙", "Ajustes", onClick = abrirConfiguracoes)
        BotaoDaBarra("botao-encerrar", "■", "Encerrar", Color(0xFFB3261E)) { controlador()?.encerrar() }
        // Indicador discreto da conexão; o ponto (alvo de 76 dp) também abre os ajustes.
        Box(
            Modifier.testTag("botao-configuracoes").size(76.dp).clickable(onClick = abrirConfiguracoes),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(12.dp).background(corDoEstado(tela.estado), androidx.compose.foundation.shape.CircleShape))
        }
        Text(tela.estado.texto, Modifier.testTag("estado"), color = Color(0xFFE0E0E0), fontSize = 14.sp, maxLines = 1)
        Box {
            TextButton({ sons = !sons }, Modifier.heightIn(min = 76.dp).testTag("sons-nao-enviaveis")) {
                Text("alguns sons não chegam à central", fontSize = 13.sp, color = Color(0xFFBDBDBD))
            }
            androidx.compose.material3.DropdownMenu(expanded = sons, onDismissRequest = { sons = false }) {
                SONS_NAO_ENVIAVEIS.forEach { Text(it, Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
            }
        }
        Avisos(tela.avisos)
    }
}

private fun corDoEstado(estado: EstadoCentral) = when (estado) {
    EstadoCentral.Espelhando -> Color(0xFF4CAF50)
    is EstadoCentral.Reconectando, EstadoCentral.DesbloqueieCelular, EstadoCentral.AguardandoConfirmacao -> Color(0xFFFFC107)
    else -> Color(0xFF9E9E9E)
}

@Composable
private fun BotaoDaBarra(tag: String, simbolo: String, rotulo: String, cor: Color? = null, onClick: () -> Unit) {
    val view = LocalView.current
    val ultimo = remember { arrayOf("") } // não é estado da tela: só evita log repetido
    Button(
        onClick = onClick,
        modifier = Modifier
            .size(width = 84.dp, height = 76.dp)
            .testTag(tag)
            .onGloballyPositioned {
                val b = it.boundsInWindow()
                val janela = IntArray(2).also(view::getLocationOnScreen)
                val linha = "botao=$tag ${b.left.toInt() + janela[0]},${b.top.toInt() + janela[1]},${b.right.toInt() + janela[0]},${b.bottom.toInt() + janela[1]}"
                if (linha != ultimo[0]) {
                    ultimo[0] = linha
                    registrar(linha)
                }
            },
        shape = MaterialTheme.shapes.large,
        contentPadding = PaddingValues(4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = cor ?: Color(0x33FFFFFF),
            contentColor = Color.White,
        ),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(simbolo, fontSize = 22.sp, maxLines = 1)
            Text(rotulo, fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
private fun Avisos(avisos: Set<Aviso>) {
    if (avisos.isNotEmpty()) {
        Column(Modifier.testTag("avisos").widthIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            avisos.forEach { Text(it.texto, color = Color(0xFFFFC107), fontSize = 13.sp) }
        }
    }
}
