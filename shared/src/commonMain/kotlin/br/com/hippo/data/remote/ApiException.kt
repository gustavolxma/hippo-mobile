package br.com.hippo.data.remote

import br.com.hippo.data.remote.dto.MessageDto
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.CancellationException

/** Erro respondido pela API: código HTTP e a mensagem pronta para o usuário (quando houver). */
class ApiException(val status: Int, val apiMessage: String?) :
    Exception("A API respondeu $status: $apiMessage")

/** Executa uma chamada e converte as respostas de erro em [ApiException]. Falhas de rede passam direto. */
internal suspend fun <T> apiCall(block: suspend () -> T): T = try {
    block()
} catch (e: ResponseException) {
    val message = try {
        e.response.body<MessageDto>().mensagem
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null // Resposta sem corpo ou que não veio da nossa API (ex.: página do provedor).
    }
    throw ApiException(e.response.status.value, message)
}
