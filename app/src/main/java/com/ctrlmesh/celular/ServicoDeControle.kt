package com.ctrlmesh.celular

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.ctrlmesh.MapeadorDeToque
import com.ctrlmesh.protocolo.AcaoToque
import com.ctrlmesh.protocolo.Mensagem
import com.ctrlmesh.protocolo.TeclaDeSistema

/**
 * Executa no celular os toques e teclas vindos da central (RN-035, RN-037).
 * Os toques vão ao vivo: cada dedo é um traço contínuo (`continueStroke`), despachado em trechos curtos enquanto o
 * dedo se move; um trecho novo só sai quando o anterior termina, juntando os pontos que chegaram nesse meio-tempo.
 */
class ServicoDeControle : AccessibilityService() {

    private class Dedo(var ultimo: Pair<Float, Float>) {
        var traco: GestureDescription.StrokeDescription? = null
        val pontos = ArrayList<Pair<Float, Float>>()
        var levantou = false
        var ultimoDespachoMs = SystemClock.uptimeMillis()
    }

    private val dedos = LinkedHashMap<Int, Dedo>()
    private var emAndamento = false

    private val aoTerminarTrecho = object : GestureResultCallback() {
        override fun onCompleted(gestureDescription: GestureDescription?) = trechoTerminou(cancelado = false)
        override fun onCancelled(gestureDescription: GestureDescription?) = trechoTerminou(cancelado = true)
    }

    override fun onServiceConnected() {
        instancia = this
    }

    override fun onDestroy() {
        if (instancia === this) instancia = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    fun tecla(tecla: TeclaDeSistema) {
        performGlobalAction(
            when (tecla) {
                TeclaDeSistema.VOLTAR -> GLOBAL_ACTION_BACK
                TeclaDeSistema.INICIO -> GLOBAL_ACTION_HOME
                TeclaDeSistema.RECENTES -> GLOBAL_ACTION_RECENTS
            },
        )
    }

    @Synchronized
    fun toque(toque: Mensagem.Toque) {
        val (largura, altura) = tamanhoDaTela()
        val ponto = MapeadorDeToque.noCelular(toque, largura, altura)
        val dedo = dedos[toque.ponteiro]
        when (toque.acao) {
            AcaoToque.DESCE -> if (ponto != null && dedo == null) dedos[toque.ponteiro] = Dedo(ponto).also { it.pontos += ponto }
            AcaoToque.MOVE -> if (ponto != null && dedo != null && !dedo.levantou) dedo.pontos += ponto
            AcaoToque.SOBE, AcaoToque.CANCELA -> if (dedo != null) {
                if (ponto != null && toque.acao == AcaoToque.SOBE) dedo.pontos += ponto
                dedo.levantou = true
            }
        }
        despachar()
    }

    /** Monta um trecho com todos os dedos na tela (os parados continuam no lugar) e despacha. */
    private fun despachar() {
        if (emAndamento) return
        if (dedos.values.none { it.pontos.isNotEmpty() || it.levantou }) return
        val agora = SystemClock.uptimeMillis()
        val gesto = GestureDescription.Builder()
        dedos.values.take(GestureDescription.getMaxStrokeCount()).forEach { dedo ->
            val caminho = Path().apply { moveTo(dedo.ultimo.first, dedo.ultimo.second) }
            // Traço novo começa no próprio ponto; um segmento (mesmo de tamanho zero) é obrigatório.
            val pontos = dedo.pontos.ifEmpty { listOf(dedo.ultimo) }
            pontos.forEach { caminho.lineTo(it.first, it.second) }
            val duracao = (agora - dedo.ultimoDespachoMs).coerceIn(TRECHO_MINIMO_MS, TRECHO_MAXIMO_MS)
            val anterior = dedo.traco
            dedo.traco = if (anterior == null) {
                GestureDescription.StrokeDescription(caminho, 0, duracao, !dedo.levantou)
            } else {
                anterior.continueStroke(caminho, 0, duracao, !dedo.levantou)
            }
            gesto.addStroke(dedo.traco!!)
            dedo.ultimo = pontos.last()
            dedo.pontos.clear()
            dedo.ultimoDespachoMs = agora
        }
        dedos.values.removeAll { it.levantou }
        emAndamento = runCatching { dispatchGesture(gesto.build(), aoTerminarTrecho, null) }.getOrDefault(false)
        if (!emAndamento) dedos.clear()
    }

    @Synchronized
    private fun trechoTerminou(cancelado: Boolean) {
        emAndamento = false
        // Cancelado (ex.: o dono tocou na tela do celular): não dá para continuar os traços; recomeça do zero.
        if (cancelado) dedos.clear() else despachar()
    }

    private fun tamanhoDaTela() = TelaDoCelular.tamanho(this)

    companion object {
        private const val TRECHO_MINIMO_MS = 1L
        private const val TRECHO_MAXIMO_MS = 50L

        @Volatile var instancia: ServicoDeControle? = null
            private set
    }
}
