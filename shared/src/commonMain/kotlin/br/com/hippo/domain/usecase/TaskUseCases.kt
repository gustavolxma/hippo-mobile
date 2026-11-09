package br.com.hippo.domain.usecase

import br.com.hippo.domain.model.Task
import br.com.hippo.domain.model.TaskPriority
import br.com.hippo.domain.model.TaskReminder
import br.com.hippo.domain.reminder.TaskReminderSync
import br.com.hippo.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.uuid.Uuid

/**
 * Observa as tarefas já ordenadas: pendentes primeiro, depois pela data/hora mais
 * próxima e, no mesmo horário, pela prioridade. É o ponto de partida para o
 * "algoritmo de priorização automática" previsto no projeto.
 */
class ObserveTasksUseCase(private val repository: TaskRepository) {
    operator fun invoke(): Flow<List<Task>> =
        repository.observeTasks().map { tasks -> tasks.sortedWith(taskComparator) }

    companion object {
        val taskComparator: Comparator<Task> =
            compareBy<Task> { it.isDone }
                .thenBy(nullsLast()) { it.dueDate }
                .thenBy(nullsLast()) { it.dueTime }
                .thenByDescending { it.priority.weight }
    }
}

class GetTaskUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(id: String): Task? = repository.getTask(id)
}

/** Dados que o usuário preenche ao criar ou editar uma tarefa. */
data class TaskInput(
    val title: String,
    val description: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val reminder: TaskReminder = TaskReminder.NONE,
)

class TaskValidationException(message: String) : IllegalArgumentException(message)

/**
 * Cria a tarefa quando [existing] é nulo; caso contrário, atualiza a existente.
 * Em seguida agenda (ou cancela) a notificação de lembrete dela.
 */
class SaveTaskUseCase(
    private val repository: TaskRepository,
    private val reminders: TaskReminderSync,
) {
    suspend operator fun invoke(input: TaskInput, existing: Task? = null): Result<Task> {
        val title = input.title.trim()
        if (title.isEmpty()) {
            return Result.failure(TaskValidationException("O título da tarefa é obrigatório"))
        }
        if (input.dueTime != null && input.dueDate == null) {
            return Result.failure(TaskValidationException("Escolha uma data para o horário"))
        }
        val task = Task(
            id = existing?.id ?: Uuid.random().toString(),
            title = title,
            description = input.description.trim(),
            priority = input.priority,
            dueDate = input.dueDate,
            dueTime = input.dueTime,
            // Sem horário não há como calcular quando o lembrete deve tocar.
            reminder = if (input.dueTime == null) TaskReminder.NONE else input.reminder,
            isDone = existing?.isDone ?: false,
        )
        return runCatching {
            if (existing == null) repository.addTask(task) else repository.updateTask(task)
        }.map {
            reminders.sync(task)
            task
        }
    }
}

/** Concluir cancela o lembrete; reabrir a tarefa agenda de novo (se ainda for futuro). */
class ToggleTaskDoneUseCase(
    private val repository: TaskRepository,
    private val reminders: TaskReminderSync,
) {
    suspend operator fun invoke(task: Task): Result<Unit> {
        val updated = task.copy(isDone = !task.isDone)
        return runCatching { repository.updateTask(updated) }.map { reminders.sync(updated) }
    }
}

class DeleteTaskUseCase(
    private val repository: TaskRepository,
    private val reminders: TaskReminderSync,
) {
    suspend operator fun invoke(id: String): Result<Unit> =
        runCatching { repository.deleteTask(id) }.map { reminders.cancel(id) }
}
