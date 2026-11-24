package br.com.hippo.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import br.com.hippo.core.network.createHttpClient
import br.com.hippo.data.local.SessionStorage
import br.com.hippo.data.local.db.HippoDatabase
import br.com.hippo.data.remote.AiApi
import br.com.hippo.data.remote.AuthApi
import br.com.hippo.data.remote.TaskApi
import br.com.hippo.data.voice.AiCommandInterpreter
import br.com.hippo.domain.model.Task
import br.com.hippo.domain.model.TaskPriority
import br.com.hippo.domain.model.TaskReminder
import br.com.hippo.domain.model.User
import br.com.hippo.domain.repository.AuthException
import br.com.hippo.domain.voice.VoiceCommand
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** DataStore em memória: o de arquivo não consegue sobrescrever o arquivo no Windows. */
private class InMemoryDataStore : DataStore<Preferences> {
    override val data = MutableStateFlow(emptyPreferences())
    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        transform(data.value).also { data.value = it }
}

/** Repositórios da API com respostas HTTP simuladas, conferindo também o que o app envia. */
class RemoteRepositoriesTest {

    private val session = SessionStorage(InMemoryDataStore())
    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { HippoDatabase.Schema.create(it) }
    private val local = LocalTaskRepository(HippoDatabase(driver), session.user, Dispatchers.Unconfined)

    /** Respostas por "MÉTODO caminho"; o que não estiver aqui responde 404. */
    private val responses = mutableMapOf<String, Pair<HttpStatusCode, String>>()
    private val requests = mutableListOf<String>()
    private val tokens = mutableListOf<String?>()

    private val client = createHttpClient(
        engine = MockEngine { request ->
            val route = "${request.method.value} ${request.url.encodedPath}" +
                request.url.encodedQuery.let { if (it.isEmpty()) "" else "?$it" }
            requests += (route + " " + request.bodyText()).trim()
            tokens += request.headers[HttpHeaders.Authorization]
            val (status, body) = responses[route] ?: (HttpStatusCode.NotFound to """{"mensagem":"não existe"}""")
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        },
        baseUrl = "https://api.teste/",
        sessionStorage = session,
    )
    private val auth = RemoteAuthRepository(AuthApi(client), session)
    private val tasks = RemoteTaskRepository(TaskApi(client), local)

    private val maria = User("7", "Maria", "maria@email.com")
    private val tokenJson =
        """{"token":"Bearer abc","usuario":{"id":7,"nome":"Maria","email":"maria@email.com","dataCadastro":"01-11-2026 10:00:00"}}"""

    private val meeting = Task(
        id = "qualquer",
        title = "Reunião",
        priority = TaskPriority.HIGH,
        dueDate = LocalDate(2026, 12, 10),
        dueTime = LocalTime(15, 0),
        reminder = TaskReminder.MINUTES_30,
    )

    private fun HttpRequestData.bodyText(): String = when (val content: OutgoingContent = body) {
        is TextContent -> content.text
        else -> ""
    }

    private fun answer(route: String, body: String = "", status: HttpStatusCode = HttpStatusCode.OK) {
        responses[route] = status to body
    }

    @AfterTest
    fun tearDown() {
        client.close()
        driver.close()
    }

    // ---------- Conta ----------

    @Test
    fun `login abre a sessao so depois do codigo e guarda o token`() = runTest {
        answer("POST /usuarios/login", """{"mensagem":"ok"}""", HttpStatusCode.Accepted)
        answer("POST /usuarios/login/verificar", tokenJson)

        auth.login("maria@email.com", "senha1234")
        assertNull(auth.currentUser.first())

        assertEquals(maria, auth.verifyCode("maria@email.com", "123456"))
        assertEquals(maria, auth.currentUser.first())
        assertEquals("Bearer abc", session.currentAccessToken())
        assertEquals("""POST /usuarios/login {"email":"maria@email.com","senha":"senha1234"}""", requests.first())
    }

    @Test
    fun `cadastro confirma e reenvia o codigo pelos enderecos do cadastro`() = runTest {
        answer("POST /usuarios", """{"nome":"Maria"}""", HttpStatusCode.Created)
        answer("POST /usuarios/codigo/reenviar", """{"mensagem":"ok"}""", HttpStatusCode.Accepted)
        answer("POST /usuarios/verificar-cadastro", tokenJson)

        auth.register("Maria", "maria@email.com", "senha1234")
        auth.resendCode("maria@email.com")
        auth.verifyCode("maria@email.com", "123456")

        assertEquals("""POST /usuarios/codigo/reenviar {"email":"maria@email.com","finalidade":"CADASTRO"}""", requests[1])
        assertTrue(requests[2].startsWith("POST /usuarios/verificar-cadastro"))
    }

    @Test
    fun `sem saber a finalidade tenta o codigo de login e depois o de cadastro`() = runTest {
        answer("POST /usuarios/login/verificar", """{"mensagem":"Código inválido ou expirado!"}""", HttpStatusCode.BadRequest)
        answer("POST /usuarios/verificar-cadastro", tokenJson)

        assertEquals(maria, auth.verifyCode("maria@email.com", "123456"))
    }

    @Test
    fun `erros da api viram mensagens para o usuario`() = runTest {
        answer("POST /usuarios/login", """{"mensagem":"E-mail e/ou Senha Inválidos"}""", HttpStatusCode.Unauthorized)
        assertFailsWith<AuthException.InvalidCredentials> { auth.login("maria@email.com", "errada") }

        answer("POST /usuarios", """{"mensagem":"Email já existe"}""", HttpStatusCode.Conflict)
        assertFailsWith<AuthException.EmailAlreadyInUse> { auth.register("Maria", "maria@email.com", "senha1234") }

        answer("POST /usuarios/login", """{"mensagem":"Aguarde alguns segundos"}""", HttpStatusCode.TooManyRequests)
        val error = assertFailsWith<AuthException.Rejected> { auth.login("maria@email.com", "senha1234") }
        assertEquals("Aguarde alguns segundos", error.message)
    }

    @Test
    fun `alterar a conta troca o token e so envia o que mudou`() = runTest {
        session.saveSession("Bearer antigo", maria)
        answer("PUT /usuarios", tokenJson.replace("maria@email.com", "nova@email.com"))

        val updated = auth.updateAccount("123456", newEmail = "nova@email.com", newPassword = null)

        assertEquals("nova@email.com", updated.email)
        assertEquals("Bearer abc", session.currentAccessToken())
        assertEquals("""PUT /usuarios {"codigo":"123456","novoEmail":"nova@email.com"}""", requests.single())
        assertEquals("Bearer antigo", tokens.single())
    }

    @Test
    fun `token recusado encerra a sessao`() = runTest {
        session.saveSession("Bearer vencido", maria)
        answer("GET /tarefas", "", HttpStatusCode.Unauthorized)

        assertFailsWith<Exception> { tasks.refresh() }

        assertNull(session.user.first())
    }

    // ---------- Tarefas ----------

    @Test
    fun `atualizar troca a lista local pela da api`() = runTest {
        session.saveSession("Bearer abc", maria)
        local.addTask(Task(id = "99", title = "Excluída em outro aparelho"))
        answer(
            "GET /tarefas",
            """[{"id":1,"nomeTarefa":"Reunião","descricao":"","statusTarefa":"CONCLUIDA","dataCriacao":"01-11-2026 10:00:00",
                "dataEvento":"10-12-2026 15:00:00","temHora":true,"prioridade":"ALTA","lembreteMinutosAntes":30,"idUsuario":7},
                {"id":2,"nomeTarefa":"Só o dia","descricao":"x","statusTarefa":"PENDENTE","dataEvento":"10-12-2026 00:00:00",
                "temHora":false,"prioridade":"BAIXA","lembreteMinutosAntes":null},
                {"id":3,"nomeTarefa":"Sem data","descricao":"","statusTarefa":"PENDENTE","dataEvento":null}]""",
        )

        tasks.refresh()

        assertEquals(
            listOf(
                meeting.copy(id = "1", isDone = true),
                Task(id = "2", title = "Só o dia", description = "x", priority = TaskPriority.LOW, dueDate = LocalDate(2026, 12, 10)),
                Task(id = "3", title = "Sem data"),
            ),
            tasks.observeTasks().first().sortedBy { it.id },
        )
        assertEquals("Bearer abc", tokens.single())
    }

    @Test
    fun `criar usa o id da api`() = runTest {
        session.saveSession("Bearer abc", maria)
        answer("POST /tarefas", """{"id":42,"nomeTarefa":"Reunião"}""", HttpStatusCode.Created)

        val saved = tasks.addTask(meeting)

        assertEquals("42", saved.id)
        assertEquals(listOf(meeting.copy(id = "42")), tasks.observeTasks().first())
        assertEquals(
            """POST /tarefas {"nomeTarefa":"Reunião","descricao":"","dataEvento":"10-12-2026 15:00:00",""" +
                """"temHora":true,"prioridade":"ALTA","lembreteMinutosAntes":30}""",
            requests.single(),
        )
    }

    @Test
    fun `concluir muda so o status e editar muda so os dados`() = runTest {
        session.saveSession("Bearer abc", maria)
        val task = local.addTask(meeting.copy(id = "42"))
        answer("PATCH /tarefas/42/status?status=CONCLUIDA", "{}")
        answer("PUT /tarefas?id=42", "{}")

        tasks.updateTask(task.copy(isDone = true))
        tasks.updateTask(task.copy(isDone = true, dueDate = null, dueTime = null, reminder = TaskReminder.NONE))

        assertEquals("PATCH /tarefas/42/status?status=CONCLUIDA", requests[0])
        // Campos vazios não são enviados, e a API os apaga.
        assertEquals(
            """PUT /tarefas?id=42 {"nomeTarefa":"Reunião","descricao":"","temHora":false,"prioridade":"ALTA"}""",
            requests[1],
        )
        assertEquals(2, requests.size)
    }

    @Test
    fun `falha da api nao altera o aparelho e tarefa ja excluida e removida`() = runTest {
        session.saveSession("Bearer abc", maria)
        val task = local.addTask(meeting.copy(id = "42"))
        answer("PATCH /tarefas/42/status?status=CONCLUIDA", "", HttpStatusCode.InternalServerError)

        assertFailsWith<Exception> { tasks.updateTask(task.copy(isDone = true)) }
        assertEquals(listOf(task), tasks.observeTasks().first())

        tasks.deleteTask("42") // A API responde 404
        assertTrue(tasks.observeTasks().first().isEmpty())
    }

    // ---------- Voz ----------

    @Test
    fun `ia devolve a tarefa interpretada`() = runTest {
        session.saveSession("Bearer abc", maria)
        answer(
            "POST /ia/interpretar",
            """{"entendeu":true,"nomeTarefa":"Reunião","descricao":null,"data":"10-12-2026","hora":"15:00",
                "prioridade":"ALTA","lembreteMinutosAntes":30}""",
        )
        val now = LocalDateTime(2026, 12, 9, 8, 5)

        val command = AiCommandInterpreter(AiApi(client)).interpret("reunião amanhã às 15h", now)

        assertEquals(
            listOf(LocalDate(2026, 12, 10), LocalTime(15, 0), TaskPriority.HIGH, TaskReminder.MINUTES_30),
            (command as VoiceCommand.CreateTask).input.run { listOf(dueDate, dueTime, priority, reminder) },
        )
        assertEquals(
            """POST /ia/interpretar {"texto":"reunião amanhã às 15h","agora":"09-12-2026 08:05:00"}""",
            requests.single(),
        )

        answer("POST /ia/interpretar", """{"entendeu":false}""")
        assertEquals(VoiceCommand.NotUnderstood, AiCommandInterpreter(AiApi(client)).interpret("oi", now))
    }
}
