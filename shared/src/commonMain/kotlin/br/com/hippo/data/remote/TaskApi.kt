package br.com.hippo.data.remote

import br.com.hippo.data.remote.dto.TaskDto
import br.com.hippo.data.remote.dto.TaskInputDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody

/** Endpoints de tarefas da API (TarefaController). */
class TaskApi(private val client: HttpClient) {

    suspend fun getTasks(): List<TaskDto> = apiCall { client.get("tarefas").body() }

    suspend fun createTask(task: TaskInputDto): TaskDto =
        apiCall { client.post("tarefas") { setBody(task) }.body() }

    /** Substitui todos os dados da tarefa, menos o status. */
    suspend fun updateTask(id: String, task: TaskInputDto) {
        apiCall {
            client.put("tarefas") {
                parameter("id", id)
                setBody(task)
            }
        }
    }

    suspend fun updateStatus(id: String, status: String) {
        apiCall { client.patch("tarefas/$id/status") { parameter("status", status) } }
    }

    suspend fun deleteTask(id: String) {
        apiCall { client.delete("tarefas") { parameter("id", id) } }
    }
}
