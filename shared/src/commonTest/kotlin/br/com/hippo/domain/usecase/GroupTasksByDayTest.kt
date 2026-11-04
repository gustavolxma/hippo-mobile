package br.com.hippo.domain.usecase

import br.com.hippo.domain.model.Task
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class GroupTasksByDayTest {

    private val today = LocalDate(2026, 10, 28)

    @Test
    fun `separa tarefas nas secoes da tela inicial`() {
        val tasks = listOf(
            Task(id = "today", title = "Hoje", dueDate = today),
            Task(id = "today-done", title = "Hoje feita", dueDate = today, isDone = true),
            Task(id = "late", title = "Atrasada", dueDate = LocalDate(2026, 10, 27)),
            Task(id = "late-done", title = "Atrasada feita", dueDate = LocalDate(2026, 10, 27), isDone = true),
            Task(id = "next", title = "Amanhã", dueDate = LocalDate(2026, 10, 29)),
            Task(id = "none", title = "Sem data"),
        )

        val groups = groupTasksByDay(tasks, today).associate { group ->
            group.section to group.tasks.map { it.id }
        }

        assertEquals(
            mapOf(
                TaskSection.OVERDUE to listOf("late"),
                TaskSection.TODAY to listOf("today", "today-done"),
                TaskSection.UPCOMING to listOf("next"),
                TaskSection.NO_DATE to listOf("none"),
                TaskSection.DONE to listOf("late-done"),
            ),
            groups,
        )
    }

    @Test
    fun `omite secoes vazias e mantem a ordem das secoes`() {
        val tasks = listOf(
            Task(id = "none", title = "Sem data"),
            Task(id = "today", title = "Hoje", dueDate = today),
        )

        val sections = groupTasksByDay(tasks, today).map { it.section }

        assertEquals(listOf(TaskSection.TODAY, TaskSection.NO_DATE), sections)
    }
}
