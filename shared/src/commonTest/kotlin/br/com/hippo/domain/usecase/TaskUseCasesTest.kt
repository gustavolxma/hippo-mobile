package br.com.hippo.domain.usecase

import br.com.hippo.data.repository.InMemoryTaskRepository
import br.com.hippo.domain.model.Task
import br.com.hippo.domain.model.TaskPriority
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TaskUseCasesTest {

    @Test
    fun `ordena pendentes primeiro e depois por prioridade e vencimento`() = runTest {
        val repository = InMemoryTaskRepository(
            listOf(
                Task(id = "done", title = "Feita", priority = TaskPriority.URGENT, isDone = true),
                Task(id = "low", title = "Baixa", priority = TaskPriority.LOW),
                Task(id = "high-late", title = "Alta tarde", priority = TaskPriority.HIGH, dueAt = LocalDateTime(2026, 12, 1, 10, 0)),
                Task(id = "high-soon", title = "Alta cedo", priority = TaskPriority.HIGH, dueAt = LocalDateTime(2026, 11, 1, 10, 0)),
            )
        )

        val ids = ObserveTasksUseCase(repository)().first().map { it.id }

        assertEquals(listOf("high-soon", "high-late", "low", "done"), ids)
    }

    @Test
    fun `nao adiciona tarefa com titulo vazio`() = runTest {
        val repository = InMemoryTaskRepository(emptyList())

        val result = AddTaskUseCase(repository)(title = "   ")

        assertTrue(result.isFailure)
        assertTrue(repository.observeTasks().first().isEmpty())
    }

    @Test
    fun `adiciona tarefa com titulo sem espacos extras`() = runTest {
        val repository = InMemoryTaskRepository(emptyList())

        AddTaskUseCase(repository)(title = "  Estudar KMP  ", priority = TaskPriority.HIGH)

        val saved = repository.observeTasks().first().single()
        assertEquals("Estudar KMP", saved.title)
        assertEquals(TaskPriority.HIGH, saved.priority)
    }
}
