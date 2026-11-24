package br.com.hippo.data.remote.dto

import br.com.hippo.domain.model.User
import kotlinx.serialization.Serializable

// Os nomes dos campos são os da API (em português).

@Serializable
data class RegisterDto(val nome: String, val email: String, val senha: String)

@Serializable
data class LoginDto(val email: String, val senha: String)

@Serializable
data class VerifyCodeDto(val email: String, val codigo: String)

/** [finalidade]: CADASTRO ou LOGIN. */
@Serializable
data class ResendCodeDto(val email: String, val finalidade: String)

@Serializable
data class ChangeAccountDto(val codigo: String, val novoEmail: String?, val novaSenha: String?)

/** O [token] já vem no formato do cabeçalho Authorization: "Bearer ...". */
@Serializable
data class TokenDto(val token: String, val usuario: UserDto)

@Serializable
data class UserDto(val id: Long, val nome: String, val email: String) {
    fun toDomain() = User(id = id.toString(), name = nome, email = email)
}

/** Corpo das respostas de erro da API. */
@Serializable
data class MessageDto(val mensagem: String? = null)
