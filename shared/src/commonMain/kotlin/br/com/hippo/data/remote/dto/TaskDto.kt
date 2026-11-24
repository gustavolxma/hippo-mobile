package br.com.hippo.data.remote.dto

import br.com.hippo.domain.model.Task
import br.com.hippo.domain.model.TaskPriority
import br.com.hippo.domain.model.TaskReminder
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import kotlinx.datetime.format.char
import kotlinx.serialization.Serializable

/** Formato de data e hora da API: "28-10-2026 14:00:00". */
internal val ApiDateTimeFormat = LocalDateTime.Format {
    day()
    char('-')
    monthNumber()
    char('-')
    year()
    char(' ')
    hour()
    char(':')
    minute()
    char(':')
    second()
}

private const val STATUS_DONE = "CONCLUIDA"
private const val STATUS_PENDING = "PENDENTE"

/**
 * Tarefa devolvida pela API. Manter o DTO separado do modelo de domínio evita que
 * mudanças na API vazem para o resto do app.
 */
@Serializable
data class TaskDto(
    val id: Long,
    val nomeTarefa: String,
    val descricao: String? = null,
    val statusTarefa: String? = null,
    val dataEvento: String? = null,
    /** `false` quando a tarefa tem só o dia (a hora de [dataEvento] é ignorada). */
    val temHora: Boolean? = null,
    val prioridade: String? = null,
    val lembreteMinutosAntes: Int? = null,
)

/** Dados enviados para criar ou alterar uma tarefa. O que for nulo fica vazio na API. */
@Serializable
data class TaskInputDto(
    val nomeTarefa: String,
    val descricao: String,
    val dataEvento: String?,
    val temHora: Boolean,
    val prioridade: String,
    val lembreteMinutosAntes: Int?,
)

fun TaskDto.toDomain(): Task {
    val dateTime = dataEvento?.let { ApiDateTimeFormat.parse(it) }
    return Task(
        id = id.toString(),
        title = nomeTarefa,
        description = descricao.orEmpty(),
        priority = priorityFromApi(prioridade),
        dueDate = dateTime?.date,
        dueTime = dateTime?.time?.takeIf { temHora != false },
        reminder = reminderFromApi(lembreteMinutosAntes),
        isDone = statusTarefa == STATUS_DONE,
    )
}

fun Task.toInputDto() = TaskInputDto(
    nomeTarefa = title,
    descricao = description,
    dataEvento = dueDate?.atTime(dueTime ?: LocalTime(0, 0))?.let { ApiDateTimeFormat.format(it) },
    temHora = dueTime != null,
    prioridade = when (priority) {
        TaskPriority.LOW -> "BAIXA"
        TaskPriority.MEDIUM -> "MEDIA"
        TaskPriority.HIGH -> "ALTA"
    },
    lembreteMinutosAntes = reminder.minutesBefore,
)

internal fun Task.apiStatus(): String = if (isDone) STATUS_DONE else STATUS_PENDING

internal fun priorityFromApi(value: String?): TaskPriority = when (value) {
    "ALTA" -> TaskPriority.HIGH
    "BAIXA" -> TaskPriority.LOW
    else -> TaskPriority.MEDIUM
}

internal fun reminderFromApi(minutesBefore: Int?): TaskReminder =
    TaskReminder.entries.firstOrNull { it.minutesBefore == minutesBefore } ?: TaskReminder.NONE
