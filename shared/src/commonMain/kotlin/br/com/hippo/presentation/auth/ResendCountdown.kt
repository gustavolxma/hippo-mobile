package br.com.hippo.presentation.auth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Tempo de espera até poder pedir outro código de verificação. */
const val RESEND_COOLDOWN_SECONDS = 45

/** Conta de [seconds] até 0, avisando a cada segundo (usado no "Reenviar código em 0:45"). */
internal fun CoroutineScope.launchCountdown(
    seconds: Int = RESEND_COOLDOWN_SECONDS,
    onTick: (secondsLeft: Int) -> Unit,
): Job = launch {
    for (left in seconds downTo 0) {
        onTick(left)
        if (left > 0) delay(1_000)
    }
}

/** "0:45" */
internal fun formatCountdown(seconds: Int): String = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
