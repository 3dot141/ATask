package com.yes365.atask.ui

import com.yes365.atask.domain.model.SharedContent
import com.yes365.atask.ui.navigation.AppLink
import com.yes365.atask.ui.navigation.ViewTarget
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * What the platform asks the UI to show: a task, a view, the task entry sheet, dictation or shared content.
 * [VicuApp] reads these and clears each one when it has acted on it. On Android the activity holds
 * its own copies; iOS keeps one of these for the whole process, so a notification or a link that
 * arrives before the UI exists is still shown.
 */
class AppInputs {
    val initialTaskId = MutableStateFlow<Long?>(null)
    val showTaskEntry = MutableStateFlow(false)
    val showTaskEntryProjectId = MutableStateFlow<Long?>(null)
    val navigateToView = MutableStateFlow<ViewTarget?>(null)
    val startDictation = MutableStateFlow(false)
    val sharedContent = MutableStateFlow<SharedContent?>(null)

    fun open(link: AppLink) {
        when (link) {
            is AppLink.OpenTask -> initialTaskId.value = link.taskId
            is AppLink.OpenView -> navigateToView.value = link.target
            is AppLink.AddTask -> {
                showTaskEntryProjectId.value = link.projectId
                showTaskEntry.value = true
            }
            AppLink.Dictate -> startDictation.value = true
        }
    }

    fun share(content: SharedContent) {
        sharedContent.value = content
    }
}
