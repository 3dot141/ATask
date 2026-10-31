package com.yes365.atask.ui.screens.taskentry

import com.yes365.atask.util.RECURRENCE_DAY_SECONDS
import com.yes365.atask.util.RECURRENCE_MODE_FROM_COMPLETION
import com.yes365.atask.util.RECURRENCE_MODE_MONTHLY
import com.yes365.atask.util.RecurrenceValue
import com.yes365.atask.util.parser.ParsedRecurrence
import com.yes365.atask.util.parser.RecurrenceUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class TaskEntryRecurrenceTest {

    @Test
    fun `NLP recurrence is used while picker is untouched`() {
        assertEquals(
            RecurrenceValue(0, RECURRENCE_MODE_MONTHLY),
            resolveTaskEntryRecurrence(
                manualRecurrence = null,
                parserEnabled = true,
                parsedRecurrence = ParsedRecurrence(1, RecurrenceUnit.MONTH),
            ),
        )
    }

    @Test
    fun `manual recurrence overrides NLP recurrence`() {
        val manual = RecurrenceValue(RECURRENCE_DAY_SECONDS, RECURRENCE_MODE_FROM_COMPLETION)

        assertEquals(
            manual,
            resolveTaskEntryRecurrence(
                manualRecurrence = manual,
                parserEnabled = true,
                parsedRecurrence = ParsedRecurrence(2, RecurrenceUnit.WEEK),
            ),
        )
    }

    @Test
    fun `manual None explicitly clears NLP recurrence`() {
        assertEquals(
            RecurrenceValue.NONE,
            resolveTaskEntryRecurrence(
                manualRecurrence = RecurrenceValue.NONE,
                parserEnabled = true,
                parsedRecurrence = ParsedRecurrence(1, RecurrenceUnit.DAY),
            ),
        )
    }

    @Test
    fun `disabled parser does not apply parsed recurrence`() {
        assertEquals(
            RecurrenceValue.NONE,
            resolveTaskEntryRecurrence(
                manualRecurrence = null,
                parserEnabled = false,
                parsedRecurrence = ParsedRecurrence(1, RecurrenceUnit.DAY),
            ),
        )
    }
}
