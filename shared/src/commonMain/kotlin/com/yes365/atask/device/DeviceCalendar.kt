package com.yes365.atask.device

/** An event read from the phone's own calendar, already flattened for display. */
data class DeviceEvent(
    val id: String,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val calendarName: String,
    /** The calendar's own colour as ARGB; 0 when unknown (the row falls back to the theme colour). */
    val colorArgb: Long = 0L,
)

/** One calendar of the phone, for the "which calendars" setting. */
data class DeviceCalendarInfo(
    val id: String,
    val title: String,
    val source: String,
    /** The calendar's own colour as ARGB; 0 when unknown. */
    val colorArgb: Long = 0L,
)

/** Whether the app may read the device calendar. */
enum class CalendarAccess { NotDetermined, Denied, Granted }

/**
 * The phone's calendar, read-only.
 *
 * iOS answers from EventKit; Android has no implementation in this build (access reads as granted
 * with no calendars and no events), which keeps the shared screens free of `expect` branches.
 */
expect class DeviceCalendar() {
    fun access(): CalendarAccess
    suspend fun requestAccess(): CalendarAccess

    /** The calendars on the phone, for the visibility setting. Empty when access is not granted. */
    suspend fun calendars(): List<DeviceCalendarInfo>

    /**
     * The events that overlap [startMillis, endMillis), in start order, leaving out the calendars
     * whose identifier is in [excludedCalendarIds].
     */
    suspend fun events(startMillis: Long, endMillis: Long, excludedCalendarIds: Set<String>): List<DeviceEvent>
}
