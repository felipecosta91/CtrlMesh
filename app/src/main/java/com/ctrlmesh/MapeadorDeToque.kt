package com.ctrlmesh

import com.ctrlmesh.central.LayoutCentral
import com.ctrlmesh.central.PontoNormalizado
import com.ctrlmesh.protocolo.Mensagem

object MapeadorDeToque {
    fun naCentral(layout: LayoutCentral, x: Float, y: Float): PontoNormalizado? {
        val img = layout.imagem
        if (x < img.esquerda || x >= img.direita || y < img.topo || y >= img.base) return null
        return PontoNormalizado((x - img.esquerda) / img.largura, (y - img.topo) / img.altura)
    }

    fun noCelular(toque: Mensagem.Toque, larguraTela: Int, alturaTela: Int): Pair<Float, Float>? {
        val quadroEmPaisagem = toque.larguraQuadro > toque.alturaQuadro
        val telaEmPaisagem = larguraTela > alturaTela
        if (quadroEmPaisagem != telaEmPaisagem) return null
        return toque.x.coerceIn(0f, 1f) * larguraTela to toque.y.coerceIn(0f, 1f) * alturaTela
    }
}
