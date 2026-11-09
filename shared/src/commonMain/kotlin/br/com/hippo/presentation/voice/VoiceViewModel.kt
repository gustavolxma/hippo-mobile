package br.com.hippo.presentation.voice

import androidx.lifecycle.viewModelScope
import br.com.hippo.core.mvi.MviViewModel
import br.com.hippo.domain.metrics.VoiceInteraction
import br.com.hippo.domain.metrics.VoiceMetricsRepository
import br.com.hippo.domain.metrics.VoiceOutcome
import br.com.hippo.domain.usecase.InterpretVoiceCommandUseCase
import br.com.hippo.domain.usecase.SaveTaskUseCase
import br.com.hippo.domain.usecase.TaskInput
import br.com.hippo.domain.voice.SpeechError
import br.com.hippo.domain.voice.SpeechEvent
import br.com.hippo.domain.voice.SpeechRecognitionException
import br.com.hippo.domain.voice.VoiceCommand
import br.com.hippo.domain.voice.VoiceRecognizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.TimeSource

/**
 * Fluxo da tela de Voz: microfone → transcrição → interpretação → cartão de confirmação.
 * Confirmar cria a tarefa; Editar abre os Detalhes já preenchidos.
 *
 * Cada uso também é registrado em [metrics] (tempos e desfecho), para medir a solução.
 */
class VoiceViewModel(
    private val recognizer: VoiceRecognizer,
    private val interpretCommand: InterpretVoiceCommandUseCase,
    private val saveTask: SaveTaskUseCase,
    private val metrics: VoiceMetricsRepository,
    private val clock: Clock,
) : MviViewModel<VoiceState, VoiceIntent, VoiceEffect>(VoiceState()) {

    private var listening: Job? = null

    /** Registro do comando que está no cartão "Entendi!", à espera de Confirmar ou Editar. */
    private var pendingInteractionId: Long? = null

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
                resolvePending(VoiceOutcome.EDITED)
                sendEffect(VoiceEffect.NavigateToNewTask(currentState.task))
                setState { VoiceState() }
            }
            VoiceIntent.CreateManually -> sendEffect(VoiceEffect.NavigateToNewTask(draft = null))
            VoiceIntent.ScreenHidden -> when (currentState.phase) {
                VoicePhase.LISTENING -> {
                    listening?.cancel()
                    setState { VoiceState() }
                }
                VoicePhase.UNDERSTOOD -> resolvePending(VoiceOutcome.DISCARDED)
                else -> Unit
            }
        }
    }

    private fun startListening() {
        if (!recognizer.isAvailable()) {
            showError(SpeechError.NOT_AVAILABLE)
            return
        }
        // Falar de novo com um cartão na tela descarta o comando anterior.
        resolvePending(VoiceOutcome.DISCARDED)
        listening?.cancel()
        setState { VoiceState(phase = VoicePhase.LISTENING) }

        val startedAt = TimeSource.Monotonic.markNow()
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
            .onCompletion { cause ->
                if (cause == null) onSpeechFinished(finalText, startedAt.elapsedNow().inWholeMilliseconds)
            }
            .catch { error ->
                val reason = (error as? SpeechRecognitionException)?.error ?: SpeechError.UNKNOWN
                record(transcript = "", result = null, VoiceOutcome.RECOGNITION_FAILED, startedAt.elapsedNow().inWholeMilliseconds, null)
                showError(reason)
            }
            .launchIn(viewModelScope)
    }

    private suspend fun onSpeechFinished(text: String?, recognitionMs: Long) {
        if (text.isNullOrBlank()) {
            record(transcript = "", result = null, VoiceOutcome.RECOGNITION_FAILED, recognitionMs, null)
            showError(SpeechError.NO_MATCH)
            return
        }
        setState { copy(phase = VoicePhase.PROCESSING) }
        val interpretationStart = TimeSource.Monotonic.markNow()
        val command = interpretCommand(text)
        val interpretationMs = interpretationStart.elapsedNow().inWholeMilliseconds

        when (command) {
            is VoiceCommand.CreateTask -> {
                pendingInteractionId = record(text, command.input, VoiceOutcome.PENDING, recognitionMs, interpretationMs)
                setState { copy(phase = VoicePhase.UNDERSTOOD, task = command.input) }
            }
            VoiceCommand.NotUnderstood -> {
                record(text, null, VoiceOutcome.NOT_UNDERSTOOD, recognitionMs, interpretationMs)
                setState { copy(phase = VoicePhase.NOT_UNDERSTOOD) }
            }
        }
    }

    private fun confirm() {
        val task = currentState.task ?: return
        if (currentState.isSaving) return
        setState { copy(isSaving = true) }
        viewModelScope.launch {
            saveTask(task)
                .onSuccess {
                    resolvePending(VoiceOutcome.CONFIRMED)
                    setState { VoiceState() }
                    sendEffect(VoiceEffect.ShowMessage("Tarefa criada: ${it.title}"))
                }
                .onFailure {
                    setState { copy(isSaving = false) }
                    sendEffect(VoiceEffect.ShowMessage("Não foi possível criar a tarefa"))
                }
        }
    }

    private suspend fun record(
        transcript: String,
        result: TaskInput?,
        outcome: VoiceOutcome,
        recognitionMs: Long?,
        interpretationMs: Long?,
    ): Long? = ignoringFailures {
        metrics.record(
            VoiceInteraction(
                createdAt = clock.now(),
                interpreter = interpretCommand.interpreterName,
                transcript = transcript,
                result = result,
                outcome = outcome,
                recognitionMs = recognitionMs,
                interpretationMs = interpretationMs,
            ),
        )
    }

    /** Fecha o registro pendente com o desfecho (só uma vez por comando). */
    private fun resolvePending(outcome: VoiceOutcome) {
        val id = pendingInteractionId ?: return
        pendingInteractionId = null
        viewModelScope.launch { ignoringFailures { metrics.updateOutcome(id, outcome) } }
    }

    /** Métricas nunca podem atrapalhar o uso do app: falhas ao gravar são ignoradas. */
    private suspend fun <T> ignoringFailures(block: suspend () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
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
