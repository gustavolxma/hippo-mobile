package br.com.hippo.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * Entidade central do domínio. Não depende de framework nenhum (Ktor, Compose, banco...),
 * seguindo a regra de dependência da Clean Architecture.
 *
 * Data e hora são separadas porque uma tarefa pode ter dia mas não horário.
 */
data class Task(
    val id: String,
    val title: String,
    val description: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val reminder: TaskReminder = TaskReminder.NONE,
    val isDone: Boolean = false,
)

enum class TaskPriority(val weight: Int) {
    LOW(0),
    MEDIUM(1),
    HIGH(2),
}

/** Quanto tempo antes do horário da tarefa o lembrete dispara. */
enum class TaskReminder(val minutesBefore: Int?) {
    NONE(null),
    AT_TIME(0),
    MINUTES_10(10),
    MINUTES_30(30),
    HOUR_1(60),
    DAY_1(24 * 60),
}
