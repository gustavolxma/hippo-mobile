package br.com.hippo.domain.repository

import br.com.hippo.domain.model.Task
import kotlinx.coroutines.flow.Flow

/**
 * Contrato do domínio para acesso às tarefas. A camada de dados decide se a origem
 * é memória, banco local ou o backend — o restante do app não precisa saber.
 */
interface TaskRepository {
    fun observeTasks(): Flow<List<Task>>
    suspend fun getTask(id: String): Task?

    /** Salva a tarefa e a devolve como ficou: o id pode mudar, pois quem o define é a API. */
    suspend fun addTask(task: Task): Task
    suspend fun updateTask(task: Task): Task
    suspend fun deleteTask(id: String)

    /** Busca as tarefas mais recentes na API. Sem API, não há o que fazer. */
    suspend fun refresh()
}
