package br.com.hippo.domain.reminder

import br.com.hippo.data.repository.InMemoryTaskRepository
import br.com.hippo.domain.model.Task
import br.com.hippo.domain.model.TaskReminder
import br.com.hippo.domain.usecase.DeleteTaskUseCase
import br.com.hippo.domain.usecase.SaveTaskUseCase
import br.com.hippo.domain.usecase.TaskInput
import br.com.hippo.domain.usecase.ToggleTaskDoneUseCase
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant

/** Agendador de mentira: só registra o que foi pedido. */
class FakeReminderScheduler : ReminderScheduler {
    val scheduled = mutableMapOf<String, ScheduledReminder>()
    val calls = mutableListOf<String>()

    override suspend fun schedule(reminder: ScheduledReminder) {
        scheduled[reminder.taskId] = reminder
        calls += "schedule:${reminder.taskId}"
    }

    override suspend fun cancel(taskId: String) {
        scheduled.remove(taskId)
        calls += "cancel:$taskId"
    }
}

/** Sincronizador com relógio fixo em 09/11/2026 9h (UTC), para testes. */
fun testReminderSync(scheduler: ReminderScheduler = FakeReminderScheduler()) = TaskReminderSync(
    scheduler = scheduler,
    clock = object : Clock {
        override fun now() = Instant.parse("2026-11-09T09:00:00Z")
    },
    timeZone = { TimeZone.UTC },
)

class RemindersTest {

    private val now = LocalDateTime(2026, 11, 9, 9, 0)
    private val today = LocalDate(2026, 11, 9)

    private fun task(
        time: LocalTime? = LocalTime(15, 0),
        reminder: TaskReminder = TaskReminder.MINUTES_30,
        isDone: Boolean = false,
    ) = Task(id = "1", title = "Enviar relatório", dueDate = today, dueTime = time, reminder = reminder, isDone = isDone)

    @Test
    fun `calcula o momento e o texto do lembrete`() {
        val reminder = reminderFor(task(), now, TimeZone.UTC)!!

        assertEquals(LocalDateTime(2026, 11, 9, 14, 30), reminder.triggerAt)
        assertEquals("Em 30 minutos, às 15:00", reminder.message)
        assertEquals("Enviar relatório", reminder.title)
    }

    @Test
    fun `lembrete de um dia antes volta para o dia anterior`() {
        val reminder = reminderFor(
            task().copy(dueDate = LocalDate(2026, 11, 12), reminder = TaskReminder.DAY_1),
            now,
            TimeZone.UTC,
        )!!

        assertEquals(LocalDateTime(2026, 11, 11, 15, 0), reminder.triggerAt)
    }

    @Test
    fun `nao agenda sem lembrete sem horario concluida ou no passado`() {
        assertNull(reminderFor(task(reminder = TaskReminder.NONE), now, TimeZone.UTC))
        assertNull(reminderFor(task(time = null), now, TimeZone.UTC))
        assertNull(reminderFor(task(isDone = true), now, TimeZone.UTC))
        assertNull(reminderFor(task(time = LocalTime(9, 10)), now, TimeZone.UTC))
    }

    @Test
    fun `salvar agenda concluir cancela reabrir agenda e excluir cancela`() = runTest {
        val scheduler = FakeReminderScheduler()
        val sync = testReminderSync(scheduler)
        val repository = InMemoryTaskRepository()

        val saved = SaveTaskUseCase(repository, sync)(
            TaskInput(title = "Reunião", dueDate = today, dueTime = LocalTime(15, 0), reminder = TaskReminder.AT_TIME),
        ).getOrThrow()
        ToggleTaskDoneUseCase(repository, sync)(saved)
        ToggleTaskDoneUseCase(repository, sync)(saved.copy(isDone = true))
        DeleteTaskUseCase(repository, sync)(saved.id)

        val id = saved.id
        assertEquals(listOf("schedule:$id", "cancel:$id", "schedule:$id", "cancel:$id"), scheduler.calls)
    }

    @Test
    fun `falha ao agendar nao impede salvar a tarefa`() = runTest {
        val failing = object : ReminderScheduler {
            override suspend fun schedule(reminder: ScheduledReminder) = error("sem permissão")
            override suspend fun cancel(taskId: String) = error("sem permissão")
        }
        val result = SaveTaskUseCase(InMemoryTaskRepository(), testReminderSync(failing))(
            TaskInput(title = "Reunião", dueDate = today, dueTime = LocalTime(15, 0), reminder = TaskReminder.AT_TIME),
        )

        assertEquals("Reunião", result.getOrThrow().title)
    }
}
