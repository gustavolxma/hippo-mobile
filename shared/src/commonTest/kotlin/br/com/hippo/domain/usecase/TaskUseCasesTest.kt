package br.com.hippo.domain.usecase

import br.com.hippo.data.repository.InMemoryTaskRepository
import br.com.hippo.domain.model.Task
import br.com.hippo.domain.model.TaskPriority
import br.com.hippo.domain.model.TaskReminder
import br.com.hippo.domain.reminder.testReminderSync
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TaskUseCasesTest {

    private val today = LocalDate(2026, 10, 28)

    @Test
    fun `ordena pendentes primeiro e depois por data hora e prioridade`() = runTest {
        val repository = InMemoryTaskRepository(
            listOf(
                Task(id = "done", title = "Feita", priority = TaskPriority.HIGH, dueDate = today, isDone = true),
                Task(id = "no-date", title = "Sem data", priority = TaskPriority.HIGH),
                Task(id = "late", title = "Tarde", dueDate = today, dueTime = LocalTime(18, 0)),
                Task(id = "early-low", title = "Cedo baixa", priority = TaskPriority.LOW, dueDate = today, dueTime = LocalTime(9, 0)),
                Task(id = "early-high", title = "Cedo alta", priority = TaskPriority.HIGH, dueDate = today, dueTime = LocalTime(9, 0)),
            )
        )

        val ids = ObserveTasksUseCase(repository)().first().map { it.id }

        assertEquals(listOf("early-high", "early-low", "late", "no-date", "done"), ids)
    }

    @Test
    fun `nao salva tarefa com titulo vazio`() = runTest {
        val repository = InMemoryTaskRepository()

        val result = SaveTaskUseCase(repository, testReminderSync())(TaskInput(title = "   "))

        assertIs<TaskValidationException>(result.exceptionOrNull())
        assertTrue(repository.observeTasks().first().isEmpty())
    }

    @Test
    fun `exige data quando ha horario`() = runTest {
        val result = SaveTaskUseCase(InMemoryTaskRepository(), testReminderSync())(
            TaskInput(title = "Reunião", dueTime = LocalTime(10, 0)),
        )

        assertIs<TaskValidationException>(result.exceptionOrNull())
    }

    @Test
    fun `remove lembrete quando nao ha horario`() = runTest {
        val saved = SaveTaskUseCase(InMemoryTaskRepository(), testReminderSync())(
            TaskInput(title = "Estudar", dueDate = today, reminder = TaskReminder.MINUTES_30),
        ).getOrThrow()

        assertEquals(TaskReminder.NONE, saved.reminder)
    }

    @Test
    fun `cria tarefa nova com titulo sem espacos extras`() = runTest {
        val repository = InMemoryTaskRepository()

        SaveTaskUseCase(repository, testReminderSync())(TaskInput(title = "  Estudar KMP  ", priority = TaskPriority.HIGH))

        val saved = repository.observeTasks().first().single()
        assertEquals("Estudar KMP", saved.title)
        assertEquals(TaskPriority.HIGH, saved.priority)
    }

    @Test
    fun `edita tarefa existente mantendo id e status`() = runTest {
        val existing = Task(id = "1", title = "Antigo", isDone = true)
        val repository = InMemoryTaskRepository(listOf(existing))

        SaveTaskUseCase(repository, testReminderSync())(TaskInput(title = "Novo"), existing = existing)

        val saved = repository.observeTasks().first().single()
        assertEquals("1", saved.id)
        assertEquals("Novo", saved.title)
        assertTrue(saved.isDone)
    }
}
