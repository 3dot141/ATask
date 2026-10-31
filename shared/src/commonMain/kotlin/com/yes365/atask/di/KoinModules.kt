package com.yes365.atask.di

import com.yes365.atask.location.DeviceLocation
import com.yes365.atask.location.LocationReminderCoordinator
import com.yes365.atask.location.LocationReminderStore
import com.yes365.atask.location.LocationRepository
import com.yes365.atask.location.SavedPlacesStore
import org.koin.dsl.module
import org.koin.core.context.startKoin
import org.koin.core.KoinApplication
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import com.yes365.atask.data.local.*
import com.yes365.atask.data.local.dao.TaskDao
import com.yes365.atask.data.mapper.*
import com.yes365.atask.data.repository.*
import com.yes365.atask.domain.repository.*
import com.yes365.atask.data.remote.*
import com.yes365.atask.data.remote.api.VikunjaApiService
import com.yes365.atask.auth.*
import com.yes365.atask.util.AppDispatchers
import com.yes365.atask.ui.navigation.NavigationTicker
import com.yes365.atask.ui.screens.shared.CompletionToast
import com.yes365.atask.ui.screens.shared.CompletionToastCenter
import com.yes365.atask.ui.screens.shared.ProjectActions
import com.yes365.atask.ui.screens.guided.GuidedProgress
import com.yes365.atask.ui.screens.guided.GuidedWorldSource
import com.yes365.atask.ui.screens.guided.GuidedWriter
import com.yes365.atask.util.AppMessages
import com.yes365.atask.util.postIfRefused
import com.yes365.atask.util.BuildInfo
import com.yes365.atask.permission.NotificationPermissionCoordinator
import com.yes365.atask.util.DayClock
import com.yes365.atask.util.NetworkMonitor
import com.yes365.atask.util.NotificationActions
import com.yes365.atask.util.RoutineReminderCheck
import com.yes365.atask.data.sync.CarrierFinder
import com.yes365.atask.data.sync.LabelRefresher
import com.yes365.atask.data.sync.ProjectRefresher
import com.yes365.atask.data.sync.ProjectTaskCounts
import com.yes365.atask.data.sync.ScreenRefresher
import com.yes365.atask.data.sync.SyncStaleness
import com.yes365.atask.data.sync.TaskRefresher
import com.yes365.atask.util.SystemTimeSource
import com.yes365.atask.util.TextTaskCreator
import com.yes365.atask.ui.components.fab.FabHint
import com.yes365.atask.voice.HoldToTalk
import com.yes365.atask.voice.SelectingSpeechInput
import com.yes365.atask.ai.aiModule
import com.yes365.atask.ai.dictation.DictationMic
import com.yes365.atask.ai.dictation.DictationService
import com.yes365.atask.voice.VoiceAddResult
import com.yes365.atask.util.TimeSource
import com.yes365.atask.widget.TaskWidgetStates
import kotlinx.coroutines.flow.first
import com.yes365.atask.util.CustomListContext
import com.yes365.atask.worker.SyncEngine
import com.yes365.atask.worker.CollaborationSync

val databaseModule = module {
    single {
        getDatabaseBuilder(get()).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build()
    }
    single { get<VikunjaDatabase>().taskDao() }
    single { get<VikunjaDatabase>().projectDao() }
    single { get<VikunjaDatabase>().labelDao() }
    single { get<VikunjaDatabase>().pendingActionDao() }
    single { get<VikunjaDatabase>().attachmentDao() }
    single { get<VikunjaDatabase>().routineArchiveDao() }
    single { get<VikunjaDatabase>().localDataDao() }
    single { get<VikunjaDatabase>().collaborationDao() }

    single { BehaviorPrefsStore(createDataStore(get(), "behavior_prefs")) }
    single { BottomBarPrefsStore(createDataStore(get(), "bottom_bar_prefs")) }
    single { CarrierIdStore(createDataStore(get(), "carrier_ids")) }
    single { CustomListStore(createDataStore(get(), "custom_lists")) }
    single { LabelOrderPrefsStore(createDataStore(get(), "label_order_prefs")) }
    single { LogbookPrefsStore(createDataStore(get(), "logbook_prefs")) }
    single { CalendarPrefsStore(createDataStore(get(), "calendar_prefs")) }
    single { NlpPrefsStore(createDataStore(get(), "nlp_prefs")) }
    single { NotificationPermissionStore(createDataStore(get(), "notification_permission")) }
    single { NotificationPrefsStore(createDataStore(get(), "notification_prefs")) }
    single { ProjectSectionPrefsStore(createDataStore(get(), "project_section_prefs")) }
    single { ReminderAlarmRegistry(createDataStore(get(), "reminder_alarm_registry"), get()) }
    single { ReviewPrefsStore(createDataStore(get(), "review_prefs")) }
    single { RoutinePrefsStore(createDataStore(get(), "routine_prefs")) }
    single { SnoozeStore(createDataStore(get(), "snooze_prefs"), get()) }
    single { SyncCursorStore(createDataStore(get(), "sync_cursor")) }
    single { SyncedPrefsStore(createDataStore(get(), "synced_prefs")) }
    single { TempIdGenerator(createDataStore(get(), "temp_ids")) }
    single { ThemePrefsStore(createDataStore(get(), "theme_prefs")) }
    single { SavedPlacesStore(createDataStore(get(), "saved_places"), get()) }
    single { LocationReminderStore(createDataStore(get(), "location_reminders"), get()) }
    single { LocationRepository(synced = get<SyncedPreferences>(), positions = get(), local = get()) }
    single { WidgetPrefsStore(createDataStore(get(), "widget_prefs")) }
    single { FabHintStore(createDataStore(get(), "fab_hint")) }
}

val networkModule = module {
    single { createApiJson() }
    single { BaseUrlHolder(get()) }
    single {
        KtorClientFactory.create(
            engine = get(),
            json = get(),
            baseUrlHolder = get(),
            authManager = get(),
            // Set by the host app from its BuildConfig before Koin starts.
            enableLogging = BuildInfo.isDebug,
            tokenCheck = ApiTokenCheck(time = get()),
            userAgent = VikunjaUserAgent.current(),
        )
    }
    single { VikunjaApiService(get(), get()) }
}

val repositoryModule = module {
    single { TaskMapper(get()) }
    single { LabelMapper() }
    single { ProjectMapper() }
    single { AttachmentMapper() }

    single { TaskRefresher(taskDao = get(), pendingActionDao = get(), api = get(), taskMapper = get(), platformHooks = get(), cursorStore = get(), time = get(), currentUser = get<AuthManager>(), routineDeviceId = get<RoutinePrefsStore>()::getOrCreateDeviceId) }
    single {
        LabelRefresher(
            labelDao = get(),
            taskDao = get(),
            pendingActionDao = get(),
            api = get(),
            labelMapper = get(),
            taskMapper = get(),
        )
    }
    single { ProjectRefresher(projectDao = get(), pendingActionDao = get(), api = get(), projectMapper = get()) }

    single {
        val taskDao = get<TaskDao>()
        ListPositioner(
            api = get(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
            time = get(),
            storePosition = { taskId, position -> taskDao.updatePosition(taskId, position) },
        )
    }
    single { TaskWriteGate(pendingActionDao = get()) }
    single<TaskRepository> {
        TaskRepositoryImpl(
            taskDao = get(),
            api = get(),
            pendingActionDao = get(),
            taskMapper = get(),
            platformHooks = get(),
            json = get(),
            behaviorPrefsStore = get(),
            logbookPrefsStore = get(),
            dayClock = get(),
            tempIds = get(),
            refresher = get(),
            positioner = get(),
            writeGate = get(),
        )
    }
    single<ProjectRepository> {
        ProjectRepositoryImpl(
            projectDao = get(),
            api = get(),
            projectMapper = get(),
            projectRefresher = get(),
            pendingActionDao = get(),
            platformHooks = get(),
        )
    }
    single<LabelRepository> {
        LabelRepositoryImpl(
            labelDao = get(),
            taskDao = get(),
            pendingActionDao = get(),
            api = get(),
            labelMapper = get(),
            taskMapper = get(),
            platformHooks = get(),
            json = get(),
            tempIds = get(),
            labelRefresher = get(),
            writeGate = get(),
        )
    }
    single<AttachmentRepository> {
        AttachmentRepositoryImpl(
            attachmentDao = get(),
            api = get(),
            attachmentMapper = get(),
            platformFiles = get(),
        )
    }
    single { CarrierFinder(api = get(), store = get(), time = get()) }
    single<CustomListRepository> {
        CustomListRepositoryImpl(
            store = get(),
            api = get(),
            authManager = get(),
            platformHooks = get(),
            json = get(),
            carrierFinder = get(),
            time = get(),
        )
    }
    single {
        SyncedPreferences(
            store = get(),
            api = get(),
            carrierFinder = get(),
            authManager = get(),
            platformHooks = get(),
            json = get(),
            projectDao = get(),
            messages = get(),
            time = get(),
        )
    }
    // A change redraws the widgets that show a project in its order (SyncedPreferences.set does).
    single { TaskOrderPreferences(synced = get<SyncedPreferences>()) }
    single { RoutinePersonalPreferences(synced = get<SyncedPreferences>()) }
    single { ProjectCategoryPreferences(synced = get<SyncedPreferences>()) }
    single { WeeklyReviewPreferences(synced = get<SyncedPreferences>()) }
    single<CollaborationRepository> {
        CollaborationRepositoryImpl(
            collaborationDao = get(),
            taskDao = get(),
            projectDao = get(),
            pendingActionDao = get(),
            api = get(),
            taskMapper = get(),
            projectMapper = get(),
            tokenStorage = get(),
            tempIds = get(),
            platformHooks = get(),
            writeGate = get(),
        )
    }
    single { CollaborationSync(api = get(), collaborationDao = get(), pendingActionDao = get(), tokenStorage = get()) }
    single {
        RoutineArchiveStore(
            api = get(),
            json = get(),
            currentUser = get<AuthManager>(),
            readRoutines = RoutineArchiveStore.cachedRoutines(taskDao = get(), json = get()),
        )
    }
    single<RoutineRepository> {
        RoutineRepositoryImpl(
            taskDao = get(),
            archiveDao = get(),
            taskMapper = get(),
            taskRepository = get(),
            authManager = get(),
            prefsStore = get(),
            platformHooks = get(),
            json = get(),
            archiveStore = get(),
            personal = get(),
            projectDao = get(),
            refresher = get(),
        )
    }
}

val commonModule = module {
    single { DailySummaryReader(get()) }
    single {
        TextTaskCreator(
            taskRepository = get(),
            projectRepository = get(),
            labelRepository = get(),
            authManager = get(),
            nlpPrefsStore = get(),
            notificationPrefsStore = get(),
            dayClock = get(),
            time = get(),
            locations = get(),
            locationPermission = get(),
        )
    }
    single { FabHint(store = get(), scope = get()) }
    // Local speech when a speech model is chosen, else the platform recognizer (holding +, Talk).
    single {
        val dictationService = get<DictationService>()
        SelectingSpeechInput(
            platform = get(),
            recorder = get(),
            host = get(),
            models = get(),
            settingsStore = get(),
            scope = get(),
            hint = { dictationService.vocabularyHint() },
        )
    }
    // Holding the + to say a task: quick capture, the words through the same path as the other voice
    // adds. Sliding up to the lock hands over to smart dictation (docs/ai-assistant-v1.md, section 4).
    single {
        val creator = get<TextTaskCreator>()
        val tasks = get<TaskRepository>()
        val hint = get<FabHint>()
        val messages = get<AppMessages>()
        val smart = get<DictationMic>()
        HoldToTalk(
            speech = get<SelectingSpeechInput>(),
            messages = messages,
            scope = get(),
            addTask = { text, format, defaults ->
                when (val result = creator.create(text, format, defaults.projectId, defaults.dueDate)) {
                    is TextTaskCreator.Result.Created -> VoiceAddResult(result.confirmation) {
                        messages.postIfRefused(tasks.delete(result.task.id), "undo the task")
                    }
                    is TextTaskCreator.Result.Failed -> VoiceAddResult(result.message)
                }
            },
            onVoiceAdded = hint::onVoiceUsed,
            canLock = { smart.ready.value },
            onLock = smart::open,
            onLockedWords = smart::listen,
        )
    }
    single { NotificationActions(taskRepository = get(), routineRepository = get(), reminders = get(), hooks = get()) }
    single { RoutineReminderCheck(repository = get(), prefsStore = get()) }
    single { TokenRefreshRun(authManager = get(), tokenStorage = get(), baseUrlHolder = get()) }
    single {
        TaskWidgetStates(
            taskDao = get(),
            projectDao = get(),
            taskRepository = get(),
            tokenStorage = get(),
            customListStore = get(),
            widgetPrefsStore = get(),
            behaviorPrefsStore = get(),
            time = get(),
            collaborationContext = {
                val collaboration: CollaborationRepository = get()
                CustomListContext.of(collaboration.state.first(), collaboration.signals.first())
            },
            taskOrders = get(),
        )
    }
    single {
        ProjectTaskCounts(
            api = get(),
            serverKey = { get<AuthManager>().getVikunjaUrl() },
            time = get(),
            scope = get(),
        )
    }
    single<ProjectProgressSource> {
        ProjectProgressSourceImpl(
            taskDao = get(),
            counts = get(),
            carrierFinder = get(),
            archiveStore = get(),
            authManager = get(),
            json = get(),
        )
    }
    single { SyncStaleness() }
    single {
        ScreenRefresher(
            taskRepository = get(),
            projectRepository = get(),
            labelRepository = get(),
            staleness = get(),
            collaboration = get(),
        )
    }
    single { AppMessages() }
    single { NavigationTicker() }
    single { AppDispatchers() }
    single<TimeSource> { SystemTimeSource }
    single { DayClock(scope = get(), time = get()) }
    single { NotificationPermissionCoordinator(platform = get(), store = get(), scope = get()) }
    single {
        LocalDataWiper(
            dao = get(),
            customLists = get(),
            bottomBarPrefs = get(),
            platformHooks = get(),
            syncStaleness = get(),
            projectSectionPrefs = get(),
            labelOrderPrefs = get(),
            routinePrefs = get(),
            widgetPrefs = get(),
            syncCursor = get(),
            carrierIds = get(),
            listPositions = get(),
            syncedPreferences = get(),
            locationReminders = get(),
            assistantConversations = get(),
            keptTranscript = get(),
        )
    }
    single {
        LocationReminderCoordinator(
            locations = get(),
            taskRepository = get(),
            tempIds = get(),
            geofencer = get(),
            notifier = get(),
            notificationPrefs = get(),
            scope = get(),
            near = { get<DeviceLocation>().current() },
        )
    }
    single { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
    single<CompletionToast> { CompletionToastCenter(messages = get(), taskRepository = get(), time = get(), scope = get()) }
    // Guided organizing (docs/cross-app-semantics-v1.md, section 14).
    single {
        GuidedWorldSource(
            tasks = get(),
            projects = get(),
            auth = get(),
            reviewPrefs = get(),
            taskOrder = get(),
            collabState = get<CollaborationRepository>().state,
            dayClock = get(),
        )
    }
    single { GuidedWriter(tasks = get(), proposals = get(), dayClock = get()) }
    single { GuidedProgress() }
    single {
        ProjectActions(
            projectRepository = get(),
            authManager = get(),
            repositoryHooks = get(),
            dayClock = get(),
            collaboration = get(),
            syncedPreferences = get(),
            categoryPreferences = get(),
            appMessages = get(),
            appScope = get(),
        )
    }
    single {
        AuthManager(
            platformAuthHooks = get(),
            tokenStorage = get(),
            apiServiceProvider = { get() },
            appScope = get(),
            networkMonitor = get()
        )
    }
    single {
        AccountSession(
            tokenStorage = get(),
            apiService = get(),
            wiper = get(),
        )
    }
    single { SessionCleanup(authManager = get(), wiper = get()) }
    single {
        PasswordLoginHandler(
            apiServiceProvider = { get() }
        )
    }
    single {
        OidcHandler(
            apiServiceProvider = { get() }
        )
    }
    single {
        SyncEngine(
            pendingActionDao = get(),
            taskDao = get(),
            labelDao = get(),
            projectDao = get(),
            api = get(),
            taskMapper = get(),
            labelMapper = get(),
            projectMapper = get(),
            taskRefresher = get(),
            labelRefresher = get(),
            projectRefresher = get(),
            platformHooks = get(),
            json = get(),
            baseUrlHolder = get(),
            authManager = get(),
            customListRepository = get(),
            routineRepository = get(),
            positioner = get(),
            tempIds = get(),
            writeGate = get(),
            projectCounts = get(),
            collabSync = get(),
            collaboration = get(),
            syncedPreferences = get(),
            locations = get(),
        )
    }
}

val sharedModules = listOf(
    databaseModule,
    networkModule,
    repositoryModule,
    commonModule,
    aiModule,
    viewModelModule,
)
