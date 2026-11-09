package br.com.hippo.presentation.auth.register

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.hippo.domain.usecase.AuthField
import br.com.hippo.domain.usecase.MIN_PASSWORD_LENGTH
import br.com.hippo.presentation.auth.AuthScaffold
import br.com.hippo.presentation.auth.FormCard
import br.com.hippo.presentation.auth.FormErrorText
import br.com.hippo.presentation.components.HippoPrimaryButton
import br.com.hippo.presentation.components.HippoTextField
import br.com.hippo.presentation.components.HippoTextLink
import br.com.hippo.presentation.components.HippoTopBar
import br.com.hippo.presentation.theme.HippoColors
import br.com.hippo.presentation.theme.HippoTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RegisterRoute(
    onNavigateToVerification: (email: String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: RegisterViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is RegisterEffect.NavigateToVerification -> onNavigateToVerification(effect.email)
                RegisterEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    RegisterScreen(state, viewModel::onIntent)
}

@Composable
fun RegisterScreen(state: RegisterState, onIntent: (RegisterIntent) -> Unit) {
    AuthScaffold {
        HippoTopBar(title = "Criar conta", onBack = { onIntent(RegisterIntent.Back) })

        FormCard {
            HippoTextField(
                label = "Nome",
                value = state.name,
                onValueChange = { onIntent(RegisterIntent.NameChanged(it)) },
                error = state.errors[AuthField.NAME],
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            )
            HippoTextField(
                label = "E-mail",
                value = state.email,
                onValueChange = { onIntent(RegisterIntent.EmailChanged(it)) },
                error = state.errors[AuthField.EMAIL],
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            )
            HippoTextField(
                label = "Senha (mínimo $MIN_PASSWORD_LENGTH caracteres)",
                value = state.password,
                onValueChange = { onIntent(RegisterIntent.PasswordChanged(it)) },
                error = state.errors[AuthField.PASSWORD],
                enabled = !state.isLoading,
                isPassword = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            HippoTextField(
                label = "Confirmar senha",
                value = state.confirmPassword,
                onValueChange = { onIntent(RegisterIntent.ConfirmPasswordChanged(it)) },
                error = state.errors[AuthField.CONFIRM_PASSWORD],
                enabled = !state.isLoading,
                isPassword = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onIntent(RegisterIntent.Submit) }),
            )
            FormErrorText(state.errors.general)
        }

        HippoPrimaryButton(
            text = if (state.isLoading) "Criando conta..." else "Criar conta",
            onClick = { onIntent(RegisterIntent.Submit) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text("Já tem conta?", style = HippoTheme.type.body.copy(fontSize = 14.sp), color = HippoColors.OnMuted)
            HippoTextLink("Entrar", onClick = { onIntent(RegisterIntent.Back) })
        }
    }
}
