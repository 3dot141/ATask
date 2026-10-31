package com.rendyhd.vicu.util.parser

import com.rendyhd.vicu.util.DueDates
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus

/**
 * The Chinese half of the quick-add date parser.
 *
 * Chinese has no spaces, so the English extractor's word boundaries never match. This one matches
 * date and time phrases directly on the text instead, and returns the same [DateResult] the English
 * path does: one token per phrase (a date and a time stay two tokens, so the words between them
 * survive in the title), date-only phrases keep the contract's end-of-day placeholder, and a time
 * with no date means today when it is still ahead and tomorrow otherwise.
 */
internal fun containsCjk(text: String): Boolean = text.any { it in '一'..'鿿' }

private val DAY_WORDS = mapOf(
    "大后天" to 3,
    "后天" to 2,
    "明天" to 1,
    "明日" to 1,
    "今天" to 0,
    "今日" to 0,
    "昨天" to -1,
    "前天" to -2,
)

private val WEEKDAY_CHARS = mapOf(
    '一' to 1, '二' to 2, '三' to 3, '四' to 4, '五' to 5, '六' to 6, '日' to 7, '天' to 7,
)

private val MERIDIEM = listOf("凌晨", "早上", "早晨", "上午", "中午", "下午", "傍晚", "晚上", "夜里", "晚间")

private val DAY_WORD_RE = Regex("大后天|后天|明天|明日|今天|今日|昨天|前天")
private val WEEKDAY_RE = Regex("(下下|下|这|本)?\\s*(?:周|星期|礼拜)\\s*([一二三四五六日天])")
private val RELATIVE_RE = Regex("(\\d+|[一二两三四五六七八九十]+)\\s*个?\\s*(天|周|星期|礼拜|月|年)\\s*(?:之?后|以后)")
private val MONTH_DAY_RE = Regex("(?:(\\d{4})\\s*年\\s*)?(\\d{1,2})\\s*月\\s*(\\d{1,2})\\s*[日号]?")
private val CLOCK_RE = Regex(
    "(" + MERIDIEM.joinToString("|") + ")?\\s*(\\d{1,2}|[一二三四五六七八九十]{1,3})\\s*[点時时]\\s*(半|\\d{1,2}\\s*分?)?"
)
private val COLON_TIME_RE = Regex("(?:(凌晨|早上|上午|中午|下午|傍晚|晚上))?\\s*(\\d{1,2}):(\\d{2})")

/** Parses the leading number of a Chinese numeral ("十一" → 11); null when it is not one. */
private fun chineseNumber(text: String): Int? {
    text.toIntOrNull()?.let { return it }
    if (text.isEmpty()) return null
    val digits = mapOf(
        '一' to 1, '二' to 2, '两' to 2, '三' to 3, '四' to 4,
        '五' to 5, '六' to 6, '七' to 7, '八' to 8, '九' to 9,
    )
    return when {
        text == "十" -> 10
        text.startsWith("十") -> digits[text.getOrNull(1)]?.plus(10)
        text.length == 2 && text[0] == '十' -> null
        text.endsWith("十") -> digits[text[0]]?.times(10)
        text.length == 2 && text[1] == '十' -> null
        text.length >= 3 && text[1] == '十' -> {
            val tens = digits[text[0]] ?: return null
            val ones = digits[text[2]] ?: return null
            tens * 10 + ones
        }
        text.length == 1 -> digits[text[0]]
        else -> null
    }
}

private class DateHit(val start: Int, val end: Int, val date: LocalDate, val namesDate: Boolean = true)

private fun findDate(input: String, today: LocalDate, consumed: List<IntRange>): DateHit? {
    val hits = mutableListOf<DateHit>()

    DAY_WORD_RE.findAll(input).forEach { m ->
        val days = DAY_WORDS[m.value] ?: return@forEach
        hits += DateHit(m.range.first, m.range.last + 1, today.plus(days, DateTimeUnit.DAY))
    }
    WEEKDAY_RE.findAll(input).find { it.groups[2] != null }?.let { m ->
        val target = WEEKDAY_CHARS[m.groupValues[2].first()] ?: return@let
        val modifier = m.groupValues[1]
        val todayIndex = today.dayOfWeek.ordinal + 1
        var delta = (target - todayIndex + 7) % 7
        // A bare weekday means its next occurrence, today counts; "下周" is the week after this one.
        if (modifier == "下" || modifier == "下下") {
            val weeks = if (modifier == "下下") 2 else 1
            delta += weeks * 7
        }
        hits += DateHit(m.range.first, m.range.last + 1, today.plus(delta, DateTimeUnit.DAY))
    }
    RELATIVE_RE.findAll(input).forEach { m ->
        val count = chineseNumber(m.groupValues[1]) ?: return@forEach
        val date = when (m.groupValues[2]) {
            "天" -> today.plus(count, DateTimeUnit.DAY)
            "周", "星期", "礼拜" -> today.plus(count * 7, DateTimeUnit.DAY)
            "月" -> today.plus(count, DateTimeUnit.MONTH)
            else -> today.plus(count, DateTimeUnit.YEAR)
        }
        hits += DateHit(m.range.first, m.range.last + 1, date)
    }
    MONTH_DAY_RE.findAll(input).forEach { m ->
        val year = m.groupValues[1].toIntOrNull() ?: today.year
        val month = m.groupValues[2].toIntOrNull() ?: return@forEach
        val day = m.groupValues[3].toIntOrNull() ?: return@forEach
        if (month !in 1..12 || day !in 1..31) return@forEach
        hits += DateHit(m.range.first, m.range.last + 1, LocalDate(year, month, day))
    }

    return hits
        .filter { hit -> consumed.none { it.first < hit.end && hit.start <= it.last } }
        .minByOrNull { it.start }
}

private class TimeHit(val start: Int, val end: Int, val time: LocalTime?)

private fun findTime(input: String, consumed: List<IntRange>): TimeHit? {
    val hits = mutableListOf<TimeHit>()

    COLON_TIME_RE.findAll(input).forEach { m ->
        val hour = m.groupValues[2].toIntOrNull() ?: return@forEach
        val minute = m.groupValues[3].toIntOrNull() ?: return@forEach
        if (hour !in 0..23 || minute !in 0..59) return@forEach
        val h = applyMeridiem(hour, m.groupValues[1])
        hits += TimeHit(m.range.first, m.range.last + 1, if (h == null) null else LocalTime(h, minute))
    }
    CLOCK_RE.findAll(input).forEach { m ->
        val raw = m.groupValues[2]
        val hour = chineseNumber(raw) ?: return@forEach
        if (hour !in 0..24) return@forEach
        val minute = when {
            m.groupValues[3] == "半" -> 30
            m.groupValues[3].isNotBlank() -> m.groupValues[3].filter { it.isDigit() }.toIntOrNull() ?: 0
            else -> 0
        }
        if (minute !in 0..59) return@forEach
        val h = applyMeridiem(hour % 24, m.groupValues[1]) ?: return@forEach
        hits += TimeHit(m.range.first, m.range.last + 1, LocalTime(h, minute))
    }

    return hits
        .filter { hit -> consumed.none { it.first < hit.end && hit.start <= it.last } }
        .minByOrNull { it.start }
}

/** "下午3点" → 15, "晚上8点" → 20, "中午12点" → 12, "凌晨1点" → 1. */
private fun applyMeridiem(hour: Int, word: String): Int? = when (word) {
    "", null -> hour
    "凌晨" -> if (hour == 12) 0 else hour
    "早上", "早晨", "上午" -> hour
    "中午" -> if (hour in 1..2) hour + 12 else hour
    "下午", "傍晚" -> if (hour < 12) hour + 12 else hour
    "晚上", "夜里", "晚间" -> if (hour < 12) hour + 12 else hour
    else -> hour
}

fun extractDateZh(input: String, consumed: MutableList<IntRange>, options: DateOptions): DateResult {
    val today = options.reference.date
    val date = findDate(input, today, consumed)
    val time = findTime(input, consumed)

    if (date == null && time == null) return DateResult(null, emptyList())
    // A caller looking for a date elsewhere ("every monday 10am") must not take a bare time here.
    if (date == null && options.skipTimeOnly) return DateResult(null, emptyList())

    val tokens = mutableListOf<ParsedToken>()
    var due: LocalDateTime? = null
    var hasTime = false

    val timeValue = time?.time
    val dateValue: LocalDate? = date?.date
    when {
        dateValue != null && timeValue != null -> {
            due = LocalDateTime(dateValue, timeValue)
            hasTime = true
        }
        dateValue != null -> {
            due = LocalDateTime(dateValue, DueDates.DATE_ONLY_TIME)
        }
        timeValue != null -> {
            // A time alone: today when it is still ahead, otherwise tomorrow.
            val day = if (timeValue >= options.reference.time) today else today.plus(1, DateTimeUnit.DAY)
            due = LocalDateTime(day, timeValue)
            hasTime = true
        }
    }

    date?.let {
        consumed.add(it.start until it.end)
        tokens += ParsedToken(TokenType.DATE, it.start, it.end, dateValue, input.substring(it.start, it.end))
    }
    time?.let {
        consumed.add(it.start until it.end)
        tokens += ParsedToken(TokenType.DATE, it.start, it.end, timeValue, input.substring(it.start, it.end))
    }

    // A time-only phrase has no separate date token; the due date carries the day it resolved to.
    return DateResult(due, tokens, hasTime)
}
