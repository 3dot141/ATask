package com.yes365.atask.ui.screens.taskdetail

import com.yes365.atask.auth.AuthManager
import com.yes365.atask.auth.FakeNetworkMonitor
import com.yes365.atask.auth.InMemoryTokenStorage
import com.yes365.atask.auth.RecordingAuthHooks
import com.yes365.atask.data.local.BehaviorPrefsStore
import com.yes365.atask.data.local.NlpPrefsStore
import com.yes365.atask.data.local.TempIdGenerator
import com.yes365.atask.data.repository.InMemoryPreferencesDataStore
import com.yes365.atask.domain.model.Project
import com.yes365.atask.domain.model.Task
import com.yes365.atask.ui.FakeAttachmentRepository
import com.yes365.atask.ui.FakeLabelRepository
import com.yes365.atask.ui.FakeProjectRepository
import com.yes365.atask.ui.FakeTaskRepository
import com.yes365.atask.util.AppMessages
import com.yes365.atask.util.DayClock
import com.yes365.atask.util.FakePlatformFiles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** A task made offline (from quick add's Details) stays in the editor when its create goes through. */
@OptIn(ExperimentalCoroutinesApi::class)
class TaskDetailViewModelTempIdTest {

    @BeforeTest
    fun setMain() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMain() {
        Dispatchers.resetMain()
    }

    private val scope = CoroutineScope(SupervisorJob())
    private val tasks = FakeTaskRepository()
    private val tempIds = TempIdGenerator(InMemoryPreferencesDataStore())

    private fun viewModel() = TaskDetailViewModel(
        taskRepository = tasks,
        labelRepository = FakeLabelRepository(),
        attachmentRepository = FakeAttachmentRepository(),
        projectRepository = FakeProjectRepository(listOf(Project(id = 1, title = "Inbox"))),
        authManager = AuthManager(
            platformAuthHooks = RecordingAuthHooks(),
            tokenStorage = InMemoryTokenStorage(),
            apiServiceProvider = { error("no network in this test") },
            appScope = scope,
            networkMonitor = FakeNetworkMonitor(),
        ),
        behaviorPrefsStore = BehaviorPrefsStore(InMemoryPreferencesDataStore()),
        nlpPrefsStore = NlpPrefsStore(InMemoryPreferencesDataStore()),
        platformFiles = FakePlatformFiles(),
        appMessages = AppMessages(),
        dayClock = DayClock(scope, ticking = false),
        tempIds = tempIds,
    )

    @Test
    fun `the editor moves to the server's id and keeps the edits`() = runTest {
        tasks.put(Task(id = -3, title = "Call mom", projectId = 1))
        val vm = viewModel()
        vm.loadTask(-3)
        runCurrent()
        vm.updateTitle("Call mom about Sunday")

        // The sync: the create went through as task 77 and the temporary row is gone.
        tempIds.rememberRealId(-3, 77)
        tasks.put(Task(id = 77, title = "Call mom", projectId = 1))
        tasks.remove(-3)
        runCurrent()

        val state = vm.uiState.value
        assertEquals(77, state.requestedTaskId)
        assertEquals(-3, state.followedFromTaskId)
        assertEquals(77, state.task?.id)
        assertEquals("Call mom about Sunday", state.task?.title)
        assertFalse(state.isLoading)
        scope.cancel()
    }

    @Test
    fun `a task deleted elsewhere is not followed anywhere`() = runTest {
        tasks.put(Task(id = -4, title = "Gone", projectId = 1))
        val vm = viewModel()
        vm.loadTask(-4)
        runCurrent()

        tasks.remove(-4)
        runCurrent()

        assertEquals(-4, vm.uiState.value.requestedTaskId)
        assertEquals(0, vm.uiState.value.followedFromTaskId)
        scope.cancel()
    }
}
