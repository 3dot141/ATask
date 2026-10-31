package com.yes365.atask.worker

import com.yes365.atask.data.local.BehaviorPrefsStore
import com.yes365.atask.data.local.LogbookPrefsStore
import com.yes365.atask.data.local.TempIdGenerator
import com.yes365.atask.data.mapper.TaskMapper
import com.yes365.atask.data.repository.InMemoryPreferencesDataStore
import com.yes365.atask.data.repository.ListPositioner
import com.yes365.atask.data.repository.TaskRepositoryImpl
import com.yes365.atask.data.sync.TaskListServer
import com.yes365.atask.util.DayClock
import com.yes365.atask.worker.SyncEngineHarness.Companion.emptyPage
import com.yes365.atask.auth.authTestJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The sync engine and the repository (screens) share one refresh path: what one pulled, the
 * other does not pull again.
 */
class SharedRefreshPathTest {

    private fun harness(server: TaskListServer): SyncEngineHarness =
        SyncEngineHarness { request ->
            when (request.url.encodedPath) {
                "/labels", "/projects" -> emptyPage()
                else -> server.handle(this, request)
            }
        }

    private fun repositoryOver(h: SyncEngineHarness) = TaskRepositoryImpl(
        taskDao = h.taskDao,
        api = h.api,
        pendingActionDao = h.pendingActionDao,
        taskMapper = TaskMapper(authTestJson),
        platformHooks = h.hooks,
        json = authTestJson,
        behaviorPrefsStore = BehaviorPrefsStore(InMemoryPreferencesDataStore()),
        logbookPrefsStore = LogbookPrefsStore(InMemoryPreferencesDataStore()),
        dayClock = DayClock(CoroutineScope(Job()), ticking = false),
        tempIds = TempIdGenerator(InMemoryPreferencesDataStore()),
        refresher = h.refresher,
        positioner = ListPositioner(h.api, CoroutineScope(Job())),
    )

    @Test
    fun `after the engine reconciled - a screen refresh is incremental`() = runTest {
        val server = TaskListServer().apply { put(1, "A", updated = "2026-10-06T08:00:00Z") }
        val h = harness(server)
        h.engine.performSync()
        val afterSync = server.lists().size
        assertTrue(server.lists().any { it.filter == "done = false" }, "the first sync is a full reconcile")

        repositoryOver(h).refreshAll()

        // The delta, then the one-task page whose total says whether anything was deleted.
        val screenRequests = server.lists().drop(afterSync)
        assertEquals(2, screenRequests.size, screenRequests.toString())
        assertTrue(screenRequests.first().filter!!.startsWith("updated >= "), screenRequests.toString())
        assertEquals("done = false", screenRequests.last().filter)
        assertEquals("1", screenRequests.last().param("per_page"), "a count, not a listing")
        h.close()
    }

    @Test
    fun `after a screen refresh reconciled - the next sync is incremental`() = runTest {
        val server = TaskListServer().apply { put(1, "A", updated = "2026-10-06T08:00:00Z") }
        val h = harness(server)
        repositoryOver(h).refreshAll()
        val afterScreen = server.lists().size

        h.engine.performSync()

        val syncRequests = server.lists().drop(afterScreen)
        assertTrue(syncRequests.none { it.filter == "done = false" && it.param("per_page") != "1" }, "no listing of the open tasks")
        assertFalse(syncRequests.isEmpty(), "the sync still asks what changed")
        h.close()
    }

    @Test
    fun `both paths deliver the same change to the same cache`() = runTest {
        val server = TaskListServer().apply { put(1, "A", updated = "2026-10-06T08:00:00Z") }
        val h = harness(server)
        h.engine.performSync()
        server.put(1, "A renamed", updated = "2026-10-06T10:00:00Z")

        repositoryOver(h).refreshAll()

        assertEquals("A renamed", h.taskDao.entity(1)!!.title)
        h.close()
    }
}
