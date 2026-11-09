package br.com.hippo.presentation.components

import br.com.hippo.domain.model.Task
import br.com.hippo.domain.model.TaskPriority
import br.com.hippo.domain.model.TaskReminder
import br.com.hippo.domain.usecase.TaskSection
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.number
import kotlinx.datetime.plus

// Textos e formatos exibidos ao usuário. Ficam na apresentação, não no domínio.

val TaskPriority.label: String
    get() = when (this) {
        TaskPriority.LOW -> "Baixa"
        TaskPriority.MEDIUM -> "Média"
        TaskPriority.HIGH -> "Alta"
    }

val TaskReminder.label: String
    get() = when (this) {
        TaskReminder.NONE -> "Sem lembrete"
        TaskReminder.AT_TIME -> "Na hora"
        TaskReminder.MINUTES_10 -> "10 min antes"
        TaskReminder.MINUTES_30 -> "30 min antes"
        TaskReminder.HOUR_1 -> "1 hora antes"
        TaskReminder.DAY_1 -> "1 dia antes"
    }

val TaskSection.title: String
    get() = when (this) {
        TaskSection.OVERDUE -> "Atrasadas"
        TaskSection.TODAY -> "Hoje"
        TaskSection.UPCOMING -> "Próximas"
        TaskSection.NO_DATE -> "Sem data"
        TaskSection.DONE -> "Concluídas"
    }

private fun Int.twoDigits() = toString().padStart(2, '0')

/** 28/10/2026 */
fun LocalDate.formatted(): String = "${day.twoDigits()}/${month.number.twoDigits()}/$year"

/** 28/10 */
fun LocalDate.formattedShort(): String = "${day.twoDigits()}/${month.number.twoDigits()}"

/** 14:00 */
fun LocalTime.formatted(): String = "${hour.twoDigits()}:${minute.twoDigits()}"

/**
 * Linha de apoio do cartão: "14:00 · Prioridade alta". Fora de "Hoje",
 * a data aparece antes: "28/10 · 14:00 · Prioridade alta".
 */
fun Task.subtitle(showDate: Boolean): String = buildList {
    if (showDate) dueDate?.let { add(it.formattedShort()) }
    dueTime?.let { add(it.formatted()) }
    add("Prioridade ${priority.label.lowercase()}")
}.joinToString(" · ")

/** "Hoje, 15:00", "Amanhã, 15:00", "13/11, 15:00", "13/11" ou "Sem data". */
fun describeWhen(date: LocalDate?, time: LocalTime?, today: LocalDate): String {
    val day = when (date) {
        null -> return time?.let { "Hoje, ${it.formatted()}" } ?: "Sem data"
        today -> "Hoje"
        today.plus(DatePeriod(days = 1)) -> "Amanhã"
        else -> date.formattedShort()
    }
    return time?.let { "$day, ${it.formatted()}" } ?: day
}
