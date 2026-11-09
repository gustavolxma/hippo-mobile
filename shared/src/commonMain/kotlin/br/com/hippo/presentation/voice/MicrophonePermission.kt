package br.com.hippo.presentation.voice

import androidx.compose.runtime.Composable

/**
 * Devolve uma função que pede acesso ao microfone e avisa o resultado em [onResult].
 * Se a permissão já foi dada, responde `true` na hora, sem mostrar nada.
 */
@Composable
expect fun rememberMicrophonePermissionRequest(onResult: (granted: Boolean) -> Unit): () -> Unit
