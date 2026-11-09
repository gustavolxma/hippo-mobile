package br.com.hippo.domain.voice

import kotlinx.coroutines.flow.Flow

/** Eventos de uma sessão de reconhecimento de fala. */
sealed interface SpeechEvent {
    /** O microfone abriu: o usuário já pode falar. */
    data object Listening : SpeechEvent

    /** Transcrição parcial, atualizada enquanto o usuário fala. */
    data class Partial(val text: String) : SpeechEvent

    /** Transcrição final; o fluxo termina logo depois. */
    data class Final(val text: String) : SpeechEvent
}

enum class SpeechError { NO_PERMISSION, NOT_AVAILABLE, NO_MATCH, NETWORK, UNKNOWN }

class SpeechRecognitionException(val error: SpeechError) : Exception("Falha no reconhecimento de voz: $error")

/**
 * Reconhecimento de fala (voz → texto). Cada plataforma usa o serviço nativo:
 * `android.speech.SpeechRecognizer` no Android e `SFSpeechRecognizer` (Speech framework) no iOS.
 */
interface VoiceRecognizer {
    fun isAvailable(): Boolean

    /**
     * Abre o microfone e emite os eventos até a transcrição final (o Android para sozinho
     * quando o usuário fica em silêncio). Falhas chegam como [SpeechRecognitionException].
     * Cancelar a coleta do fluxo encerra a escuta.
     */
    fun listen(languageTag: String = "pt-BR"): Flow<SpeechEvent>

    /** Para de ouvir e gera a transcrição final com o que já foi dito. */
    fun stop()
}
