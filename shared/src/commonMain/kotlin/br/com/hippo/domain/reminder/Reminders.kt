package br.com.hippo.domain.reminder

import br.com.hippo.domain.model.Task
import br.com.hippo.domain.model.TaskReminder
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

/** Uma notificação de lembrete pronta para ser agendada no aparelho. */
data class ScheduledReminder(
    val taskId: String,
    val title: String,
    val message: String,
    val triggerAt: LocalDateTime,
)

/**
 * Agenda notificações locais. Cada plataforma usa o mecanismo nativo:
 * AlarmManager no Android e UNUserNotificationCenter no iOS.
 */
interface ReminderScheduler {
    suspend fun schedule(reminder: ScheduledReminder)
    suspend fun cancel(taskId: String)
}

/**
 * Calcula o lembrete de uma tarefa, ou `null` se não houver o que agendar
 * (sem lembrete, sem horário, já concluída ou com o momento já passado).
 */
fun reminderFor(task: Task, now: LocalDateTime, timeZone: TimeZone): ScheduledReminder? {
    val minutesBefore = task.reminder.minutesBefore ?: return null
    val date = task.dueDate ?: return null
    val time = task.dueTime ?: return null
    if (task.isDone) return null

    val dueAt = date.atTime(time).toInstant(timeZone)
    val triggerAt = (dueAt - minutesBefore.minutes).toLocalDateTime(timeZone)
    if (triggerAt <= now) return null

    return ScheduledReminder(
        taskId = task.id,
        title = task.title,
        message = reminderMessage(task.reminder, time.toString()),
        triggerAt = triggerAt,
    )
}

/** Texto da notificação: "Está na hora (15:00)", "Em 30 minutos, às 15:00"... */
internal fun reminderMessage(reminder: TaskReminder, time: String): String = when (reminder) {
    TaskReminder.NONE, TaskReminder.AT_TIME -> "Está na hora ($time)"
    TaskReminder.MINUTES_10 -> "Em 10 minutos, às $time"
    TaskReminder.MINUTES_30 -> "Em 30 minutos, às $time"
    TaskReminder.HOUR_1 -> "Em 1 hora, às $time"
    TaskReminder.DAY_1 -> "Amanhã às $time"
}

/**
 * Mantém a notificação de uma tarefa em dia com o estado dela: agenda quando há
 * lembrete futuro e cancela nos demais casos (concluída, sem lembrete, excluída).
 * Falhas aqui nunca impedem salvar a tarefa.
 */
class TaskReminderSync(
    private val scheduler: ReminderScheduler,
    private val clock: Clock,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {
    suspend fun sync(task: Task) = ignoringFailures {
        val zone = timeZone()
        val reminder = reminderFor(task, clock.now().toLocalDateTime(zone), zone)
        if (reminder != null) scheduler.schedule(reminder) else scheduler.cancel(task.id)
    }

    suspend fun cancel(taskId: String) = ignoringFailures { scheduler.cancel(taskId) }

    private suspend fun ignoringFailures(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Sem permissão de notificação, por exemplo: a tarefa é salva mesmo assim.
        }
    }
}
