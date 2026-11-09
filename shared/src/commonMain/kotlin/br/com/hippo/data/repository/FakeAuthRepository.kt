package br.com.hippo.data.repository

import br.com.hippo.data.local.SessionStorage
import br.com.hippo.domain.model.User
import br.com.hippo.domain.repository.AuthException
import br.com.hippo.domain.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.uuid.Uuid

/**
 * Autenticação falsa, usada enquanto a API não está no ar. As contas ficam em memória
 * (somem ao fechar o app), mas a sessão é salva de verdade no DataStore.
 *
 * - Conta de demonstração: [DEMO_EMAIL] / [DEMO_PASSWORD] (os dados do protótipo).
 * - O código de verificação (login e cadastro) é sempre [FAKE_CODE]: não há envio de e-mail.
 */
class FakeAuthRepository(
    private val sessionStorage: SessionStorage,
    private val networkDelayMillis: Long = 800,
) : AuthRepository {

    private data class Account(val user: User, val password: String)

    private val mutex = Mutex()
    private val accounts = mutableMapOf(
        DEMO_EMAIL to Account(User(id = "demo", name = "Maria Silva", email = DEMO_EMAIL), DEMO_PASSWORD),
    )
    /**
     * Logins e cadastros aguardando o código de verificação, por e-mail. Ao confirmar,
     * a conta passa a existir (cadastro) ou continua igual (login) e a sessão é aberta.
     */
    private val pending = mutableMapOf<String, Account>()

    override val currentUser: Flow<User?> = sessionStorage.user

    override suspend fun login(email: String, password: String) {
        delay(networkDelayMillis)
        mutex.withLock {
            val account = accounts[email]
            if (account == null || account.password != password) throw AuthException.InvalidCredentials()
            pending[email] = account
        }
    }

    override suspend fun register(name: String, email: String, password: String) {
        delay(networkDelayMillis)
        mutex.withLock {
            if (email in accounts) throw AuthException.EmailAlreadyInUse()
            pending[email] = Account(User(id = Uuid.random().toString(), name = name, email = email), password)
        }
    }

    override suspend fun verifyCode(email: String, code: String): User {
        delay(networkDelayMillis)
        val account = mutex.withLock {
            val account = pending[email]
            if (account == null || code != FAKE_CODE) throw AuthException.InvalidCode()
            pending.remove(email)
            accounts[email] = account
            account
        }
        startSession(account.user)
        return account.user
    }

    override suspend fun resendCode(email: String) {
        delay(networkDelayMillis)
    }

    override suspend fun logout() {
        sessionStorage.clear()
    }

    private suspend fun startSession(user: User) {
        sessionStorage.saveSession(token = "fake-token-${user.id}", user = user)
    }

    companion object {
        const val DEMO_EMAIL = "maria@email.com"
        const val DEMO_PASSWORD = "senha1234"
        const val FAKE_CODE = "123456"
    }
}
