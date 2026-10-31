package com.yes365.atask.ui.components.task

import androidx.compose.runtime.compositionLocalOf
import com.yes365.atask.data.local.SubtaskDisplayMode

/** App-wide subtask presentation preference, supplied once by [com.yes365.atask.ui.VicuApp]. */
val LocalSubtaskDisplayMode = compositionLocalOf { SubtaskDisplayMode.INSIDE_TASK }
