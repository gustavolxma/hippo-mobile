package br.com.hippo.presentation.auth.verification

import br.com.hippo.presentation.auth.AuthFormErrors

data class VerificationState(
    val email: String,
    val code: String = "",
    val isLoading: Boolean = false,
    /** Segundos até poder reenviar o código; 0 libera o botão. */
    val resendSecondsLeft: Int = 0,
    val errors: AuthFormErrors = AuthFormErrors(),
    /** Só no modo sem API: o código fixo aceito, mostrado para facilitar os testes. */
    val demoCode: String? = null,
)

sealed interface VerificationIntent {
    data class CodeChanged(val value: String) : VerificationIntent
    data object Submit : VerificationIntent
    data object Resend : VerificationIntent
    data object Back : VerificationIntent
}

sealed interface VerificationEffect {
    data object NavigateBack : VerificationEffect
    data class ShowMessage(val message: String) : VerificationEffect
}
