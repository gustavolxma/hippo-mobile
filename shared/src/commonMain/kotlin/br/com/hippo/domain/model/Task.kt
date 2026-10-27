package br.com.hippo.domain.model

import kotlinx.datetime.LocalDateTime

/**
 * Entidade central do domínio. Não depende de framework nenhum (Ktor, Compose, banco...),
 * seguindo a regra de dependência da Clean Architecture.
 */
data class Task(
    val id: String,
    val title: String,
    val description: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueAt: LocalDateTime? = null,
    val isDone: Boolean = false,
)

enum class TaskPriority(val weight: Int) {
    LOW(0),
    MEDIUM(1),
    HIGH(2),
    URGENT(3),
}
