package br.com.hippo.presentation.auth.register

import br.com.hippo.presentation.auth.AuthFormErrors

data class RegisterState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errors: AuthFormErrors = AuthFormErrors(),
)

sealed interface RegisterIntent {
    data class NameChanged(val value: String) : RegisterIntent
    data class EmailChanged(val value: String) : RegisterIntent
    data class PasswordChanged(val value: String) : RegisterIntent
    data class ConfirmPasswordChanged(val value: String) : RegisterIntent
    data object Submit : RegisterIntent
    data object Back : RegisterIntent
}

sealed interface RegisterEffect {
    data class NavigateToVerification(val email: String) : RegisterEffect
    data object NavigateBack : RegisterEffect
}
