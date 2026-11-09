package br.com.hippo.presentation.components

import androidx.compose.runtime.Composable

/* No iOS, o IosReminderScheduler pede a autorização ao agendar o primeiro lembrete. */
@Composable
actual fun rememberNotificationPermissionRequest(): () -> Unit = {}
