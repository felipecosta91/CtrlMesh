package com.ctrlmesh.celular

import com.ctrlmesh.protocolo.Aviso
import com.ctrlmesh.protocolo.ModoEspelhamento

/** Uma central por vez, por ordem de chegada (RN-078). */
class FilaDeCentrais {
    private val ordem = mutableListOf<Long>()

    val ativa: Long? @Synchronized get() = ordem.firstOrNull()

    @Synchronized
    fun chegou(id: Long): Boolean {
        if (id !in ordem) ordem += id
        return ordem.first() == id
    }

    @Synchronized
    fun saiu(id: Long): Long? {
        val eraAtiva = ordem.firstOrNull() == id
        ordem.remove(id)
        return if (eraAtiva) ordem.firstOrNull() else null
    }

    /** Retomada (RN-054): a nova conexão herda a vaga da ativa que caiu, à frente das que esperam. */
    @Synchronized
    fun retomar(id: Long) {
        ordem.remove(id)
        if (ordem.isNotEmpty()) ordem.removeAt(0)
        ordem.add(0, id)
    }
}

data class Subrede(val endereco: String, val prefixo: Int)

object RedeDoRoteador {
    fun aceita(enderecoDaCentral: String, redesDoRoteador: List<Subrede>): Boolean {
        val central = ipv4(semPrefixoMapeado(enderecoDaCentral)) ?: return false
        if (central ushr 24 == 127L) return false
        return redesDoRoteador.any { rede ->
            val endereco = ipv4(rede.endereco) ?: return@any false
            if (rede.prefixo !in 1..32) return@any false
            val mascara = (-1L shl (32 - rede.prefixo)) and 0xFFFFFFFFL
            central and mascara == endereco and mascara
        }
    }

    /** IPv4 mapeado em IPv6 (`::ffff:a.b.c.d`) vira `a.b.c.d`. */
    fun semPrefixoMapeado(endereco: String): String {
        val limpo = endereco.trim().removePrefix("/")
        return if (limpo.startsWith("::ffff:", ignoreCase = true)) limpo.substring(7) else limpo
    }

    private fun ipv4(texto: String): Long? {
        val partes = texto.trim().split('.')
        if (partes.size != 4) return null
        return partes.fold(0L) { acc, parte ->
            val octeto = parte.takeIf { it.isNotEmpty() && it.length <= 3 && it.all(Char::isDigit) }?.toInt() ?: return null
            if (octeto > 255) return null
            (acc shl 8) or octeto.toLong()
        }
    }
}

data class PlanoDeAudio(val enviarSom: Boolean, val silenciarCelular: Boolean, val avisos: Set<Aviso>) {
    companion object {
        fun decidir(podeGravarAudio: Boolean, silenciarZeraSomEnviado: Boolean): PlanoDeAudio = when {
            !podeGravarAudio -> PlanoDeAudio(enviarSom = false, silenciarCelular = false, avisos = setOf(Aviso.FALTA_GRAVAR_AUDIO))
            silenciarZeraSomEnviado -> PlanoDeAudio(enviarSom = true, silenciarCelular = false, avisos = setOf(Aviso.NAO_SILENCIA))
            else -> PlanoDeAudio(enviarSom = true, silenciarCelular = true, avisos = emptySet())
        }
    }
}

interface AjustesDoCelular {
    var rotacao: Int
    var giroAutomatico: Boolean
    var brilho: Int
    var brilhoAutomatico: Boolean
    var tempoDeBloqueioMs: Int
    var volumeMidia: Int
    var volumeToque: Int
    var volumeAlarme: Int
}

data class AjustesSalvos(
    val rotacao: Int, val giroAutomatico: Boolean, val brilho: Int, val brilhoAutomatico: Boolean,
    val tempoDeBloqueioMs: Int, val volumeMidia: Int,
)

interface ArmazenamentoDeRestauracao {
    fun salvar(ajustes: AjustesSalvos)
    fun ler(): AjustesSalvos?
    fun apagar()
}

class RestauracaoDoCelular(private val ajustes: AjustesDoCelular, private val armazenamento: ArmazenamentoDeRestauracao) {

    fun aplicar(podeGirar: Boolean, silenciarMidia: Boolean) {
        if (armazenamento.ler() == null) {
            armazenamento.salvar(
                AjustesSalvos(
                    ajustes.rotacao, ajustes.giroAutomatico, ajustes.brilho, ajustes.brilhoAutomatico,
                    ajustes.tempoDeBloqueioMs, ajustes.volumeMidia,
                ),
            )
        }
        if (podeGirar) {
            ajustes.giroAutomatico = false
            ajustes.rotacao = ROTACAO_PAISAGEM
        }
        // Brilho nunca é alterado (RN-112). Tela ligada durante o espelhamento (RN-111); restaurado junto com o resto.
        ajustes.tempoDeBloqueioMs = TEMPO_DE_BLOQUEIO_ESPELHANDO_MS
        if (silenciarMidia) ajustes.volumeMidia = 0
    }

    fun restaurar() {
        val salvo = armazenamento.ler() ?: return
        ajustes.rotacao = salvo.rotacao
        ajustes.giroAutomatico = salvo.giroAutomatico
        ajustes.tempoDeBloqueioMs = salvo.tempoDeBloqueioMs
        ajustes.volumeMidia = salvo.volumeMidia
        armazenamento.apagar()
    }

    companion object {
        private const val ROTACAO_PAISAGEM = 1
        private const val TEMPO_DE_BLOQUEIO_ESPELHANDO_MS = 24 * 60 * 60 * 1000
    }
}

enum class EstadoShizuku(val texto: String) {
    ATIVO("Shizuku ativo"),
    DESLIGADO("Shizuku desligado"),
    SEM_AUTORIZACAO("CtrlMesh sem autorização no Shizuku"),
    ;

    companion object {
        fun de(binderVivo: Boolean, autorizado: Boolean): EstadoShizuku = when {
            !binderVivo -> DESLIGADO
            autorizado -> ATIVO
            else -> SEM_AUTORIZACAO
        }
    }
}

/** Contrato §10.2. */
object EscolhaDeModo {
    fun escolher(shizuku: EstadoShizuku): ModoEspelhamento =
        if (shizuku == EstadoShizuku.ATIVO) ModoEspelhamento.TELA_VIRTUAL else ModoEspelhamento.TELA_FISICA

    fun avisos(modo: ModoEspelhamento): Set<Aviso> =
        if (modo == ModoEspelhamento.TELA_FISICA) setOf(Aviso.MODO_TELA_DO_CELULAR) else emptySet()
}

/** Modo fixado ao começar o espelhamento e mantido até encerrar, inclusive em "reconectando" (§10.2). */
class ModoDaSessao {
    var atual: ModoEspelhamento? = null
        private set

    fun iniciar(shizuku: EstadoShizuku): ModoEspelhamento = atual ?: EscolhaDeModo.escolher(shizuku).also { atual = it }

    /** O ajudante falhou ao criar a tela virtual: cai para o plano B antes de enviar `Modo`. */
    fun cairParaTelaFisica() {
        atual = ModoEspelhamento.TELA_FISICA
    }

    fun encerrar() {
        atual = null
    }
}
