package br.com.hippo.presentation.auth.login

import br.com.hippo.presentation.auth.AuthFormErrors

data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errors: AuthFormErrors = AuthFormErrors(),
)

sealed interface LoginIntent {
    data class EmailChanged(val value: String) : LoginIntent
    data class PasswordChanged(val value: String) : LoginIntent
    data object Submit : LoginIntent
    data object ForgotPassword : LoginIntent
    data object CreateAccount : LoginIntent
}

sealed interface LoginEffect {
    data object NavigateToRegister : LoginEffect
    data class NavigateToVerification(val email: String) : LoginEffect
    data class ShowMessage(val message: String) : LoginEffect
}
