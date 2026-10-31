package com.yes365.atask.data.repository

import com.yes365.atask.domain.model.Task
import com.yes365.atask.domain.repository.PlatformRepositoryHooks
import com.yes365.atask.ios.IosBackgroundTasks
import com.yes365.atask.notification.IosNotificationScheduler
import com.yes365.atask.share.IosShareInbox
import com.yes365.atask.util.IosCompletionSoundPlayer
import com.yes365.atask.widget.IosWidgetSnapshots
import com.yes365.atask.worker.SyncRequester
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The iOS side of the repositories: sync requests go to [SyncRequester], the completion sound to
 * [IosCompletionSoundPlayer]. Reminders are not set one task at a time as on Android: every
 * change asks [IosNotificationScheduler] to plan the whole set again (debounced), since iOS keeps
 * at most 64 pending notifications and the daily summaries are planned ahead from the same data.
 */
class IosRepositoryHooks(
    private val syncRequester: () -> SyncRequester,
    private val soundPlayer: IosCompletionSoundPlayer,
    private val notifications: () -> IosNotificationScheduler,
    private val backgroundTasks: () -> IosBackgroundTasks,
    private val widgets: () -> IosWidgetSnapshots,
    private val shareInbox: () -> IosShareInbox,
    private val appScope: CoroutineScope,
) : PlatformRepositoryHooks {

    override fun triggerSync() {
        syncRequester().request()
    }

    /** Task data changed: the widgets show it and the summaries planned ahead count it. */
    override fun updateWidgets() {
        widgets().requestWrite()
        notifications().requestReplan()
    }

    override fun playCompletionSound() {
        appScope.launch { soundPlayer.play() }
    }

    override suspend fun scheduleAlarm(task: Task) {
        notifications().requestReplan()
    }

    override suspend fun cancelAlarm(taskId: Long) {
        notifications().requestReplan()
    }

    override suspend fun rescheduleAlarms() {
        notifications().requestReplan()
    }

    override suspend fun updateAlarms(changed: List<Task>, removedTaskIds: Set<Long>) {
        notifications().requestReplan()
    }

    override suspend fun cancelAllAlarms() {
        notifications().cancelAll()
    }

    override suspend fun cancelAccountBackgroundWork() {
        backgroundTasks().cancelAll()
    }

    /**
     * WidgetKit keeps each widget's configuration, so it cannot be cleared as on Android: the
     * account's tag is retired instead, and widgets set to its projects and lists say to choose
     * another.
     */
    override suspend fun clearWidgetConfigurations() {
        widgets().retireAccount()
    }

    override suspend fun discardPendingShares() {
        shareInbox().discardAll()
    }

    override suspend fun routinesChanged() {
        widgets().requestWrite()
        notifications().requestReplan()
    }
}
