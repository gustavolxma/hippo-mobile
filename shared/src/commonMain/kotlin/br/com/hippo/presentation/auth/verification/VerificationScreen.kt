package br.com.hippo.presentation.auth.verification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.hippo.domain.usecase.AuthField
import br.com.hippo.presentation.auth.AuthScaffold
import br.com.hippo.presentation.auth.FormErrorText
import br.com.hippo.presentation.components.CodeInput
import br.com.hippo.presentation.components.HippoPrimaryButton
import br.com.hippo.presentation.components.HippoTextLink
import br.com.hippo.presentation.components.HippoTopBar
import br.com.hippo.presentation.theme.HippoColors
import br.com.hippo.presentation.theme.HippoShapes
import br.com.hippo.presentation.theme.HippoTheme
import hippo.shared.generated.resources.Res
import hippo.shared.generated.resources.ic_shield
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun VerificationRoute(
    email: String,
    onNavigateBack: () -> Unit,
    viewModel: VerificationViewModel = koinViewModel(key = email) { parametersOf(email) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                VerificationEffect.NavigateBack -> onNavigateBack()
                is VerificationEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    VerificationScreen(state, snackbarHostState, viewModel::onIntent)
}

@Composable
fun VerificationScreen(
    state: VerificationState,
    snackbarHostState: SnackbarHostState,
    onIntent: (VerificationIntent) -> Unit,
) {
    val muted = HippoTheme.type.body.copy(fontSize = 14.sp)

    AuthScaffold(snackbarHostState) {
        HippoTopBar(title = null, onBack = { onIntent(VerificationIntent.Back) })

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .background(HippoColors.Accent.copy(alpha = 0.14f), HippoShapes.Circle),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_shield),
                    contentDescription = null,
                    tint = HippoColors.Accent,
                    modifier = Modifier.size(36.dp),
                )
            }
            Text(
                "Digite o código",
                style = HippoTheme.type.title,
                modifier = Modifier.padding(top = 16.dp).semantics { heading() },
            )
            Text(
                "Para sua segurança, enviamos um código de 6 dígitos para ${state.email}.",
                style = muted,
                color = HippoColors.OnMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        CodeInput(
            value = state.code,
            onValueChange = { onIntent(VerificationIntent.CodeChanged(it)) },
            isError = state.errors[AuthField.CODE] != null || state.errors.general != null,
            onDone = { onIntent(VerificationIntent.Submit) },
            modifier = Modifier.padding(top = 12.dp),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            FormErrorText(state.errors[AuthField.CODE] ?: state.errors.general)
            if (state.resendSecondsLeft > 0) {
                val seconds = state.resendSecondsLeft
                Text(
                    "Reenviar código em ${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}",
                    style = muted,
                    color = HippoColors.OnMuted,
                )
            } else {
                HippoTextLink("Reenviar código", onClick = { onIntent(VerificationIntent.Resend) })
            }
        }

        HippoPrimaryButton(
            text = if (state.isLoading) "Verificando..." else "Verificar",
            onClick = { onIntent(VerificationIntent.Submit) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(top = 8.dp),
        )
        Text(
            "Não recebeu? Confira a caixa de spam.",
            style = HippoTheme.type.body.copy(fontSize = 12.sp),
            color = HippoColors.OnMuted,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        state.demoCode?.let {
            Text(
                "Modo de teste (sem API): use o código $it",
                style = HippoTheme.type.caption,
                color = HippoColors.Warning,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}
