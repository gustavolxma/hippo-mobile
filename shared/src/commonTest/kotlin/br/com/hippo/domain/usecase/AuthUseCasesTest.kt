package br.com.hippo.domain.usecase

import br.com.hippo.domain.model.User
import br.com.hippo.domain.repository.AuthException
import br.com.hippo.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Repositório de mentira que só registra as chamadas, para testar os casos de uso. */
private class RecordingAuthRepository : AuthRepository {
    val calls = mutableListOf<String>()
    var failWith: Exception? = null
    private val user = User("1", "Maria", "maria@email.com")

    override val currentUser: Flow<User?> = MutableStateFlow(null)

    override suspend fun login(email: String, password: String) {
        calls += "login:$email"
        failWith?.let { throw it }
    }

    override suspend fun register(name: String, email: String, password: String) {
        calls += "register:$name:$email"
        failWith?.let { throw it }
    }

    override suspend fun verifyCode(email: String, code: String): User {
        calls += "verify:$email:$code"
        failWith?.let { throw it }
        return user
    }

    override suspend fun resendCode(email: String) {
        calls += "resend:$email"
    }

    override suspend fun requestAccountChangeCode() {
        calls += "requestChangeCode"
    }

    override suspend fun updateAccount(code: String, newEmail: String?, newPassword: String?): User {
        calls += "update:$code:$newEmail:$newPassword"
        return user
    }

    override suspend fun logout() {
        calls += "logout"
    }
}

class AuthUseCasesTest {

    private val repository = RecordingAuthRepository()

    private fun Result<*>.fieldErrors(): Map<AuthField, String> =
        assertIs<FormValidationException>(exceptionOrNull()).errors

    @Test
    fun `login normaliza o email e devolve para onde o codigo foi enviado`() = runTest {
        val result = LoginUseCase(repository)("  Maria@Email.com ", "senha1234")

        assertEquals("maria@email.com", result.getOrThrow())
        assertEquals(listOf("login:maria@email.com"), repository.calls)
    }

    @Test
    fun `login valida email e senha sem chamar o repositorio`() = runTest {
        val errors = LoginUseCase(repository)("maria", "").fieldErrors()

        assertEquals(setOf(AuthField.EMAIL, AuthField.PASSWORD), errors.keys)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `login repassa erro de credenciais`() = runTest {
        repository.failWith = AuthException.InvalidCredentials()

        val result = LoginUseCase(repository)("maria@email.com", "errada")

        assertIs<AuthException.InvalidCredentials>(result.exceptionOrNull())
    }

    @Test
    fun `cadastro exige nome senha minima e confirmacao igual`() = runTest {
        val errors = RegisterUseCase(repository)(" ", "maria@email.com", "curta", "outra").fieldErrors()

        assertEquals(setOf(AuthField.NAME, AuthField.PASSWORD, AuthField.CONFIRM_PASSWORD), errors.keys)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `cadastro valido devolve o email normalizado`() = runTest {
        val result = RegisterUseCase(repository)(" Maria ", "MARIA@email.com", "senha1234", "senha1234")

        assertEquals("maria@email.com", result.getOrThrow())
        assertEquals(listOf("register:Maria:maria@email.com"), repository.calls)
    }

    @Test
    fun `codigo precisa ter 6 digitos numericos`() = runTest {
        val useCase = VerifyCodeUseCase(repository)

        assertEquals(setOf(AuthField.CODE), useCase("maria@email.com", "123").fieldErrors().keys)
        assertEquals(setOf(AuthField.CODE), useCase("maria@email.com", "12345a").fieldErrors().keys)
        assertTrue(useCase("maria@email.com", "123456").isSuccess)
    }

    @Test
    fun `alterar conta envia so o que mudou`() = runTest {
        val useCase = UpdateAccountUseCase(repository)

        useCase("maria@email.com", "123456", newEmail = "maria@email.com", newPassword = "novasenha1")
        useCase("maria@email.com", "123456", newEmail = " Nova@Email.com ", newPassword = "")

        assertEquals(
            listOf("update:123456:null:novasenha1", "update:123456:nova@email.com:null"),
            repository.calls,
        )
    }

    @Test
    fun `alterar conta sem nenhuma mudanca e recusado`() = runTest {
        val result = UpdateAccountUseCase(repository)("maria@email.com", "123456", "maria@email.com", "")

        assertIs<AuthException.NothingToChange>(result.exceptionOrNull())
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun `alterar conta valida codigo email e senha`() = runTest {
        val errors = UpdateAccountUseCase(repository)("maria@email.com", "12", "invalido", "curta").fieldErrors()

        assertEquals(setOf(AuthField.CODE, AuthField.EMAIL, AuthField.PASSWORD), errors.keys)
    }

    @Test
    fun `validacao de email`() {
        assertNull(validateEmail("maria@email.com"))
        assertEquals("Informe seu e-mail", validateEmail(""))
        assertEquals("E-mail inválido", validateEmail("maria@email"))
    }
}
