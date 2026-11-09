package br.com.hippo.data.voice

import br.com.hippo.domain.voice.SpeechError
import br.com.hippo.domain.voice.SpeechEvent
import br.com.hippo.domain.voice.SpeechRecognitionException
import br.com.hippo.domain.voice.VoiceRecognizer
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionDuckOthers
import platform.AVFAudio.AVAudioSessionCategoryRecord
import platform.AVFAudio.AVAudioSessionModeMeasurement
import platform.AVFAudio.AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation
import platform.AVFAudio.setActive
import platform.Foundation.NSError
import platform.Foundation.NSLocale
import platform.Speech.SFSpeechAudioBufferRecognitionRequest
import platform.Speech.SFSpeechRecognizer
import platform.Speech.SFSpeechRecognizerAuthorizationStatus
import kotlin.coroutines.resume

/**
 * Reconhecimento de fala no iOS com o framework Speech da Apple — escrito em Kotlin,
 * usando as APIs nativas expostas em `platform.*`, sem nenhuma linha de Swift.
 *
 * Diferente do Android, o iOS não para sozinho ao detectar silêncio, então a escuta
 * termina após [SILENCE_TIMEOUT_MS] sem novas palavras (ou quando [stop] é chamado).
 *
 * ATENÇÃO: compila no Windows, mas só pode ser testado em um Mac (simulador ou iPhone).
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosVoiceRecognizer : VoiceRecognizer {

    private var currentRequest: SFSpeechAudioBufferRecognitionRequest? = null

    override fun isAvailable(): Boolean = SFSpeechRecognizer(NSLocale(localeIdentifier = "pt-BR")).isAvailable()

    override fun listen(languageTag: String): Flow<SpeechEvent> = callbackFlow {
        if (!requestSpeechAuthorization() || !requestMicrophonePermission()) {
            close(SpeechRecognitionException(SpeechError.NO_PERMISSION))
            awaitClose()
            return@callbackFlow
        }
        val recognizer = SFSpeechRecognizer(NSLocale(localeIdentifier = languageTag))
        if (!recognizer.isAvailable()) {
            close(SpeechRecognitionException(SpeechError.NOT_AVAILABLE))
            awaitClose()
            return@callbackFlow
        }

        val session = AVAudioSession.sharedInstance()
        val request = SFSpeechAudioBufferRecognitionRequest().apply { shouldReportPartialResults = true }
        currentRequest = request
        val engine = AVAudioEngine()
        val input = engine.inputNode

        val started = memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            session.setCategory(
                AVAudioSessionCategoryRecord,
                mode = AVAudioSessionModeMeasurement,
                options = AVAudioSessionCategoryOptionDuckOthers,
                error = error.ptr,
            )
            session.setActive(true, error = error.ptr)

            // Entrega o áudio do microfone, em pedaços, para o reconhecedor.
            input.installTapOnBus(0u, bufferSize = 1024u, format = input.outputFormatForBus(0u)) { buffer, _ ->
                buffer?.let { request.appendAudioPCMBuffer(it) }
            }
            engine.prepare()
            engine.startAndReturnError(error.ptr)
        }
        if (!started) {
            close(SpeechRecognitionException(SpeechError.UNKNOWN))
        } else {
            trySend(SpeechEvent.Listening)
        }

        var silenceTimer: Job? = null
        val task = recognizer.recognitionTaskWithRequest(request) { result, error ->
            when {
                result != null && result.isFinal() -> {
                    trySend(SpeechEvent.Final(result.bestTranscription.formattedString))
                    close()
                }
                result != null -> {
                    trySend(SpeechEvent.Partial(result.bestTranscription.formattedString))
                    silenceTimer?.cancel()
                    silenceTimer = launch {
                        delay(SILENCE_TIMEOUT_MS)
                        request.endAudio()
                    }
                }
                error != null -> close(SpeechRecognitionException(SpeechError.NO_MATCH))
            }
        }

        awaitClose {
            silenceTimer?.cancel()
            engine.stop()
            input.removeTapOnBus(0u)
            request.endAudio()
            task.cancel()
            session.setActive(false, withOptions = AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation, error = null)
            if (currentRequest === request) currentRequest = null
        }
    }

    override fun stop() {
        currentRequest?.endAudio()
    }

    private suspend fun requestSpeechAuthorization(): Boolean = suspendCancellableCoroutine { cont ->
        SFSpeechRecognizer.requestAuthorization { status ->
            cont.resume(status == SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized)
        }
    }

    private suspend fun requestMicrophonePermission(): Boolean = suspendCancellableCoroutine { cont ->
        AVAudioSession.sharedInstance().requestRecordPermission { granted -> cont.resume(granted) }
    }

    private companion object {
        const val SILENCE_TIMEOUT_MS = 1_500L
    }
}
