package br.com.hippo.presentation.settings.changeaccount

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.hippo.domain.usecase.AuthField
import br.com.hippo.presentation.auth.AuthScaffold
import br.com.hippo.presentation.auth.FormCard
import br.com.hippo.presentation.auth.FormErrorText
import br.com.hippo.presentation.auth.ResendCode
import br.com.hippo.presentation.components.CodeInput
import br.com.hippo.presentation.components.HippoPrimaryButton
import br.com.hippo.presentation.components.HippoTextField
import br.com.hippo.presentation.components.HippoTopBar
import br.com.hippo.presentation.theme.HippoColors
import br.com.hippo.presentation.theme.HippoTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChangeAccountRoute(
    onNavigateBack: () -> Unit,
    viewModel: ChangeAccountViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ChangeAccountEffect.NavigateBack -> onNavigateBack()
                is ChangeAccountEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    ChangeAccountScreen(state, snackbarHostState, viewModel::onIntent)
}

@Composable
fun ChangeAccountScreen(
    state: ChangeAccountState,
    snackbarHostState: SnackbarHostState,
    onIntent: (ChangeAccountIntent) -> Unit,
) {
    val muted = HippoTheme.type.body.copy(fontSize = 14.sp)

    AuthScaffold(snackbarHostState) {
        HippoTopBar(title = "Alterar e-mail ou senha", onBack = { onIntent(ChangeAccountIntent.Back) })

        if (state.currentEmail.isNotEmpty()) {
            Text(
                "Enviamos um código de 6 dígitos para ${state.currentEmail}. Digite-o para continuar.",
                style = muted,
                color = HippoColors.OnMuted,
            )
        }
        CodeInput(
            value = state.code,
            onValueChange = { onIntent(ChangeAccountIntent.CodeChanged(it)) },
            isError = state.errors[AuthField.CODE] != null,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            FormErrorText(state.errors[AuthField.CODE])
            ResendCode(state.resendSecondsLeft, onResend = { onIntent(ChangeAccountIntent.Resend) })
        }

        FormCard {
            HippoTextField(
                label = "Novo e-mail",
                value = state.newEmail,
                onValueChange = { onIntent(ChangeAccountIntent.NewEmailChanged(it)) },
                error = state.errors[AuthField.EMAIL],
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            )
            HippoTextField(
                label = "Nova senha",
                value = state.newPassword,
                onValueChange = { onIntent(ChangeAccountIntent.NewPasswordChanged(it)) },
                error = state.errors[AuthField.PASSWORD],
                enabled = !state.isLoading,
                isPassword = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onIntent(ChangeAccountIntent.Submit) }),
            )
            Text(
                "Preencha só o que quiser alterar.",
                style = HippoTheme.type.body.copy(fontSize = 12.sp),
                color = HippoColors.OnMuted,
            )
            FormErrorText(state.errors.general)
        }

        HippoPrimaryButton(
            text = if (state.isLoading) "Confirmando..." else "Confirmar alteração",
            onClick = { onIntent(ChangeAccountIntent.Submit) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
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
