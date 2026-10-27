package br.com.hippo.core.network

import br.com.hippo.data.local.SessionStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

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
    install(Logging) {
        level = LogLevel.INFO
    }
    install(Auth) {
        bearer {
            loadTokens {
                sessionStorage.currentAccessToken()?.let { BearerTokens(it, null) }
            }
        }
    }
    defaultRequest {
        url(baseUrl)
        contentType(ContentType.Application.Json)
    }
}
