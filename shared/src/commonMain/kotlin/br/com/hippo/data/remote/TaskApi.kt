package br.com.hippo.data.remote

import br.com.hippo.data.remote.dto.TaskDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody

/** Endpoints REST de tarefas (provisórios até o backend definir o contrato). */
class TaskApi(private val client: HttpClient) {

    suspend fun getTasks(): List<TaskDto> = client.get("tasks").body()

    suspend fun createTask(task: TaskDto): TaskDto =
        client.post("tasks") { setBody(task) }.body()

    suspend fun updateTask(task: TaskDto): TaskDto =
        client.put("tasks/${task.id}") { setBody(task) }.body()

    suspend fun deleteTask(id: String) {
        client.delete("tasks/$id")
    }
}
