package com.yes365.atask.voice

import com.yes365.atask.domain.model.Project
import com.yes365.atask.location.SavedPlace
import com.yes365.atask.ui.screens.taskentry.FieldSource
import com.yes365.atask.ui.screens.taskentry.TaskEntryUiState
import com.yes365.atask.ui.screens.taskentry.entryLabelsWords
import com.yes365.atask.ui.screens.taskentry.entryPlaceChipLabel
import com.yes365.atask.ui.screens.taskentry.entryPriorityName
import com.yes365.atask.ui.screens.taskentry.resolveEntryFields
import com.yes365.atask.util.ClockDay
import com.yes365.atask.util.DateContext
import com.yes365.atask.util.DateDisplay
import com.yes365.atask.util.DateDisplayFormat
import com.yes365.atask.util.DateUtils
import com.yes365.atask.util.parser.ParsedToken
import com.yes365.atask.util.parser.ParserConfig
import com.yes365.atask.util.parser.TaskParser
import com.yes365.atask.util.parser.TokenType
import kotlinx.datetime.LocalDateTime

/** A field read from the words, as the voice card shows it under them ("Tomorrow 15:00", "Work"). */
data class VoicePill(val type: TokenType, val label: String)

/** The words heard so far, read like quick add: what to highlight, and the fields as pills. */
data class VoicePreview(val tokens: List<ParsedToken>, val pills: List<VoicePill>) {
    companion object {
        val EMPTY = VoicePreview(emptyList(), emptyList())
    }
}

/**
 * Reads [text] the way the task will be added (`TextTaskCreator`: the parser with the user's
 * settings, the quick add field rules, the [defaults] of the list unless the words name a project
 * or a date), so the card shows what letting go adds. The list's own project and date are pills
 * too (not highlighted words), so the card says where the task goes before anything is said.
 */
fun voicePreview(
    text: String,
    config: ParserConfig,
    now: LocalDateTime,
    day: ClockDay,
    projects: List<Project>,
    inboxId: Long,
    format: DateDisplayFormat,
    defaults: TaskDefaults = TaskDefaults(),
    places: List<SavedPlace> = emptyList(),
): VoicePreview {
    val parsed = if (text.isNotBlank() && config.enabled) {
        TaskParser.parse(text, config, reference = now, zone = day.zone, places = places)
    } else {
        null
    }
    val defaultProject = defaults.projectId?.takeIf { id -> projects.any { it.id == id } } ?: inboxId
    val fields = resolveEntryFields(
        TaskEntryUiState(
            title = text,
            projectId = defaultProject,
            dueDate = defaults.dueDate.orEmpty(),
            allProjects = projects,
            parseResult = parsed,
            parserConfig = config,
        ),
        day.zone,
    )
    val pills = buildList {
        if (fields.dueSource != null && fields.dueDate.isNotBlank()) {
            add(VoicePill(TokenType.DATE, DateDisplay.formatDue(DateContext.CHIP, fields.dueDate, day.date, day.zone, format)))
        }
        // The project the words name, or the list's own; the Inbox, where tasks go anyway, needs no pill.
        val namedProject = fields.parsedProjectName != null && !fields.projectNotFound
        if (namedProject || fields.projectId != inboxId) {
            projects.find { it.id == fields.projectId }?.let { add(VoicePill(TokenType.PROJECT, it.title)) }
        }
        entryLabelsWords(emptyList(), parsed?.labels.orEmpty())?.let { add(VoicePill(TokenType.LABEL, it)) }
        if (fields.prioritySource == FieldSource.TEXT) {
            entryPriorityName(fields.priority)?.let { add(VoicePill(TokenType.PRIORITY, it)) }
        }
        if (fields.recurrenceSource == FieldSource.TEXT) {
            add(VoicePill(TokenType.RECURRENCE, DateUtils.formatRecurrence(fields.recurrence.repeatAfter, fields.recurrence.repeatMode)))
        }
        // "When I get home" for a saved place: the location reminder the task gets.
        parsed?.place?.let { add(VoicePill(TokenType.PLACE, entryPlaceChipLabel(it))) }
    }
    // A project that does not exist is not highlighted: the task goes to the Inbox.
    val tokens = parsed?.tokens.orEmpty().filter { it.type != TokenType.PROJECT || !fields.projectNotFound }
    return VoicePreview(tokens, pills)
}
