package br.com.hippo.data.remote

import br.com.hippo.data.remote.dto.InterpretCommandDto
import br.com.hippo.data.remote.dto.InterpretedCommandDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

/** Interpretação de comandos com IA (IaController). */
class AiApi(private val client: HttpClient) {

    suspend fun interpret(body: InterpretCommandDto): InterpretedCommandDto =
        apiCall { client.post("ia/interpretar") { setBody(body) }.body() }
}
