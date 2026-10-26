package com.ctrlmesh.celular.virtual

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import com.ctrlmesh.protocolo.AppDoCelular
import java.io.ByteArrayOutputStream

/** Apps com ícone na tela inicial do celular, para o início do CtrlMesh na central (RN-095, contrato §4 `ListaDeApps`). */
object AppsDoCelular {
    private const val LADO_DO_ICONE = 96
    private const val ICONE_MAXIMO_BYTES = 16 * 1024

    fun listar(context: Context): List<AppDoCelular> {
        val pm = context.packageManager
        val inicio = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        return pm.queryIntentActivities(inicio, 0)
            .map { it.activityInfo }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .map { info -> AppDoCelular(info.packageName, info.loadLabel(pm).toString(), icone(info.loadIcon(pm))) }
    }

    private val MASCARAS = listOf(0xFF, 0xF8, 0xF0, 0xE0, 0xC0)

    /** Zera os bits baixos de cada canal (mantém a transparência). */
    private fun posterizar(origem: Bitmap, mascara: Int): Bitmap {
        if (mascara == 0xFF) return origem
        val pixels = IntArray(LADO_DO_ICONE * LADO_DO_ICONE)
        origem.getPixels(pixels, 0, LADO_DO_ICONE, 0, 0, LADO_DO_ICONE, LADO_DO_ICONE)
        val m = (0xFF shl 24) or (mascara shl 16) or (mascara shl 8) or mascara
        for (i in pixels.indices) pixels[i] = pixels[i] and m
        return Bitmap.createBitmap(pixels, LADO_DO_ICONE, LADO_DO_ICONE, Bitmap.Config.ARGB_8888)
    }

    private fun comprimir(bitmap: Bitmap) =
        ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()

    private fun icone(desenho: android.graphics.drawable.Drawable): ByteArray = runCatching {
        val bitmap = Bitmap.createBitmap(LADO_DO_ICONE, LADO_DO_ICONE, Bitmap.Config.ARGB_8888)
        desenho.setBounds(0, 0, LADO_DO_ICONE, LADO_DO_ICONE)
        desenho.draw(Canvas(bitmap))
        // Ícones com muitos detalhes passam de 16 KiB em PNG: reduz a profundidade de cor até caber (o PNG comprime
        // muito melhor com menos tons), sempre em 96×96 como o contrato pede.
        val png = MASCARAS.asSequence().map { comprimir(posterizar(bitmap, it)) }.firstOrNull { it.size <= ICONE_MAXIMO_BYTES }
        bitmap.recycle()
        png ?: ByteArray(0)
    }.getOrDefault(ByteArray(0))
}
