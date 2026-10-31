package com.yes365.atask.ui.screens.today

import com.yes365.atask.auth.AuthManager
import com.yes365.atask.auth.FakeNetworkMonitor
import com.yes365.atask.auth.InMemoryTokenStorage
import com.yes365.atask.auth.RecordingAuthHooks
import com.yes365.atask.ui.fakeScreenRefresher
import com.yes365.atask.domain.model.OccurrenceStatus
import com.yes365.atask.domain.model.Project
import com.yes365.atask.domain.model.Routine
import com.yes365.atask.domain.model.RoutineDay
import com.yes365.atask.domain.model.RoutineDraft
import com.yes365.atask.domain.model.RoutineOccurrenceRecord
import com.yes365.atask.domain.model.Task
import com.yes365.atask.domain.repository.RoutineParseIssue
import com.yes365.atask.domain.repository.RoutineRepository
import com.yes365.atask.ui.FakeLabelRepository
import com.yes365.atask.ui.FakeProjectRepository
import com.yes365.atask.ui.FakeTaskRepository
import com.yes365.atask.util.CrossAppFixture
import com.yes365.atask.util.CrossAppFixture.fixture
import com.yes365.atask.util.CrossAppFixture.local
import com.yes365.atask.util.DayClock
import com.yes365.atask.util.NetworkResult
import com.yes365.atask.util.SchedulerTimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import com.yes365.atask.data.local.RoutinePrefsStore
import com.yes365.atask.data.repository.InMemoryPreferencesDataStore

/**
 * The Today list has an Overdue section (local date before today) above the Today section (local
 * date equal to today), built from the shared fixture tasks, and both follow the day at midnight.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelOverdueTest {

    @BeforeTest
    fun setMain() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMain() {
        Dispatchers.resetMain()
    }

    private class EmptyRoutines : RoutineRepository {
        override fun observeActive(): Flow<List<Routine>> = flowOf(emptyList())
        override fun observeArchived(): Flow<List<Routine>> = flowOf(emptyList())
        override fun observeRoutine(routineId: String): Flow<Routine?> = flowOf(null)
        override fun observeDay(date: String): Flow<RoutineDay> = flowOf(RoutineDay(date, emptyList()))
        override fun observeHistory(routineId: String): Flow<List<RoutineOccurrenceRecord>> = flowOf(emptyList())
        override fun observeIssues(): Flow<List<RoutineParseIssue>> = flowOf(emptyList())
        override suspend fun create(draft: RoutineDraft): NetworkResult<Routine> = NetworkResult.Error("not faked")
        override suspend fun update(routineId: String, draft: RoutineDraft): NetworkResult<Routine> =
            NetworkResult.Error("not faked")

        override suspend fun setOccurrenceStatus(
            routineId: String,
            date: String,
            slotId: String,
            status: OccurrenceStatus,
            note: String,
        ): NetworkResult<Routine> = NetworkResult.Error("not faked")

        override suspend fun archive(routineId: String, archived: Boolean): NetworkResult<Routine> =
            NetworkResult.Error("not faked")

        override suspend fun deletePermanently(routineId: String): NetworkResult<Unit> = NetworkResult.Success(Unit)
        override suspend fun finalizeAndPrune(): NetworkResult<Unit> = NetworkResult.Success(Unit)
        override suspend fun exportCsv(): String = ""
    }

    private class Rig(val vm: TodayViewModel, val tasks: FakeTaskRepository, val authScope: CoroutineScope)

    private fun TestScope.rig(now: String, zone: TimeZone): Rig {
        val tasks = FakeTaskRepository()
        val authScope = CoroutineScope(SupervisorJob())
        val time = SchedulerTimeSource(testScheduler, local(now, zone), zone)
        val projects = FakeProjectRepository(
            listOf(Project(id = 10, title = "Alpha"), Project(id = 11, title = "Beta"), Project(id = 12, title = "Gamma")),
        )
        val labels = FakeLabelRepository()
        val vm = TodayViewModel(
            taskRepository = tasks,
            projectRepository = projects,
            labelRepository = labels,
            routineRepository = EmptyRoutines(),
            routinePrefsStore = RoutinePrefsStore(InMemoryPreferencesDataStore()),
            authManager = AuthManager(
                platformAuthHooks = RecordingAuthHooks(),
                tokenStorage = InMemoryTokenStorage(),
                apiServiceProvider = { error("no network in this test") },
                appScope = authScope,
                networkMonitor = FakeNetworkMonitor(),
            ),
            refresher = fakeScreenRefresher(tasks, projects, labels),
            dayClock = DayClock(backgroundScope, time),
        )
        return Rig(vm, tasks, authScope)
    }

    /** The open fixture tasks that are due on or before the fixture's first smart-list day. */
    private fun dueTasks(zone: TimeZone): List<Task> =
        fixture.tasks
            .filter { !it.done && it.due != null && it.due <= "2026-10-06T23:59:59" }
            .map {
                Task(
                    id = it.id,
                    title = "Task ${it.id}",
                    projectId = it.projectId,
                    dueDate = local(it.due!!, zone).toString(),
                )
            }

    private fun TodayUiState.overdueIds() = overdueGroups.flatMap { g -> g.tasks.map { it.id } }.sorted()
    private fun TodayUiState.todayIds() = projectGroups.flatMap { g -> g.tasks.map { it.id } }.sorted()

    @Test
    fun `overdue and today are separate sections built from the shared fixture`() = runTest {
        val vector = fixture.smartLists.first { it.today == "2026-10-06" }
        for (zone in CrossAppFixture.zones) {
            val rig = rig("2026-10-06T10:00:00", zone)
            rig.tasks.todayTasks.value = dueTasks(zone)
            runCurrent()

            assertEquals(vector.todayOverdue.sorted(), rig.vm.uiState.value.overdueIds(), "overdue in $zone")
            assertEquals(vector.todayToday.sorted(), rig.vm.uiState.value.todayIds(), "today in $zone")
            rig.authScope.cancel()
        }
    }

    @Test
    fun `a task due earlier today is in the Today section - not overdue`() = runTest {
        val zone = TimeZone.of("Europe/Amsterdam")
        val rig = rig("2026-10-06T10:00:00", zone)
        rig.tasks.todayTasks.value = dueTasks(zone)
        runCurrent()

        // Task 3 is due at 08:00 today.
        assertEquals(true, 3L in rig.vm.uiState.value.todayIds())
        assertEquals(false, 3L in rig.vm.uiState.value.overdueIds())
        rig.authScope.cancel()
    }

    @Test
    fun `the next upcoming task is the first of the nearest day and follows the day`() = runTest {
        val zone = TimeZone.of("Europe/Amsterdam")
        val rig = rig("2026-10-06T10:00:00", zone)
        rig.tasks.todayTasks.value = emptyList()
        rig.tasks.upcomingTasks.value = listOf(
            Task(id = 21, title = "Later", projectId = 10, dueDate = local("2026-10-09T23:59:59", zone).toString()),
            Task(id = 22, title = "Sooner", projectId = 11, dueDate = local("2026-10-07T23:59:59", zone).toString()),
        )
        runCurrent()
        assertEquals(22L, rig.vm.uiState.value.nextUpcoming?.id)

        rig.tasks.upcomingTasks.value = emptyList()
        runCurrent()
        assertEquals(null, rig.vm.uiState.value.nextUpcoming)
        rig.authScope.cancel()
    }

    @Test
    fun `at midnight today's tasks move to the Overdue section`() = runTest {
        val zone = TimeZone.of("Europe/Amsterdam")
        val rig = rig("2026-10-06T23:30:00", zone)
        rig.tasks.todayTasks.value = dueTasks(zone)
        runCurrent()
        assertEquals(listOf(2L, 3L, 13L), rig.vm.uiState.value.todayIds())

        advanceTimeBy(31.minutes)
        runCurrent()

        val vector = fixture.smartLists.first { it.today == "2026-10-07" }
        assertEquals(vector.todayOverdue.sorted(), rig.vm.uiState.value.overdueIds())
        assertEquals(emptyList(), rig.vm.uiState.value.todayIds())
        rig.authScope.cancel()
    }
}
