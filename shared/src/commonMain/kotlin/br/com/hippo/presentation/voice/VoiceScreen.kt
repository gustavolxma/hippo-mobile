package br.com.hippo.presentation.voice

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.hippo.domain.usecase.TaskInput
import br.com.hippo.presentation.components.HippoPrimaryButton
import br.com.hippo.presentation.components.HippoSecondaryButton
import br.com.hippo.presentation.components.HippoTextLink
import br.com.hippo.presentation.components.describeWhen
import br.com.hippo.presentation.components.label
import br.com.hippo.presentation.theme.HippoColors
import br.com.hippo.presentation.theme.HippoShapes
import br.com.hippo.presentation.theme.HippoTheme
import br.com.hippo.presentation.theme.hippoCard
import hippo.shared.generated.resources.Res
import hippo.shared.generated.resources.ic_mic
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

@Composable
fun VoiceRoute(
    onNavigateToNewTask: (draft: TaskInput?) -> Unit,
    viewModel: VoiceViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val requestPermission = rememberMicrophonePermissionRequest { granted ->
        viewModel.onIntent(VoiceIntent.PermissionResult(granted))
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                VoiceEffect.RequestMicrophonePermission -> requestPermission()
                is VoiceEffect.NavigateToNewTask -> onNavigateToNewTask(effect.draft)
                is VoiceEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }
    DisposableEffect(viewModel) {
        onDispose { viewModel.onIntent(VoiceIntent.ScreenHidden) }
    }

    Box(Modifier.fillMaxSize()) {
        VoiceScreen(state, viewModel::onIntent)
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun VoiceScreen(state: VoiceState, onIntent: (VoiceIntent) -> Unit) {
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp),
    ) {
        Text("Fale com o Hippo", style = HippoTheme.type.title, modifier = Modifier.semantics { heading() })
        Text(
            text = when (state.phase) {
                VoicePhase.LISTENING -> "Ouvindo... toque de novo para parar"
                VoicePhase.PROCESSING -> "Entendendo o que você disse..."
                else -> "Toque no microfone e diga o que precisa"
            },
            style = HippoTheme.type.body.copy(fontSize = 14.sp),
            color = HippoColors.OnMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )

        MicButton(
            listening = state.phase == VoicePhase.LISTENING,
            enabled = state.phase != VoicePhase.PROCESSING && !state.isSaving,
            onClick = { onIntent(VoiceIntent.MicClicked) },
        )

        if (state.transcript.isNotBlank()) {
            Text(
                text = "“${state.transcript}”",
                style = HippoTheme.type.body.copy(fontStyle = FontStyle.Italic),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .hippoCard(HippoShapes.Large)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            )
        }

        when (state.phase) {
            VoicePhase.PROCESSING -> CircularProgressIndicator(color = HippoColors.AccentText)
            VoicePhase.UNDERSTOOD -> state.task?.let { task ->
                UnderstoodCard(task, today, isSaving = state.isSaving, onIntent = onIntent)
            }
            VoicePhase.NOT_UNDERSTOOD -> MessageCard(
                "Não consegui identificar a tarefa. Tente algo como “Me lembra de pagar a conta amanhã às 10h”.",
            )
            VoicePhase.ERROR -> state.errorMessage?.let { MessageCard(it, isError = true) }
            else -> Unit
        }

        HippoTextLink("Criar tarefa manualmente", onClick = { onIntent(VoiceIntent.CreateManually) })
    }
}

/** Microfone lima no centro de dois círculos violeta, que pulsam enquanto o app ouve. */
@Composable
private fun MicButton(listening: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val pulse = if (listening) {
        val transition = rememberInfiniteTransition()
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 700), RepeatMode.Reverse),
        ).value
    } else {
        1f
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(190.dp)
            .scale(pulse)
            .background(HippoColors.Primary.copy(alpha = 0.22f), HippoShapes.Circle),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(140.dp).background(HippoColors.Primary.copy(alpha = 0.45f), HippoShapes.Circle),
        ) {
            IconButton(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.size(96.dp).background(HippoColors.Accent, HippoShapes.Circle),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_mic),
                    contentDescription = if (listening) "Parar de ouvir" else "Falar",
                    tint = HippoColors.OnAccent,
                    modifier = Modifier.size(44.dp),
                )
            }
        }
    }
}

/** Cartão "Entendi! Nova tarefa" com o resumo e as ações Confirmar / Editar. */
@Composable
private fun UnderstoodCard(task: TaskInput, today: LocalDate, isSaving: Boolean, onIntent: (VoiceIntent) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(HippoColors.Surface, HippoShapes.Large)
            .border(2.dp, HippoColors.Primary, HippoShapes.Large)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(
            "Entendi! Nova tarefa",
            style = HippoTheme.type.body.copy(fontSize = 16.sp, fontWeight = FontWeight.Black),
            color = HippoColors.AccentText,
            modifier = Modifier.padding(bottom = 2.dp),
        )
        SummaryRow("Tarefa", task.title)
        SummaryRow("Quando", describeWhen(task.dueDate, task.dueTime, today))
        SummaryRow("Prioridade", task.priority.label)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            HippoPrimaryButton(
                text = if (isSaving) "Criando..." else "Confirmar tarefa",
                onClick = { onIntent(VoiceIntent.Confirm) },
                enabled = !isSaving,
                modifier = Modifier.weight(1f),
            )
            HippoSecondaryButton("Editar", onClick = { onIntent(VoiceIntent.Edit) }, enabled = !isSaving)
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
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

@Composable
private fun MessageCard(message: String, isError: Boolean = false) {
    Text(
        text = message,
        style = HippoTheme.type.body.copy(fontSize = 14.sp),
        color = if (isError) HippoColors.Danger else HippoColors.OnMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .hippoCard(HippoShapes.Large)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}
