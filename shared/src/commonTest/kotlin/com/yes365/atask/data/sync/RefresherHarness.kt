package com.yes365.atask.data.sync

import com.yes365.atask.auth.CurrentUser
import com.yes365.atask.auth.authTestJson
import com.yes365.atask.data.local.SyncCursorStore
import com.yes365.atask.data.local.entity.PendingActionEntity
import com.yes365.atask.data.mapper.TaskMapper
import com.yes365.atask.data.remote.api.VikunjaApiService
import com.yes365.atask.data.repository.FakePendingActionDao
import com.yes365.atask.data.repository.FakeTaskDao
import com.yes365.atask.data.repository.InMemoryPreferencesDataStore
import com.yes365.atask.data.repository.RecordingRepositoryHooks
import com.yes365.atask.util.MutableTimeSource
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlin.time.Instant

/** A [TaskRefresher] over in-memory fakes and a [TaskListServer]. */
class RefresherHarness(
    val server: TaskListServer = TaskListServer(),
    val taskDao: FakeTaskDao = FakeTaskDao(),
    val pendingActionDao: FakePendingActionDao = FakePendingActionDao(),
    val hooks: RecordingRepositoryHooks = RecordingRepositoryHooks(),
    val time: MutableTimeSource = MutableTimeSource(Instant.parse("2026-10-06T12:00:00Z")),
    val cursorStore: SyncCursorStore = SyncCursorStore(InMemoryPreferencesDataStore()),
    val currentUser: CurrentUser = CurrentUser.fixed(1),
    /** This device's id in routine records; with it a refresh writes lost routine changes again. */
    val routineDeviceId: String? = null,
) {
    private val client = HttpClient(MockEngine { request -> server.handle(this, request) }) {
        install(ContentNegotiation) { json(authTestJson) }
    }
    val api = VikunjaApiService(client, authTestJson)
    val mapper = TaskMapper(authTestJson)

    val refresher = TaskRefresher(
        taskDao = taskDao,
        pendingActionDao = pendingActionDao,
        api = api,
        taskMapper = mapper,
        platformHooks = hooks,
        cursorStore = cursorStore,
        time = time,
        currentUser = currentUser,
        routineDeviceId = routineDeviceId?.let { id -> { id } },
    )

    suspend fun queueTaskAction(taskId: Long, type: String = "update", status: String = "pending") {
        pendingActionDao.insert(
            PendingActionEntity(
                entityType = "task", entityId = taskId, actionType = type, payload = "{}", status = status,
            ),
        )
    }
}
