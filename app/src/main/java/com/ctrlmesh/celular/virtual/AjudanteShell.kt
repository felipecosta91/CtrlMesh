package com.ctrlmesh.celular.virtual

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.hardware.input.InputManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Binder
import android.os.Build
import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import kotlin.concurrent.thread
import kotlin.system.exitProcess

/**
 * Ajudante com permissão de shell (uid 2000), iniciado pelo Shizuku como UserService (contrato §10.1).
 * Roda num processo próprio (`app_process`), por isso pode usar as mesmas APIs que o scrcpy usa pelo adb:
 * tela virtual confiável, início de atividade num displayId, injeção de toque/tecla e captura do som (REMOTE_SUBMIX).
 * A interface é um Binder simples (sem AIDL): ver [Ajudante] para os códigos.
 */
class AjudanteShell(contextoDoShizuku: Context) : Binder() {

    /**
     * O Shizuku nos dá um contexto com o pacote do app (uid 10xxx), mas o processo roda com o uid do shell (2000):
     * o sistema recusa chamadas cujo pacote não combina com o uid. Como o scrcpy-server (FakeContext), usamos um
     * contexto do sistema que se apresenta como `com.android.shell`.
     */
    private val context: Context = ContextoDoShell(runCatching { contextoDoSistema() }.getOrDefault(contextoDoShizuku))

    private var tela: VirtualDisplay? = null
    private var acordada: android.os.PowerManager.WakeLock? = null
    private val ponteiros = LinkedHashMap<Int, Pair<Float, Float>>()
    private var inicioDoToque = 0L
    @Volatile private var somRodando = false

    @Suppress("unused") // o Shizuku pode instanciar sem Context em versões antigas
    constructor() : this(contextoDoSistema())

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        if (code == Ajudante.DESTRUIR) {
            fecharTela()
            exitProcess(0)
        }
        if (code == INTERFACE_TRANSACTION) return super.onTransact(code, data, reply, flags)
        data.enforceInterface(Ajudante.DESCRITOR)
        try {
            when (code) {
                Ajudante.CRIAR_TELA -> {
                    val superficie = Surface.CREATOR.createFromParcel(data)
                    val id = criarTela(superficie, data.readInt(), data.readInt(), data.readInt())
                    reply?.writeNoException()
                    reply?.writeInt(id)
                }
                Ajudante.FECHAR_TELA -> { fecharTela(); reply?.writeNoException() }
                Ajudante.ABRIR_APP -> { val ok = abrirApp(data.readString().orEmpty()); reply?.writeNoException(); reply?.writeInt(if (ok) 1 else 0) }
                Ajudante.TOQUE -> { toque(data.readInt(), data.readInt(), data.readFloat(), data.readFloat()); reply?.writeNoException() }
                Ajudante.TECLA -> { tecla(data.readInt()); reply?.writeNoException() }
                Ajudante.ALTERNAR_TAREFA -> { alternarTarefa(); reply?.writeNoException() }
                Ajudante.TEM_APP_NA_FRENTE -> { val tem = temAppNaFrente(); reply?.writeNoException(); reply?.writeInt(if (tem) 1 else 0) }
                Ajudante.INICIAR_SOM -> {
                    val ok = iniciarSom(ParcelFileDescriptor.CREATOR.createFromParcel(data))
                    reply?.writeNoException()
                    reply?.writeInt(if (ok) 1 else 0)
                }
                Ajudante.PARAR_SOM -> { somRodando = false; reply?.writeNoException() }
                else -> return super.onTransact(code, data, reply, flags)
            }
        } catch (e: Exception) {
            // A exceção completa (com a causa) volta ao app, que a registra com a tag CtrlMesh.
            val texto = android.util.Log.getStackTraceString(e).take(LIMITE_DO_ERRO)
            reply?.writeException(IllegalStateException(texto))
        }
        return true
    }

    // ---------- tela virtual (§10.3) ----------

    @Synchronized
    private fun criarTela(superficie: Surface, largura: Int, altura: Int, dpi: Int): Int {
        val atual = tela
        if (atual != null) {
            atual.resize(largura, altura, dpi)
            atual.surface = superficie
            manterAcordada(atual.display.displayId)
            return atual.display.displayId
        }
        // Mesma API do scrcpy: DisplayManager(Context) criado com o contexto do shell.
        val telas = DisplayManager::class.java.getDeclaredConstructor(Context::class.java).apply { isAccessible = true }
            .newInstance(context) as DisplayManager
        val erros = ArrayList<String>()
        val nova = FLAGS.firstNotNullOfOrNull { flags ->
            runCatching { telas.createVirtualDisplay(NOME, largura, altura, dpi, superficie, flags) }
                .onFailure { erros += "flags=$flags: ${it.javaClass.name}: ${it.message} ${it.cause?.let { c -> "causa=${c.javaClass.name}: ${c.message}" } ?: ""}" }
                .getOrNull()
        } ?: error("não foi possível criar a tela virtual; tentativas: ${erros.joinToString(" | ")}")
        tela = nova
        manterAcordada(nova.display.displayId)
        return nova.display.displayId
    }

    /**
     * A tela virtual tem grupo de energia próprio (OWN_DISPLAY_GROUP): sem atividade, ele dorme no tempo de bloqueio
     * do celular e a tela fica OFF (imagem preta). Uma trava de tela ligada associada ao displayId (API oculta do
     * Android 13+) acorda e mantém só esse grupo, sem acender a tela do celular (RN-098).
     */
    @SuppressLint("WakelockTimeout", "InvalidWakeLockTag")
    private fun manterAcordada(displayId: Int) {
        if (acordada?.isHeld == true) return
        val energia = context.getSystemService(android.os.PowerManager::class.java)
        @Suppress("DEPRECATION")
        val nivel = android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK or android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP
        val trava = runCatching {
            energia.javaClass.getMethod("newWakeLock", Int::class.javaPrimitiveType, String::class.java, Int::class.javaPrimitiveType)
                .invoke(energia, nivel, "CtrlMesh:telaVirtual", displayId) as android.os.PowerManager.WakeLock
        }.getOrElse { erro("trava por displayId indisponível", it); return }
        trava.setReferenceCounted(false)
        trava.acquire()
        acordada = trava
    }

    private fun erro(texto: String, e: Throwable) {
        android.util.Log.w("CtrlMesh", "ajudante: $texto: ${e.javaClass.name}: ${e.message}")
    }

    @Synchronized
    private fun fecharTela() {
        acordada?.let { runCatching { it.release() } }
        acordada = null
        somRodando = false
        tela?.release() // sem DESTROY_CONTENT_ON_REMOVAL: os apps continuam vivos na tela do celular (RN-107)
        tela = null
    }

    private fun idDaTela(): Int = tela?.display?.displayId ?: error("sem tela virtual")

    // ---------- apps e tarefas ----------

    private fun abrirApp(pacote: String): Boolean {
        if (!PACOTE_VALIDO.matches(pacote)) return false // vem da rede: nunca vai para um comando sem validar
        val id = idDaTela()
        val componente = context.packageManager.getLaunchIntentForPackage(pacote)?.component ?: return false
        // RN-096: a tarefa que já existe na tela do celular é movida para a virtual.
        tarefas().firstOrNull { it.pacote == pacote && it.displayId != id }?.let { executar("am", "display", "move-stack", it.raiz.toString(), id.toString()) }
        return iniciarNaTela(componente.flattenToString(), id)
    }

    private fun iniciarNaTela(componente: String, id: Int): Boolean = executar(
        "am", "start", "--display", id.toString(), "-a", Intent.ACTION_MAIN, "-c", Intent.CATEGORY_LAUNCHER,
        "-f", (Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED).toString(), "-n", componente,
    )

    /** RECENTES (RN-097): a tarefa anterior da tela virtual vem para a frente. */
    private fun alternarTarefa() {
        val id = idDaTela()
        val naTela = tarefas().filter { it.displayId == id }
        val anterior = naTela.getOrNull(1) ?: return
        iniciarNaTela(anterior.componente, id)
    }

    private fun temAppNaFrente(): Boolean {
        val id = tela?.display?.displayId ?: return false
        return tarefas().any { it.displayId == id }
    }

    private class Tarefa(val raiz: Int, val displayId: Int, val componente: String) {
        val pacote get() = componente.substringBefore('/')
    }

    /** `am stack list`: raízes na ordem de cima para baixo, com as tarefas de cada uma. */
    private fun tarefas(): List<Tarefa> {
        val saida = saidaDe("am", "stack", "list") ?: return emptyList()
        val lista = ArrayList<Tarefa>()
        var raiz = -1
        var display = -1
        saida.lineSequence().forEach { linha ->
            val t = linha.trim()
            if (t.startsWith("RootTask") || t.startsWith("Stack")) {
                raiz = Regex("""id=(\d+)""").find(t)?.groupValues?.get(1)?.toInt() ?: -1
                display = Regex("""displayId=(\d+)""").find(t)?.groupValues?.get(1)?.toInt() ?: -1
            } else if (t.startsWith("taskId=")) {
                val componente = Regex("""taskId=\d+: (\S+)""").find(t)?.groupValues?.get(1) ?: return@forEach
                lista += Tarefa(raiz, display, componente)
            }
        }
        return lista
    }

    // ---------- toque e teclas (§10.4) ----------

    @Synchronized
    private fun toque(acao: Int, ponteiro: Int, x: Float, y: Float) {
        val id = idDaTela()
        val agora = SystemClock.uptimeMillis()
        val acaoDoEvento: Int
        when (acao) {
            DESCE -> {
                if (ponteiros.isEmpty()) inicioDoToque = agora
                ponteiros[ponteiro] = x to y
                val indice = ponteiros.keys.indexOf(ponteiro)
                acaoDoEvento = if (ponteiros.size == 1) MotionEvent.ACTION_DOWN
                else MotionEvent.ACTION_POINTER_DOWN or (indice shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
            }
            MOVE -> {
                if (ponteiro !in ponteiros) return
                ponteiros[ponteiro] = x to y
                acaoDoEvento = MotionEvent.ACTION_MOVE
            }
            else -> {
                if (ponteiro !in ponteiros) return
                ponteiros[ponteiro] = x to y
                val indice = ponteiros.keys.indexOf(ponteiro)
                acaoDoEvento = when {
                    acao == CANCELA -> MotionEvent.ACTION_CANCEL
                    ponteiros.size == 1 -> MotionEvent.ACTION_UP
                    else -> MotionEvent.ACTION_POINTER_UP or (indice shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
                }
            }
        }
        val ids = ponteiros.keys.toList()
        val propriedades = Array(ids.size) { i -> MotionEvent.PointerProperties().apply { this.id = ids[i]; toolType = MotionEvent.TOOL_TYPE_FINGER } }
        val coordenadas = Array(ids.size) { i ->
            MotionEvent.PointerCoords().apply {
                val p = ponteiros.getValue(ids[i])
                this.x = p.first; this.y = p.second; pressure = 1f; size = 1f
            }
        }
        val evento = MotionEvent.obtain(
            inicioDoToque, agora, acaoDoEvento, ids.size, propriedades, coordenadas, 0, 0, 1f, 1f, 0, 0,
            InputDevice.SOURCE_TOUCHSCREEN, 0,
        )
        injetar(evento, id)
        evento.recycle()
        if (acao == SOBE || acao == CANCELA) ponteiros.remove(ponteiro)
        if (acao == CANCELA) ponteiros.clear()
    }

    private fun tecla(codigo: Int) {
        val id = idDaTela()
        val agora = SystemClock.uptimeMillis()
        listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP).forEach { acao ->
            injetar(
                KeyEvent(agora, agora, acao, codigo, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, InputDevice.SOURCE_KEYBOARD),
                id,
            )
        }
    }

    @SuppressLint("BlockedPrivateApi", "DiscouragedPrivateApi")
    private fun injetar(evento: InputEvent, displayId: Int) {
        InputEvent::class.java.getMethod("setDisplayId", Int::class.javaPrimitiveType).invoke(evento, displayId)
        // Android 14+: InputManagerGlobal; antes, InputManager.getInstance() (como o scrcpy).
        val gerente: Any = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Class.forName("android.hardware.input.InputManagerGlobal").getMethod("getInstance").invoke(null)!!
        } else {
            InputManager::class.java.getDeclaredMethod("getInstance").invoke(null)!!
        }
        gerente.javaClass.getMethod("injectInputEvent", InputEvent::class.java, Int::class.javaPrimitiveType)
            .invoke(gerente, evento, INJETAR_SEM_ESPERAR)
    }

    // ---------- som (§10.5) ----------

    /**
     * REMOTE_SUBMIX: enquanto grava, o som de mídia sai só por aqui (não no alto-falante do celular) e volta ao
     * normal quando para (RN-106). PCM 48 kHz estéreo 16 bits, escrito no pipe do app.
     */
    @SuppressLint("MissingPermission", "WrongConstant") // REMOTE_SUBMIX é permitido ao shell (CAPTURE_AUDIO_OUTPUT)
    private fun iniciarSom(destino: ParcelFileDescriptor): Boolean {
        somRodando = false
        val formato = AudioFormat.Builder().setSampleRate(TAXA).setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT).build()
        val minimo = AudioRecord.getMinBufferSize(TAXA, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        val construtor = AudioRecord.Builder().setAudioSource(MediaRecorder.AudioSource.REMOTE_SUBMIX)
            .setAudioFormat(formato).setBufferSizeInBytes(maxOf(minimo, QUADRO * 8))
        // O uid é o do shell: a atribuição precisa ser do pacote do shell para o AppOps aceitar.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            construtor.setContext(context)
        }
        val gravador = construtor.build()
        if (gravador.state != AudioRecord.STATE_INITIALIZED) {
            gravador.release()
            return false
        }
        somRodando = true
        thread(isDaemon = true, name = "ajudante-som") {
            val saida = ParcelFileDescriptor.AutoCloseOutputStream(destino)
            val buffer = ByteArray(QUADRO)
            try {
                gravador.startRecording()
                while (somRodando) {
                    val lidos = gravador.read(buffer, 0, buffer.size)
                    if (lidos > 0) saida.write(buffer, 0, lidos)
                }
            } catch (_: Exception) {
                // o app fechou o pipe
            } finally {
                runCatching { gravador.stop() }
                gravador.release()
                runCatching { saida.close() }
            }
        }
        return true
    }

    // ---------- utilidades ----------

    private fun executar(vararg comando: String): Boolean =
        runCatching { ProcessBuilder(*comando).redirectErrorStream(true).start().apply { inputStream.readBytes() }.waitFor() == 0 }
            .getOrDefault(false)

    private fun saidaDe(vararg comando: String): String? =
        runCatching { ProcessBuilder(*comando).redirectErrorStream(true).start().inputStream.bufferedReader().readText() }.getOrNull()

    private companion object {
        const val NOME = "CtrlMesh"
        const val TAXA = 48_000
        const val QUADRO = TAXA / 50 * 4 // 20 ms estéreo 16 bits
        const val INJETAR_SEM_ESPERAR = 0
        const val PACOTE_DO_SHELL = "com.android.shell"
        const val LIMITE_DO_ERRO = 3_000
        val PACOTE_VALIDO = Regex("""[A-Za-z0-9_]+(\.[A-Za-z0-9_]+)+""")

        // AcaoToque no fio
        const val DESCE = 0
        const val MOVE = 1
        const val SOBE = 2
        const val CANCELA = 3

        // DisplayManager.VIRTUAL_DISPLAY_FLAG_* (alguns ocultos). O 2º conjunto é exatamente o do scrcpy --new-display;
        // o 1º não tem DESTROY_CONTENT_ON_REMOVAL, para os apps continuarem vivos ao fechar a tela (RN-107).
        private const val PUBLIC = 1
        private const val PRESENTATION = 1 shl 1
        private const val OWN_CONTENT_ONLY = 1 shl 3
        private const val SUPPORTS_TOUCH = 1 shl 6
        private const val ROTATES_WITH_CONTENT = 1 shl 7
        private const val DESTROY_CONTENT_ON_REMOVAL = 1 shl 8
        private const val TRUSTED = 1 shl 10
        private const val OWN_DISPLAY_GROUP = 1 shl 11
        private const val ALWAYS_UNLOCKED = 1 shl 12
        private const val TOUCH_FEEDBACK_DISABLED = 1 shl 13
        private val BASE = PUBLIC or PRESENTATION or OWN_CONTENT_ONLY or SUPPORTS_TOUCH or ROTATES_WITH_CONTENT or TRUSTED
        private val ANDROID_13 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            OWN_DISPLAY_GROUP or ALWAYS_UNLOCKED or TOUCH_FEEDBACK_DISABLED
        } else {
            0
        }
        val FLAGS = listOf(
            BASE or ANDROID_13,
            BASE or ANDROID_13 or DESTROY_CONTENT_ON_REMOVAL,
            BASE,
            PUBLIC or OWN_CONTENT_ONLY or SUPPORTS_TOUCH,
        )

        @SuppressLint("PrivateApi")
        fun contextoDoSistema(): Context {
            val classe = Class.forName("android.app.ActivityThread")
            val atual = classe.getMethod("currentActivityThread").invoke(null) ?: classe.getMethod("systemMain").invoke(null)
            return classe.getMethod("getSystemContext").invoke(atual) as Context
        }
    }
}

/** Contexto do sistema que se apresenta como o pacote do shell (uid 2000), como o FakeContext do scrcpy-server. */
private class ContextoDoShell(base: Context) : android.content.ContextWrapper(base) {
    override fun getPackageName() = "com.android.shell"
    override fun getOpPackageName() = "com.android.shell"
    override fun getApplicationContext(): Context = this

    @SuppressLint("NewApi")
    override fun getAttributionSource(): android.content.AttributionSource =
        android.content.AttributionSource.Builder(android.os.Process.SHELL_UID).setPackageName("com.android.shell").build()
}

/** Códigos do Binder do ajudante, compartilhados com o cliente no app. */
object Ajudante {
    const val DESCRITOR = "com.ctrlmesh.celular.virtual.Ajudante"
    const val CRIAR_TELA = 1
    const val FECHAR_TELA = 2
    const val ABRIR_APP = 3
    const val TOQUE = 4
    const val TECLA = 5
    const val ALTERNAR_TAREFA = 6
    const val TEM_APP_NA_FRENTE = 7
    const val INICIAR_SOM = 8
    const val PARAR_SOM = 9
    const val DESTRUIR = 16777115 // ShizukuApiConstants.USER_SERVICE_TRANSACTION_destroy
}
