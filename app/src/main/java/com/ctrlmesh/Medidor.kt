package com.ctrlmesh

/**
 * Medição de desempenho para o log (tag CtrlMesh): contadores por janela e atraso "excedente".
 * Os relógios dos dois aparelhos não são sincronizados; o atraso excedente é (chegada − origem) menos o menor valor
 * já visto, isto é, o quanto cada item demorou além do mais rápido. Some metade do RTT para ter a ordem do atraso total.
 */
class Medidor {
    private var deslocamentoMinimoMs = Long.MAX_VALUE
    private val atrasos = ArrayList<Long>()
    private val contadores = LinkedHashMap<String, Int>()

    @Synchronized
    fun contar(nome: String, quantidade: Int = 1) {
        contadores[nome] = (contadores[nome] ?: 0) + quantidade
    }

    /** [origemMs] no relógio do outro aparelho; [chegadaMs] no relógio local. */
    @Synchronized
    fun chegou(origemMs: Long, chegadaMs: Long) {
        deslocamentoMinimoMs = minOf(deslocamentoMinimoMs, chegadaMs - origemMs)
    }

    @Synchronized
    fun concluiu(origemMs: Long, agoraMs: Long) {
        if (deslocamentoMinimoMs != Long.MAX_VALUE && atrasos.size < LIMITE) atrasos += agoraMs - origemMs - deslocamentoMinimoMs
    }

    @Synchronized
    fun reiniciarRelogio() {
        deslocamentoMinimoMs = Long.MAX_VALUE
    }

    /** Linha de resumo da janela; zera contadores e atrasos. */
    @Synchronized
    fun resumo(janelaMs: Long): String {
        val segundos = janelaMs / 1000.0
        val taxas = contadores.entries.joinToString(" ") { "${it.key}=${"%.1f".format(it.value / segundos)}/s" }
        val ordenados = atrasos.sorted()
        val atraso = if (ordenados.isEmpty()) "atraso_excedente_ms=-" else
            "atraso_excedente_ms med=${ordenados[ordenados.size / 2]} p95=${ordenados[(ordenados.size * 95 / 100).coerceAtMost(ordenados.size - 1)]}"
        contadores.keys.forEach { contadores[it] = 0 }
        atrasos.clear()
        return "$taxas $atraso"
    }

    private companion object {
        const val LIMITE = 10_000
    }
}

fun agoraMs(): Long = System.nanoTime() / 1_000_000
