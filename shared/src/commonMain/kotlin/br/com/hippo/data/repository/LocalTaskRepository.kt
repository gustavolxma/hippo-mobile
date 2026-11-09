package br.com.hippo.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import br.com.hippo.data.local.db.HippoDatabase
import br.com.hippo.data.local.db.TaskEntity
import br.com.hippo.domain.model.Task
import br.com.hippo.domain.model.TaskPriority
import br.com.hippo.domain.model.TaskReminder
import br.com.hippo.domain.model.User
import br.com.hippo.domain.repository.TaskRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * Tarefas guardadas no banco SQLite do aparelho (SQLDelight). Cada tarefa pertence ao
 * usuário logado; trocar de conta troca automaticamente a lista observada.
 *
 * Quando a API estiver no ar, o RemoteTaskRepository passa a ser a fonte; este banco
 * pode virar o cache offline dele.
 */
class LocalTaskRepository(
    database: HippoDatabase,
    /** Usuário logado (vem da sessão salva); `null` quando ninguém entrou. */
    private val currentUser: Flow<User?>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TaskRepository {

    private val queries = database.taskEntityQueries

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeTasks(): Flow<List<Task>> =
        currentUser
            .map { it?.id }
            .distinctUntilChanged()
            .flatMapLatest { ownerId ->
                if (ownerId == null) {
                    flowOf(emptyList())
                } else {
                    queries.selectByOwner(ownerId).asFlow().mapToList(ioDispatcher)
                        .map { rows -> rows.map { it.toDomain() } }
                }
            }

    override suspend fun getTask(id: String): Task? = withContext(ioDispatcher) {
        val owner = currentOwner() ?: return@withContext null
        queries.selectById(id, owner).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun addTask(task: Task) = save(task)

    override suspend fun updateTask(task: Task) = save(task)

    override suspend fun deleteTask(id: String) = withContext(ioDispatcher) {
        val owner = requireOwner()
        queries.deleteById(id, owner)
        Unit
    }

    private suspend fun save(task: Task) = withContext(ioDispatcher) {
        queries.upsert(task.toEntity(requireOwner()))
        Unit
    }

    private suspend fun currentOwner(): String? = currentUser.first()?.id

    private suspend fun requireOwner(): String =
        currentOwner() ?: error("Nenhum usuário logado para salvar a tarefa")
}

private fun TaskEntity.toDomain() = Task(
    id = id,
    title = title,
    description = description,
    priority = TaskPriority.entries.firstOrNull { it.name == priority } ?: TaskPriority.MEDIUM,
    dueDate = due_date?.let(LocalDate::parse),
    dueTime = due_time?.let(LocalTime::parse),
    reminder = TaskReminder.entries.firstOrNull { it.name == reminder } ?: TaskReminder.NONE,
    isDone = is_done != 0L,
)

private fun Task.toEntity(ownerId: String) = TaskEntity(
    id = id,
    owner_id = ownerId,
    title = title,
    description = description,
    priority = priority.name,
    due_date = dueDate?.toString(),
    due_time = dueTime?.toString(),
    reminder = reminder.name,
    is_done = if (isDone) 1L else 0L,
)
