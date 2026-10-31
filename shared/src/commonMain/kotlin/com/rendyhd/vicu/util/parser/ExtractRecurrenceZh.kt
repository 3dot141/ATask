package com.rendyhd.vicu.util.parser

/**
 * The Chinese recurrence phrases: 每天 / 每周 / 每月 / 每年, 每N天, 每隔一天, and 每周一.
 *
 * Like the Chinese date path, nothing is consumed and no token is emitted: the words stay in the
 * title, only the recurrence is taken. "每周一" returns the weekday's region so the caller can use
 * it as the due date, the same contract as the English "every monday".
 */
private val CN_UNIT = mapOf(
    "天" to RecurrenceUnit.DAY,
    "日" to RecurrenceUnit.DAY,
    "周" to RecurrenceUnit.WEEK,
    "週" to RecurrenceUnit.WEEK,
    "星期" to RecurrenceUnit.WEEK,
    "礼拜" to RecurrenceUnit.WEEK,
    "月" to RecurrenceUnit.MONTH,
    "年" to RecurrenceUnit.YEAR,
)

private val CN_EVERY_UNIT_RE = Regex("每\\s*(\\d+|[一二两三四五六七八九十]+)?\\s*个?\\s*(天|日|周|週|星期|礼拜|月|年)")
private val CN_EVERY_OTHER_DAY_RE = Regex("每隔\\s*([一二两三四五六七八九十]+|\\d+)\\s*天")
private val CN_EVERY_WEEKDAY_RE = Regex("每\\s*(?:周|週|星期|礼拜)\\s*([一二三四五六日天])")

fun extractRecurrenceZh(input: String, consumed: List<IntRange>): RecurrenceResult? {
    if (!containsCjk(input)) return null
    fun free(start: Int, end: Int) = consumed.none { it.first < end && start <= it.last }

    // 每隔一天 → every other day.
    CN_EVERY_OTHER_DAY_RE.find(input)?.let { m ->
        if (!free(m.range.first, m.range.last + 1)) return@let
        val n = chineseDigit(m.groupValues[1])
        if (n != null) {
            return RecurrenceResult(ParsedRecurrence(n + 1, RecurrenceUnit.DAY), emptyList())
        }
    }

    // 每周一 → weekly, and the weekday is the due date.
    CN_EVERY_WEEKDAY_RE.find(input)?.let { m ->
        if (free(m.range.first, m.range.last + 1)) {
            return RecurrenceResult(
                ParsedRecurrence(1, RecurrenceUnit.WEEK),
                emptyList(),
                weekday = m.range,
            )
        }
    }

    // 每N天 / 每周 / 每月 / 每年.
    CN_EVERY_UNIT_RE.find(input)?.let { m ->
        if (!free(m.range.first, m.range.last + 1)) return@let
        val unit = CN_UNIT[m.groupValues[2]] ?: return@let
        val raw = m.groupValues[1]
        val interval = if (raw.isEmpty()) 1 else (raw.toIntOrNull() ?: chineseDigit(raw)) ?: return@let
        if (interval < 1) return@let
        return RecurrenceResult(ParsedRecurrence(interval, unit), emptyList())
    }

    return null
}

/** "一"…"十二", "两" and plain digits. */
internal fun chineseDigit(text: String): Int? {
    text.toIntOrNull()?.let { return it }
    val digits = mapOf(
        '一' to 1, '二' to 2, '两' to 2, '三' to 3, '四' to 4,
        '五' to 5, '六' to 6, '七' to 7, '八' to 8, '九' to 9,
    )
    return when {
        text == "十" -> 10
        text.startsWith("十") -> digits[text.getOrNull(1)]?.plus(10)
        text.endsWith("十") -> digits[text[0]]?.times(10)
        text.length == 3 && text[1] == '十' -> {
            val tens = digits[text[0]] ?: return null
            val ones = digits[text[2]] ?: return null
            tens * 10 + ones
        }
        text.length == 1 -> digits[text[0]]
        else -> null
    }
}
