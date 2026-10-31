package com.yes365.atask.ai

import com.yes365.atask.ai.ask.AskBackend
import com.yes365.atask.ai.ask.AskList
import com.yes365.atask.ai.ask.AskSnapshot
import com.yes365.atask.ai.ask.ConversationStore
import com.yes365.atask.ai.ask.Proposals
import com.yes365.atask.ai.dictation.DictationContext
import com.yes365.atask.ai.dictation.DictationController
import com.yes365.atask.ai.dictation.DictationMic
import com.yes365.atask.ai.dictation.DictationService
import com.yes365.atask.ai.dictation.DraftWriter
import com.yes365.atask.ai.dictation.KeptTranscriptStore
import com.yes365.atask.ai.dictation.PlanWriter
import com.yes365.atask.ai.organize.OrganizeService
import com.yes365.atask.ai.organize.OrganizeSuggester
import com.yes365.atask.ai.provider.LlmClient
import com.yes365.atask.auth.AuthManager
import com.yes365.atask.data.local.ReviewPrefsStore
import com.yes365.atask.data.local.createDataStore
import com.yes365.atask.data.repository.TaskOrderPreferences
import com.yes365.atask.domain.model.Task
import com.yes365.atask.domain.repository.CollaborationRepository
import com.yes365.atask.domain.repository.CustomListRepository
import com.yes365.atask.domain.repository.LabelRepository
import com.yes365.atask.domain.repository.ProjectRepository
import com.yes365.atask.domain.repository.TaskRepository
import com.yes365.atask.speech.SpeechEngineHost
import com.yes365.atask.speech.SpeechModelManager
import com.yes365.atask.util.AppDispatchers
import com.yes365.atask.util.DayClock
import com.yes365.atask.ui.screens.ask.AskEntry
import com.yes365.atask.voice.SelectingSpeechInput
import com.yes365.atask.voice.TaskDefaults
import kotlinx.coroutines.flow.first
import org.koin.dsl.module

/** What Ask reads, from the repositories (`docs/ai-assistant-v1.md`, section 5). */
class RepositoryAskBackend(
    private val tasks: TaskRepository,
    private val projects: ProjectRepository,
    private val labels: LabelRepository,
    private val customLists: CustomListRepository,
    private val collaboration: CollaborationRepository,
    private val taskOrder: TaskOrderPreferences,
    private val reviewPrefs: ReviewPrefsStore,
    private val auth: AuthManager,
    private val dayClock: DayClock,
    private val dictation: DictationService,
) : AskBackend {
    override suspend fun snapshot(): AskSnapshot {
        dayClock.refresh()
        val day = dayClock.day.value
        val review = reviewPrefs.getPrefs().first()
        return AskSnapshot(
            tasks = tasks.getAllTasksFlat().first(),
            projects = projects.getAllIncludingArchived().first(),
            labels = labels.getAll().first(),
            customLists = customLists.lists.first(),
            collab = collaboration.state.first(),
            signals = collaboration.signals.first(),
            order = taskOrder.settings.first(),
            inboxId = auth.getInboxProjectId()?.takeIf { it > 0 },
            today = day.date,
            zone = day.zone,
            reviewCadenceDays = review.defaultCadenceDays,
            reviewEnabled = review.enabled,
            reviewExcludeInbox = review.excludeInbox,
        )
    }

    override suspend fun logbook(): List<Task> = tasks.getLogbookTasks().first()

    override suspend fun dictationContext(list: AskList): DictationContext =
        dictation.context(TaskDefaults(projectId = list.projectId, listName = list.name))
}

/**
 * The assistant's shared definitions. The platform modules bind [AiKeyStore] and the speech seams
 * (`SpeechEngineNative`, `AudioRecorder`, `SpeechOutput`, `SpeechStorage`).
 */
val aiModule = module {
    single { AiSettingsStore(createDataStore(get(), "ai_settings")) }
    single { ConversationStore(createDataStore(get(), "ai_conversations")) }
    single { KeptTranscriptStore(createDataStore(get(), "ai_kept_transcript")) }
    single { LlmClient(engine = get()) }
    single { AiProviders(client = get(), keys = get()) }
    single { SpeechEngineHost(native = get(), storage = get(), io = get<AppDispatchers>().io) }
    single { SpeechModelManager(engine = get(), storage = get(), scope = get(), io = get<AppDispatchers>().io) }
    single {
        DictationService(
            providers = get(),
            settings = get(),
            projects = get(),
            labels = get(),
            collaboration = get(),
            auth = get(),
            dayClock = get(),
            locations = get(),
            time = get(),
        )
    }
    single { DraftWriter(tasks = get(), labels = get(), notificationPrefs = get(), locations = get()) }
    single { PlanWriter(projects = get(), drafts = get()) }
    single { Proposals(tasks = get(), labels = get(), drafts = get()) }
    single<OrganizeSuggester> { OrganizeService(providers = get(), settings = get(), backend = get(), dictation = get()) }
    single {
        DictationController(
            service = get(),
            writer = get(),
            planWriter = get(),
            kept = get(),
            textTasks = get(),
            tasks = get(),
            messages = get(),
            dayClock = get(),
            scope = get(),
        )
    }
    single {
        DictationMic(
            controller = get(),
            speech = get<SelectingSpeechInput>(),
            settingsStore = get(),
            messages = get(),
            scope = get(),
        )
    }
    single { AskEntry() }
    single<AskBackend> {
        RepositoryAskBackend(
            tasks = get(),
            projects = get(),
            labels = get(),
            customLists = get(),
            collaboration = get(),
            taskOrder = get(),
            reviewPrefs = get(),
            auth = get(),
            dayClock = get(),
            dictation = get(),
        )
    }
}
