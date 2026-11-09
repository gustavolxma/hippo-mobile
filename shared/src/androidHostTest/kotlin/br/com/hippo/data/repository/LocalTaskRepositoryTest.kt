package br.com.hippo.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import br.com.hippo.data.local.db.HippoDatabase
import br.com.hippo.domain.model.Task
import br.com.hippo.domain.model.TaskPriority
import br.com.hippo.domain.model.TaskReminder
import br.com.hippo.domain.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Testa o repositório com um banco SQLite real, em memória, rodando no computador. */
class LocalTaskRepositoryTest {

    private val currentUser = MutableStateFlow<User?>(null)
    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { HippoDatabase.Schema.create(it) }
    private val repository = LocalTaskRepository(HippoDatabase(driver), currentUser, Dispatchers.Unconfined)

    private val maria = User("maria-id", "Maria", "maria@email.com")
    private val joao = User("joao-id", "João", "joao@email.com")

    private val task = Task(
        id = "t1",
        title = "Entregar capítulo 2",
        description = "Revisar referências",
        priority = TaskPriority.HIGH,
        dueDate = LocalDate(2026, 11, 10),
        dueTime = LocalTime(14, 0),
        reminder = TaskReminder.MINUTES_30,
    )

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `salva e le todos os campos da tarefa`() = runTest {
        currentUser.value = maria

        repository.addTask(task)

        assertEquals(listOf(task), repository.observeTasks().first())
        assertEquals(task, repository.getTask("t1"))
    }

    @Test
    fun `atualizar substitui e excluir remove`() = runTest {
        currentUser.value = maria
        repository.addTask(task)

        repository.updateTask(task.copy(title = "Novo título", isDone = true))
        assertEquals(listOf(task.copy(title = "Novo título", isDone = true)), repository.observeTasks().first())

        repository.deleteTask("t1")
        assertTrue(repository.observeTasks().first().isEmpty())
    }

    @Test
    fun `cada usuario ve so as proprias tarefas`() = runTest {
        currentUser.value = maria
        repository.addTask(task)

        currentUser.value = joao
        assertTrue(repository.observeTasks().first().isEmpty())
        assertNull(repository.getTask("t1"))

        currentUser.value = maria
        assertEquals(listOf(task), repository.observeTasks().first())
    }

    @Test
    fun `sem usuario logado a lista e vazia`() = runTest {
        currentUser.value = maria
        repository.addTask(task)

        currentUser.value = null

        assertTrue(repository.observeTasks().first().isEmpty())
    }
}
