package com.rendyhd.vicu.util.parser

import com.rendyhd.vicu.util.DueDates
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The Chinese quick-add phrases: a date, a time, or both, and the title left after taking them
 * out. The reference is a fixed Saturday morning so the weekday cases are pinned.
 */
class ChineseDatesTest {

    private val zone = TimeZone.of("Asia/Shanghai")
    private val ref = LocalDateTime(2026, 10, 31, 9, 0) // Saturday

    private fun parse(text: String) = TaskParser.parse(text, reference = ref, zone = zone)

    @Test
    fun `tomorrow with an afternoon time`() {
        val result = parse("明天下午三点开会")
        assertEquals("明天下午三点开会", result.title)
        assertEquals(LocalDateTime(2026, 11, 1, 15, 0), result.dueDate)
        assertTrue(result.dueDateHasTime)
    }

    @Test
    fun `day after tomorrow with a colon time`() {
        val result = parse("后天 10:30 交报告")
        assertEquals("后天 10:30 交报告", result.title)
        assertEquals(LocalDateTime(2026, 11, 2, 10, 30), result.dueDate)
        assertTrue(result.dueDateHasTime)
    }

    @Test
    fun `next week's weekday is a date only`() {
        val result = parse("下周一开会")
        assertEquals("下周一开会", result.title)
        assertEquals(LocalDateTime(LocalDate(2026, 11, 2), DueDates.DATE_ONLY_TIME), result.dueDate)
        assertTrue(!result.dueDateHasTime)
    }

    @Test
    fun `month and day with an evening time`() {
        val result = parse("11月5日晚上8点聚餐")
        assertEquals("11月5日晚上8点聚餐", result.title)
        assertEquals(LocalDateTime(2026, 11, 5, 20, 0), result.dueDate)
    }

    @Test
    fun `relative days`() {
        val result = parse("3天后提醒我交水费")
        assertEquals("3天后提醒我交水费", result.title)
        assertEquals(LocalDateTime(LocalDate(2026, 11, 3), DueDates.DATE_ONLY_TIME), result.dueDate)
    }

    @Test
    fun `today alone is a date`() {
        val result = parse("今天")
        assertEquals(LocalDateTime(LocalDate(2026, 10, 31), DueDates.DATE_ONLY_TIME), result.dueDate)
    }

    @Test
    fun `a time still ahead means today`() {
        val result = parse("下午3点打电话")
        assertEquals("下午3点打电话", result.title)
        assertEquals(LocalDateTime(2026, 10, 31, 15, 0), result.dueDate)
    }

    @Test
    fun `a time already passed means tomorrow`() {
        val result = parse("晚上8点跑步", )
        assertEquals("晚上8点跑步", result.title)
        // 20:00 is ahead of 09:00, so this one is today; check the passed case with a morning time.
        assertEquals(LocalDateTime(2026, 10, 31, 20, 0), result.dueDate)

        val morning = parse("早上7点跑步")
        assertEquals(LocalDateTime(2026, 11, 1, 7, 0), morning.dueDate)
    }

    @Test
    fun `english text keeps using the english parser`() {
        val result = parse("call mom tomorrow at 3pm")
        assertEquals("call mom", result.title)
        assertEquals(LocalDateTime(2026, 11, 1, 15, 0), result.dueDate)
    }

    @Test
    fun `this week's weekday is the calendar week even when it has passed`() {
        // Saturday 31 October: 本周三 is the 28th, 下周三 is the 4th of November.
        assertEquals(
            LocalDateTime(LocalDate(2026, 10, 28), DueDates.DATE_ONLY_TIME),
            parse("本周三交周报").dueDate,
        )
        assertEquals(
            LocalDateTime(LocalDate(2026, 11, 4), DueDates.DATE_ONLY_TIME),
            parse("下周三交周报").dueDate,
        )
    }

    @Test
    fun `chinese recurrence words`() {
        assertEquals(ParsedRecurrence(1, RecurrenceUnit.DAY), parse("每天跑步").recurrence)
        assertEquals(ParsedRecurrence(1, RecurrenceUnit.WEEK), parse("每周复盘").recurrence)
        assertEquals(ParsedRecurrence(1, RecurrenceUnit.MONTH), parse("每月交房租").recurrence)
        assertEquals(ParsedRecurrence(1, RecurrenceUnit.YEAR), parse("每年体检").recurrence)
        assertEquals(ParsedRecurrence(3, RecurrenceUnit.DAY), parse("每3天浇花").recurrence)
        assertEquals(ParsedRecurrence(2, RecurrenceUnit.DAY), parse("每隔一天跑步").recurrence)
        // 每周一 is weekly and the Monday is the due date.
        val weekly = parse("每周一开会")
        assertEquals(ParsedRecurrence(1, RecurrenceUnit.WEEK), weekly.recurrence)
        assertEquals(LocalDateTime(LocalDate(2026, 11, 2), DueDates.DATE_ONLY_TIME), weekly.dueDate)
    }

    @Test
    fun `a task without a date keeps its title`() {
        val result = parse("买牛奶")
        assertNull(result.dueDate)
        assertEquals("买牛奶", result.title)
    }
}
