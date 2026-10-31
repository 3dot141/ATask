package com.yes365.atask.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yes365.atask.device.CalendarAccess
import com.yes365.atask.device.DeviceCalendarInfo
import com.yes365.atask.device.DeviceEvent
import com.yes365.atask.domain.model.Task
import com.yes365.atask.ui.components.shared.FabClearance
import com.yes365.atask.ui.components.shared.LocalClockDay
import com.yes365.atask.ui.components.shared.ScreenFab
import com.yes365.atask.ui.components.shared.VicuTopAppBar
import com.yes365.atask.ui.components.shared.rememberVicuTopBarScroll
import com.yes365.atask.ui.i18n.LocalStrings
import com.yes365.atask.ui.navigation.ScreenReady
import com.yes365.atask.util.WhenLogic
import com.yes365.atask.voice.TaskDefaults
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Instant
import kotlinx.datetime.toLocalDateTime

/**
 * The month screen that replaced Upcoming: the phone's calendar events and the open tasks, one
 * grid for both. Tapping a day shows its events and tasks underneath, the way the iOS Calendar's
 * month view does; the tasks open the task editor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onTaskClick: (Long) -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onShowTaskEntry: (Long?, String?) -> Unit = { _, _ -> },
    viewModel: CalendarViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ScreenReady(!state.isLoading)
    val s = LocalStrings.current
    val listState = rememberLazyListState()
    val topBarScroll = rememberVicuTopBarScroll()
    val zone = LocalClockDay.current.zone
    var showCalendarPicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = topBarScroll.modifier,
        topBar = {
            VicuTopAppBar(
                title = { Text(s.calendarTitle) },
                onOpenDrawer = onOpenDrawer,
                scroll = topBarScroll,
                onNavigateToSearch = onNavigateToSearch,
                extraActions = if (state.access == CalendarAccess.Granted && state.calendars.isNotEmpty()) {
                    {
                        IconButton(onClick = { showCalendarPicker = true }) {
                            Icon(Icons.Outlined.Tune, contentDescription = s.chooseCalendars)
                        }
                    }
                } else {
                    null
                },
            )
        },
        floatingActionButton = {
            ScreenFab(
                onClick = { onShowTaskEntry(null, null) },
                expanded = !listState.canScrollBackward,
                taskDefaults = TaskDefaults(listName = "Calendar"),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            MonthHeader(
                title = s.monthTitle(state.month.year, state.month.monthNumber),
                onPrevious = { viewModel.showMonth(-1) },
                onNext = { viewModel.showMonth(1) },
                previousLabel = s.previousMonth,
                nextLabel = s.nextMonth,
                showToday = state.month != WhenLogic.monthStart(state.today) || state.selected != state.today,
                todayLabel = s.todayLabel,
                onToday = viewModel::goToToday,
            )
            WeekdayRow(s.weekdaysShort)
            MonthGrid(
                modifier = Modifier.pointerInput(Unit) {
                    // Swiping the grid changes the month, the way the native calendar pages.
                    val threshold = 48.dp.toPx()
                    var drag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { drag = 0f },
                        onDragEnd = {
                            if (drag >= threshold) viewModel.showMonth(-1)
                            else if (drag <= -threshold) viewModel.showMonth(1)
                        },
                    ) { change, amount ->
                        change.consume()
                        drag += amount
                    }
                },
                cells = state.cells,
                today = state.today,
                selected = state.selected,
                onSelect = viewModel::select,
                taskColor = MaterialTheme.colorScheme.primary,
                eventColor = MaterialTheme.colorScheme.secondary,
                todayLabel = s.todayLabel,
            )
            HorizontalDivider(Modifier.padding(top = 8.dp))
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = FabClearance),
            ) {
                item(key = "day_title") {
                    Text(
                        text = s.dayTitle(state.selected),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
                    )
                }
                if (state.access != CalendarAccess.Granted) {
                    item(key = "permission") {
                        CalendarPermissionCard(
                            title = s.calendarPermissionTitle,
                            body = s.calendarPermissionBody,
                            action = s.allowCalendarAccess,
                            onAllow = viewModel::requestCalendarAccess,
                        )
                    }
                }
                if (state.dayEvents.isNotEmpty()) {
                    item(key = "events_header") { SectionLabel(s.eventsSection) }
                    items(state.dayEvents, key = { "event_${it.id}" }) { event ->
                        EventRow(event = event, allDayLabel = s.allDay, zone = zone)
                    }
                }
                if (state.dayTasks.isNotEmpty()) {
                    item(key = "tasks_header") { SectionLabel(s.tasksSection) }
                    items(state.dayTasks, key = { "task_${it.id}" }) { task ->
                        CalendarTaskRow(
                            task = task,
                            onClick = { onTaskClick(task.id) },
                            onToggleDone = { viewModel.toggleDone(task) },
                            zone = zone,
                        )
                    }
                }
                if (state.dayEvents.isEmpty() && state.dayTasks.isEmpty() && state.access == CalendarAccess.Granted) {
                    item(key = "empty") {
                        EmptyDay(
                            icon = Icons.Outlined.CalendarMonth,
                            text = s.noTasksToday,
                        )
                    }
                }
            }
        }
    }

    if (showCalendarPicker) {
        CalendarPickerDialog(
            calendars = state.calendars,
            hiddenIds = state.hiddenCalendarIds,
            title = s.chooseCalendars,
            hint = s.calendarsHint,
            close = s.cancel,
            onToggle = viewModel::setCalendarVisible,
            onDismiss = { showCalendarPicker = false },
        )
    }
}

/** The phone's calendars with a tick each: an unticked one is left out of the grid and the day. */
@Composable
private fun CalendarPickerDialog(
    calendars: List<DeviceCalendarInfo>,
    hiddenIds: Set<String>,
    title: String,
    hint: String,
    close: String,
    onToggle: (String, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(
                    hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                for (calendar in calendars) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            onToggle(calendar.id, calendar.id in hiddenIds)
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = calendar.id !in hiddenIds,
                            onCheckedChange = { onToggle(calendar.id, it) },
                        )
                        Column(Modifier.weight(1f)) {
                            Text(calendar.title, style = MaterialTheme.typography.bodyLarge)
                            if (calendar.source.isNotBlank()) {
                                Text(
                                    calendar.source,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(close) } },
    )
}

@Composable
private fun MonthHeader(
    title: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    previousLabel: String,
    nextLabel: String,
    showToday: Boolean,
    todayLabel: String,
    onToday: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        if (showToday) {
            TextButton(onClick = onToday) { Text(todayLabel) }
        }
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = previousLabel)
        }
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = nextLabel)
        }
    }
}

@Composable
private fun WeekdayRow(weekdays: List<String>) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        for (name in weekdays) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MonthGrid(
    modifier: Modifier = Modifier,
    cells: List<CalendarCell>,
    today: LocalDate,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    taskColor: Color,
    eventColor: Color,
    todayLabel: String,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                for (cell in week) {
                    DayCell(
                        cell = cell,
                        isToday = cell.date == today,
                        isSelected = cell.date == selected,
                        taskColor = taskColor,
                        eventColor = eventColor,
                        todayLabel = todayLabel,
                        onClick = { onSelect(cell.date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    cell: CalendarCell,
    isToday: Boolean,
    isSelected: Boolean,
    taskColor: Color,
    eventColor: Color,
    todayLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .height(52.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = if (isToday) "$todayLabel ${cell.date}" else cell.date.toString()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .then(
                    when {
                        isSelected -> Modifier.background(colors.primary, CircleShape)
                        isToday -> Modifier.border(1.dp, colors.primary, CircleShape)
                        else -> Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = cell.date.day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    isSelected -> colors.onPrimary
                    isToday -> colors.primary
                    cell.inMonth -> colors.onSurface
                    else -> colors.onSurfaceVariant
                },
            )
        }
        Row(
            modifier = Modifier.height(8.dp).padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            repeat(cell.taskCount.coerceAtMost(3)) { Dot(taskColor) }
            repeat(cell.eventCount.coerceAtMost(2)) { Dot(eventColor) }
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(Modifier.size(4.dp).background(color, CircleShape))
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun EventRow(event: DeviceEvent, allDayLabel: String, zone: kotlinx.datetime.TimeZone) {
    val start = Instant.fromEpochMilliseconds(event.startMillis).toLocalDateTime(zone)
    val time = if (event.allDay) allDayLabel else "${start.hour.pad()}:${start.minute.pad()}"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(28.dp)
                .background(MaterialTheme.colorScheme.secondary, MaterialTheme.shapes.extraSmall),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(event.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (event.calendarName.isNotBlank()) {
                Text(
                    event.calendarName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            time,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CalendarTaskRow(
    task: Task,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
    zone: kotlinx.datetime.TimeZone,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledIconButton(
            onClick = onToggleDone,
            modifier = Modifier.size(24.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (task.done) MaterialTheme.colorScheme.primary else Color.Transparent,
                contentColor = if (task.done) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline,
            ),
        ) {
            Box(
                Modifier
                    .size(20.dp)
                    .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (task.done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CalendarPermissionCard(
    title: String,
    body: String,
    action: String,
    onAllow: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onAllow) { Text(action) }
    }
}

@Composable
private fun EmptyDay(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun Int.pad(): String = if (this < 10) "0$this" else toString()
