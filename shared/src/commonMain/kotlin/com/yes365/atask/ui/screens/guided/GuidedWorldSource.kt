package com.yes365.atask.ui.screens.guided

import com.yes365.atask.auth.AuthManager
import com.yes365.atask.data.local.ReviewPrefs
import com.yes365.atask.data.local.ReviewPrefsStore
import com.yes365.atask.data.repository.TaskOrderPreferences
import com.yes365.atask.domain.model.CollabState
import com.yes365.atask.domain.model.Project
import com.yes365.atask.domain.model.Task
import com.yes365.atask.domain.model.TaskOrderSettings
import com.yes365.atask.domain.repository.ProjectRepository
import com.yes365.atask.domain.repository.TaskRepository
import com.yes365.atask.util.ClockDay
import com.yes365.atask.util.DayClock
import com.yes365.atask.util.GuidedWorld
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * The account as the guided rules read it (docs/cross-app-semantics-v1.md, section 14): every
 * task and project, the Inbox, the day, the review settings, the task orders and who may change
 * what. Ask's agenda, the sessions and the weekly review all read it from here.
 */
class GuidedWorldSource(
    private val tasks: TaskRepository,
    private val projects: ProjectRepository,
    private val auth: AuthManager,
    private val reviewPrefs: ReviewPrefsStore,
    private val taskOrder: TaskOrderPreferences,
    /** Who may change what (the collaboration repository's state). */
    private val collabState: Flow<CollabState>,
    private val dayClock: DayClock,
) {
    private class Settings(val inboxId: Long?, val review: ReviewPrefs, val order: TaskOrderSettings, val collab: CollabState)

    private val settings: Flow<Settings> = combine(
        auth.inboxProjectId,
        reviewPrefs.getPrefs(),
        taskOrder.settings,
        collabState,
    ) { inbox, review, order, collab -> Settings(inbox?.takeIf { it > 0 }, review, order, collab) }

    val world: Flow<GuidedWorld> = combine(
        tasks.getAllTasksFlat(),
        projects.getAllIncludingArchived(),
        settings,
        dayClock.day,
    ) { all, projectList, s, day -> build(all, projectList, s, day) }

    /** The world as it is now. */
    suspend fun now(): GuidedWorld {
        dayClock.refresh()
        return world.first()
    }

    private fun build(all: List<Task>, projectList: List<Project>, s: Settings, day: ClockDay) = GuidedWorld(
        tasks = all,
        projects = projectList,
        inboxId = s.inboxId,
        today = day.date,
        zone = day.zone,
        reviewEnabled = s.review.enabled,
        reviewCadenceDays = s.review.defaultCadenceDays,
        reviewExcludeInbox = s.review.excludeInbox,
        order = s.order,
        permission = { p -> s.collab.projects[p.id]?.permission ?: p.maxPermission },
    )
}
