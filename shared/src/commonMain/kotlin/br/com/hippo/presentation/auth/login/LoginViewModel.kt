package br.com.hippo.presentation.auth.login

import androidx.lifecycle.viewModelScope
import br.com.hippo.core.mvi.MviViewModel
import br.com.hippo.domain.usecase.AuthField
import br.com.hippo.domain.usecase.LoginUseCase
import br.com.hippo.presentation.auth.AuthFormErrors
import br.com.hippo.presentation.auth.toFormErrors
import kotlinx.coroutines.launch

/**
 * Ao entrar com sucesso não há navegação explícita: a sessão salva faz a raiz do app
 * trocar para a área logada (ver AppRoot).
 */
class LoginViewModel(
    private val login: LoginUseCase,
) : MviViewModel<LoginState, LoginIntent, LoginEffect>(LoginState()) {

    override fun onIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.EmailChanged ->
                setState { copy(email = intent.value, errors = errors.clear(AuthField.EMAIL)) }
            is LoginIntent.PasswordChanged ->
                setState { copy(password = intent.value, errors = errors.clear(AuthField.PASSWORD)) }
            LoginIntent.Submit -> submit()
            LoginIntent.ForgotPassword ->
                sendEffect(LoginEffect.ShowMessage("Recuperação de senha chega junto com a API"))
            LoginIntent.CreateAccount -> sendEffect(LoginEffect.NavigateToRegister)
        }
    }

    private fun submit() {
        if (currentState.isLoading) return
        setState { copy(isLoading = true, errors = AuthFormErrors()) }
        viewModelScope.launch {
            login(currentState.email, currentState.password)
                .onFailure { error -> setState { copy(isLoading = false, errors = error.toFormErrors()) } }
        }
    }
}
