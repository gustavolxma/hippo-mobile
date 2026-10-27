package br.com.hippo.data.repository

import br.com.hippo.domain.model.Task
import br.com.hippo.domain.model.TaskPriority
import br.com.hippo.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Implementação em memória, usada enquanto o backend não está pronto.
 * Os dados se perdem ao fechar o app.
 */
class InMemoryTaskRepository(
    initialTasks: List<Task> = sampleTasks,
) : TaskRepository {

    private val tasks = MutableStateFlow(initialTasks)

    override fun observeTasks(): Flow<List<Task>> = tasks.asStateFlow()

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
        val sampleTasks = listOf(
            Task(id = "1", title = "Entregar capítulo do TCC", priority = TaskPriority.URGENT),
            Task(id = "2", title = "Reunião com o orientador", priority = TaskPriority.HIGH),
            Task(id = "3", title = "Comprar café", priority = TaskPriority.LOW),
        )
    }
}
