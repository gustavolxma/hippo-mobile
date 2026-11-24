package br.com.hippo.domain.reminder

import br.com.hippo.domain.model.Task
import br.com.hippo.domain.model.TaskReminder
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
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

/** Momento em que o lembrete toca, ou `null` se a tarefa não tem lembrete ou horário. */
private fun reminderTime(reminder: TaskReminder, date: LocalDate?, time: LocalTime?, timeZone: TimeZone): LocalDateTime? {
    val minutesBefore = reminder.minutesBefore ?: return null
    if (date == null || time == null) return null
    val dueAt = date.atTime(time).toInstant(timeZone)
    return (dueAt - minutesBefore.minutes).toLocalDateTime(timeZone)
}

/**
 * Calcula o lembrete de uma tarefa, ou `null` se não houver o que agendar
 * (sem lembrete, sem horário, já concluída ou com o momento já passado).
 */
fun reminderFor(task: Task, now: LocalDateTime, timeZone: TimeZone): ScheduledReminder? {
    if (task.isDone) return null
    val time = task.dueTime ?: return null
    val triggerAt = reminderTime(task.reminder, task.dueDate, time, timeZone) ?: return null
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

    /** Se o momento do lembrete já passou: ele não seria agendado, e o usuário precisa saber. */
    fun isInPast(reminder: TaskReminder, date: LocalDate?, time: LocalTime?): Boolean {
        val zone = timeZone()
        val triggerAt = reminderTime(reminder, date, time, zone) ?: return false
        return triggerAt <= clock.now().toLocalDateTime(zone)
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
