package br.com.hippo.domain.usecase

import br.com.hippo.domain.model.Task
import kotlinx.datetime.LocalDate

enum class TaskSection { OVERDUE, TODAY, UPCOMING, NO_DATE, DONE }

data class TaskGroup(val section: TaskSection, val tasks: List<Task>)

/**
 * Separa as tarefas (já ordenadas) nas seções da tela inicial. Tarefas concluídas
 * de hoje continuam em "Hoje", como no protótipo; as de outros dias vão para "Concluídas".
 */
fun groupTasksByDay(tasks: List<Task>, today: LocalDate): List<TaskGroup> {
    val bySection = tasks.groupBy { task ->
        val date = task.dueDate
        when {
            date == today -> TaskSection.TODAY
            task.isDone -> TaskSection.DONE
            date == null -> TaskSection.NO_DATE
            date < today -> TaskSection.OVERDUE
            else -> TaskSection.UPCOMING
        }
    }
    return TaskSection.entries.mapNotNull { section ->
        bySection[section]?.let { TaskGroup(section, it) }
    }
}
