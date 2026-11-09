package br.com.hippo.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import br.com.hippo.domain.usecase.ResyncRemindersUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * O Android apaga os alarmes quando o celular reinicia (e eles ficam no horário errado se
 * o fuso ou a hora mudarem). Este receiver reagenda os lembretes a partir do banco local.
 */
class RescheduleRemindersReceiver : BroadcastReceiver(), KoinComponent {

    private val resyncReminders: ResyncRemindersUseCase by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        // goAsync dá alguns segundos extras para terminar o trabalho fora da thread principal.
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                resyncReminders()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
        )
    }
}
