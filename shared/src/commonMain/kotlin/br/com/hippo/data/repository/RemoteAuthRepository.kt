package br.com.hippo.data.repository

import br.com.hippo.data.local.SessionStorage
import br.com.hippo.data.remote.ApiException
import br.com.hippo.data.remote.AuthApi
import br.com.hippo.data.remote.dto.ChangeAccountDto
import br.com.hippo.data.remote.dto.LoginDto
import br.com.hippo.data.remote.dto.RegisterDto
import br.com.hippo.data.remote.dto.ResendCodeDto
import br.com.hippo.data.remote.dto.TokenDto
import br.com.hippo.data.remote.dto.VerifyCodeDto
import br.com.hippo.domain.model.User
import br.com.hippo.domain.repository.AuthException
import br.com.hippo.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

/** Autenticação pela API. Os erros dela viram [AuthException]; falhas de rede passam direto. */
class RemoteAuthRepository(
    private val api: AuthApi,
    private val sessionStorage: SessionStorage,
) : AuthRepository {

    /** Para que serve o código enviado: a API tem um endpoint de confirmação para cada caso. */
    private enum class CodePurpose(val apiName: String) { LOGIN("LOGIN"), REGISTER("CADASTRO") }

    private val pending = mutableMapOf<String, CodePurpose>()

    override val currentUser: Flow<User?> = sessionStorage.user

    override suspend fun login(email: String, password: String) = authCall {
        try {
            api.login(LoginDto(email, password))
        } catch (e: ApiException) {
            throw if (e.status == UNAUTHORIZED) AuthException.InvalidCredentials() else e
        }
        pending[email] = CodePurpose.LOGIN
    }

    override suspend fun register(name: String, email: String, password: String) = authCall {
        api.register(RegisterDto(name, email, password))
        pending[email] = CodePurpose.REGISTER
    }

    override suspend fun verifyCode(email: String, code: String): User = authCall {
        val body = VerifyCodeDto(email, code)
        val token = forPendingPurpose(email) { purpose ->
            when (purpose) {
                CodePurpose.LOGIN -> api.verifyLogin(body)
                CodePurpose.REGISTER -> api.verifyRegister(body)
            }
        }
        pending.remove(email)
        startSession(token)
    }

    override suspend fun resendCode(email: String) = authCall {
        forPendingPurpose(email) { purpose -> api.resendCode(ResendCodeDto(email, purpose.apiName)) }
    }

    override suspend fun requestAccountChangeCode() = authCall { api.requestAccountChangeCode() }

    override suspend fun updateAccount(code: String, newEmail: String?, newPassword: String?): User = authCall {
        // O token guarda o e-mail: ao trocá-lo, o antigo deixa de valer e a API devolve um novo.
        startSession(api.changeAccount(ChangeAccountDto(code, newEmail, newPassword)))
    }

    override suspend fun logout() = sessionStorage.clear()

    private suspend fun startSession(token: TokenDto): User {
        val user = token.usuario.toDomain()
        sessionStorage.saveSession(token.token, user)
        return user
    }

    /**
     * Se o sistema fechou o app enquanto o usuário lia o e-mail, não se sabe mais se o código
     * era de login ou de cadastro: tenta um e depois o outro.
     */
    private suspend fun <T> forPendingPurpose(email: String, block: suspend (CodePurpose) -> T): T {
        pending[email]?.let { return block(it) }
        return try {
            block(CodePurpose.LOGIN)
        } catch (e: ApiException) {
            if (e.status != BAD_REQUEST) throw e
            block(CodePurpose.REGISTER)
        }
    }

    private suspend fun <T> authCall(block: suspend () -> T): T = try {
        block()
    } catch (e: ApiException) {
        throw when {
            e.status == UNAUTHORIZED -> AuthException.NotSignedIn()
            e.status == CONFLICT -> AuthException.EmailAlreadyInUse()
            e.apiMessage != null -> AuthException.Rejected(e.apiMessage)
            else -> e
        }
    }

    private companion object {
        const val BAD_REQUEST = 400
        const val UNAUTHORIZED = 401
        const val CONFLICT = 409
    }
}
