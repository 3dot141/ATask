package com.yes365.atask.ui.screens.taskentry

import com.yes365.atask.auth.AuthManager
import com.yes365.atask.auth.FakeNetworkMonitor
import com.yes365.atask.auth.InMemoryTokenStorage
import com.yes365.atask.auth.RecordingAuthHooks
import com.yes365.atask.data.local.BehaviorPrefsStore
import com.yes365.atask.data.local.NlpPrefsStore
import com.yes365.atask.data.local.NotificationPrefsStore
import com.yes365.atask.data.repository.InMemoryPreferencesDataStore
import com.yes365.atask.domain.model.Project
import com.yes365.atask.domain.model.SharedContent
import com.yes365.atask.location.FakeLocationPermission
import com.yes365.atask.location.testLocations
import com.yes365.atask.ui.FakeAttachmentRepository
import com.yes365.atask.ui.FakeLabelRepository
import com.yes365.atask.ui.FakeProjectRepository
import com.yes365.atask.ui.FakeTaskRepository
import com.yes365.atask.util.AppMessages
import com.yes365.atask.util.DayClock
import com.yes365.atask.util.FixedTimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The sheet is recreated around the same view model by a rotation. It asks the view model whether
 * a screen has initialised the draft, so the draft is kept instead of being reset to the defaults
 * or the shared text.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TaskEntryViewModelInitTest {

    @BeforeTest
    fun setMain() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMain() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(authScope: CoroutineScope): TaskEntryViewModel {
        val vm = TaskEntryViewModel(
            taskRepository = FakeTaskRepository(),
            projectRepository = FakeProjectRepository(listOf(Project(id = 1, title = "Inbox"))),
            labelRepository = FakeLabelRepository(),
            attachmentRepository = FakeAttachmentRepository(),
            authManager = AuthManager(
                platformAuthHooks = RecordingAuthHooks(),
                tokenStorage = InMemoryTokenStorage(),
                apiServiceProvider = { error("no network in this test") },
                appScope = authScope,
                networkMonitor = FakeNetworkMonitor(),
            ),
            nlpPrefsStore = NlpPrefsStore(InMemoryPreferencesDataStore()),
            notificationPrefsStore = NotificationPrefsStore(InMemoryPreferencesDataStore()),
            behaviorPrefsStore = BehaviorPrefsStore(InMemoryPreferencesDataStore()),
            appMessages = AppMessages(),
            locations = testLocations(),
            locationPermission = FakeLocationPermission(),
            dayClock = DayClock(
                backgroundScope,
                FixedTimeSource(Clock.System.now(), TimeZone.currentSystemDefault()),
                ticking = false,
            ),
        )
        runCurrent()
        return vm
    }

    @Test
    fun `a new view model has not been initialised`() = runTest {
        val authScope = CoroutineScope(SupervisorJob())
        val vm = viewModel(authScope)

        assertFalse(vm.isInitialized)
        authScope.cancel()
    }

    @Test
    fun `opening the sheet with defaults initialises it`() = runTest {
        val authScope = CoroutineScope(SupervisorJob())
        val vm = viewModel(authScope)

        vm.initWithDefaults(defaultProjectId = 1L)

        assertTrue(vm.isInitialized, "set at once, before the project lookup finishes")
        runCurrent()
        assertEquals(1L, vm.uiState.value.projectId)
        authScope.cancel()
    }

    @Test
    fun `opening the sheet with a share initialises it and fills the draft`() = runTest {
        val authScope = CoroutineScope(SupervisorJob())
        val vm = viewModel(authScope)

        vm.initWithSharedContent(1L, SharedContent(text = "Read this\nlater", subject = "An article"))
        runCurrent()

        assertTrue(vm.isInitialized)
        assertEquals("An article", vm.uiState.value.title)
        assertEquals("Read this\nlater", vm.uiState.value.description)
        authScope.cancel()
    }
}
