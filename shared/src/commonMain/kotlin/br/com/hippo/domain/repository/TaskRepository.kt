package br.com.hippo.domain.repository

import br.com.hippo.domain.model.Task
import kotlinx.coroutines.flow.Flow

/**
 * Contrato do domínio para acesso às tarefas. A camada de dados decide se a origem
 * é memória, banco local ou o backend — o restante do app não precisa saber.
 */
interface TaskRepository {
    fun observeTasks(): Flow<List<Task>>
    suspend fun addTask(task: Task)
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(id: String)
}
