package com.ctrlmesh.celular

import android.Manifest
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings

/** Autorizações verificadas nos dois papéis (RN-072, RN-075, RN-085). */
object Autorizacoes {
    fun sobrepor(context: Context) = Settings.canDrawOverlays(context)
    fun girarTela(context: Context) = Settings.System.canWrite(context)
    fun bateria(context: Context) =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
    fun gravarAudio(context: Context) =
        context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    fun acessibilidade(context: Context): Boolean {
        val ativos = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val nosso = ComponentName(context, ServicoDeControle::class.java)
        return ativos.split(':').any { ComponentName.unflattenFromString(it.trim()) == nosso }
    }

    /** Liberação permanente de captura = appop PROJECT_MEDIA concedido pelo Mac (RN-073, RN-075). */
    fun liberacaoPermanente(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        return runCatching {
            val modo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(OP_PROJECT_MEDIA, Process.myUid(), context.packageName)
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(OP_PROJECT_MEDIA, Process.myUid(), context.packageName)
            }
            modo == AppOpsManager.MODE_ALLOWED
        }.getOrDefault(false)
    }

    private const val OP_PROJECT_MEDIA = "android:project_media"
}

/** Settings.System + AudioManager. Sem WRITE_SETTINGS, as escritas de ajustes do sistema são ignoradas. */
class AjustesAndroid(private val context: Context) : AjustesDoCelular {
    private val resolvedor = context.contentResolver
    private val audio = context.getSystemService(AudioManager::class.java)

    private fun ler(nome: String, padrao: Int) = Settings.System.getInt(resolvedor, nome, padrao)
    private fun escrever(nome: String, valor: Int) {
        if (Settings.System.canWrite(context)) runCatching { Settings.System.putInt(resolvedor, nome, valor) }
    }

    override var rotacao: Int
        get() = ler(Settings.System.USER_ROTATION, 0)
        set(valor) = escrever(Settings.System.USER_ROTATION, valor)
    override var giroAutomatico: Boolean
        get() = ler(Settings.System.ACCELEROMETER_ROTATION, 0) == 1
        set(valor) = escrever(Settings.System.ACCELEROMETER_ROTATION, if (valor) 1 else 0)
    override var brilho: Int
        get() = ler(Settings.System.SCREEN_BRIGHTNESS, 128)
        set(valor) = escrever(Settings.System.SCREEN_BRIGHTNESS, valor)
    override var brilhoAutomatico: Boolean
        get() = ler(Settings.System.SCREEN_BRIGHTNESS_MODE, 0) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        set(valor) = escrever(
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            if (valor) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
        )
    override var tempoDeBloqueioMs: Int
        get() = ler(Settings.System.SCREEN_OFF_TIMEOUT, 30_000)
        set(valor) = escrever(Settings.System.SCREEN_OFF_TIMEOUT, valor)
    override var volumeMidia: Int
        get() = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        set(valor) { runCatching { audio.setStreamVolume(AudioManager.STREAM_MUSIC, valor, 0) } }
    override var volumeToque: Int
        get() = audio.getStreamVolume(AudioManager.STREAM_RING)
        set(valor) { runCatching { audio.setStreamVolume(AudioManager.STREAM_RING, valor, 0) } }
    override var volumeAlarme: Int
        get() = audio.getStreamVolume(AudioManager.STREAM_ALARM)
        set(valor) { runCatching { audio.setStreamVolume(AudioManager.STREAM_ALARM, valor, 0) } }
}

/** Sobrevive à morte do app (RN-028, RN-060). */
class ArmazenamentoAndroid(context: Context) : ArmazenamentoDeRestauracao {
    private val prefs = context.applicationContext.getSharedPreferences("ctrlmesh-restauracao", Context.MODE_PRIVATE)

    override fun salvar(ajustes: AjustesSalvos) {
        prefs.edit()
            .putBoolean("salvo", true)
            .putInt("rotacao", ajustes.rotacao)
            .putBoolean("giro", ajustes.giroAutomatico)
            .putInt("brilho", ajustes.brilho)
            .putBoolean("brilhoAuto", ajustes.brilhoAutomatico)
            .putInt("bloqueio", ajustes.tempoDeBloqueioMs)
            .putInt("midia", ajustes.volumeMidia)
            .commit()
    }

    override fun ler(): AjustesSalvos? {
        if (!prefs.getBoolean("salvo", false)) return null
        return AjustesSalvos(
            prefs.getInt("rotacao", 0), prefs.getBoolean("giro", false), prefs.getInt("brilho", 128),
            prefs.getBoolean("brilhoAuto", false), prefs.getInt("bloqueio", 30_000), prefs.getInt("midia", 0),
        )
    }

    override fun apagar() {
        prefs.edit().clear().commit()
    }
}
