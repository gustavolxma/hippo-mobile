package br.com.hippo.presentation.settings.metrics

import br.com.hippo.domain.metrics.VoiceInteraction
import br.com.hippo.domain.metrics.VoiceMetricsSummary
import br.com.hippo.domain.metrics.summarize

data class VoiceMetricsState(
    val summary: VoiceMetricsSummary = summarize(emptyList()),
    /** Comandos mais recentes primeiro. */
    val recent: List<VoiceInteraction> = emptyList(),
    val showClearDialog: Boolean = false,
)

sealed interface VoiceMetricsIntent {
    data object CopyCsv : VoiceMetricsIntent
    data object ClearClicked : VoiceMetricsIntent
    data object ClearConfirmed : VoiceMetricsIntent
    data object ClearDismissed : VoiceMetricsIntent
    data object Back : VoiceMetricsIntent
}

sealed interface VoiceMetricsEffect {
    data class CopyToClipboard(val csv: String) : VoiceMetricsEffect
    data class ShowMessage(val message: String) : VoiceMetricsEffect
    data object NavigateBack : VoiceMetricsEffect
}
