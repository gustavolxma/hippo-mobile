package br.com.hippo.presentation.settings.changeaccount

import androidx.lifecycle.viewModelScope
import br.com.hippo.core.mvi.MviViewModel
import br.com.hippo.domain.usecase.AuthField
import br.com.hippo.domain.usecase.ObserveCurrentUserUseCase
import br.com.hippo.domain.usecase.RequestAccountChangeCodeUseCase
import br.com.hippo.domain.usecase.UpdateAccountUseCase
import br.com.hippo.presentation.auth.AuthFormErrors
import br.com.hippo.presentation.auth.launchCountdown
import br.com.hippo.presentation.auth.toFormErrors
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Ao abrir, envia o código para o e-mail atual; com ele, o usuário confirma a alteração. */
class ChangeAccountViewModel(
    demoCode: String?,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val requestCode: RequestAccountChangeCodeUseCase,
    private val updateAccount: UpdateAccountUseCase,
) : MviViewModel<ChangeAccountState, ChangeAccountIntent, ChangeAccountEffect>(
    ChangeAccountState(demoCode = demoCode),
) {

    private var countdown: Job? = null

    init {
        viewModelScope.launch {
            val user = observeCurrentUser().first() ?: return@launch
            setState { copy(currentEmail = user.email, newEmail = user.email) }
            sendCode()
        }
    }

    override fun onIntent(intent: ChangeAccountIntent) {
        when (intent) {
            is ChangeAccountIntent.CodeChanged ->
                setState { copy(code = intent.value, errors = errors.clear(AuthField.CODE)) }
            is ChangeAccountIntent.NewEmailChanged ->
                setState { copy(newEmail = intent.value, errors = errors.clear(AuthField.EMAIL)) }
            is ChangeAccountIntent.NewPasswordChanged ->
                setState { copy(newPassword = intent.value, errors = errors.clear(AuthField.PASSWORD)) }
            ChangeAccountIntent.Submit -> submit()
            ChangeAccountIntent.Resend -> if (currentState.resendSecondsLeft == 0) {
                viewModelScope.launch { sendCode(isResend = true) }
            }
            ChangeAccountIntent.Back -> sendEffect(ChangeAccountEffect.NavigateBack)
        }
    }

    private suspend fun sendCode(isResend: Boolean = false) {
        countdown?.cancel()
        countdown = viewModelScope.launchCountdown { left -> setState { copy(resendSecondsLeft = left) } }
        requestCode()
            .onSuccess { if (isResend) sendEffect(ChangeAccountEffect.ShowMessage("Enviamos um novo código")) }
            .onFailure {
                countdown?.cancel()
                setState { copy(resendSecondsLeft = 0) }
                sendEffect(ChangeAccountEffect.ShowMessage("Não foi possível enviar o código"))
            }
    }

    private fun submit() {
        if (currentState.isLoading) return
        setState { copy(isLoading = true, errors = AuthFormErrors()) }
        viewModelScope.launch {
            with(currentState) { updateAccount(currentEmail, code, newEmail, newPassword) }
                .onSuccess {
                    // As Configurações já mostram os dados novos, que vêm da sessão atualizada.
                    setState { copy(isLoading = false) }
                    sendEffect(ChangeAccountEffect.NavigateBack)
                }
                .onFailure { error -> setState { copy(isLoading = false, errors = error.toFormErrors()) } }
        }
    }
}
