package com.yes365.atask.di

import com.yes365.atask.ui.screens.ask.AskViewModel
import com.yes365.atask.ui.screens.guided.GuidedReviewViewModel
import com.yes365.atask.ui.screens.guided.GuidedSortViewModel
import com.yes365.atask.ui.screens.guided.WeeklyReviewViewModel
import com.yes365.atask.ui.screens.settings.AssistantSettingsViewModel
import com.yes365.atask.ui.SyncStateViewModel
import com.yes365.atask.ui.components.collab.CollabViewModel
import com.yes365.atask.ui.components.selection.SelectionViewModel
import com.yes365.atask.ui.navigation.DrawerViewModel
import com.yes365.atask.ui.screens.anytime.AnytimeViewModel
import com.yes365.atask.ui.screens.comments.TaskCommentsViewModel
import com.yes365.atask.ui.screens.customlist.CustomListViewModel
import com.yes365.atask.ui.screens.inbox.InboxViewModel
import com.yes365.atask.ui.screens.logbook.LogbookViewModel
import com.yes365.atask.ui.screens.project.ProjectViewModel
import com.yes365.atask.ui.screens.review.ReviewViewModel
import com.yes365.atask.ui.screens.routines.RoutinesViewModel
import com.yes365.atask.ui.screens.search.SearchViewModel
import com.yes365.atask.ui.screens.settings.HiddenRoutinesViewModel
import com.yes365.atask.ui.screens.settings.SettingsViewModel
import com.yes365.atask.ui.screens.setup.SetupViewModel
import com.yes365.atask.ui.screens.tag.TagViewModel
import com.yes365.atask.ui.components.location.PlacesViewModel
import com.yes365.atask.ui.screens.taskdetail.TaskDetailViewModel
import com.yes365.atask.ui.screens.taskentry.TaskEntryViewModel
import com.yes365.atask.ui.screens.today.TodayViewModel
import com.yes365.atask.ui.screens.calendar.CalendarViewModel
import com.yes365.atask.ui.screens.upcoming.UpcomingViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Every view model of the shared UI; the same on each platform. */
val viewModelModule = module {
    viewModelOf(::SyncStateViewModel)
    viewModelOf(::SelectionViewModel)
    viewModelOf(::DrawerViewModel)
    viewModelOf(::AnytimeViewModel)
    viewModelOf(::CustomListViewModel)
    viewModelOf(::InboxViewModel)
    viewModelOf(::LogbookViewModel)
    viewModelOf(::ProjectViewModel)
    viewModelOf(::ReviewViewModel)
    viewModelOf(::RoutinesViewModel)
    viewModelOf(::HiddenRoutinesViewModel)
    viewModelOf(::SearchViewModel)
    // More constructor parameters than viewModelOf takes (22), so spelled out.
    viewModel {
        SettingsViewModel(
            authManager = get(),
            tokenStorage = get(),
            labelRepository = get(),
            projectRepository = get(),
            customListRepository = get(),
            notificationPrefsStore = get(),
            behaviorPrefsStore = get(),
            themePrefsStore = get(),
            nlpPrefsStore = get(),
            bottomBarPrefsStore = get(),
            widgetPrefsStore = get(),
            reviewPrefsStore = get(),
            logbookPrefsStore = get(),
            routinePrefsStore = get(),
            repositoryHooks = get(),
            pendingActionDao = get(),
            networkMonitor = get(),
            sessionCleanup = get(),
            apiService = get(),
            platformSettingsHooks = get(),
            syncCursor = get(),
            projectActions = get(),
            taskOrderPreferences = get(),
        )
    }
    viewModelOf(::SetupViewModel)
    viewModelOf(::TagViewModel)
    viewModelOf(::TaskDetailViewModel)
    viewModelOf(::TaskEntryViewModel)
    viewModelOf(::TodayViewModel)
    viewModelOf(::UpcomingViewModel)
    viewModelOf(::CalendarViewModel)
    viewModelOf(::CollabViewModel)
    viewModelOf(::TaskCommentsViewModel)
    viewModelOf(::PlacesViewModel)
    viewModelOf(::AssistantSettingsViewModel)
    viewModelOf(::AskViewModel)
    viewModelOf(::GuidedSortViewModel)
    viewModelOf(::GuidedReviewViewModel)
    viewModelOf(::WeeklyReviewViewModel)
}
