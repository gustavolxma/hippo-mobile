package br.com.hippo.data.repository

import br.com.hippo.domain.model.Task
import br.com.hippo.domain.model.TaskPriority
import br.com.hippo.domain.model.TaskReminder
import br.com.hippo.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus

/**
 * Implementação em memória, usada enquanto o backend não está pronto.
 * Os dados se perdem ao fechar o app.
 */
class InMemoryTaskRepository(initialTasks: List<Task> = emptyList()) : TaskRepository {

    private val tasks = MutableStateFlow(initialTasks)

    override fun observeTasks(): Flow<List<Task>> = tasks.asStateFlow()

    override suspend fun getTask(id: String): Task? = tasks.value.firstOrNull { it.id == id }

    override suspend fun addTask(task: Task) {
        tasks.update { it + task }
    }

    override suspend fun updateTask(task: Task) {
        tasks.update { list -> list.map { if (it.id == task.id) task else it } }
    }

    override suspend fun deleteTask(id: String) {
        tasks.update { list -> list.filterNot { it.id == id } }
    }

    companion object {
        /** Mesmos exemplos do protótipo, com datas relativas a [today]. */
        fun sampleTasks(today: LocalDate) = listOf(
            Task(
                id = "1",
                title = "Entregar capítulo 2 do TCC",
                description = "Revisar referências e enviar o PDF ao orientador.",
                priority = TaskPriority.HIGH,
                dueDate = today,
                dueTime = LocalTime(14, 0),
                reminder = TaskReminder.MINUTES_30,
            ),
            Task(
                id = "2",
                title = "Reunião com o orientador",
                priority = TaskPriority.MEDIUM,
                dueDate = today,
                dueTime = LocalTime(16, 30),
            ),
            Task(
                id = "3",
                title = "Treino de pernas",
                priority = TaskPriority.LOW,
                dueDate = today,
                dueTime = LocalTime(19, 0),
                isDone = true,
            ),
            Task(
                id = "4",
                title = "Enviar o relatório",
                priority = TaskPriority.HIGH,
                dueDate = today.plus(DatePeriod(days = 1)),
                dueTime = LocalTime(15, 0),
            ),
        )
    }
}
