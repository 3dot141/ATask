package com.yes365.atask.device

/**
 * No device-calendar sync in the Android build yet: the screen shows tasks only. Kept as a real
 * implementation rather than a compile-time branch so the shared calendar screen has one shape.
 */
actual class DeviceCalendar actual constructor() {
    actual fun access(): CalendarAccess = CalendarAccess.Granted
    actual suspend fun requestAccess(): CalendarAccess = CalendarAccess.Granted
    actual suspend fun calendars(): List<DeviceCalendarInfo> = emptyList()

    actual suspend fun events(
        startMillis: Long,
        endMillis: Long,
        excludedCalendarIds: Set<String>,
    ): List<DeviceEvent> = emptyList()
}
