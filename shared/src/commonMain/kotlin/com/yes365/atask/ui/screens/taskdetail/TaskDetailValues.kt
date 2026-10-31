package com.yes365.atask.ui.screens.taskdetail
import com.yes365.atask.ui.i18n.t

/** The name of a priority level, or null for none (0) and for numbers outside Vikunja's range. */
fun priorityName(priority: Int): String? = when (priority) {
    1 -> "Low"
    2 -> "Medium"
    3 -> "High"
    4 -> "Urgent"
    5 -> t("Do now")
    else -> null
}

private val LINE_BREAK = Regex("\r\n|\r|\n")

/** A title is one line: every line break (typed or pasted) becomes a space. */
fun String.withoutLineBreaks(): String = if ('\n' in this || '\r' in this) replace(LINE_BREAK, " ") else this
