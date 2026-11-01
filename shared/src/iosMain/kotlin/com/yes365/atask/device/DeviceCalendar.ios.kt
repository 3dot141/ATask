package com.yes365.atask.device

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreGraphics.CGFloat
import platform.CoreGraphics.CGFloatVar
import platform.EventKit.EKAuthorizationStatusAuthorized
import platform.EventKit.EKAuthorizationStatusDenied
import platform.EventKit.EKAuthorizationStatusFullAccess
import platform.EventKit.EKAuthorizationStatusRestricted
import platform.EventKit.EKEntityType
import platform.EventKit.EKCalendar
import platform.EventKit.EKEvent
import platform.EventKit.EKEventStore
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIColor
import kotlin.coroutines.resume

/**
 * EventKit, read-only.
 *
 * `requestFullAccessToEvents` is the iOS 17 call (the deployment target), and `eventsMatching`
 * asks for a predicate over the visible range only, so a large calendar is never read whole.
 */
@OptIn(ExperimentalForeignApi::class)
actual class DeviceCalendar actual constructor() {

    private val store = EKEventStore()

    /** A calendar colour as ARGB (0 when the colour cannot be read). */
    private fun argbOf(color: UIColor?): Long {
        if (color == null) return 0L
        return memScoped {
            val red: CGFloatVar = alloc()
            val green: CGFloatVar = alloc()
            val blue: CGFloatVar = alloc()
            val alpha: CGFloatVar = alloc()
            if (!color.getRed(red.ptr, green.ptr, blue.ptr, alpha.ptr)) return@memScoped 0L
            fun channel(value: CGFloat): Long = ((value.coerceIn(0.0, 1.0) * 255) + 0.5).toLong()
            (channel(alpha.value) shl 24) or (channel(red.value) shl 16) or
                (channel(green.value) shl 8) or channel(blue.value)
        }
    }

    actual fun access(): CalendarAccess =
        when (EKEventStore.authorizationStatusForEntityType(EKEntityType.EKEntityTypeEvent)) {
            EKAuthorizationStatusFullAccess,
            EKAuthorizationStatusAuthorized -> CalendarAccess.Granted
            EKAuthorizationStatusDenied,
            EKAuthorizationStatusRestricted -> CalendarAccess.Denied
            else -> CalendarAccess.NotDetermined
        }

    actual suspend fun requestAccess(): CalendarAccess {
        if (access() == CalendarAccess.Granted) return CalendarAccess.Granted
        return suspendCancellableCoroutine { cont ->
            store.requestFullAccessToEventsWithCompletion { granted, _ ->
                cont.resume(if (granted) CalendarAccess.Granted else CalendarAccess.Denied)
            }
        }
    }

    actual suspend fun calendars(): List<DeviceCalendarInfo> {
        if (access() != CalendarAccess.Granted) return emptyList()
        return store.calendarsForEntityType(EKEntityType.EKEntityTypeEvent)
            .filterIsInstance<EKCalendar>()
            .map { calendar ->
                DeviceCalendarInfo(
                    id = calendar.calendarIdentifier,
                    title = calendar.title,
                    source = calendar.source?.title.orEmpty(),
                    // The ObjC property keeps its original spelling (Swift's cgColor).
                    colorArgb = argbOf(UIColor.colorWithCGColor(calendar.CGColor)),
                )
            }
            .sortedBy { it.title.lowercase() }
    }

    actual suspend fun events(
        startMillis: Long,
        endMillis: Long,
        excludedCalendarIds: Set<String>,
    ): List<DeviceEvent> {
        if (access() != CalendarAccess.Granted) return emptyList()
        val start = NSDate.dateWithTimeIntervalSince1970(startMillis / 1000.0)
        val end = NSDate.dateWithTimeIntervalSince1970(endMillis / 1000.0)
        // null asks EventKit for every calendar; otherwise the predicate takes only the shown ones.
        val calendars = if (excludedCalendarIds.isEmpty()) {
            null
        } else {
            store.calendarsForEntityType(EKEntityType.EKEntityTypeEvent)
                .filterIsInstance<EKCalendar>()
                .filter { it.calendarIdentifier !in excludedCalendarIds }
        }
        val predicate = store.predicateForEventsWithStartDate(start, end, calendars)
        return store.eventsMatchingPredicate(predicate)
            .orEmpty()
            .filterIsInstance<EKEvent>()
            .map { event ->
                DeviceEvent(
                    id = event.eventIdentifier.orEmpty(),
                    title = event.title.orEmpty(),
                    startMillis = ((event.startDate?.timeIntervalSince1970 ?: 0.0) * 1000).toLong(),
                    endMillis = ((event.endDate?.timeIntervalSince1970 ?: 0.0) * 1000).toLong(),
                    allDay = event.allDay,
                    calendarName = event.calendar?.title.orEmpty(),
                    colorArgb = argbOf(event.calendar?.CGColor?.let { UIColor.colorWithCGColor(it) }),
                )
            }
            .sortedBy { it.startMillis }
    }

}
