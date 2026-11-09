package br.com.hippo.domain.repository

import br.com.hippo.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de autenticação. Hoje existe só a implementação falsa (sem API);
 * quando o backend tiver login, basta criar outra implementação e trocar no Koin.
 */
interface AuthRepository {
    /** Usuário logado, ou `null` quando não há sessão. */
    val currentUser: Flow<User?>

    /** Confere e-mail e senha e envia o código de verificação; a sessão só abre em [verifyCode]. */
    suspend fun login(email: String, password: String)

    /** Inicia o cadastro e envia o código de verificação para o [email]. */
    suspend fun register(name: String, email: String, password: String)

    /** Confirma o código enviado no login ou no cadastro e abre a sessão. */
    suspend fun verifyCode(email: String, code: String): User

    suspend fun resendCode(email: String)

    suspend fun logout()
}

/** Erros de negócio da autenticação, que a tela transforma em mensagem para o usuário. */
sealed class AuthException(message: String) : Exception(message) {
    class InvalidCredentials : AuthException("E-mail ou senha incorretos")
    class EmailAlreadyInUse : AuthException("Já existe uma conta com este e-mail")
    class InvalidCode : AuthException("Código inválido ou expirado")
}
