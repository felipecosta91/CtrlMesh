package com.ctrlmesh.central

import kotlin.math.roundToInt

data class Retangulo(val esquerda: Int, val topo: Int, val direita: Int, val base: Int) {
    val largura: Int get() = direita - esquerda
    val altura: Int get() = base - topo
}

data class LayoutCentral(val barra: Retangulo, val imagem: Retangulo)

data class PontoNormalizado(val x: Float, val y: Float)

object Layout {
    /** v3.3 (RN-109, RN-110): barra embaixo na largura toda; imagem inteira acima dela, centralizada, base no topo da barra. */
    fun calcular(larguraTela: Int, alturaTela: Int, larguraImagem: Int, alturaImagem: Int, alturaBarra: Int): LayoutCentral {
        val barra = Retangulo(0, alturaTela - alturaBarra, larguraTela, alturaTela)
        val area = encaixar(larguraTela, barra.topo, larguraImagem, alturaImagem)
        return LayoutCentral(barra, area)
    }

    /** Modo tela virtual (§10.4): a imagem ocupa toda a área acima da barra, sem faixas. */
    fun calcularTelaVirtual(larguraTela: Int, alturaTela: Int, alturaBarra: Int): LayoutCentral =
        LayoutCentral(Retangulo(0, alturaTela - alturaBarra, larguraTela, alturaTela), Retangulo(0, 0, larguraTela, alturaTela - alturaBarra))

    /** Maior retângulo com a proporção da imagem que cabe em largura × altura: centralizado na horizontal, base embaixo. */
    private fun encaixar(largura: Int, altura: Int, larguraImagem: Int, alturaImagem: Int): Retangulo {
        val limitadoPelaLargura = larguraImagem.toLong() * altura > alturaImagem.toLong() * largura
        val l = if (limitadoPelaLargura) largura else (larguraImagem.toDouble() * altura / alturaImagem).roundToInt().coerceAtMost(largura)
        val a = if (limitadoPelaLargura) (alturaImagem.toDouble() * largura / larguraImagem).roundToInt().coerceAtMost(altura) else altura
        val esquerda = (largura - l) / 2
        return Retangulo(esquerda, altura - a, esquerda + l, altura)
    }
}

/** Início do CtrlMesh (RN-095): apps em ordem alfabética pt-BR, sem diferenciar maiúsculas nem acentos. */
object InicioDoCtrlMesh {
    fun ordenar(apps: List<com.ctrlmesh.protocolo.AppDoCelular>): List<com.ctrlmesh.protocolo.AppDoCelular> {
        val collator = java.text.Collator.getInstance(java.util.Locale("pt", "BR")).apply { strength = java.text.Collator.PRIMARY }
        return apps.sortedWith { a, b -> collator.compare(a.rotulo, b.rotulo).takeIf { it != 0 } ?: a.pacote.compareTo(b.pacote) }
    }
}

object Descoberta {
    fun alvos(enderecoInformado: String?, gateway: String?): List<String> =
        listOfNotNull(enderecoInformado?.trim(), gateway?.trim()).filter { it.isNotEmpty() }.distinct()
}

class VolumeCentral(private val niveis: Int = 10) {
    var nivel: Int = niveis
        private set
    var mudo: Boolean = false
        private set
    val ganho: Float get() = if (mudo) 0f else nivel.toFloat() / niveis

    fun menos() { nivel = (nivel - 1).coerceAtLeast(0) }
    fun mais() { nivel = (nivel + 1).coerceAtMost(niveis) }
    fun alternarMudo() { mudo = !mudo }
}
