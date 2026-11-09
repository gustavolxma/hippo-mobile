package br.com.hippo.domain.usecase

import br.com.hippo.domain.model.User
import br.com.hippo.domain.repository.AuthException
import br.com.hippo.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

enum class AuthField { NAME, EMAIL, PASSWORD, CONFIRM_PASSWORD, CODE }

/** Falha de validação do formulário, com a mensagem de cada campo inválido. */
class FormValidationException(val errors: Map<AuthField, String>) :
    IllegalArgumentException(errors.values.joinToString())

const val MIN_PASSWORD_LENGTH = 8
const val VERIFICATION_CODE_LENGTH = 6

private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

internal fun validateEmail(email: String): String? = when {
    email.isBlank() -> "Informe seu e-mail"
    !EMAIL_REGEX.matches(email) -> "E-mail inválido"
    else -> null
}

/**
 * Executa [block] só se não houver erros de validação. Exceções de cancelamento
 * de corrotina são relançadas, como recomenda a documentação de Coroutines.
 */
private suspend fun <T> validated(errors: Map<AuthField, String?>, block: suspend () -> T): Result<T> {
    val invalid = errors.filterValues { it != null }.mapValues { it.value!! }
    if (invalid.isNotEmpty()) return Result.failure(FormValidationException(invalid))
    return try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}

class ObserveCurrentUserUseCase(private val repository: AuthRepository) {
    operator fun invoke(): Flow<User?> = repository.currentUser
}

/** Confere as credenciais e devolve o e-mail normalizado, para onde o código foi enviado. */
class LoginUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<String> {
        val cleanEmail = email.trim().lowercase()
        return validated(
            mapOf(
                AuthField.EMAIL to validateEmail(cleanEmail),
                AuthField.PASSWORD to if (password.isEmpty()) "Informe sua senha" else null,
            ),
        ) {
            repository.login(cleanEmail, password)
            cleanEmail
        }
    }
}

class RegisterUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
    ): Result<String> {
        val cleanEmail = email.trim().lowercase()
        return validated(
            mapOf(
                AuthField.NAME to if (name.isBlank()) "Informe seu nome" else null,
                AuthField.EMAIL to validateEmail(cleanEmail),
                AuthField.PASSWORD to if (password.length < MIN_PASSWORD_LENGTH) {
                    "A senha precisa ter pelo menos $MIN_PASSWORD_LENGTH caracteres"
                } else null,
                AuthField.CONFIRM_PASSWORD to if (confirmPassword != password) "As senhas não conferem" else null,
            ),
        ) {
            repository.register(name.trim(), cleanEmail, password)
            cleanEmail
        }
    }
}

internal fun validateCode(code: String): String? =
    if (code.length != VERIFICATION_CODE_LENGTH || !code.all { it.isDigit() }) {
        "Digite os $VERIFICATION_CODE_LENGTH dígitos do código"
    } else null

class VerifyCodeUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, code: String): Result<User> =
        validated(mapOf(AuthField.CODE to validateCode(code))) { repository.verifyCode(email, code) }
}

class RequestAccountChangeCodeUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(): Result<Unit> = validated(emptyMap()) { repository.requestAccountChangeCode() }
}

/**
 * Altera e-mail e/ou senha. Campo vazio (ou e-mail igual ao atual) significa "não alterar",
 * como diz a tela: "Preencha só o que quiser alterar".
 */
class UpdateAccountUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(
        currentEmail: String,
        code: String,
        newEmail: String,
        newPassword: String,
    ): Result<User> {
        val cleanEmail = newEmail.trim().lowercase()
        val emailChange = cleanEmail.takeIf { it.isNotEmpty() && it != currentEmail }
        val passwordChange = newPassword.takeIf { it.isNotEmpty() }
        return validated(
            mapOf(
                AuthField.CODE to validateCode(code),
                AuthField.EMAIL to emailChange?.let(::validateEmail),
                AuthField.PASSWORD to passwordChange?.let {
                    if (it.length < MIN_PASSWORD_LENGTH) "A senha precisa ter pelo menos $MIN_PASSWORD_LENGTH caracteres" else null
                },
            ),
        ) {
            if (emailChange == null && passwordChange == null) throw AuthException.NothingToChange()
            repository.updateAccount(code, emailChange, passwordChange)
        }
    }
}

class ResendCodeUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String): Result<Unit> = validated(emptyMap()) {
        repository.resendCode(email)
    }
}

class LogoutUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(): Result<Unit> = validated(emptyMap()) { repository.logout() }
}
