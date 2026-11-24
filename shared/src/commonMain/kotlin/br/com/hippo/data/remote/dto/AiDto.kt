package br.com.hippo.data.remote.dto

import br.com.hippo.domain.usecase.TaskInput
import br.com.hippo.domain.voice.VoiceCommand
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format.char
import kotlinx.serialization.Serializable

/** [agora] é a hora do celular, para a IA entender "amanhã", "às 15h"... */
@Serializable
data class InterpretCommandDto(val texto: String, val agora: String)

@Serializable
data class InterpretedCommandDto(
    val entendeu: Boolean,
    val nomeTarefa: String? = null,
    val descricao: String? = null,
    /** "28-10-2026" */
    val data: String? = null,
    /** "14:00" */
    val hora: String? = null,
    val prioridade: String? = null,
    val lembreteMinutosAntes: Int? = null,
)

private val ApiDateFormat = LocalDate.Format {
    day()
    char('-')
    monthNumber()
    char('-')
    year()
}

fun InterpretedCommandDto.toCommand(): VoiceCommand {
    if (!entendeu || nomeTarefa.isNullOrBlank()) return VoiceCommand.NotUnderstood
    val date = data?.let { ApiDateFormat.parse(it) }
    return VoiceCommand.CreateTask(
        TaskInput(
            title = nomeTarefa,
            description = descricao.orEmpty(),
            priority = priorityFromApi(prioridade),
            dueDate = date,
            dueTime = hora?.let { LocalTime.parse(it) }.takeIf { date != null },
            reminder = reminderFromApi(lembreteMinutosAntes),
        ),
    )
}
