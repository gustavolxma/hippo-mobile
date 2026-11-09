package br.com.hippo.data.remote.dto

import br.com.hippo.domain.model.Task
import br.com.hippo.domain.model.TaskPriority
import br.com.hippo.domain.model.TaskReminder
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
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
    /** ISO-8601, ex.: "2026-10-28". */
    val dueDate: String? = null,
    /** ISO-8601, ex.: "14:00". */
    val dueTime: String? = null,
    val reminderMinutesBefore: Int? = null,
    val done: Boolean = false,
)

fun TaskDto.toDomain() = Task(
    id = id,
    title = title,
    description = description.orEmpty(),
    priority = TaskPriority.entries.firstOrNull { it.name == priority } ?: TaskPriority.MEDIUM,
    dueDate = dueDate?.let(LocalDate::parse),
    dueTime = dueTime?.let(LocalTime::parse),
    reminder = TaskReminder.entries.firstOrNull { it.minutesBefore == reminderMinutesBefore }
        ?: TaskReminder.NONE,
    isDone = done,
)

fun Task.toDto() = TaskDto(
    id = id,
    title = title,
    description = description,
    priority = priority.name,
    dueDate = dueDate?.toString(),
    dueTime = dueTime?.toString(),
    reminderMinutesBefore = reminder.minutesBefore,
    done = isDone,
)
