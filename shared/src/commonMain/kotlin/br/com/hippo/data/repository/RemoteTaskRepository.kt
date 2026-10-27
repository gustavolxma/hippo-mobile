package br.com.hippo.data.repository

import br.com.hippo.data.remote.TaskApi
import br.com.hippo.data.remote.dto.toDomain
import br.com.hippo.data.remote.dto.toDto
import br.com.hippo.domain.model.Task
import br.com.hippo.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update

/**
 * Implementação que conversa com o backend. Mantém um cache em memória para que a
 * UI reaja na hora, e recarrega do servidor sempre que alguém começa a observar.
 */
class RemoteTaskRepository(private val api: TaskApi) : TaskRepository {

    private val cache = MutableStateFlow<List<Task>>(emptyList())

    override fun observeTasks(): Flow<List<Task>> =
        cache.onStart { refresh() }

    private suspend fun refresh() {
        cache.value = api.getTasks().map { it.toDomain() }
    }

    override suspend fun addTask(task: Task) {
        val created = api.createTask(task.toDto()).toDomain()
        cache.update { it + created }
    }

    override suspend fun updateTask(task: Task) {
        val updated = api.updateTask(task.toDto()).toDomain()
        cache.update { list -> list.map { if (it.id == updated.id) updated else it } }
    }

    override suspend fun deleteTask(id: String) {
        api.deleteTask(id)
        cache.update { list -> list.filterNot { it.id == id } }
    }
}
