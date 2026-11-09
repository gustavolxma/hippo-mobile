package br.com.hippo.data.repository

import br.com.hippo.domain.model.Task
import br.com.hippo.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Repositório em memória, para testar casos de uso sem banco de dados. */
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
}
