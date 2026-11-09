package br.com.hippo.data.voice

import br.com.hippo.domain.model.TaskPriority
import br.com.hippo.domain.model.TaskReminder
import br.com.hippo.domain.usecase.TaskInput
import br.com.hippo.domain.voice.CommandInterpreter
import br.com.hippo.domain.voice.VoiceCommand
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import kotlinx.datetime.plus

/**
 * Interpretador local, baseado em regras, para frases em português como:
 * "Me lembra de enviar o relatório amanhã às 15h, prioridade alta".
 *
 * Extrai prioridade, data e horário; o que sobra da frase vira o título da tarefa.
 * É propositalmente simples: serve para o app funcionar sem a API e como base de
 * comparação com a interpretação por IA, que entende linguagem mais livre.
 */
class LocalCommandInterpreter : CommandInterpreter {

    override val name: String = "local"

    override suspend fun interpret(text: String, now: LocalDateTime): VoiceCommand {
        val utterance = Utterance(text)
        val wantsReminder = utterance.consumeCommandPrefix()
        val priority = utterance.consumePriority()
        var date = utterance.consumeDate(now.date)
        val time = utterance.consumeTime()
        // "às 15h" sem dia: hoje, se ainda não passou; senão, amanhã.
        if (time != null && date == null) {
            date = if (time > now.time) now.date else now.date.plus(DatePeriod(days = 1))
        }

        val title = utterance.remainingTitle()
        if (title.isEmpty()) return VoiceCommand.NotUnderstood

        return VoiceCommand.CreateTask(
            TaskInput(
                title = title,
                priority = priority ?: TaskPriority.MEDIUM,
                dueDate = date,
                dueTime = time,
                reminder = if (wantsReminder && time != null) TaskReminder.AT_TIME else TaskReminder.NONE,
            ),
        )
    }
}

/**
 * A frase em duas versões de mesmo tamanho: a original (para o título) e uma normalizada
 * (minúsculas, sem acentos) para as buscas. Cada trecho reconhecido é apagado das duas,
 * de modo que ao final só resta o título.
 */
private class Utterance(text: String) {
    private val original = text.trim().toCharArray()
    private val normalized = CharArray(original.size) { normalize(original[it]) }

    private fun find(regex: Regex): MatchResult? = regex.find(normalized.concatToString())

    private fun consume(match: MatchResult) {
        for (i in match.range) {
            original[i] = ' '
            normalized[i] = ' '
        }
    }

    /** Remove "me lembra de", "criar tarefa", "preciso"... Retorna se o usuário pediu um lembrete. */
    fun consumeCommandPrefix(): Boolean {
        find(GREETING)?.let(::consume)
        val command = find(COMMAND_PREFIX)
        command?.let(::consume)
        find(NEED_PREFIX)?.let(::consume)
        return command?.groupValues?.get(1)?.let { it.contains("lembr") || it.contains("avis") } ?: false
    }

    fun consumePriority(): TaskPriority? {
        find(PRIORITY)?.let { match ->
            consume(match)
            return when (match.groupValues[1]) {
                "alta", "maxima", "urgente" -> TaskPriority.HIGH
                "baixa", "minima" -> TaskPriority.LOW
                else -> TaskPriority.MEDIUM
            }
        }
        find(PRIORITY_WORD)?.let { consume(it); return TaskPriority.HIGH }
        find(LOW_PRIORITY_WORD)?.let { consume(it); return TaskPriority.LOW }
        return null
    }

    fun consumeDate(today: LocalDate): LocalDate? {
        find(DAY_AFTER_TOMORROW)?.let { consume(it); return today.plus(DatePeriod(days = 2)) }
        find(TOMORROW)?.let { consume(it); return today.plus(DatePeriod(days = 1)) }
        find(TODAY)?.let { consume(it); return today }
        find(WEEKDAY)?.let { match ->
            consume(match)
            return nextWeekday(today, WEEKDAYS.getValue(match.groupValues[1]))
        }
        find(DAY_OF_MONTH)?.let { match ->
            val day = match.groupValues[1].toInt()
            val month = match.groupValues[2].takeIf { it.isNotEmpty() }?.let(::monthNumber)
            resolveDate(today, day, month)?.let { consume(match); return it }
        }
        find(NUMERIC_DATE)?.let { match ->
            resolveDate(today, match.groupValues[1].toInt(), match.groupValues[2].toInt())
                ?.let { consume(match); return it }
        }
        find(WRITTEN_DATE)?.let { match ->
            resolveDate(today, match.groupValues[1].toInt(), monthNumber(match.groupValues[2]))
                ?.let { consume(match); return it }
        }
        return null
    }

    fun consumeTime(): LocalTime? {
        find(NOON_MIDNIGHT)?.let { match ->
            consume(match)
            return if (match.groupValues[1].startsWith("meio")) LocalTime(12, 0) else LocalTime(0, 0)
        }
        for (regex in listOf(CLOCK_TIME, SPOKEN_TIME)) {
            val match = find(regex) ?: continue
            val (hourText, minuteText, period) = match.destructured
            val minute = when {
                minuteText == "meia" -> 30
                minuteText.isNotEmpty() -> minuteText.toInt()
                else -> 0
            }
            val time = buildTime(hourText.toInt(), minute, period) ?: continue
            consume(match)
            return time
        }
        return null
    }

    /** O que sobrou da frase, limpo: sem espaços duplos, pontuação solta ou conectivos sobrando. */
    fun remainingTitle(): String {
        var title = original.concatToString()
            .replace(WHITESPACE, " ")
            .replace(SPACE_BEFORE_PUNCTUATION, "$1")
            .replace(REPEATED_PUNCTUATION, ",")
            .trim { it.isWhitespace() || it in ",.;:!?-" }
        while (true) {
            val trimmed = title
                .replace(DANGLING_CONNECTOR_END, "")
                .replace(DANGLING_CONNECTOR_START, "")
                .trim { it.isWhitespace() || it in ",.;:!?-" }
            if (trimmed == title) break
            title = trimmed
        }
        return title.replaceFirstChar { it.uppercaseChar() }
    }

    private companion object {
        val GREETING = Regex("""^\s*(?:(?:ei|oi|ola)\s+)?hippo\b[\s,]*""")
        val COMMAND_PREFIX = Regex(
            """^\s*(me\s+lembr[ae]r?|lembr[ae]r?(?:-me)?|me\s+avis[ae]r?|avis[ae]r?(?:-me)?|cri[ae]r?|""" +
                """adicion[ae]r?|anot[ae]r?|agend[ae]r?|marc[ae]r?|nova)\b""" +
                """(?:\s+(?:uma|um|a))?(?:\s+(?:nova\s+)?tarefa)?(?:\s*:)?(?:\s+(?:de|que|para|pra))?\b""",
        )
        val NEED_PREFIX = Regex("""^\s*(?:eu\s+)?(?:preciso|tenho\s+que|tenho\s+de|devo)(?:\s+de)?\b""")

        val PRIORITY = Regex("""\b(?:com\s+)?(?:prioridade|urgencia)\s+(alta|maxima|urgente|media|normal|baixa|minima)\b""")
        val PRIORITY_WORD = Regex("""\b(?:e\s+)?(?:urgente|importante)\b""")
        val LOW_PRIORITY_WORD = Regex("""\bsem\s+pressa\b""")

        val DAY_AFTER_TOMORROW = Regex("""\b(?:para\s+)?depois\s+de\s+amanha\b""")
        val TOMORROW = Regex("""\b(?:para\s+)?amanha\b""")
        val TODAY = Regex("""\b(?:para\s+)?hoje\b""")
        val WEEKDAY = Regex(
            """\b(?:(?:na|nesta|nessa|no|neste|nesse|para|pra)\s+)?(?:(?:proxima|proximo)\s+)?""" +
                """(segunda|terca|quarta|quinta|sexta|sabado|domingo)(?:[\s-]feira)?\b""",
        )
        val MONTHS = "janeiro|fevereiro|marco|abril|maio|junho|julho|agosto|setembro|outubro|novembro|dezembro"
        val DAY_OF_MONTH = Regex("""\b(?:(?:no|para\s+o|pro)\s+)?dia\s+(\d{1,2})(?:\s*(?:/|de)\s*($MONTHS|\d{1,2}))?\b""")
        val NUMERIC_DATE = Regex("""\b(?:(?:em|no|para)\s+)?(\d{1,2})/(\d{1,2})(?:/\d{2,4})?\b""")
        val WRITTEN_DATE = Regex("""\b(?:(?:em|no|para)\s+)?(\d{1,2})\s+de\s+($MONTHS)\b""")

        val NOON_MIDNIGHT = Regex("""\b(?:(?:ao|a|as|ate\s+o|ate\s+a)\s+)?(meio[\s-]dia|meia[\s-]noite)\b""")
        private const val PERIOD = """(?:\s+(da\s+manha|da\s+tarde|da\s+noite|da\s+madrugada))?"""
        /** "15h", "15h30", "15:30", "às 15 horas", "3 horas da tarde". Grupos: hora, minuto, período. */
        val CLOCK_TIME = Regex(
            """\b(?:(?:as|a|pelas|para\s+as|ate\s+as)\s+)?(\d{1,2})\s*(?:h|:|horas?)\s*(\d{2}|)(?:\s*min(?:utos)?)?$PERIOD""",
        )
        /** "às 3 da tarde", "às 15", "às 10 e meia". */
        val SPOKEN_TIME = Regex("""\b(?:as|pelas|para\s+as|ate\s+as)\s+(\d{1,2})(?:\s+e\s+(meia|\d{1,2}))?""" + PERIOD + """\b""")

        val WHITESPACE = Regex("""\s+""")
        val SPACE_BEFORE_PUNCTUATION = Regex("""\s+([,.;!?])""")
        val REPEATED_PUNCTUATION = Regex("""[,;](?:\s*[,;])+""")
        val DANGLING_CONNECTOR_END = Regex("""(?i)\s+(?:e|de|do|da|no|na|em|para|pra|com|ate|até|as|às|a|à|o)$""")
        val DANGLING_CONNECTOR_START = Regex("""(?i)^(?:de|que|para|pra|e)(?:\s+|$)""")

        val WEEKDAYS = mapOf(
            "segunda" to DayOfWeek.MONDAY,
            "terca" to DayOfWeek.TUESDAY,
            "quarta" to DayOfWeek.WEDNESDAY,
            "quinta" to DayOfWeek.THURSDAY,
            "sexta" to DayOfWeek.FRIDAY,
            "sabado" to DayOfWeek.SATURDAY,
            "domingo" to DayOfWeek.SUNDAY,
        )

        fun monthNumber(text: String): Int =
            text.toIntOrNull() ?: (MONTHS.split("|").indexOf(text) + 1)

        /** Próxima ocorrência do dia da semana (se hoje for segunda, "segunda" é a da semana que vem). */
        fun nextWeekday(today: LocalDate, target: DayOfWeek): LocalDate {
            val diff = (target.isoDayNumber - today.dayOfWeek.isoDayNumber + 7) % 7
            return today.plus(DatePeriod(days = if (diff == 0) 7 else diff))
        }

        /**
         * Data futura mais próxima para "dia [day]" (deste mês ou do próximo) ou
         * "[day]/[month]" (deste ano ou do próximo). `null` se a data não existir.
         */
        fun resolveDate(today: LocalDate, day: Int, month: Int?): LocalDate? {
            if (month != null) {
                val thisYear = dateOrNull(today.year, month, day) ?: return null
                return if (thisYear >= today) thisYear else dateOrNull(today.year + 1, month, day)
            }
            var year = today.year
            var monthNumber = today.month.number
            repeat(12) {
                dateOrNull(year, monthNumber, day)?.takeIf { it >= today }?.let { return it }
                if (++monthNumber > 12) {
                    monthNumber = 1
                    year++
                }
            }
            return null
        }

        fun dateOrNull(year: Int, month: Int, day: Int): LocalDate? =
            runCatching { LocalDate(year, month, day) }.getOrNull()

        /** Aplica "da tarde/noite" (3 da tarde = 15h) e valida o horário. */
        fun buildTime(hour: Int, minute: Int, period: String): LocalTime? {
            val adjusted = when {
                (period.endsWith("tarde") || period.endsWith("noite")) && hour in 1..11 -> hour + 12
                period.endsWith("noite") && hour == 12 -> 0
                else -> hour
            }
            if (adjusted !in 0..23 || minute !in 0..59) return null
            return LocalTime(adjusted, minute)
        }

        fun normalize(char: Char): Char = when (val c = char.lowercaseChar()) {
            'á', 'à', 'â', 'ã', 'ä' -> 'a'
            'é', 'è', 'ê', 'ë' -> 'e'
            'í', 'ì', 'î', 'ï' -> 'i'
            'ó', 'ò', 'ô', 'õ', 'ö' -> 'o'
            'ú', 'ù', 'û', 'ü' -> 'u'
            'ç' -> 'c'
            else -> c
        }
    }
}
