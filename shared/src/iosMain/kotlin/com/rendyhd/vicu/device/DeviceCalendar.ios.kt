package com.rendyhd.vicu.device

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.EventKit.EKAuthorizationStatusAuthorized
import platform.EventKit.EKAuthorizationStatusDenied
import platform.EventKit.EKAuthorizationStatusFullAccess
import platform.EventKit.EKAuthorizationStatusRestricted
import platform.EventKit.EKEntityType
import platform.EventKit.EKEvent
import platform.EventKit.EKEventStore
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970
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

    actual suspend fun events(startMillis: Long, endMillis: Long): List<DeviceEvent> {
        if (access() != CalendarAccess.Granted) return emptyList()
        val start = NSDate.dateWithTimeIntervalSince1970(startMillis / 1000.0)
        val end = NSDate.dateWithTimeIntervalSince1970(endMillis / 1000.0)
        val predicate = store.predicateForEventsWithStartDate(start, end, null)
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
                )
            }
            .sortedBy { it.startMillis }
    }

}
