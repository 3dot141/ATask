package com.yes365.atask.ui

import com.yes365.atask.auth.FakeNetworkMonitor
import com.yes365.atask.data.local.SyncCursorStore
import com.yes365.atask.data.local.entity.PendingActionEntity
import com.yes365.atask.data.repository.FakePendingActionDao
import com.yes365.atask.data.repository.InMemoryPreferencesDataStore
import com.yes365.atask.ui.screens.settings.PlatformSettingsHooks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SyncStateViewModelTest {

    @BeforeTest
    fun setMain() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMain() {
        Dispatchers.resetMain()
    }

    private class RecordingSettingsHooks : PlatformSettingsHooks {
        var immediateSyncs = 0
        override val supportsQuickAddTile = false
        override fun updateWidgets() = Unit
        override fun scheduleSync(enabled: Boolean) = Unit
        override fun scheduleDailySummary(slot: String, enabled: Boolean, hour: Int, minute: Int) = Unit
        override fun sendTestNotification(): String? = null
        override fun requestQuickAddTile(onResult: (String) -> Unit) = Unit
        override fun triggerImmediateSync() {
            immediateSyncs++
        }
    }

    private fun failedAction(id: Long) = PendingActionEntity(
        entityType = "task",
        entityId = id,
        actionType = "update",
        payload = "{}",
        status = "failed",
    )

    @Test
    fun `the failed count follows the queue and retry puts the actions back to pending`() = runTest {
        val dao = FakePendingActionDao()
        val hooks = RecordingSettingsHooks()
        val vm = SyncStateViewModel(FakeNetworkMonitor(), dao, hooks, SyncCursorStore(InMemoryPreferencesDataStore()))
        val failed = mutableListOf<Int>()
        val pending = mutableListOf<Int>()
        backgroundScope.launch { vm.failedCount.collect { failed += it } }
        backgroundScope.launch { vm.pendingCount.collect { pending += it } }
        runCurrent()
        assertEquals(0, failed.last())

        dao.insert(failedAction(11))
        dao.insert(failedAction(12))
        runCurrent()
        assertEquals(2, failed.last())

        vm.retryFailed()
        runCurrent()

        assertEquals(0, failed.last())
        assertEquals(2, pending.last())
        assertEquals(1, hooks.immediateSyncs, "a sync starts right away")
    }

    @Test
    fun `discarding removes the failed actions and syncs so the tasks come back from the server`() = runTest {
        val dao = FakePendingActionDao()
        val hooks = RecordingSettingsHooks()
        val cursor = SyncCursorStore(InMemoryPreferencesDataStore())
        val vm = SyncStateViewModel(FakeNetworkMonitor(), dao, hooks, cursor)
        val failed = mutableListOf<Int>()
        backgroundScope.launch { vm.failedCount.collect { failed += it } }
        dao.insert(failedAction(11))
        runCurrent()
        assertEquals(1, failed.last())

        vm.discardFailed()
        runCurrent()

        assertEquals(0, failed.last())
        assertEquals(emptyList(), dao.snapshot())
        assertEquals(1, hooks.immediateSyncs)
        assertTrue(cursor.begin().cursor.fullReconcileRequested, "only a full reconcile restores the rows the changes protected")
    }
}
