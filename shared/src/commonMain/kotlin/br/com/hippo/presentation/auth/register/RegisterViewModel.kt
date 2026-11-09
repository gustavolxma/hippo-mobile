package br.com.hippo.presentation.auth.register

import androidx.lifecycle.viewModelScope
import br.com.hippo.core.mvi.MviViewModel
import br.com.hippo.domain.usecase.AuthField
import br.com.hippo.domain.usecase.RegisterUseCase
import br.com.hippo.presentation.auth.AuthFormErrors
import br.com.hippo.presentation.auth.toFormErrors
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val register: RegisterUseCase,
) : MviViewModel<RegisterState, RegisterIntent, RegisterEffect>(RegisterState()) {

    override fun onIntent(intent: RegisterIntent) {
        when (intent) {
            is RegisterIntent.NameChanged ->
                setState { copy(name = intent.value, errors = errors.clear(AuthField.NAME)) }
            is RegisterIntent.EmailChanged ->
                setState { copy(email = intent.value, errors = errors.clear(AuthField.EMAIL)) }
            is RegisterIntent.PasswordChanged ->
                setState { copy(password = intent.value, errors = errors.clear(AuthField.PASSWORD)) }
            is RegisterIntent.ConfirmPasswordChanged ->
                setState { copy(confirmPassword = intent.value, errors = errors.clear(AuthField.CONFIRM_PASSWORD)) }
            RegisterIntent.Submit -> submit()
            RegisterIntent.Back -> sendEffect(RegisterEffect.NavigateBack)
        }
    }

    private fun submit() {
        if (currentState.isLoading) return
        setState { copy(isLoading = true, errors = AuthFormErrors()) }
        viewModelScope.launch {
            with(currentState) { register(name, email, password, confirmPassword) }
                .onSuccess { email ->
                    setState { copy(isLoading = false) }
                    sendEffect(RegisterEffect.NavigateToVerification(email))
                }
                .onFailure { error -> setState { copy(isLoading = false, errors = error.toFormErrors()) } }
        }
    }
}
