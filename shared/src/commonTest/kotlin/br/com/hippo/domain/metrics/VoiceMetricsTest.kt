package br.com.hippo.domain.metrics

import br.com.hippo.domain.model.TaskPriority
import br.com.hippo.domain.usecase.TaskInput
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class VoiceMetricsTest {

    private fun interaction(
        outcome: VoiceOutcome,
        transcript: String = "comprar pão",
        recognitionMs: Long? = 1000,
        interpretationMs: Long? = 4,
    ) = VoiceInteraction(
        createdAt = Instant.parse("2026-11-09T12:00:00Z"),
        interpreter = "local",
        transcript = transcript,
        result = TaskInput(title = "Comprar pão"),
        outcome = outcome,
        recognitionMs = recognitionMs,
        interpretationMs = interpretationMs,
    )

    @Test
    fun `calcula taxas e medias`() {
        val summary = summarize(
            listOf(
                interaction(VoiceOutcome.CONFIRMED, recognitionMs = 1000, interpretationMs = 2),
                interaction(VoiceOutcome.CONFIRMED, recognitionMs = 2000, interpretationMs = 4),
                interaction(VoiceOutcome.CONFIRMED, recognitionMs = 3000, interpretationMs = 6),
                interaction(VoiceOutcome.EDITED),
                interaction(VoiceOutcome.NOT_UNDERSTOOD),
                interaction(VoiceOutcome.RECOGNITION_FAILED, recognitionMs = null, interpretationMs = null),
            ),
        )

        assertEquals(6, summary.total)
        assertEquals(5, summary.transcribed)
        assertEquals(4, summary.understood)
        assertEquals(80.0, summary.understoodRate)
        assertEquals(75.0, summary.confirmedWithoutEditRate)
        assertEquals(1600L, summary.averageRecognitionMs)
        assertEquals(4L, summary.averageInterpretationMs) // (2 + 4 + 6 + 4 + 4) / 5
    }

    @Test
    fun `sem registros as taxas ficam vazias`() {
        val summary = summarize(emptyList())

        assertEquals(0, summary.total)
        assertNull(summary.understoodRate)
        assertNull(summary.confirmedWithoutEditRate)
        assertNull(summary.averageRecognitionMs)
    }

    @Test
    fun `exporta csv com cabecalho e escapa ponto e virgula`() {
        val item = interaction(VoiceOutcome.CONFIRMED, transcript = "pão; leite").copy(
            result = TaskInput(
                title = "Comprar pão",
                priority = TaskPriority.HIGH,
                dueDate = LocalDate(2026, 11, 10),
                dueTime = LocalTime(15, 0),
            ),
        )

        val lines = toCsv(listOf(item)).trim().lines()

        assertEquals(
            "data_hora;interpretador;fala;titulo;data;hora;prioridade;desfecho;reconhecimento_ms;interpretacao_ms",
            lines[0],
        )
        assertEquals(
            "2026-11-09T12:00:00Z;local;\"pão; leite\";Comprar pão;2026-11-10;15:00;HIGH;CONFIRMED;1000;4",
            lines[1],
        )
    }
}
