package br.com.hippo.data.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import br.com.hippo.domain.voice.SpeechError
import br.com.hippo.domain.voice.SpeechEvent
import br.com.hippo.domain.voice.SpeechRecognitionException
import br.com.hippo.domain.voice.VoiceRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

/** Reconhecimento de fala com o serviço do próprio Android (Google, no emulador e na maioria dos aparelhos). */
class AndroidVoiceRecognizer(private val context: Context) : VoiceRecognizer {

    private var current: SpeechRecognizer? = null

    override fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    override fun listen(languageTag: String): Flow<SpeechEvent> = callbackFlow {
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        current = recognizer

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                trySend(SpeechEvent.Listening)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                partialResults.firstResult()?.let { trySend(SpeechEvent.Partial(it)) }
            }

            override fun onResults(results: Bundle?) {
                val text = results.firstResult()
                if (text.isNullOrBlank()) {
                    close(SpeechRecognitionException(SpeechError.NO_MATCH))
                } else {
                    trySend(SpeechEvent.Final(text))
                    close()
                }
            }

            override fun onError(error: Int) {
                close(SpeechRecognitionException(error.toSpeechError()))
            }

            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        recognizer.startListening(intent)

        awaitClose {
            recognizer.destroy()
            if (current === recognizer) current = null
        }
    }.flowOn(Dispatchers.Main) // o SpeechRecognizer do Android só funciona na thread principal

    override fun stop() {
        current?.stopListening()
    }

    private fun Bundle?.firstResult(): String? =
        this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()

    private fun Int.toSpeechError(): SpeechError = when (this) {
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechError.NO_PERMISSION
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechError.NO_MATCH
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT, SpeechRecognizer.ERROR_SERVER ->
            SpeechError.NETWORK
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
            SpeechError.NOT_AVAILABLE
        else -> SpeechError.UNKNOWN
    }
}
