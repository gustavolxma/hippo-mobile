package br.com.hippo.data.reminder

import br.com.hippo.domain.reminder.ReminderScheduler
import br.com.hippo.domain.reminder.ScheduledReminder
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.number
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionList
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject
import kotlin.coroutines.resume

/**
 * Lembretes no iOS com notificações locais (UserNotifications), escrito em Kotlin.
 * A autorização é pedida na primeira vez que um lembrete é agendado.
 *
 * ATENÇÃO: compila no Windows, mas só pode ser testado em um Mac (simulador ou iPhone).
 */
class IosReminderScheduler : ReminderScheduler {

    private val center = UNUserNotificationCenter.currentNotificationCenter()

    // O iOS guarda o delegate como referência fraca; manter aqui evita que ele suma.
    private val foregroundDelegate = ForegroundNotificationDelegate()

    init {
        center.delegate = foregroundDelegate
    }

    override suspend fun schedule(reminder: ScheduledReminder) {
        if (!requestAuthorization()) return

        val content = UNMutableNotificationContent().apply {
            setTitle(reminder.title)
            setBody(reminder.message)
            setSound(UNNotificationSound.defaultSound())
        }
        val at = reminder.triggerAt
        val components = NSDateComponents().apply {
            setYear(at.year.toLong())
            setMonth(at.month.number.toLong())
            setDay(at.day.toLong())
            setHour(at.hour.toLong())
            setMinute(at.minute.toLong())
        }
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, repeats = false)
        // Usar o id da tarefa como identificador faz um novo agendamento substituir o anterior.
        val request = UNNotificationRequest.requestWithIdentifier(reminder.taskId, content, trigger)
        center.addNotificationRequest(request, withCompletionHandler = null)
    }

    override suspend fun cancel(taskId: String) {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(taskId))
    }

    private suspend fun requestAuthorization(): Boolean = suspendCancellableCoroutine { cont ->
        center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { granted, _ ->
            cont.resume(granted)
        }
    }
}

/** Por padrão o iOS não mostra notificações com o app aberto; este delegate libera o banner. */
private class ForegroundNotificationDelegate : NSObject(), UNUserNotificationCenterDelegateProtocol {
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (UNNotificationPresentationOptions) -> Unit,
    ) {
        withCompletionHandler(
            UNNotificationPresentationOptionBanner or UNNotificationPresentationOptionList or
                UNNotificationPresentationOptionSound,
        )
    }
}
