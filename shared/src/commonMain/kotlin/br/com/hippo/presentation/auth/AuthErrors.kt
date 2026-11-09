package br.com.hippo.presentation.auth

import br.com.hippo.domain.repository.AuthException
import br.com.hippo.domain.usecase.AuthField
import br.com.hippo.domain.usecase.FormValidationException

/** Erros de um formulário de autenticação: por campo e um erro geral. */
data class AuthFormErrors(
    val fields: Map<AuthField, String> = emptyMap(),
    val general: String? = null,
) {
    operator fun get(field: AuthField): String? = fields[field]

    fun clear(field: AuthField) = copy(fields = fields - field, general = null)
}

/** Converte a falha de um caso de uso no que a tela deve mostrar. */
internal fun Throwable.toFormErrors(): AuthFormErrors = when (this) {
    is FormValidationException -> AuthFormErrors(fields = errors)
    is AuthException -> AuthFormErrors(general = message)
    else -> AuthFormErrors(general = "Não foi possível conectar. Tente novamente.")
}
