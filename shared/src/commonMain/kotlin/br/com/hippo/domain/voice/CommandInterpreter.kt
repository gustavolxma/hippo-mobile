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
 * Hoje a implementação é local, baseada em regras ([br.com.hippo.data.voice.LocalCommandInterpreter]).
 * Quando a API tiver a integração com IA (Gemini/OpenAI), basta criar outra implementação
 * que envia o texto ao backend — a tela de Voz não muda.
 */
interface CommandInterpreter {
    /** [now] permite resolver "hoje", "amanhã", "às 15h" em relação ao momento da fala. */
    suspend fun interpret(text: String, now: LocalDateTime): VoiceCommand
}
