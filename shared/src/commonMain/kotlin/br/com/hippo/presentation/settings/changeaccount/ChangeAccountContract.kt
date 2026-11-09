package br.com.hippo.presentation.settings.changeaccount

import br.com.hippo.presentation.auth.AuthFormErrors

data class ChangeAccountState(
    /** E-mail atual, para onde o código foi enviado. */
    val currentEmail: String = "",
    val code: String = "",
    val newEmail: String = "",
    val newPassword: String = "",
    val isLoading: Boolean = false,
    val resendSecondsLeft: Int = 0,
    val errors: AuthFormErrors = AuthFormErrors(),
    /** Só no modo sem API: o código fixo aceito, mostrado para facilitar os testes. */
    val demoCode: String? = null,
)

sealed interface ChangeAccountIntent {
    data class CodeChanged(val value: String) : ChangeAccountIntent
    data class NewEmailChanged(val value: String) : ChangeAccountIntent
    data class NewPasswordChanged(val value: String) : ChangeAccountIntent
    data object Submit : ChangeAccountIntent
    data object Resend : ChangeAccountIntent
    data object Back : ChangeAccountIntent
}

sealed interface ChangeAccountEffect {
    data object NavigateBack : ChangeAccountEffect
    data class ShowMessage(val message: String) : ChangeAccountEffect
}
