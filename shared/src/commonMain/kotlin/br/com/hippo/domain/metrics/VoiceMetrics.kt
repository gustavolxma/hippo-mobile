package br.com.hippo.domain.metrics

import br.com.hippo.domain.usecase.TaskInput
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/** O que aconteceu com um comando de voz depois de interpretado. */
enum class VoiceOutcome {
    /** Entendido, aguardando o usuário confirmar ou editar. */
    PENDING,
    /** Confirmado como estava: interpretação correta. */
    CONFIRMED,
    /** O usuário abriu para editar: interpretação precisou de correção. */
    EDITED,
    /** Entendido, mas o usuário falou de novo ou saiu sem confirmar. */
    DISCARDED,
    /** A fala foi transcrita, mas não virou tarefa. */
    NOT_UNDERSTOOD,
    /** Falha no reconhecimento de fala (silêncio, rede, permissão...). */
    RECOGNITION_FAILED,
}

data class VoiceInteraction(
    val id: Long = 0,
    val createdAt: Instant,
    /** "local" (regras no aparelho) ou, quando houver API, "ia". */
    val interpreter: String,
    val transcript: String,
    val result: TaskInput?,
    val outcome: VoiceOutcome,
    val recognitionMs: Long?,
    val interpretationMs: Long?,
)

/** Registros dos comandos de voz do usuário logado (ficam só no aparelho). */
interface VoiceMetricsRepository {
    fun observe(): Flow<List<VoiceInteraction>>

    /** Salva o registro e devolve o id, usado depois para atualizar o desfecho. */
    suspend fun record(interaction: VoiceInteraction): Long?

    suspend fun updateOutcome(id: Long, outcome: VoiceOutcome)

    suspend fun clear()
}

/** Números para o capítulo de resultados do TCC. Percentuais de 0 a 100. */
data class VoiceMetricsSummary(
    val total: Int,
    val recognitionFailures: Int,
    /** Falas transcritas (total menos falhas de reconhecimento). */
    val transcribed: Int,
    val understood: Int,
    val notUnderstood: Int,
    val confirmed: Int,
    val edited: Int,
    val discarded: Int,
    /** Entre as falas transcritas, quantas viraram tarefa. */
    val understoodRate: Double?,
    /** Entre as tarefas confirmadas ou editadas, quantas não precisaram de correção. */
    val confirmedWithoutEditRate: Double?,
    val averageRecognitionMs: Long?,
    val averageInterpretationMs: Long?,
)

fun summarize(interactions: List<VoiceInteraction>): VoiceMetricsSummary {
    fun count(vararg outcomes: VoiceOutcome) = interactions.count { it.outcome in outcomes }
    fun percent(part: Int, whole: Int) = if (whole == 0) null else part * 100.0 / whole

    val failures = count(VoiceOutcome.RECOGNITION_FAILED)
    val notUnderstood = count(VoiceOutcome.NOT_UNDERSTOOD)
    val confirmed = count(VoiceOutcome.CONFIRMED)
    val edited = count(VoiceOutcome.EDITED)
    val transcribed = interactions.size - failures
    val understood = transcribed - notUnderstood

    return VoiceMetricsSummary(
        total = interactions.size,
        recognitionFailures = failures,
        transcribed = transcribed,
        understood = understood,
        notUnderstood = notUnderstood,
        confirmed = confirmed,
        edited = edited,
        discarded = count(VoiceOutcome.DISCARDED),
        understoodRate = percent(understood, transcribed),
        confirmedWithoutEditRate = percent(confirmed, confirmed + edited),
        averageRecognitionMs = interactions.mapNotNull { it.recognitionMs }.averageOrNull(),
        averageInterpretationMs = interactions.mapNotNull { it.interpretationMs }.averageOrNull(),
    )
}

private fun List<Long>.averageOrNull(): Long? = if (isEmpty()) null else sum() / size

/** Exporta os registros em CSV (separado por ";", que o Excel em português abre direto). */
fun toCsv(interactions: List<VoiceInteraction>): String = buildString {
    appendLine("data_hora;interpretador;fala;titulo;data;hora;prioridade;desfecho;reconhecimento_ms;interpretacao_ms")
    interactions.sortedBy { it.createdAt }.forEach { item ->
        val fields = listOf(
            item.createdAt.toString(),
            item.interpreter,
            item.transcript,
            item.result?.title.orEmpty(),
            item.result?.dueDate?.toString().orEmpty(),
            item.result?.dueTime?.toString().orEmpty(),
            item.result?.priority?.name.orEmpty(),
            item.outcome.name,
            item.recognitionMs?.toString().orEmpty(),
            item.interpretationMs?.toString().orEmpty(),
        )
        appendLine(fields.joinToString(";") { it.csvEscaped() })
    }
}

private fun String.csvEscaped(): String =
    if (any { it == ';' || it == '"' || it == '\n' }) "\"" + replace("\"", "\"\"") + "\"" else this
