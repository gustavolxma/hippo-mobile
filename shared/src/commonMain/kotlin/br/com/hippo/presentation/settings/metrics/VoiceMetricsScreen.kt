package br.com.hippo.presentation.settings.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.hippo.domain.metrics.VoiceInteraction
import br.com.hippo.domain.metrics.VoiceMetricsSummary
import br.com.hippo.domain.metrics.VoiceOutcome
import br.com.hippo.presentation.auth.AuthScaffold
import br.com.hippo.presentation.components.HippoConfirmDialog
import br.com.hippo.presentation.components.HippoDangerOutlinedButton
import br.com.hippo.presentation.components.HippoSecondaryButton
import br.com.hippo.presentation.components.HippoTopBar
import br.com.hippo.presentation.components.formatted
import br.com.hippo.presentation.components.formattedShort
import br.com.hippo.presentation.components.plainTextClipEntry
import br.com.hippo.presentation.theme.HippoColors
import br.com.hippo.presentation.theme.HippoShapes
import br.com.hippo.presentation.theme.HippoTheme
import br.com.hippo.presentation.theme.hippoCard
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

@Composable
fun VoiceMetricsRoute(
    onNavigateBack: () -> Unit,
    viewModel: VoiceMetricsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboard.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is VoiceMetricsEffect.CopyToClipboard -> {
                    clipboard.setClipEntry(plainTextClipEntry("Métricas de voz", effect.csv))
                    snackbarHostState.showSnackbar("CSV copiado. Cole numa planilha para analisar.")
                }
                is VoiceMetricsEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
                VoiceMetricsEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    VoiceMetricsScreen(state, snackbarHostState, viewModel::onIntent)
}

@Composable
fun VoiceMetricsScreen(
    state: VoiceMetricsState,
    snackbarHostState: SnackbarHostState,
    onIntent: (VoiceMetricsIntent) -> Unit,
) {
    val summary = state.summary

    AuthScaffold(snackbarHostState) {
        HippoTopBar(title = "Métricas de voz", onBack = { onIntent(VoiceMetricsIntent.Back) })
        Text(
            "Cada comando de voz fica registrado só neste aparelho, para medir como a interpretação está se saindo.",
            style = HippoTheme.type.body.copy(fontSize = 14.sp),
            color = HippoColors.OnMuted,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Comandos", summary.total.toString(), Modifier.weight(1f))
            StatCard("Entendidos", summary.understoodRate.asPercent(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Sem precisar editar", summary.confirmedWithoutEditRate.asPercent(), Modifier.weight(1f))
            StatCard("Reconhecimento", summary.averageRecognitionMs.asSeconds(), Modifier.weight(1f))
        }

        MetricsCard("Desfechos") { OutcomeBreakdown(summary) }

        MetricsCard("Últimos comandos") {
            if (state.recent.isEmpty()) {
                Text(
                    "Nenhum comando ainda. Use a aba Voz para começar.",
                    style = HippoTheme.type.body.copy(fontSize = 14.sp),
                    color = HippoColors.OnMuted,
                )
            }
            state.recent.forEach { RecentRow(it) }
        }

        HippoSecondaryButton(
            text = "Copiar CSV",
            onClick = { onIntent(VoiceMetricsIntent.CopyCsv) },
            modifier = Modifier.fillMaxWidth(),
        )
        HippoDangerOutlinedButton(
            text = "Limpar registros",
            onClick = { onIntent(VoiceMetricsIntent.ClearClicked) },
            enabled = summary.total > 0,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (state.showClearDialog) {
        HippoConfirmDialog(
            title = "Limpar registros?",
            message = "Os ${summary.total} comandos registrados serão apagados deste aparelho. As tarefas não são afetadas.",
            confirmText = "Limpar",
            onConfirm = { onIntent(VoiceMetricsIntent.ClearConfirmed) },
            onDismiss = { onIntent(VoiceMetricsIntent.ClearDismissed) },
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .hippoCard(HippoShapes.Large)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(value, style = HippoTheme.type.title.copy(fontSize = 24.sp), color = HippoColors.AccentText)
        Text(label, style = HippoTheme.type.caption, color = HippoColors.OnMuted)
    }
}

@Composable
private fun MetricsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .hippoCard(HippoShapes.Large)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            title,
            style = HippoTheme.type.body.copy(fontSize = 16.sp, fontWeight = FontWeight.Black),
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

@Composable
private fun OutcomeBreakdown(summary: VoiceMetricsSummary) {
    InfoRow("Confirmados como entendidos", summary.confirmed.toString())
    InfoRow("Editados antes de salvar", summary.edited.toString())
    InfoRow("Descartados", summary.discarded.toString())
    InfoRow("Não entendidos", summary.notUnderstood.toString())
    InfoRow("Falhas de reconhecimento", summary.recognitionFailures.toString())
    InfoRow(
        "Tempo médio de interpretação",
        summary.averageInterpretationMs?.let { "$it ms" } ?: "—",
    )
}

@Composable
private fun RecentRow(item: VoiceInteraction) {
    val at = item.createdAt.toLocalDateTime(TimeZone.currentSystemDefault())
    Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = item.transcript.ifBlank { "(sem fala reconhecida)" }.let { "“$it”" },
            style = HippoTheme.type.body.copy(fontSize = 14.sp, fontStyle = FontStyle.Italic),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "${at.date.formattedShort()} ${at.time.formatted()} · ${item.outcome.label}",
            style = HippoTheme.type.caption,
            color = HippoColors.OnMuted,
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text(label, style = HippoTheme.type.body.copy(fontSize = 14.sp), color = HippoColors.OnMuted)
        Text(
            value,
            style = HippoTheme.type.body.copy(fontSize = 14.sp, fontWeight = FontWeight.ExtraBold),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

private val VoiceOutcome.label: String
    get() = when (this) {
        VoiceOutcome.PENDING -> "Aguardando confirmação"
        VoiceOutcome.CONFIRMED -> "Confirmado"
        VoiceOutcome.EDITED -> "Editado"
        VoiceOutcome.DISCARDED -> "Descartado"
        VoiceOutcome.NOT_UNDERSTOOD -> "Não entendido"
        VoiceOutcome.RECOGNITION_FAILED -> "Falha no reconhecimento"
    }

private fun Double?.asPercent(): String = this?.let { "${it.roundToInt()}%" } ?: "—"

/** 1830 ms → "1,8 s" */
private fun Long?.asSeconds(): String = this?.let {
    val tenths = (it + 50) / 100
    "${tenths / 10},${tenths % 10} s"
} ?: "—"
