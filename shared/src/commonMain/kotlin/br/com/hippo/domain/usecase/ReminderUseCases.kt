package br.com.hippo.domain.usecase

import br.com.hippo.domain.model.TaskReminder
import br.com.hippo.domain.reminder.TaskReminderSync
import br.com.hippo.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * Reagenda os lembretes de todas as tarefas do usuário logado. Usado ao entrar na conta
 * e quando o Android apaga os alarmes (celular reiniciado, mudança de fuso ou de hora).
 */
class ResyncRemindersUseCase(
    private val repository: TaskRepository,
    private val reminders: TaskReminderSync,
) {
    suspend operator fun invoke() {
        repository.observeTasks().first().forEach { reminders.sync(it) }
    }
}

/** Diz se o lembrete escolhido não vai tocar porque o momento dele já passou. */
class IsReminderInPastUseCase(private val reminders: TaskReminderSync) {
    operator fun invoke(reminder: TaskReminder, date: LocalDate?, time: LocalTime?): Boolean =
        reminders.isInPast(reminder, date, time)
}

/** Cancela os lembretes do usuário logado (ao sair da conta, ninguém mais deve recebê-los). */
class CancelAllRemindersUseCase(
    private val repository: TaskRepository,
    private val reminders: TaskReminderSync,
) {
    suspend operator fun invoke() {
        repository.observeTasks().first().forEach { reminders.cancel(it.id) }
    }
}
