package br.com.hippo.presentation.voice

import androidx.compose.runtime.Composable

/*
 * No iOS, as autorizações de microfone e de reconhecimento de fala são pedidas pelo
 * próprio IosVoiceRecognizer ao começar a ouvir (o sistema mostra os alertas usando os
 * textos do Info.plist). Se o usuário negar, a escuta falha com NO_PERMISSION.
 */
@Composable
actual fun rememberMicrophonePermissionRequest(onResult: (granted: Boolean) -> Unit): () -> Unit =
    { onResult(true) }
