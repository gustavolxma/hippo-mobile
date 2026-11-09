package br.com.hippo.presentation.auth.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.hippo.domain.usecase.AuthField
import br.com.hippo.presentation.auth.AuthScaffold
import br.com.hippo.presentation.auth.FormCard
import br.com.hippo.presentation.auth.FormErrorText
import br.com.hippo.presentation.components.HippoPrimaryButton
import br.com.hippo.presentation.components.HippoSecondaryButton
import br.com.hippo.presentation.components.HippoTextField
import br.com.hippo.presentation.components.HippoTextLink
import br.com.hippo.presentation.theme.HippoColors
import br.com.hippo.presentation.theme.HippoShapes
import br.com.hippo.presentation.theme.HippoTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginRoute(
    onNavigateToRegister: () -> Unit,
    onNavigateToVerification: (email: String) -> Unit,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                LoginEffect.NavigateToRegister -> onNavigateToRegister()
                is LoginEffect.NavigateToVerification -> onNavigateToVerification(effect.email)
                is LoginEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    LoginScreen(state, snackbarHostState, viewModel::onIntent)
}

@Composable
fun LoginScreen(
    state: LoginState,
    snackbarHostState: SnackbarHostState,
    onIntent: (LoginIntent) -> Unit,
) {
    AuthScaffold(snackbarHostState) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(88.dp)
                    .background(HippoColors.Primary, HippoShapes.Circle),
            ) {
                Text("H", style = HippoTheme.type.title.copy(fontSize = 40.sp))
            }
            Text(
                "Hippo",
                style = HippoTheme.type.title,
                modifier = Modifier.padding(top = 16.dp).semantics { heading() },
            )
            Text(
                "Organize suas tarefas com a sua voz",
                style = HippoTheme.type.body.copy(fontSize = 14.sp),
                color = HippoColors.OnMuted,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        FormCard {
            HippoTextField(
                label = "E-mail",
                value = state.email,
                onValueChange = { onIntent(LoginIntent.EmailChanged(it)) },
                error = state.errors[AuthField.EMAIL],
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            )
            HippoTextField(
                label = "Senha",
                value = state.password,
                onValueChange = { onIntent(LoginIntent.PasswordChanged(it)) },
                error = state.errors[AuthField.PASSWORD],
                enabled = !state.isLoading,
                isPassword = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onIntent(LoginIntent.Submit) }),
            )
            FormErrorText(state.errors.general)
            HippoTextLink(
                text = "Esqueci minha senha",
                onClick = { onIntent(LoginIntent.ForgotPassword) },
                modifier = Modifier.align(Alignment.End),
            )
        }

        HippoPrimaryButton(
            text = if (state.isLoading) "Entrando..." else "Entrar",
            onClick = { onIntent(LoginIntent.Submit) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Ainda não tem conta?", style = HippoTheme.type.body.copy(fontSize = 14.sp), color = HippoColors.OnMuted)
            HippoSecondaryButton(
                text = "Criar conta",
                onClick = { onIntent(LoginIntent.CreateAccount) },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            )
        }
    }
}
