package com.ctrlmesh.celular.virtual

import android.content.Context
import android.os.ParcelFileDescriptor
import android.view.KeyEvent
import com.ctrlmesh.MapeadorDeToque
import com.ctrlmesh.Medidor
import com.ctrlmesh.celular.CapturaDeAudio
import com.ctrlmesh.celular.CodificadorDeVideo
import com.ctrlmesh.celular.FonteDePcm
import com.ctrlmesh.protocolo.Mensagem
import com.ctrlmesh.protocolo.TamanhoDaTelaVirtual
import com.ctrlmesh.protocolo.TeclaDeSistema
import com.ctrlmesh.registrar

/**
 * Modo tela virtual (§10.3–10.5): a tela é criada pelo ajudante sobre a superfície do nosso codificador; toques,
 * teclas e apps vão ao ajudante; o som vem do ajudante por um pipe e é codificado aqui.
 */
class CapturaVirtual(private val context: Context, private val ajudante: ClienteDoAjudante) {
    val medidor = Medidor()
    val medidorDeAudio = Medidor()
    private val codificador = CodificadorDeVideo(medidor)
    private var audio: CapturaDeAudio? = null
    @Volatile var tamanho = 0 to 0
        private set
    @Volatile var displayId = -1
        private set

    /** Cria (ou reaproveita) a tela virtual para a área da central e começa a transmitir. Falso se o ajudante falhar. */
    @Synchronized
    fun transmitir(larguraArea: Int, alturaArea: Int, saida: (Mensagem) -> Unit): Boolean {
        val (largura, altura) = TamanhoDaTelaVirtual.calcular(larguraArea, alturaArea)
        val dpi = TamanhoDaTelaVirtual.densidade(maxOf(largura, altura))
        val superficie = codificador.iniciar(largura, altura, saida)
        val id = ajudante.criarTela(superficie, largura, altura, dpi)
        if (id < 0) {
            codificador.parar()
            return false
        }
        if (id != displayId || tamanho != largura to altura) registrar("display_virtual=${largura}x$altura@$dpi id=$id")
        tamanho = largura to altura
        displayId = id
        return true
    }

    fun pedirQuadroChave() = codificador.pedirQuadroChave()

    /** Pico do PCM que veio do ajudante na janela atual (0 = silêncio), -1 sem som. */
    fun picoDeAudio(): Int = audio?.lerEZerarPico() ?: -1

    /** Para o vídeo (ex.: "reconectando"), mantendo a tela virtual e os apps. */
    @Synchronized
    fun pararVideo() = codificador.parar()

    @Synchronized
    fun transmitirSom(saida: (Mensagem) -> Unit) {
        if (audio != null) return
        val (leitura, escrita) = ParcelFileDescriptor.createPipe()
        val ok = ajudante.iniciarSom(escrita)
        runCatching { escrita.close() }
        if (!ok) {
            runCatching { leitura.close() }
            registrar("audio: ajudante não conseguiu capturar o som")
            return
        }
        val fonte = FonteDePcm.doPipe(ParcelFileDescriptor.AutoCloseInputStream(leitura)) { ajudante.pararSom() }
        audio = CapturaDeAudio(context, fonte, saida, volume = null, medidor = medidorDeAudio)
    }

    @Synchronized
    fun pararSom() {
        audio?.parar()
        audio = null
        ajudante.pararSom()
    }

    /** Encerramento: tela virtual fechada; os apps continuam vivos no celular (RN-107). */
    @Synchronized
    fun fechar() {
        pararSom()
        codificador.parar()
        ajudante.fecharTela()
        if (displayId >= 0) registrar("display_virtual=fechada")
        displayId = -1
        tamanho = 0 to 0
    }

    fun toque(toque: Mensagem.Toque) {
        val (largura, altura) = tamanho
        val ponto = MapeadorDeToque.noCelular(toque, largura, altura) ?: return
        ajudante.toque(toque.acao.ordinal, toque.ponteiro, ponto.first, ponto.second)
    }

    /** RN-097: Voltar e Recentes só na tela virtual; Início é mostrado pela central e não faz nada aqui. */
    fun tecla(tecla: TeclaDeSistema) {
        when (tecla) {
            TeclaDeSistema.VOLTAR -> ajudante.tecla(KeyEvent.KEYCODE_BACK)
            TeclaDeSistema.RECENTES -> ajudante.alternarTarefa()
            TeclaDeSistema.INICIO -> Unit
        }
    }

    fun abrirApp(pacote: String) {
        if (ajudante.abrirApp(pacote)) registrar("app_aberto=$pacote")
    }

    fun temAppNaFrente(): Boolean = ajudante.temAppNaFrente()
}
