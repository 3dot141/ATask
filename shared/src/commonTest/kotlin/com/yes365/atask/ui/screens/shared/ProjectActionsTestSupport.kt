package com.yes365.atask.ui.screens.shared

import com.yes365.atask.auth.AuthManager
import com.yes365.atask.auth.FakeNetworkMonitor
import com.yes365.atask.auth.InMemoryTokenStorage
import com.yes365.atask.auth.RecordingAuthHooks
import com.yes365.atask.data.repository.ProjectCategoryPreferences
import com.yes365.atask.data.repository.RecordingRepositoryHooks
import com.yes365.atask.domain.repository.PlatformRepositoryHooks
import com.yes365.atask.domain.repository.ProjectRepository
import com.yes365.atask.util.DayClock
import com.yes365.atask.util.FixedTimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

/** An [AuthManager] with nothing behind it: it holds the Inbox id and never goes to the network. */
fun offlineAuthManager(
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
): AuthManager = AuthManager(
    platformAuthHooks = RecordingAuthHooks(),
    tokenStorage = InMemoryTokenStorage(),
    apiServiceProvider = { error("no network in this test") },
    appScope = scope,
    networkMonitor = FakeNetworkMonitor(),
)

/** [ProjectActions] over [projects], for view model tests. Today is 2026-10-10 in UTC unless [now] says otherwise. */
fun testProjectActions(
    projects: ProjectRepository,
    auth: AuthManager = offlineAuthManager(),
    hooks: PlatformRepositoryHooks = RecordingRepositoryHooks(),
    now: Instant = Instant.parse("2026-10-10T10:00:00Z"),
    categories: ProjectCategoryPreferences? = null,
): ProjectActions = ProjectActions(
    projectRepository = projects,
    authManager = auth,
    repositoryHooks = hooks,
    dayClock = DayClock(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        time = FixedTimeSource(now, TimeZone.UTC),
        ticking = false,
    ),
    categoryPreferences = categories,
)
