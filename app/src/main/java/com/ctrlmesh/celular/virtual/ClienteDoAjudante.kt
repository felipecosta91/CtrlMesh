package com.ctrlmesh.celular.virtual

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.view.Surface
import com.ctrlmesh.celular.EstadoShizuku
import com.ctrlmesh.registrar
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import rikka.shizuku.Shizuku

/** Lado do app: estado do Shizuku (§10.1) e chamadas ao [AjudanteShell]. */
class ClienteDoAjudante(private val context: Context) {
    @Volatile private var binder: IBinder? = null
    @Volatile private var conectado = CountDownLatch(1)

    private val argumentos = Shizuku.UserServiceArgs(ComponentName(context.packageName, AjudanteShell::class.java.name))
        .processNameSuffix("ajudante")
        .daemon(false)
        .version(VERSAO)

    private val conexao = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, servico: IBinder?) {
            binder = servico?.takeIf { it.pingBinder() }
            conectado.countDown()
            registrar("ajudante=${if (binder != null) "conectado" else "falhou"}")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            binder = null
            conectado = CountDownLatch(1)
            registrar("ajudante=desconectado")
        }
    }

    val pronto: Boolean get() = binder?.isBinderAlive == true

    fun estado(): EstadoShizuku {
        val vivo = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        val autorizado = vivo && runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }.getOrDefault(false)
        return EstadoShizuku.de(vivo, autorizado)
    }

    /** Liga o ajudante (sem esperar). Só com o Shizuku ativo; o CtrlMesh nunca liga o Shizuku (RN-103). */
    fun ligar() {
        if (pronto || estado() != EstadoShizuku.ATIVO) return
        runCatching { Shizuku.bindUserService(argumentos, conexao) }.onFailure { registrar("ajudante=falhou ${it.message}") }
    }

    /** Liga e espera ficar pronto; na thread principal não espera (o retorno do Shizuku chega por ela). */
    fun garantir(esperaMs: Long): Boolean {
        if (pronto) return true
        ligar()
        if (Looper.myLooper() == Looper.getMainLooper()) return pronto
        conectado.await(esperaMs, TimeUnit.MILLISECONDS)
        return pronto
    }

    fun desligar() {
        runCatching { Shizuku.unbindUserService(argumentos, conexao, true) }
        binder = null
    }

    fun criarTela(superficie: Surface, largura: Int, altura: Int, dpi: Int): Int = chamar(Ajudante.CRIAR_TELA, {
        superficie.writeToParcel(it, 0)
        it.writeInt(largura); it.writeInt(altura); it.writeInt(dpi)
    }) { it.readInt() } ?: -1

    fun fecharTela() { chamar(Ajudante.FECHAR_TELA) {} }
    fun abrirApp(pacote: String): Boolean = chamar(Ajudante.ABRIR_APP, { it.writeString(pacote) }) { it.readInt() == 1 } ?: false
    fun toque(acao: Int, ponteiro: Int, x: Float, y: Float) {
        chamar(Ajudante.TOQUE, { it.writeInt(acao); it.writeInt(ponteiro); it.writeFloat(x); it.writeFloat(y) }) {}
    }
    fun tecla(codigo: Int) { chamar(Ajudante.TECLA, { it.writeInt(codigo) }) {} }
    fun alternarTarefa() { chamar(Ajudante.ALTERNAR_TAREFA) {} }
    fun temAppNaFrente(): Boolean = chamar(Ajudante.TEM_APP_NA_FRENTE) { it.readInt() == 1 } ?: false
    fun iniciarSom(destino: ParcelFileDescriptor): Boolean =
        chamar(Ajudante.INICIAR_SOM, { destino.writeToParcel(it, 0) }) { it.readInt() == 1 } ?: false
    fun pararSom() { chamar(Ajudante.PARAR_SOM) {} }

    private fun <T> chamar(codigo: Int, escrever: (Parcel) -> Unit = {}, ler: (Parcel) -> T): T? {
        val b = binder ?: return null
        val dados = Parcel.obtain()
        val resposta = Parcel.obtain()
        return try {
            dados.writeInterfaceToken(Ajudante.DESCRITOR)
            escrever(dados)
            b.transact(codigo, dados, resposta, 0)
            resposta.readException()
            ler(resposta)
        } catch (e: Exception) {
            registrar("ajudante erro codigo=$codigo: ${e.message}")
            null
        } finally {
            dados.recycle()
            resposta.recycle()
        }
    }

    companion object {
        const val CODIGO_PEDIDO = 4701
        private const val VERSAO = 2
    }
}
