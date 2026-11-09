package br.com.hippo.presentation.auth.verification

import androidx.lifecycle.viewModelScope
import br.com.hippo.core.mvi.MviViewModel
import br.com.hippo.domain.usecase.AuthField
import br.com.hippo.domain.usecase.ResendCodeUseCase
import br.com.hippo.domain.usecase.VERIFICATION_CODE_LENGTH
import br.com.hippo.domain.usecase.VerifyCodeUseCase
import br.com.hippo.presentation.auth.AuthFormErrors
import br.com.hippo.presentation.auth.toFormErrors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val RESEND_COOLDOWN_SECONDS = 45

/**
 * Ao verificar com sucesso, a sessão é aberta e a raiz do app troca para a área logada,
 * por isso não há efeito de navegação para isso.
 */
class VerificationViewModel(
    email: String,
    demoCode: String?,
    private val verifyCode: VerifyCodeUseCase,
    private val resendCode: ResendCodeUseCase,
) : MviViewModel<VerificationState, VerificationIntent, VerificationEffect>(
    VerificationState(email = email, demoCode = demoCode),
) {

    private var countdown: Job? = null

    init {
        startCountdown()
    }

    override fun onIntent(intent: VerificationIntent) {
        when (intent) {
            is VerificationIntent.CodeChanged -> {
                setState { copy(code = intent.value, errors = errors.clear(AuthField.CODE)) }
                // Verifica sozinho ao completar os dígitos, poupando um toque.
                if (intent.value.length == VERIFICATION_CODE_LENGTH) submit()
            }
            VerificationIntent.Submit -> submit()
            VerificationIntent.Resend -> resend()
            VerificationIntent.Back -> sendEffect(VerificationEffect.NavigateBack)
        }
    }

    private fun submit() {
        if (currentState.isLoading) return
        setState { copy(isLoading = true, errors = AuthFormErrors()) }
        viewModelScope.launch {
            verifyCode(currentState.email, currentState.code)
                .onFailure { error -> setState { copy(isLoading = false, errors = error.toFormErrors()) } }
        }
    }

    private fun resend() {
        if (currentState.resendSecondsLeft > 0) return
        startCountdown()
        viewModelScope.launch {
            resendCode(currentState.email)
                .onSuccess { sendEffect(VerificationEffect.ShowMessage("Enviamos um novo código")) }
                .onFailure {
                    countdown?.cancel()
                    setState { copy(resendSecondsLeft = 0) }
                    sendEffect(VerificationEffect.ShowMessage("Não foi possível reenviar o código"))
                }
        }
    }

    private fun startCountdown() {
        countdown?.cancel()
        countdown = viewModelScope.launch {
            for (seconds in RESEND_COOLDOWN_SECONDS downTo 0) {
                setState { copy(resendSecondsLeft = seconds) }
                if (seconds > 0) delay(1_000)
            }
        }
    }
}
