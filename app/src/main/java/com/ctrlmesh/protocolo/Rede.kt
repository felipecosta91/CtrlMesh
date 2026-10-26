package com.ctrlmesh.protocolo

data class Alocacao(val audioKbps: Int, val videoKbps: Int)

/** Som constante (Opus estéreo); o vídeo absorve a falta de banda (RN-067). */
object PoliticaDeBanda {
    const val AUDIO_KBPS = 128
    const val VIDEO_MAXIMO_KBPS = 8_000

    fun alocar(bandaKbps: Int): Alocacao =
        Alocacao(AUDIO_KBPS, (bandaKbps - AUDIO_KBPS).coerceIn(0, VIDEO_MAXIMO_KBPS))
}

/**
 * Fila do celular: controle > som > vídeo. Acima de [limiteBytes] descarta vídeo (nunca som) e
 * só volta a aceitar vídeo num quadro-chave (RN-056, RN-067).
 */
class FilaDeEnvio(private val limiteBytes: Int) {
    private val controle = ArrayDeque<Mensagem>()
    private val som = ArrayDeque<Mensagem.AudioQuadro>()
    private val video = ArrayDeque<Mensagem>()
    private var bytes = 0
    private var aguardandoChave = false
    private val trava = Object()

    val bytesNaFila: Int get() = synchronized(trava) { bytes }

    /** Total de quadros de vídeo descartados (medição). */
    var descartesDeVideo = 0
        private set

    /** Chamado quando um quadro de vídeo é descartado: o codificador deve gerar um quadro-chave logo. */
    @Volatile var aoDescartarVideo: (() -> Unit)? = null

    fun ofertar(mensagem: Mensagem) = synchronized(trava) {
        when (mensagem) {
            is Mensagem.AudioQuadro -> { som.addLast(mensagem); bytes += mensagem.dados.size }
            is Mensagem.VideoQuadro -> ofertarVideo(mensagem)
            is Mensagem.VideoConfig -> video.addLast(mensagem)
            else -> controle.addLast(mensagem)
        }
        trava.notifyAll()
    }

    private fun ofertarVideo(quadro: Mensagem.VideoQuadro) {
        if (quadro.chave) {
            // Quadro-chave sempre entra: se não couber, o vídeo antigo sai (ele não serve mais).
            if (bytes + quadro.dados.size > limiteBytes) descartarVideo()
            aguardandoChave = false
            video.addLast(quadro)
            bytes += quadro.dados.size
            return
        }
        if (aguardandoChave) {
            descartesDeVideo++
            return
        }
        if (bytes + quadro.dados.size > limiteBytes) {
            aguardandoChave = true
            descartesDeVideo++
            aoDescartarVideo?.invoke()
            return
        }
        video.addLast(quadro)
        bytes += quadro.dados.size
    }

    private fun descartarVideo() {
        // VideoConfig na fila continua: o decodificador precisa dele antes do próximo quadro-chave.
        val configs = video.filterIsInstance<Mensagem.VideoConfig>()
        video.filterIsInstance<Mensagem.VideoQuadro>().forEach { bytes -= it.dados.size }
        video.clear()
        video.addAll(configs)
    }

    fun proxima(): Mensagem? = synchronized(trava) {
        val m = controle.removeFirstOrNull() ?: som.removeFirstOrNull() ?: video.removeFirstOrNull() ?: return null
        when (m) {
            is Mensagem.AudioQuadro -> bytes -= m.dados.size
            is Mensagem.VideoQuadro -> bytes -= m.dados.size
            else -> Unit
        }
        m
    }

    /** Bloqueia até haver mensagem ou o tempo acabar. */
    fun esperarProxima(timeoutMs: Long): Mensagem? = synchronized(trava) {
        proxima() ?: run {
            trava.wait(timeoutMs)
            proxima()
        }
    }

    fun limpar() = synchronized(trava) {
        controle.clear(); som.clear(); video.clear(); bytes = 0; aguardandoChave = false
    }
}

class DetectorDeQueda(inicioMs: Long, private val limiteMs: Long = 3_000) {
    @Volatile private var ultimoMs = inicioMs

    fun recebeu(agoraMs: Long) { ultimoMs = agoraMs }

    fun caiu(agoraMs: Long): Boolean = agoraMs - ultimoMs >= limiteMs
}

/** RNF-016: tela do celular reduzida (nunca ampliada), proporção preservada, cabendo na área e com lado maior ≤ 1280. */
object TamanhoDoVideo {
    const val LADO_MAXIMO = 1280
    private const val ALINHAMENTO = 16

    fun calcular(larguraTela: Int, alturaTela: Int, larguraArea: Int, alturaArea: Int): Pair<Int, Int> {
        val escala = minOf(
            1.0,
            LADO_MAXIMO.toDouble() / maxOf(larguraTela, alturaTela),
            larguraArea.toDouble() / larguraTela,
            alturaArea.toDouble() / alturaTela,
        )
        fun alinhar(v: Double) = (v.toInt() / ALINHAMENTO * ALINHAMENTO).coerceAtLeast(ALINHAMENTO)
        return alinhar(larguraTela * escala) to alinhar(alturaTela * escala)
    }
}

/**
 * CSD do Opus no formato que todo decodificador Android aceita: csd-0 = OpusHead, csd-1 = atraso do codec (ns, int64
 * little-endian), csd-2 = pré-rolagem (ns). O codificador C2 do Android entrega um bloco só com marcadores
 * ("AOPUSHDR"/"AOPUSDLY"/"AOPUSPRL"), que o decodificador OMX de centrais antigas não entende.
 */
object CsdDoOpus {
    private const val TAXA = 48_000L
    private const val PRE_ROLAGEM_NS = 80_000_000L

    fun normalizar(csd: List<ByteArray>): List<ByteArray> {
        val primeiro = csd.firstOrNull() ?: return csd
        val marcadores = if (comeca(primeiro, "AOPUSHDR")) lerMarcadores(primeiro) else null
        val cabecalho = marcadores?.get("AOPUSHDR") ?: primeiro.takeIf { comeca(it, "OpusHead") } ?: return csd
        if (marcadores == null && csd.size >= 3) return csd
        val preSkip = if (cabecalho.size >= 12) (cabecalho[10].toInt() and 0xFF) or ((cabecalho[11].toInt() and 0xFF) shl 8) else 0
        val atraso = marcadores?.get("AOPUSDLY") ?: int64(preSkip * 1_000_000_000L / TAXA)
        val preRolagem = marcadores?.get("AOPUSPRL") ?: int64(PRE_ROLAGEM_NS)
        return listOf(cabecalho, atraso, preRolagem)
    }

    private fun comeca(b: ByteArray, texto: String) =
        b.size >= texto.length && String(b, 0, texto.length, Charsets.US_ASCII) == texto

    private fun lerMarcadores(b: ByteArray): Map<String, ByteArray> {
        val r = java.nio.ByteBuffer.wrap(b).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        val mapa = HashMap<String, ByteArray>()
        while (r.remaining() >= 16) {
            val nome = ByteArray(8).also { r.get(it) }.toString(Charsets.US_ASCII)
            val tamanho = r.long.toInt()
            if (tamanho < 0 || tamanho > r.remaining()) break
            mapa[nome] = ByteArray(tamanho).also { r.get(it) }
        }
        return mapa
    }

    private fun int64(valor: Long) =
        java.nio.ByteBuffer.allocate(8).order(java.nio.ByteOrder.LITTLE_ENDIAN).putLong(valor).array()
}

/** Tela virtual do modo Shizuku (contrato §10.3): horizontal, proporção da área da central, lado maior ≤ 1280. */
object TamanhoDaTelaVirtual {
    const val LADO_MAXIMO = 1280
    private const val ALINHAMENTO = 8
    private const val DENSIDADE_MINIMA = 120

    fun calcular(larguraArea: Int, alturaArea: Int): Pair<Int, Int> {
        val maior = maxOf(larguraArea, alturaArea)
        val menor = minOf(larguraArea, alturaArea)
        val largura = minOf(LADO_MAXIMO, maior)
        val altura = (menor.toLong() * largura / maior).toInt()
        fun alinhar(v: Int) = (v / ALINHAMENTO * ALINHAMENTO).coerceAtLeast(ALINHAMENTO)
        return alinhar(largura) to alinhar(altura)
    }

    fun densidade(ladoMaior: Int): Int = maxOf(DENSIDADE_MINIMA, Math.round(160.0 * ladoMaior / 960).toInt())
}
