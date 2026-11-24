package br.com.hippo.core.network

import br.com.hippo.data.local.SessionStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

/**
 * Cria o HttpClient do Ktor. O [engine] vem de cada plataforma:
 * OkHttp no Android e Darwin (NSURLSession) no iOS.
 */
fun createHttpClient(
    engine: HttpClientEngine,
    baseUrl: String,
    sessionStorage: SessionStorage,
): HttpClient = HttpClient(engine) {
    expectSuccess = true

    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        })
    }
    install(HttpTimeout) {
        connectTimeoutMillis = 30.seconds.inWholeMilliseconds
        // No plano gratuito o servidor dorme sem uso e pode levar cerca de 1 minuto para acordar.
        requestTimeoutMillis = 90.seconds.inWholeMilliseconds
        socketTimeoutMillis = 90.seconds.inWholeMilliseconds
    }
    install(Logging) {
        level = LogLevel.INFO
        sanitizeHeader { it == HttpHeaders.Authorization }
    }
    install(sessionPlugin(sessionStorage))
    defaultRequest {
        url(baseUrl)
        contentType(ContentType.Application.Json)
    }
}

/**
 * Envia o token da sessão em toda requisição (a API já o devolve como "Bearer ..."). Se o
 * servidor responder 401 a um token, a sessão expirou: ela é apagada e o app volta ao Login.
 *
 * Não usamos o plugin Auth do Ktor porque ele guarda o token em memória, e aqui o token
 * muda ao trocar de conta ou de e-mail.
 */
private fun sessionPlugin(sessionStorage: SessionStorage) = createClientPlugin("HippoSession") {
    onRequest { request, _ ->
        sessionStorage.currentAccessToken()?.let { request.headers[HttpHeaders.Authorization] = it }
    }
    onResponse { response ->
        if (response.status != HttpStatusCode.Unauthorized) return@onResponse
        val sentToken = response.request.headers[HttpHeaders.Authorization] ?: return@onResponse
        // Só se ainda for a mesma sessão: o usuário pode ter entrado de novo enquanto isso.
        if (sentToken == sessionStorage.currentAccessToken()) sessionStorage.clear()
    }
}
