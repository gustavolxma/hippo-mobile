package br.com.hippo.presentation.voice

import androidx.lifecycle.viewModelScope
import br.com.hippo.core.mvi.MviViewModel
import br.com.hippo.domain.usecase.InterpretVoiceCommandUseCase
import br.com.hippo.domain.usecase.SaveTaskUseCase
import br.com.hippo.domain.voice.SpeechError
import br.com.hippo.domain.voice.SpeechEvent
import br.com.hippo.domain.voice.SpeechRecognitionException
import br.com.hippo.domain.voice.VoiceCommand
import br.com.hippo.domain.voice.VoiceRecognizer
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Fluxo da tela de Voz: microfone → transcrição → interpretação → cartão de confirmação.
 * Confirmar cria a tarefa; Editar abre os Detalhes já preenchidos.
 */
class VoiceViewModel(
    private val recognizer: VoiceRecognizer,
    private val interpretCommand: InterpretVoiceCommandUseCase,
    private val saveTask: SaveTaskUseCase,
) : MviViewModel<VoiceState, VoiceIntent, VoiceEffect>(VoiceState()) {

    private var listening: Job? = null

    override fun onIntent(intent: VoiceIntent) {
        when (intent) {
            VoiceIntent.MicClicked -> when (currentState.phase) {
                VoicePhase.LISTENING -> recognizer.stop()
                VoicePhase.PROCESSING -> Unit
                else -> sendEffect(VoiceEffect.RequestMicrophonePermission)
            }
            is VoiceIntent.PermissionResult ->
                if (intent.granted) startListening() else showError(SpeechError.NO_PERMISSION)
            VoiceIntent.Confirm -> confirm()
            VoiceIntent.Edit -> {
                sendEffect(VoiceEffect.NavigateToNewTask(currentState.task))
                setState { VoiceState() }
            }
            VoiceIntent.CreateManually -> sendEffect(VoiceEffect.NavigateToNewTask(draft = null))
            VoiceIntent.ScreenHidden -> if (currentState.phase == VoicePhase.LISTENING) {
                listening?.cancel()
                setState { VoiceState() }
            }
        }
    }

    private fun startListening() {
        if (!recognizer.isAvailable()) {
            showError(SpeechError.NOT_AVAILABLE)
            return
        }
        listening?.cancel()
        setState { VoiceState(phase = VoicePhase.LISTENING) }
        var finalText: String? = null
        listening = recognizer.listen()
            .onEach { event ->
                when (event) {
                    SpeechEvent.Listening -> Unit
                    is SpeechEvent.Partial -> setState { copy(transcript = event.text) }
                    is SpeechEvent.Final -> {
                        finalText = event.text
                        setState { copy(transcript = event.text) }
                    }
                }
            }
            .onCompletion { cause -> if (cause == null) onSpeechFinished(finalText) }
            .catch { error ->
                val reason = (error as? SpeechRecognitionException)?.error ?: SpeechError.UNKNOWN
                showError(reason)
            }
            .launchIn(viewModelScope)
    }

    private suspend fun onSpeechFinished(text: String?) {
        if (text.isNullOrBlank()) {
            showError(SpeechError.NO_MATCH)
            return
        }
        setState { copy(phase = VoicePhase.PROCESSING) }
        when (val command = interpretCommand(text)) {
            is VoiceCommand.CreateTask -> setState { copy(phase = VoicePhase.UNDERSTOOD, task = command.input) }
            VoiceCommand.NotUnderstood -> setState { copy(phase = VoicePhase.NOT_UNDERSTOOD) }
        }
    }

    private fun confirm() {
        val task = currentState.task ?: return
        if (currentState.isSaving) return
        setState { copy(isSaving = true) }
        viewModelScope.launch {
            saveTask(task)
                .onSuccess {
                    setState { VoiceState() }
                    sendEffect(VoiceEffect.ShowMessage("Tarefa criada: ${it.title}"))
                }
                .onFailure {
                    setState { copy(isSaving = false) }
                    sendEffect(VoiceEffect.ShowMessage("Não foi possível criar a tarefa"))
                }
        }
    }

    private fun showError(error: SpeechError) {
        val message = when (error) {
            SpeechError.NO_PERMISSION -> "Permita o acesso ao microfone para usar comandos de voz."
            SpeechError.NOT_AVAILABLE -> "O reconhecimento de voz não está disponível neste aparelho."
            SpeechError.NO_MATCH -> "Não ouvi nada. Toque no microfone e tente de novo."
            SpeechError.NETWORK -> "Sem conexão para reconhecer a voz. Tente de novo."
            SpeechError.UNKNOWN -> "Algo deu errado ao ouvir. Tente de novo."
        }
        setState { VoiceState(phase = VoicePhase.ERROR, errorMessage = message) }
    }
}
