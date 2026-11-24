package br.com.hippo.data.repository

import br.com.hippo.data.remote.ApiException
import br.com.hippo.data.remote.TaskApi
import br.com.hippo.data.remote.dto.apiStatus
import br.com.hippo.data.remote.dto.toDomain
import br.com.hippo.data.remote.dto.toInputDto
import br.com.hippo.domain.model.Task
import br.com.hippo.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Tarefas da API, com o banco do aparelho ([local]) como cópia: as telas e os lembretes leem
 * sempre dele, então a lista aparece na hora e pode ser vista sem internet. Criar, alterar e
 * excluir só valem depois de confirmados pela API.
 */
class RemoteTaskRepository(
    private val api: TaskApi,
    private val local: LocalTaskRepository,
) : TaskRepository {

    /** Uma operação por vez, para uma atualização lenta não desfazer uma alteração mais nova. */
    private val mutex = Mutex()

    override fun observeTasks(): Flow<List<Task>> = local.observeTasks()

    override suspend fun getTask(id: String): Task? = local.getTask(id)

    override suspend fun refresh() = mutex.withLock {
        val owner = local.currentOwner() ?: return@withLock
        local.replaceAll(owner, api.getTasks().map { it.toDomain() })
    }

    override suspend fun addTask(task: Task): Task = mutex.withLock {
        // Quem define o id é a API.
        val id = api.createTask(task.toInputDto()).id.toString()
        local.addTask(task.copy(id = id))
    }

    override suspend fun updateTask(task: Task): Task = mutex.withLock {
        val previous = local.getTask(task.id)
        // Na API, o status é alterado em um endpoint separado dos outros dados.
        if (previous?.copy(isDone = task.isDone) != task) api.updateTask(task.id, task.toInputDto())
        if (previous?.isDone != task.isDone) api.updateStatus(task.id, task.apiStatus())
        local.updateTask(task)
    }

    override suspend fun deleteTask(id: String) = mutex.withLock {
        try {
            api.deleteTask(id)
        } catch (e: ApiException) {
            // Já excluída em outro aparelho: só falta tirar daqui.
            if (e.status != NOT_FOUND) throw e
        }
        local.deleteTask(id)
    }

    private companion object {
        const val NOT_FOUND = 404
    }
}
