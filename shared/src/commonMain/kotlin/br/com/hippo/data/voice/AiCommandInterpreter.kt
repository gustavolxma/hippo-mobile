package br.com.hippo.data.voice

import br.com.hippo.data.remote.AiApi
import br.com.hippo.data.remote.dto.ApiDateTimeFormat
import br.com.hippo.data.remote.dto.InterpretCommandDto
import br.com.hippo.data.remote.dto.toCommand
import br.com.hippo.domain.voice.CommandInterpreter
import br.com.hippo.domain.voice.VoiceCommand
import kotlinx.datetime.LocalDateTime

/** Interpreta a frase com a IA (Gemini), por meio da API. Precisa de internet e de login. */
class AiCommandInterpreter(private val api: AiApi) : CommandInterpreter {

    override val name: String = "ia"

    override suspend fun interpret(text: String, now: LocalDateTime): VoiceCommand =
        api.interpret(InterpretCommandDto(texto = text, agora = ApiDateTimeFormat.format(now))).toCommand()
}
