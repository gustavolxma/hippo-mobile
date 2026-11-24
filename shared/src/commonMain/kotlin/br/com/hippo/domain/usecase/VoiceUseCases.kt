package br.com.hippo.domain.usecase

import br.com.hippo.domain.voice.CommandInterpreter
import br.com.hippo.domain.voice.VoiceCommand
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

/** O que foi entendido e quem interpretou ("ia" ou "local"), para as métricas. */
data class Interpretation(val command: VoiceCommand, val interpreter: String)

/**
 * Interpreta a frase transcrita usando a data/hora atual do aparelho como referência.
 * Se o [interpreter] falhar ou demorar (sem internet, servidor acordando), usa o [fallback].
 */
class InterpretVoiceCommandUseCase(
    private val interpreter: CommandInterpreter,
    private val clock: Clock,
    private val fallback: CommandInterpreter? = null,
) {
    /** Interpretador principal, registrado quando nem chegou a haver o que interpretar. */
    val interpreterName: String get() = interpreter.name

    suspend operator fun invoke(text: String): Interpretation {
        val now = clock.now().toLocalDateTime(TimeZone.currentSystemDefault())
        if (fallback == null) return Interpretation(interpreter.interpret(text, now), interpreter.name)

        val command = tryInterpret(text, now)
        return if (command != null) {
            Interpretation(command, interpreter.name)
        } else {
            Interpretation(fallback.interpret(text, now), fallback.name)
        }
    }

    private suspend fun tryInterpret(text: String, now: LocalDateTime): VoiceCommand? = try {
        withTimeoutOrNull(TIMEOUT) { interpreter.interpret(text, now) }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private companion object {
        /** Mais que isso, a espera atrapalha mais do que a IA ajuda. */
        val TIMEOUT = 15.seconds
    }
}
