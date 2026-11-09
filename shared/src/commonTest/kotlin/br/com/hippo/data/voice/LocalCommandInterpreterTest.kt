package br.com.hippo.data.voice

import br.com.hippo.domain.model.TaskPriority
import br.com.hippo.domain.model.TaskReminder
import br.com.hippo.domain.usecase.TaskInput
import br.com.hippo.domain.voice.VoiceCommand
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class LocalCommandInterpreterTest {

    private val interpreter = LocalCommandInterpreter()

    /** Segunda-feira, 09/11/2026, 9h. */
    private val now = LocalDateTime(2026, 11, 9, 9, 0)

    private suspend fun task(text: String): TaskInput =
        assertIs<VoiceCommand.CreateTask>(interpreter.interpret(text, now), "Frase: $text").input

    @Test
    fun `frase do prototipo`() = runTest {
        val input = task("Me lembra de enviar o relatório amanhã às 15h, prioridade alta")

        assertEquals("Enviar o relatório", input.title)
        assertEquals(LocalDate(2026, 11, 10), input.dueDate)
        assertEquals(LocalTime(15, 0), input.dueTime)
        assertEquals(TaskPriority.HIGH, input.priority)
        assertEquals(TaskReminder.AT_TIME, input.reminder)
    }

    @Test
    fun `so o titulo`() = runTest {
        val input = task("estudar kotlin multiplatform")

        assertEquals("Estudar kotlin multiplatform", input.title)
        assertNull(input.dueDate)
        assertNull(input.dueTime)
        assertEquals(TaskPriority.MEDIUM, input.priority)
        assertEquals(TaskReminder.NONE, input.reminder)
    }

    @Test
    fun `formatos de horario`() = runTest {
        assertEquals(LocalTime(15, 30), task("reunião às 15h30").dueTime)
        assertEquals(LocalTime(15, 30), task("reunião às 15:30").dueTime)
        assertEquals(LocalTime(10, 0), task("reunião às 10 horas").dueTime)
        assertEquals(LocalTime(15, 0), task("reunião às 3 da tarde").dueTime)
        assertEquals(LocalTime(20, 0), task("jantar às 8 da noite").dueTime)
        assertEquals(LocalTime(10, 30), task("reunião às 10 e meia").dueTime)
        assertEquals(LocalTime(12, 0), task("almoço ao meio-dia").dueTime)
    }

    @Test
    fun `horario sem dia vira hoje ou amanha`() = runTest {
        assertEquals(LocalDate(2026, 11, 9), task("ligar para o banco às 14h").dueDate)
        // 8h já passou (agora são 9h), então fica para amanhã.
        assertEquals(LocalDate(2026, 11, 10), task("ligar para o banco às 8h").dueDate)
    }

    @Test
    fun `datas relativas e dias da semana`() = runTest {
        assertEquals(LocalDate(2026, 11, 9), task("pagar conta hoje").dueDate)
        assertEquals(LocalDate(2026, 11, 11), task("pagar conta depois de amanhã").dueDate)
        assertEquals(LocalDate(2026, 11, 13), task("comprar pão na sexta").dueDate)
        assertEquals(LocalDate(2026, 11, 13), task("comprar pão na sexta-feira").dueDate)
        // Hoje é segunda: "segunda" é a da semana que vem.
        assertEquals(LocalDate(2026, 11, 16), task("academia na segunda").dueDate)
        assertEquals(LocalDate(2026, 11, 15), task("visitar a avó no domingo").dueDate)
    }

    @Test
    fun `datas com dia e mes`() = runTest {
        assertEquals(LocalDate(2026, 11, 20), task("entregar trabalho dia 20").dueDate)
        // Dia 5 já passou neste mês.
        assertEquals(LocalDate(2026, 12, 5), task("entregar trabalho dia 5").dueDate)
        assertEquals(LocalDate(2026, 12, 15), task("entregar trabalho 15/12").dueDate)
        assertEquals(LocalDate(2027, 3, 1), task("entregar trabalho dia 1 de março").dueDate)
        assertEquals(LocalDate(2027, 1, 10), task("entregar trabalho 10/01").dueDate)
    }

    @Test
    fun `prioridades`() = runTest {
        assertEquals(TaskPriority.HIGH, task("pagar boleto urgente").priority)
        assertEquals(TaskPriority.LOW, task("organizar a gaveta prioridade baixa").priority)
        assertEquals(TaskPriority.MEDIUM, task("lavar o carro com prioridade média").priority)
        assertEquals(TaskPriority.LOW, task("ler um livro sem pressa").priority)
    }

    @Test
    fun `remove prefixos de comando`() = runTest {
        assertEquals("Comprar leite", task("criar tarefa comprar leite").title)
        assertEquals("Comprar leite", task("Hippo, adicionar uma tarefa de comprar leite").title)
        assertEquals("Comprar leite", task("preciso comprar leite").title)
        assertEquals("Comprar leite", task("nova tarefa: comprar leite").title)
    }

    @Test
    fun `frase completa com varios elementos`() = runTest {
        val input = task("me lembre de reunião com o orientador na quinta às 16h30 prioridade média")

        assertEquals("Reunião com o orientador", input.title)
        assertEquals(LocalDate(2026, 11, 12), input.dueDate)
        assertEquals(LocalTime(16, 30), input.dueTime)
        assertEquals(TaskPriority.MEDIUM, input.priority)
    }

    @Test
    fun `nao entende frase sem tarefa`() = runTest {
        assertIs<VoiceCommand.NotUnderstood>(interpreter.interpret("", now))
        assertIs<VoiceCommand.NotUnderstood>(interpreter.interpret("amanhã às 10h", now))
        assertIs<VoiceCommand.NotUnderstood>(interpreter.interpret("me lembra de", now))
    }
}
