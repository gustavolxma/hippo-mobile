package br.com.hippo.presentation.voice

import br.com.hippo.data.repository.InMemoryTaskRepository
import br.com.hippo.data.voice.LocalCommandInterpreter
import br.com.hippo.domain.metrics.VoiceInteraction
import br.com.hippo.domain.metrics.VoiceMetricsRepository
import br.com.hippo.domain.metrics.VoiceOutcome
import br.com.hippo.domain.model.TaskPriority
import br.com.hippo.domain.reminder.testReminderSync
import br.com.hippo.domain.usecase.InterpretVoiceCommandUseCase
import br.com.hippo.domain.usecase.SaveTaskUseCase
import br.com.hippo.domain.voice.SpeechError
import br.com.hippo.domain.voice.SpeechEvent
import br.com.hippo.domain.voice.SpeechRecognitionException
import br.com.hippo.domain.voice.VoiceRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlin.time.Instant

/** Reconhecedor de mentira: "ouve" frases pré-definidas, sem microfone. */
private class FakeVoiceRecognizer(
    private val events: List<SpeechEvent> = emptyList(),
    private val failWith: SpeechError? = null,
) : VoiceRecognizer {
    override fun isAvailable() = true
    override fun listen(languageTag: String): Flow<SpeechEvent> = flow {
        events.forEach { emit(it) }
        failWith?.let { throw SpeechRecognitionException(it) }
    }
    override fun stop() = Unit
}

/** Relógio fixo em 09/11/2026 12h UTC. */
private val fixedClock = object : Clock {
    override fun now() = Instant.parse("2026-11-09T12:00:00Z")
}

/** Métricas em memória: guarda os registros e os desfechos. */
private class FakeVoiceMetrics : VoiceMetricsRepository {
    val items = mutableListOf<VoiceInteraction>()
    override fun observe(): Flow<List<VoiceInteraction>> = flowOf(items)
    override suspend fun record(interaction: VoiceInteraction): Long {
        items += interaction.copy(id = items.size + 1L)
        return items.size.toLong()
    }
    override suspend fun updateOutcome(id: Long, outcome: VoiceOutcome) {
        val index = items.indexOfFirst { it.id == id }
        items[index] = items[index].copy(outcome = outcome)
    }
    override suspend fun clear() = items.clear()
}

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceViewModelTest {

    private val repository = InMemoryTaskRepository()
    private val metrics = FakeVoiceMetrics()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(recognizer: VoiceRecognizer) = VoiceViewModel(
        recognizer = recognizer,
        interpretCommand = InterpretVoiceCommandUseCase(LocalCommandInterpreter(), fixedClock),
        saveTask = SaveTaskUseCase(repository, testReminderSync()),
        metrics = metrics,
        clock = fixedClock,
    )

    private fun outcomes() = metrics.items.map { it.outcome }

    private val prototypePhrase = listOf(
        SpeechEvent.Listening,
        SpeechEvent.Partial("Me lembra de enviar"),
        SpeechEvent.Final("Me lembra de enviar o relatório amanhã às 15h, prioridade alta"),
    )

    @Test
    fun `fala entendida mostra o cartao e confirmar cria a tarefa`() = runTest {
        val vm = viewModel(FakeVoiceRecognizer(prototypePhrase))

        vm.onIntent(VoiceIntent.PermissionResult(granted = true))

        assertEquals(VoicePhase.UNDERSTOOD, vm.state.value.phase)
        assertEquals("Enviar o relatório", vm.state.value.task?.title)
        assertEquals(TaskPriority.HIGH, vm.state.value.task?.priority)

        vm.onIntent(VoiceIntent.Confirm)

        val saved = repository.observeTasks().first().single()
        assertEquals("Enviar o relatório", saved.title)
        assertEquals(VoicePhase.IDLE, vm.state.value.phase)
        assertEquals(listOf(VoiceOutcome.CONFIRMED), outcomes())
        assertEquals("local", metrics.items.single().interpreter)
    }

    @Test
    fun `editar leva aos detalhes com o rascunho`() = runTest {
        val vm = viewModel(FakeVoiceRecognizer(prototypePhrase))
        vm.onIntent(VoiceIntent.PermissionResult(granted = true))

        vm.onIntent(VoiceIntent.Edit)

        val effect = assertIs<VoiceEffect.NavigateToNewTask>(vm.effects.first())
        assertEquals("Enviar o relatório", effect.draft?.title)
        assertEquals(listOf(VoiceOutcome.EDITED), outcomes())
    }

    @Test
    fun `frase sem tarefa nao e entendida`() = runTest {
        val vm = viewModel(FakeVoiceRecognizer(listOf(SpeechEvent.Final("amanhã às 10h"))))

        vm.onIntent(VoiceIntent.PermissionResult(granted = true))

        assertEquals(VoicePhase.NOT_UNDERSTOOD, vm.state.value.phase)
        assertEquals(listOf(VoiceOutcome.NOT_UNDERSTOOD), outcomes())
        assertEquals("amanhã às 10h", metrics.items.single().transcript)
    }

    @Test
    fun `falar de novo descarta o comando anterior`() = runTest {
        val vm = viewModel(FakeVoiceRecognizer(prototypePhrase))
        vm.onIntent(VoiceIntent.PermissionResult(granted = true))

        vm.onIntent(VoiceIntent.PermissionResult(granted = true))

        assertEquals(listOf(VoiceOutcome.DISCARDED, VoiceOutcome.PENDING), outcomes())
    }

    @Test
    fun `erros de reconhecimento e permissao viram mensagem`() = runTest {
        val vm = viewModel(FakeVoiceRecognizer(failWith = SpeechError.NO_MATCH))

        vm.onIntent(VoiceIntent.PermissionResult(granted = true))
        assertEquals(VoicePhase.ERROR, vm.state.value.phase)
        assertEquals(listOf(VoiceOutcome.RECOGNITION_FAILED), outcomes())

        vm.onIntent(VoiceIntent.PermissionResult(granted = false))
        assertEquals(VoicePhase.ERROR, vm.state.value.phase)
        assertEquals("Permita o acesso ao microfone para usar comandos de voz.", vm.state.value.errorMessage)
    }
}
