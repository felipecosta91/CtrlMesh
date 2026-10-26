package com.ctrlmesh

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context

/**
 * Rede de segurança do início automático (RN-012): centrais que só entram em espera não mandam BOOT_COMPLETED ao
 * acordar, e o sistema pode ter matado o processo. Este job, persistido e com rede Wi-Fi (não tarifada), sobe o
 * [ServicoDeEscuta] quando a central volta a uma rede, e se reagenda.
 */
class TarefaDeRede : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        registrar("tarefa de rede: subindo a escuta")
        ServicoDeEscuta.iniciar(this)
        agendar(this)
        return false
    }

    override fun onStopJob(params: JobParameters?) = false

    companion object {
        private const val ID = 4702
        // Sem o atraso, a restrição (Wi-Fi presente) continuaria satisfeita e o job rodaria em laço.
        private const val INTERVALO_MINIMO_MS = 60_000L

        fun agendar(context: Context) {
            val agendador = context.getSystemService(JobScheduler::class.java)
            val tarefa = JobInfo.Builder(ID, ComponentName(context, TarefaDeRede::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .setMinimumLatency(INTERVALO_MINIMO_MS)
                .setPersisted(true)
                .build()
            runCatching { agendador.schedule(tarefa) }.onFailure { registrar("tarefa de rede não agendada: ${it.message}") }
        }
    }
}
