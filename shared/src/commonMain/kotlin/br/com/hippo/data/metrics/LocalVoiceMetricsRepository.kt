package br.com.hippo.data.metrics

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import br.com.hippo.data.local.db.HippoDatabase
import br.com.hippo.data.local.db.VoiceInteractionEntity
import br.com.hippo.domain.metrics.VoiceInteraction
import br.com.hippo.domain.metrics.VoiceMetricsRepository
import br.com.hippo.domain.metrics.VoiceOutcome
import br.com.hippo.domain.model.TaskPriority
import br.com.hippo.domain.model.User
import br.com.hippo.domain.usecase.TaskInput
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Instant

/** Guarda os registros de voz no banco local, separados por usuário. */
class LocalVoiceMetricsRepository(
    database: HippoDatabase,
    private val currentUser: Flow<User?>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : VoiceMetricsRepository {

    private val queries = database.voiceInteractionEntityQueries

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observe(): Flow<List<VoiceInteraction>> =
        currentUser.map { it?.id }.distinctUntilChanged().flatMapLatest { ownerId ->
            if (ownerId == null) {
                flowOf(emptyList())
            } else {
                queries.selectByOwner(ownerId).asFlow().mapToList(ioDispatcher)
                    .map { rows -> rows.map { it.toDomain() } }
            }
        }

    override suspend fun record(interaction: VoiceInteraction): Long? = withContext(ioDispatcher) {
        val owner = currentUser.first()?.id ?: return@withContext null
        queries.transactionWithResult {
            with(interaction) {
                queries.insert(
                    owner_id = owner,
                    created_at = createdAt.toString(),
                    interpreter = interpreter,
                    transcript = transcript,
                    result_title = result?.title,
                    result_date = result?.dueDate?.toString(),
                    result_time = result?.dueTime?.toString(),
                    result_priority = result?.priority?.name,
                    outcome = outcome.name,
                    recognition_ms = recognitionMs,
                    interpretation_ms = interpretationMs,
                )
            }
            queries.lastInsertedId().executeAsOne()
        }
    }

    override suspend fun updateOutcome(id: Long, outcome: VoiceOutcome) = withContext(ioDispatcher) {
        queries.updateOutcome(outcome.name, id)
        Unit
    }

    override suspend fun clear() = withContext(ioDispatcher) {
        currentUser.first()?.id?.let { queries.deleteByOwner(it) }
        Unit
    }
}

private fun VoiceInteractionEntity.toDomain() = VoiceInteraction(
    id = id,
    createdAt = Instant.parse(created_at),
    interpreter = interpreter,
    transcript = transcript,
    result = result_title?.let { title ->
        TaskInput(
            title = title,
            priority = TaskPriority.entries.firstOrNull { it.name == result_priority } ?: TaskPriority.MEDIUM,
            dueDate = result_date?.let(LocalDate::parse),
            dueTime = result_time?.let(LocalTime::parse),
        )
    },
    outcome = VoiceOutcome.entries.firstOrNull { it.name == outcome } ?: VoiceOutcome.PENDING,
    recognitionMs = recognition_ms,
    interpretationMs = interpretation_ms,
)
