package com.rendyhd.vicu.device

/** An event read from the phone's own calendar, already flattened for display. */
data class DeviceEvent(
    val id: String,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val calendarName: String,
)

/** Whether the app may read the device calendar. */
enum class CalendarAccess { NotDetermined, Denied, Granted }

/**
 * The phone's calendar, read-only.
 *
 * iOS answers from EventKit; Android has no implementation in this build (access reads as granted
 * with no events), which keeps the shared screens free of `expect` branches.
 */
expect class DeviceCalendar() {
    fun access(): CalendarAccess
    suspend fun requestAccess(): CalendarAccess

    /** The events that overlap [startMillis, endMillis), in start order. */
    suspend fun events(startMillis: Long, endMillis: Long): List<DeviceEvent>
}
