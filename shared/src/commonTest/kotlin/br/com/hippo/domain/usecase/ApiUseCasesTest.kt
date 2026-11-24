package br.com.hippo.domain.usecase

import br.com.hippo.data.repository.InMemoryTaskRepository
import br.com.hippo.data.voice.LocalCommandInterpreter
import br.com.hippo.domain.model.Task
import br.com.hippo.domain.reminder.testReminderSync
import br.com.hippo.domain.voice.CommandInterpreter
import br.com.hippo.domain.voice.VoiceCommand
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock

/** Comportamento dos casos de uso quando a API responde, falha ou demora. */
class ApiUseCasesTest {

    private class FakeAi(private val answer: suspend () -> VoiceCommand) : CommandInterpreter {
        override val name = "ia"
        override suspend fun interpret(text: String, now: LocalDateTime) = answer()
    }

    private fun interpret(answer: suspend () -> VoiceCommand) =
        InterpretVoiceCommandUseCase(FakeAi(answer), Clock.System, fallback = LocalCommandInterpreter())

    @Test
    fun `usa a resposta da ia`() = runTest {
        val result = interpret { VoiceCommand.CreateTask(TaskInput(title = "Da IA")) }("comprar pão")

        assertEquals("ia", result.interpreter)
        assertEquals("Da IA", assertIs<VoiceCommand.CreateTask>(result.command).input.title)
    }

    @Test
    fun `se a ia nao entendeu nao tenta as regras locais`() = runTest {
        val result = interpret { VoiceCommand.NotUnderstood }("comprar pão")

        assertEquals(Interpretation(VoiceCommand.NotUnderstood, "ia"), result)
    }

    @Test
    fun `se a ia falha ou demora usa as regras locais`() = runTest {
        for (answer in listOf<suspend () -> VoiceCommand>({ error("sem internet") }, { awaitCancellation() })) {
            val result = interpret(answer)("comprar pão")

            assertEquals("local", result.interpreter)
            assertEquals("Comprar pão", assertIs<VoiceCommand.CreateTask>(result.command).input.title)
        }
    }

    @Test
    fun `atualizar traz as tarefas da api e falha sem perder as salvas`() = runTest {
        val repository = InMemoryTaskRepository(listOf(Task(id = "1", title = "Salva")))
        val refresh = RefreshTasksUseCase(repository, testReminderSync())

        repository.failRefreshWith = IllegalStateException("sem internet")
        assertTrue(refresh().isFailure)
        assertEquals(listOf("1"), repository.observeTasks().first().map { it.id })

        repository.failRefreshWith = null
        repository.remoteTasks = listOf(Task(id = "2", title = "Nova"))
        assertTrue(refresh().isSuccess)
        assertEquals(listOf("2"), repository.observeTasks().first().map { it.id })
    }
}
