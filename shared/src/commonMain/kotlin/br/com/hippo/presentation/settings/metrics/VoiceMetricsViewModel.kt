package br.com.hippo.presentation.settings.metrics

import androidx.lifecycle.viewModelScope
import br.com.hippo.core.mvi.MviViewModel
import br.com.hippo.domain.metrics.VoiceInteraction
import br.com.hippo.domain.metrics.VoiceMetricsRepository
import br.com.hippo.domain.metrics.summarize
import br.com.hippo.domain.metrics.toCsv
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/** Resumo e lista dos comandos de voz registrados, com exportação em CSV. */
class VoiceMetricsViewModel(
    private val metrics: VoiceMetricsRepository,
) : MviViewModel<VoiceMetricsState, VoiceMetricsIntent, VoiceMetricsEffect>(VoiceMetricsState()) {

    private var all: List<VoiceInteraction> = emptyList()

    init {
        metrics.observe()
            .onEach { interactions ->
                all = interactions
                setState { copy(summary = summarize(interactions), recent = interactions.take(RECENT_LIMIT)) }
            }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: VoiceMetricsIntent) {
        when (intent) {
            VoiceMetricsIntent.CopyCsv ->
                if (all.isEmpty()) {
                    sendEffect(VoiceMetricsEffect.ShowMessage("Ainda não há comandos registrados"))
                } else {
                    sendEffect(VoiceMetricsEffect.CopyToClipboard(toCsv(all)))
                }
            VoiceMetricsIntent.ClearClicked -> setState { copy(showClearDialog = true) }
            VoiceMetricsIntent.ClearDismissed -> setState { copy(showClearDialog = false) }
            VoiceMetricsIntent.ClearConfirmed -> {
                setState { copy(showClearDialog = false) }
                viewModelScope.launch { metrics.clear() }
            }
            VoiceMetricsIntent.Back -> sendEffect(VoiceMetricsEffect.NavigateBack)
        }
    }

    private companion object {
        const val RECENT_LIMIT = 20
    }
}
