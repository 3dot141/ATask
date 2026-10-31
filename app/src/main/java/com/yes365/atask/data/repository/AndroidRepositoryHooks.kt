package com.yes365.atask.data.repository

import android.content.Context
import com.yes365.atask.domain.model.Task
import com.yes365.atask.domain.repository.PlatformRepositoryHooks
import com.yes365.atask.notification.AlarmScheduler
import com.yes365.atask.notification.DailySummaryScheduler
import com.yes365.atask.notification.RoutineAlarmScheduler
import com.yes365.atask.util.CompletionSoundPlayer
import com.yes365.atask.widget.WidgetUpdateScheduler
import com.yes365.atask.widget.RoutineWidget
import com.yes365.atask.widget.WidgetConfigStore
import com.yes365.atask.worker.PeriodicSyncScheduler
import com.yes365.atask.worker.RoutineMaintenanceScheduler
import com.yes365.atask.worker.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AndroidRepositoryHooks(
    private val context: Context,
    private val alarmScheduler: AlarmScheduler,
    private val routineAlarmSchedulerProvider: () -> RoutineAlarmScheduler,
    private val completionSoundPlayer: CompletionSoundPlayer,
    private val appScope: CoroutineScope,
    private val dailySummaryScheduler: DailySummaryScheduler,
) : PlatformRepositoryHooks {

    override fun triggerSync() {
        SyncScheduler.enqueueWhenOnline(context)
    }

    override fun updateWidgets() {
        WidgetUpdateScheduler.enqueueImmediateUpdateAll(context)
    }

    override fun playCompletionSound() {
        appScope.launch(Dispatchers.IO) {
            completionSoundPlayer.play()
        }
    }

    override suspend fun scheduleAlarm(task: Task) {
        alarmScheduler.scheduleForTask(task)
    }

    override suspend fun cancelAlarm(taskId: Long) {
        alarmScheduler.cancelForTask(taskId)
    }

    override suspend fun rescheduleAlarms() {
        alarmScheduler.rescheduleAll()
        routineAlarmSchedulerProvider().rescheduleAll()
    }

    override suspend fun updateAlarms(changed: List<Task>, removedTaskIds: Set<Long>) {
        alarmScheduler.updateAlarms(changed, removedTaskIds)
    }

    override suspend fun cancelAllAlarms() {
        alarmScheduler.cancelAll()
        routineAlarmSchedulerProvider().cancelAll()
    }

    override suspend fun cancelAccountBackgroundWork() {
        dailySummaryScheduler.cancel(DailySummaryScheduler.SLOT_MORNING)
        dailySummaryScheduler.cancel(DailySummaryScheduler.SLOT_AFTERNOON)
        RoutineMaintenanceScheduler.cancel(context)
        PeriodicSyncScheduler.cancel(context)
    }

    override suspend fun clearWidgetConfigurations() {
        WidgetConfigStore.clearAll(context)
    }

    override suspend fun routinesChanged() {
        routineAlarmSchedulerProvider().rescheduleAll()
        RoutineWidget().updateAllWidgets(context)
        updateWidgets()
    }
}
