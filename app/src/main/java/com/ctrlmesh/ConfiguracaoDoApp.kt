package com.ctrlmesh

import android.content.Context
import android.util.Log
import com.ctrlmesh.protocolo.Papel

class ConfiguracaoDoApp(private val context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("ctrlmesh", Context.MODE_PRIVATE)

    var papel: Papel?
        get() = prefs.getString(PAPEL, null)?.let { runCatching { Papel.valueOf(it) }.getOrNull() }
        set(valor) { prefs.edit().putString(PAPEL, valor?.name).commit() }

    var enderecoDoCelular: String?
        get() = prefs.getString(ENDERECO, null)
        set(valor) { prefs.edit().putString(ENDERECO, valor?.trim()?.takeIf { it.isNotEmpty() }).commit() }

    fun redefinir() {
        prefs.edit().clear().commit()
        com.ctrlmesh.celular.ArmazenamentoAndroid(context).apagar()
    }

    private companion object {
        const val PAPEL = "papel"
        const val ENDERECO = "endereco"
    }
}

const val TAG = "CtrlMesh"

/** Linhas do contrato §7.2 (roteiro e2e). */
fun registrar(linha: String) {
    Log.i(TAG, linha)
}
