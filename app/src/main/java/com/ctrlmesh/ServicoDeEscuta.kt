package com.ctrlmesh

import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.ctrlmesh.celular.ControladorCelular
import com.ctrlmesh.central.ControladorCentral
import com.ctrlmesh.protocolo.Papel

/** Serviço em primeiro plano dos dois papéis (RN-012): o celular escuta, a central procura o celular. */
class ServicoDeEscuta : Service() {
    private var papel: Papel? = null
    private var central: ControladorCentral? = null
    private var celular: ControladorCelular? = null

    private val desligamento = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = pararControladores()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Notificacoes.criarCanais(this)
        TarefaDeRede.agendar(this)
        registerReceiver(desligamento, IntentFilter(Intent.ACTION_SHUTDOWN))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (papel == null) emPrimeiroPlano(Notificacoes.servico(this, "em escuta"), capturando = false)
        val novo = ConfiguracaoDoApp(this).papel
        if (novo == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (novo != papel) {
            pararControladores()
            papel = novo
            when (novo) {
                Papel.CELULAR -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    celular = ControladorCelular(this, this).also { it.iniciar() }
                }
                Papel.MULTIMIDIA -> central = ControladorCentral(this).also { it.iniciar() }
            }
        }
        if (intent?.action == ACAO_ENCERRAR && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) celular?.encerrarTocado()
        return START_STICKY
    }

    override fun onDestroy() {
        pararControladores()
        runCatching { unregisterReceiver(desligamento) }
        super.onDestroy()
    }

    private fun pararControladores() {
        central?.parar()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) celular?.parar()
        central = null
        celular = null
        papel = null
    }

    /**
     * Escuta = connectedDevice; durante a captura do celular, também mediaProjection e microphone (Android 14+).
     * Se o sistema recusar algum tipo, tenta com menos tipos e, no fim, só atualiza a notificação.
     */
    fun emPrimeiroPlano(notificacao: Notification, capturando: Boolean, comAudio: Boolean = false) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            startForeground(Notificacoes.ID_SERVICO, notificacao)
            return
        }
        val base = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        val projecao = if (capturando) base or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION else base
        val microfone = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
        val tentativas = listOfNotNull(projecao or microfone, projecao, base).let { if (capturando && comAudio) it else it.drop(1) }.distinct()
        for (tipos in tentativas) {
            try {
                startForeground(Notificacoes.ID_SERVICO, notificacao, tipos)
                return
            } catch (e: RuntimeException) {
                registrar("primeiro plano recusado tipos=$tipos: ${e.javaClass.simpleName}")
            }
        }
        Notificacoes.atualizarServico(this, notificacao)
    }

    companion object {
        const val ACAO_ENCERRAR = "com.ctrlmesh.ENCERRAR"

        fun iniciar(context: Context) {
            if (ConfiguracaoDoApp(context).papel == null) return
            runCatching { context.startForegroundService(Intent(context, ServicoDeEscuta::class.java)) }
                .onFailure { registrar("falha ao iniciar a escuta: ${it.javaClass.simpleName}") }
        }
    }
}

class ReceptorDeInicio : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                TarefaDeRede.agendar(context)
                ServicoDeEscuta.iniciar(context)
            }
        }
    }
}
