package com.yes365.atask.util

import com.yes365.atask.auth.AuthManager
import com.yes365.atask.auth.FakeNetworkMonitor
import com.yes365.atask.auth.InMemoryTokenStorage
import com.yes365.atask.auth.RecordingAuthHooks
import com.yes365.atask.data.local.NlpPrefsStore
import com.yes365.atask.data.local.NotificationPrefsStore
import com.yes365.atask.data.remote.createApiJson
import com.yes365.atask.data.repository.InMemoryPreferencesDataStore
import com.yes365.atask.location.GeoPoint
import com.yes365.atask.location.LocationAccess
import com.yes365.atask.location.LocationPermissionPlatform
import com.yes365.atask.location.LocationReminder
import com.yes365.atask.location.SyncedLocation
import com.yes365.atask.location.testLocations
import com.yes365.atask.data.repository.InMemorySyncedEntries
import com.yes365.atask.location.LocationTrigger
import com.yes365.atask.domain.model.Label
import com.yes365.atask.domain.model.Project
import com.yes365.atask.domain.model.Task
import com.yes365.atask.ui.FakeLabelRepository
import com.yes365.atask.ui.FakeProjectRepository
import com.yes365.atask.ui.FakeTaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

/** A voice command makes a task the way quick add reads the same words. */
class TextTaskCreatorTest {

    private val scope = CoroutineScope(SupervisorJob())
    private val zone = TimeZone.of("Europe/Amsterdam")
    // Tuesday 20 October 2026, 10:00 in Amsterdam.
    private val time = MutableTimeSource(Instant.parse("2026-10-20T08:00:00Z"), zone)
    private val storage = InMemoryTokenStorage(initialUserId = 1)
    private val tasks = FakeTaskRepository().apply {
        createHandler = { task -> NetworkResult.Success(task.copy(id = -1)) }
    }
    private val labels = FakeLabelRepository()
    private val format = DateDisplayFormat.system(is24Hour = true)
    private val synced = InMemorySyncedEntries()
    private val locations = testLocations(synced)
    private val locationAccess = MutableStateFlow(LocationAccess.ALWAYS)
    private val permission = object : LocationPermissionPlatform {
        override val access: StateFlow<LocationAccess> = locationAccess
        override fun refresh() {}
        override fun request() = true
        override fun openSettings() {}
    }

    @AfterTest
    fun close() = scope.cancel()

    private suspend fun creator(signedIn: Boolean = true, inbox: Long? = 1): TextTaskCreator {
        if (signedIn) {
            storage.storeVikunjaUrl("https://vikunja.example")
            storage.storeApiToken("test-token", 0)
        }
        inbox?.let { storage.storeInboxProjectId(it) }
        val auth = AuthManager(
            platformAuthHooks = RecordingAuthHooks(),
            tokenStorage = storage,
            apiServiceProvider = { error("no network in this test") },
            appScope = scope,
            networkMonitor = FakeNetworkMonitor(),
        )
        auth.initialize()
        return TextTaskCreator(
            taskRepository = tasks,
            projectRepository = FakeProjectRepository(listOf(Project(id = 1, title = "Inbox"), Project(id = 2, title = "Work"))),
            labelRepository = labels,
            authManager = auth,
            nlpPrefsStore = NlpPrefsStore(InMemoryPreferencesDataStore()),
            notificationPrefsStore = NotificationPrefsStore(InMemoryPreferencesDataStore()),
            dayClock = DayClock(scope, time, ticking = false),
            time = time,
            locations = locations,
            locationPermission = permission,
        )
    }

    private fun created(): Task = tasks.created.single()

    @Test
    fun `the sentence is read like quick add and the task is queued in the Inbox`() = runTest {
        val result = creator().create("Call mom tomorrow 3pm", format)

        assertIs<TextTaskCreator.Result.Created>(result)
        val task = created()
        assertEquals("Call mom", task.title)
        assertEquals(1, task.projectId)
        assertEquals(
            LocalDate(2026, 10, 21),
            Instant.parse(task.dueDate).toLocalDateTime(zone).date,
        )
        assertEquals("Added “Call mom”, tomorrow, 15:00", result.confirmation)
    }

    @Test
    fun `a project named in the text gets the task and the reply says so`() = runTest {
        val result = creator().create("Write the report #Work", format)

        assertIs<TextTaskCreator.Result.Created>(result)
        assertEquals(2, created().projectId)
        assertEquals("Added “Write the report” to Work", result.confirmation)
    }

    @Test
    fun `the list it was said in gives its project and date`() = runTest {
        val today = DueDates.today(LocalDate(2026, 10, 20), zone).toString()
        val result = creator().create("Call mom", format, defaultProjectId = 2, defaultDueDate = today)

        assertIs<TextTaskCreator.Result.Created>(result)
        assertEquals(2, created().projectId)
        assertEquals(today, created().dueDate)
        assertEquals("Added “Call mom” to Work, today", result.confirmation)
    }

    @Test
    fun `a date in the words wins over the list date`() = runTest {
        val today = DueDates.today(LocalDate(2026, 10, 20), zone).toString()
        creator().create("Call mom tomorrow", format, defaultProjectId = 2, defaultDueDate = today)

        assertEquals(2, created().projectId)
        assertEquals(LocalDate(2026, 10, 21), Instant.parse(created().dueDate).toLocalDateTime(zone).date)
    }

    @Test
    fun `a list project that is gone sends the task to the Inbox`() = runTest {
        creator().create("Call mom", format, defaultProjectId = 99)

        assertEquals(1, created().projectId)
    }

    @Test
    fun `a label is matched by name - and one that does not exist is created`() = runTest {
        labels.labels.value = listOf(Label(id = 7, title = "Errands", hexColor = ""))

        creator().create("Buy milk @errands @garden", format)

        assertEquals(listOf("garden"), labels.created.map { it.title })
        assertTrue(created().title == "Buy milk")
    }

    @Test
    fun `nothing is made when signed out or without an Inbox`() = runTest {
        assertIs<TextTaskCreator.Result.Failed>(creator(signedIn = false).create("Call mom", format))
        assertTrue(tasks.created.isEmpty())
    }

    @Test
    fun `when I get home sets an arrive reminder at the saved Home`() = runTest {
        val home = locations.addPlace("Home", GeoPoint(52.37, 4.89))

        val result = creator().create("Call mom tomorrow when I get home", format)

        assertIs<TextTaskCreator.Result.Created>(result)
        assertEquals("Call mom", created().title)
        assertEquals(LocalDate(2026, 10, 21), Instant.parse(created().dueDate).toLocalDateTime(zone).date)
        // Created offline (a temporary id): the reminder waits on this phone until the task has its server id.
        val reminder = locations.allReminders().single()
        assertEquals(result.task.id, reminder.taskId)
        assertEquals(false, reminder.synced)
        assertEquals(home.id, reminder.placeId)
        assertEquals(LocationTrigger.ARRIVE, reminder.trigger)
        assertEquals("Added “Call mom”, tomorrow, when you arrive at Home", result.confirmation)
    }

    @Test
    fun `without Always location access the reply says what to change`() = runTest {
        locations.addPlace("Work", GeoPoint(52.34, 4.87))
        locationAccess.value = LocationAccess.WHILE_USING

        val result = creator().create("Take the laptop when I leave work", format)

        assertIs<TextTaskCreator.Result.Created>(result)
        assertEquals(LocationTrigger.LEAVE, locations.allReminders().single().trigger)
        assertEquals("Added “Take the laptop”, when you leave Work. ${TextTaskCreator.NEEDS_ALWAYS}", result.confirmation)
    }

    @Test
    fun `a place that is not saved stays in the title and sets nothing`() = runTest {
        creator().create("Buy milk when I get to the supermarket", format)

        assertEquals("Buy milk when I get to the supermarket", created().title)
        assertTrue(locations.allReminders().isEmpty())
    }

    @Test
    fun `a task the server knows gets a synced reminder`() = runTest {
        tasks.createHandler = { task -> NetworkResult.Success(task.copy(id = 7)) }
        val home = locations.addPlace("Home", GeoPoint(52.37, 4.89))

        creator().create("Call mom when I get home", format)

        assertEquals(
            SyncedLocation.reminderValue(home.id, LocationTrigger.ARRIVE, LocationReminder.DEFAULT_RADIUS_METERS),
            synced.entries.first()[SyncedLocation.reminderKey(7)],
        )
    }

    @Test
    fun `a place saved on another phone sets the reminder and says this phone needs its position`() = runTest {
        synced.set(SyncedLocation.placeKey("elsewhere"), SyncedLocation.placeValue("Work"))

        val result = creator().create("Take the laptop when I leave work", format)

        assertIs<TextTaskCreator.Result.Created>(result)
        assertEquals("elsewhere", locations.allReminders().single().placeId)
        assertEquals("Added “Take the laptop”, when you leave Work. ${TextTaskCreator.notLocated("Work")}", result.confirmation)
    }

    @Test
    fun `an empty sentence makes nothing`() = runTest {
        val result = creator().create("   ", format)

        assertEquals(TextTaskCreator.Result.Failed("Say what the task is."), result)
        assertTrue(tasks.created.isEmpty())
    }
}
