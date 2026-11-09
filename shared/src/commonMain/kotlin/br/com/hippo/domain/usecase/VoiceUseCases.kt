package br.com.hippo.domain.usecase

import br.com.hippo.domain.voice.CommandInterpreter
import br.com.hippo.domain.voice.VoiceCommand
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/** Interpreta a frase transcrita usando a data/hora atual do aparelho como referência. */
class InterpretVoiceCommandUseCase(
    private val interpreter: CommandInterpreter,
    private val clock: Clock,
) {
    val interpreterName: String get() = interpreter.name

    suspend operator fun invoke(text: String): VoiceCommand {
        val now = clock.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return interpreter.interpret(text, now)
    }
}
