package com.ctrlmesh

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.ctrlmesh.celular.AtividadeDeCaptura

object Notificacoes {
    const val ID_SERVICO = 1
    private const val ID_VER_CELULAR = 2
    private const val ID_ESPELHAR = 3
    private const val ID_ATIVAR_TELA_HORIZONTAL = 4
    private const val PACOTE_DO_SHIZUKU = "moe.shizuku.privileged.api"
    private const val CANAL_ESCUTA = "escuta"
    private const val CANAL_ACAO = "acao"

    private fun gerenciador(context: Context) = context.getSystemService(NotificationManager::class.java)

    fun criarCanais(context: Context) {
        gerenciador(context).createNotificationChannels(
            listOf(
                NotificationChannel(CANAL_ESCUTA, "Escuta e espelhamento", NotificationManager.IMPORTANCE_LOW),
                NotificationChannel(CANAL_ACAO, "Ações do espelhamento", NotificationManager.IMPORTANCE_HIGH),
            ),
        )
    }

    private fun abrirApp(context: Context) = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Notificação do serviço: escuta (RN-012) ou indicador de espelhamento com Encerrar (RN-047). */
    fun servico(context: Context, texto: String, centralEspelhando: String? = null, telaVirtual: Boolean = false): Notification {
        val construtor = Notification.Builder(context, CANAL_ESCUTA)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("CtrlMesh")
            .setContentText(
                centralEspelhando?.let { "espelhando em $it" + if (telaVirtual) " — tela horizontal na central" else "" } ?: texto,
            )
            .setContentIntent(abrirApp(context))
            .setOngoing(true)
        if (centralEspelhando != null) {
            val encerrar = PendingIntent.getService(
                context, 1,
                Intent(context, ServicoDeEscuta::class.java).setAction(ServicoDeEscuta.ACAO_ENCERRAR),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            construtor.addAction(Notification.Action.Builder(null, "Encerrar", encerrar).build())
        }
        return construtor.build().apply { flags = flags or Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR }
    }

    fun atualizarServico(context: Context, notificacao: Notification) {
        runCatching { gerenciador(context).notify(ID_SERVICO, notificacao) }
    }

    /** Central sem sobreposição (RN-084). */
    fun verCelular(context: Context) = postar(context, ID_VER_CELULAR, "toque para ver o celular", abrirApp(context))

    fun cancelarVerCelular(context: Context) = gerenciador(context).cancel(ID_VER_CELULAR)

    /** Celular sem sobreposição (RN-019). */
    fun espelharNaCentral(context: Context) {
        val intent = PendingIntent.getActivity(
            context, 2,
            Intent(context, AtividadeDeCaptura::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        postar(context, ID_ESPELHAR, "toque para espelhar na central", intent)
    }

    fun cancelarEspelharNaCentral(context: Context) = gerenciador(context).cancel(ID_ESPELHAR)

    /** Shizuku desligado com o celular num Wi-Fi (RN-104): o toque abre o Shizuku (ou o CtrlMesh, se não instalado). */
    fun ativarTelaHorizontal(context: Context) {
        val abrir = context.packageManager.getLaunchIntentForPackage(PACOTE_DO_SHIZUKU)
            ?: Intent(context, MainActivity::class.java)
        val intent = PendingIntent.getActivity(
            context, 3, abrir.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        postar(context, ID_ATIVAR_TELA_HORIZONTAL, "toque para ativar a tela horizontal", intent)
    }

    fun cancelarAtivarTelaHorizontal(context: Context) = gerenciador(context).cancel(ID_ATIVAR_TELA_HORIZONTAL)

    private fun postar(context: Context, id: Int, texto: String, acao: PendingIntent) {
        val notificacao = Notification.Builder(context, CANAL_ACAO)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("CtrlMesh")
            .setContentText(texto)
            .setContentIntent(acao)
            .setAutoCancel(true)
            .build()
        runCatching { gerenciador(context).notify(id, notificacao) }
    }
}
