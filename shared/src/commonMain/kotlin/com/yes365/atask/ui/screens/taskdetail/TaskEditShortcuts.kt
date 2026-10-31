package com.yes365.atask.ui.screens.taskdetail

import com.yes365.atask.domain.model.Project
import com.yes365.atask.domain.model.Task
import com.yes365.atask.util.DueDates
import com.yes365.atask.util.parser.ParseResult
import com.yes365.atask.util.parser.TokenType
import com.yes365.atask.util.parser.recurrenceToVikunja
import kotlinx.datetime.TimeZone

internal data class TaskEditShortcutResult(
    val task: Task,
    val labelNames: List<String>,
)

/**
 * Applies title shortcuts to an existing task while leaving manually edited fields alone. A parsed
 * date is stored date-only unless the text named a time; [zone] is the device's current zone.
 */
internal fun applyTaskEditShortcuts(
    task: Task,
    parseResult: ParseResult,
    projects: List<Project>,
    zone: TimeZone,
    manuallyEditedTypes: Set<TokenType> = emptySet(),
): TaskEditShortcutResult {
    var updated = task.copy(title = parseResult.title)

    if (TokenType.DATE !in manuallyEditedTypes) {
        parseResult.dueDate?.let { dueDate ->
            updated = updated.copy(
                dueDate = DueDates.fromParsed(dueDate, parseResult.dueDateHasTime, zone).toString(),
            )
        }
    }

    if (TokenType.PRIORITY !in manuallyEditedTypes) {
        parseResult.priority?.let { priority ->
            updated = updated.copy(priority = priority)
        }
    }

    if (TokenType.PROJECT !in manuallyEditedTypes) {
        parseResult.project?.let { projectName ->
            projects.firstOrNull { it.title.equals(projectName, ignoreCase = true) }
                ?.let { project -> updated = updated.copy(projectId = project.id) }
        }
    }

    if (TokenType.RECURRENCE !in manuallyEditedTypes) {
        parseResult.recurrence?.let { recurrence ->
            val vikunjaRecurrence = recurrenceToVikunja(recurrence)
            updated = updated.copy(
                repeatAfter = vikunjaRecurrence.repeatAfter,
                repeatMode = vikunjaRecurrence.repeatMode,
            )
        }
    }

    return TaskEditShortcutResult(
        task = updated,
        labelNames = if (TokenType.LABEL in manuallyEditedTypes) {
            emptyList()
        } else {
            parseResult.labels.distinctBy { it.lowercase() }
        },
    )
}
