package br.com.hippo.data.remote.dto

import br.com.hippo.domain.model.Task
import br.com.hippo.domain.model.TaskPriority
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * Formato da tarefa trafegado com o backend. É PROVISÓRIO: ajuste os nomes/tipos
 * quando o contrato da API em Spring Boot for definido. Manter DTO separado do
 * modelo de domínio evita que mudanças na API vazem para o resto do app.
 */
@Serializable
data class TaskDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val priority: String = TaskPriority.MEDIUM.name,
    val dueAt: String? = null,
    val done: Boolean = false,
)

fun TaskDto.toDomain() = Task(
    id = id,
    title = title,
    description = description.orEmpty(),
    priority = TaskPriority.entries.firstOrNull { it.name == priority } ?: TaskPriority.MEDIUM,
    dueAt = dueAt?.let(LocalDateTime::parse),
    isDone = done,
)

fun Task.toDto() = TaskDto(
    id = id,
    title = title,
    description = description,
    priority = priority.name,
    dueAt = dueAt?.toString(),
    done = isDone,
)
