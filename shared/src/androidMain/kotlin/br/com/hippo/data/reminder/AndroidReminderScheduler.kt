package br.com.hippo.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import br.com.hippo.domain.reminder.ReminderScheduler
import br.com.hippo.domain.reminder.ScheduledReminder
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * Agenda os lembretes com o AlarmManager, que acorda o app no horário mesmo com ele
 * fechado. Quando o alarme toca, o [ReminderReceiver] mostra a notificação.
 *
 * Limitação: alarmes são apagados quando o celular reinicia. Como as tarefas ainda
 * ficam só em memória, reagendar após o boot só fará sentido com o banco local.
 */
class AndroidReminderScheduler(private val context: Context) : ReminderScheduler {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override suspend fun schedule(reminder: ScheduledReminder) {
        val triggerAtMillis = reminder.triggerAt.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        val pendingIntent = pendingIntent(reminder.taskId) {
            putExtra(ReminderReceiver.EXTRA_TASK_ID, reminder.taskId)
            putExtra(ReminderReceiver.EXTRA_TITLE, reminder.title)
            putExtra(ReminderReceiver.EXTRA_MESSAGE, reminder.message)
        }
        // Alarme exato quando permitido; senão, o sistema pode atrasar alguns minutos.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    override suspend fun cancel(taskId: String) {
        alarmManager.cancel(pendingIntent(taskId))
    }

    /**
     * O endereço "hippo://reminder/<id>" identifica o alarme de cada tarefa: assim
     * reagendar substitui o anterior e cancelar encontra o alarme certo.
     */
    private fun pendingIntent(taskId: String, extras: Intent.() -> Unit = {}): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMINDER
            data = "hippo://reminder/$taskId".toUri()
            extras()
        }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
