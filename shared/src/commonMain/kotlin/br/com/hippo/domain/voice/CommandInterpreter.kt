package br.com.hippo.domain.voice

import br.com.hippo.domain.usecase.TaskInput
import kotlinx.datetime.LocalDateTime

/** O que o app entendeu de uma frase falada pelo usuário. */
sealed interface VoiceCommand {
    /** Criar uma tarefa com os dados extraídos da frase. */
    data class CreateTask(val input: TaskInput) : VoiceCommand

    /** Não foi possível extrair uma tarefa da frase. */
    data object NotUnderstood : VoiceCommand
}

/**
 * Transforma o texto transcrito em um comando (Processamento de Linguagem Natural).
 *
 * Há duas implementações: com IA, pela API ([br.com.hippo.data.voice.AiCommandInterpreter]), e local,
 * baseada em regras ([br.com.hippo.data.voice.LocalCommandInterpreter]), usada quando a IA falha.
 */
interface CommandInterpreter {
    /** Identifica a implementação nas métricas: "ia" ou "local". */
    val name: String

    /** [now] permite resolver "hoje", "amanhã", "às 15h" em relação ao momento da fala. */
    suspend fun interpret(text: String, now: LocalDateTime): VoiceCommand
}
