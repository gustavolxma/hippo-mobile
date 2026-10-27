package br.com.hippo.domain.usecase

import br.com.hippo.domain.model.Task
import br.com.hippo.domain.model.TaskPriority
import br.com.hippo.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

/**
 * Observa as tarefas já ordenadas: pendentes primeiro, depois pela prioridade
 * e, por fim, pela data de vencimento mais próxima. É o ponto de partida para o
 * "algoritmo de priorização automática" previsto no projeto.
 */
class ObserveTasksUseCase(private val repository: TaskRepository) {
    operator fun invoke(): Flow<List<Task>> =
        repository.observeTasks().map { tasks -> tasks.sortedWith(taskComparator) }

    companion object {
        val taskComparator: Comparator<Task> =
            compareBy<Task> { it.isDone }
                .thenByDescending { it.priority.weight }
                .thenBy(nullsLast()) { it.dueAt }
    }
}

class AddTaskUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(
        title: String,
        description: String = "",
        priority: TaskPriority = TaskPriority.MEDIUM,
        dueAt: LocalDateTime? = null,
    ): Result<Task> {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("O título da tarefa é obrigatório"))
        }
        val task = Task(
            id = Uuid.random().toString(),
            title = cleanTitle,
            description = description.trim(),
            priority = priority,
            dueAt = dueAt,
        )
        return runCatching { repository.addTask(task) }.map { task }
    }
}

class ToggleTaskDoneUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(task: Task): Result<Unit> =
        runCatching { repository.updateTask(task.copy(isDone = !task.isDone)) }
}

class DeleteTaskUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(id: String): Result<Unit> =
        runCatching { repository.deleteTask(id) }
}
