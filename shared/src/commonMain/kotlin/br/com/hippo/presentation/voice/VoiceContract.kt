package br.com.hippo.presentation.voice

import br.com.hippo.domain.usecase.TaskInput

enum class VoicePhase {
    /** Esperando o usuário tocar no microfone. */
    IDLE,
    LISTENING,
    /** Interpretando o que foi dito. */
    PROCESSING,
    /** Entendeu: mostra o cartão "Entendi! Nova tarefa". */
    UNDERSTOOD,
    NOT_UNDERSTOOD,
    ERROR,
}

data class VoiceState(
    val phase: VoicePhase = VoicePhase.IDLE,
    val transcript: String = "",
    val task: TaskInput? = null,
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
)

sealed interface VoiceIntent {
    data object MicClicked : VoiceIntent
    data class PermissionResult(val granted: Boolean) : VoiceIntent
    data object Confirm : VoiceIntent
    data object Edit : VoiceIntent
    data object CreateManually : VoiceIntent
    /** A tela saiu de vista (troca de aba, app em segundo plano): fecha o microfone. */
    data object ScreenHidden : VoiceIntent
}

sealed interface VoiceEffect {
    data object RequestMicrophonePermission : VoiceEffect
    /** Abre os Detalhes para criar a tarefa; [draft] pré-preenche os campos. */
    data class NavigateToNewTask(val draft: TaskInput?) : VoiceEffect
    data class ShowMessage(val message: String) : VoiceEffect
}
