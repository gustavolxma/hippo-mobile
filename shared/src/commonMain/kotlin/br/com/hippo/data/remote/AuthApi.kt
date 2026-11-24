package br.com.hippo.data.remote

import br.com.hippo.data.remote.dto.ChangeAccountDto
import br.com.hippo.data.remote.dto.LoginDto
import br.com.hippo.data.remote.dto.RegisterDto
import br.com.hippo.data.remote.dto.ResendCodeDto
import br.com.hippo.data.remote.dto.TokenDto
import br.com.hippo.data.remote.dto.VerifyCodeDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody

/** Endpoints de conta da API (UsuarioController). */
class AuthApi(private val client: HttpClient) {

    suspend fun register(body: RegisterDto) {
        apiCall { client.post("usuarios") { setBody(body) } }
    }

    suspend fun verifyRegister(body: VerifyCodeDto): TokenDto =
        apiCall { client.post("usuarios/verificar-cadastro") { setBody(body) }.body() }

    suspend fun login(body: LoginDto) {
        apiCall { client.post("usuarios/login") { setBody(body) } }
    }

    suspend fun verifyLogin(body: VerifyCodeDto): TokenDto =
        apiCall { client.post("usuarios/login/verificar") { setBody(body) }.body() }

    suspend fun resendCode(body: ResendCodeDto) {
        apiCall { client.post("usuarios/codigo/reenviar") { setBody(body) } }
    }

    suspend fun requestAccountChangeCode() {
        apiCall { client.post("usuarios/conta/codigo") }
    }

    suspend fun changeAccount(body: ChangeAccountDto): TokenDto =
        apiCall { client.put("usuarios") { setBody(body) }.body() }
}
