package com.rendyhd.vicu.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rendyhd.vicu.device.CalendarAccess
import com.rendyhd.vicu.device.DeviceCalendar
import com.rendyhd.vicu.device.DeviceEvent
import com.rendyhd.vicu.domain.model.Task
import com.rendyhd.vicu.domain.repository.TaskRepository
import com.rendyhd.vicu.util.DateUtils
import com.rendyhd.vicu.util.DayClock
import com.rendyhd.vicu.util.DueDates
import com.rendyhd.vicu.util.WhenLogic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime

/** One square of the month grid. */
data class CalendarCell(
    val date: LocalDate,
    val inMonth: Boolean,
    val taskCount: Int,
    val eventCount: Int,
)

data class CalendarUiState(
    val month: LocalDate,
    val selected: LocalDate,
    val today: LocalDate,
    val cells: List<CalendarCell> = emptyList(),
    val dayTasks: List<Task> = emptyList(),
    val dayEvents: List<DeviceEvent> = emptyList(),
    val access: CalendarAccess = CalendarAccess.NotDetermined,
    val isLoading: Boolean = true,
)

/**
 * The month view: the open tasks of the account and the events of the phone's own calendar, both
 * keyed by the local day.
 *
 * Tasks come from the same due-date rules the lists use ([DueDates.localDateOf]), so a task lands
 * on the same day here as in Today or Upcoming. Events are read for the six weeks on screen
 * whenever the month or the access changes.
 */
class CalendarViewModel(
    private val taskRepository: TaskRepository,
    private val dayClock: DayClock,
) : ViewModel() {

    private val deviceCalendar = DeviceCalendar()

    private val startDay = Clock.System.todayIn(TimeZone.currentSystemDefault())
    private val _uiState = MutableStateFlow(
        CalendarUiState(
            month = WhenLogic.monthStart(startDay),
            selected = startDay,
            today = startDay,
        )
    )
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private var tasksByDay: Map<LocalDate, List<Task>> = emptyMap()
    private var eventsByDay: Map<LocalDate, List<DeviceEvent>> = emptyMap()
    private var zone: TimeZone = TimeZone.currentSystemDefault()
    private var monthTouched = false

    init {
        _uiState.update { it.copy(access = deviceCalendar.access()) }
        viewModelScope.launch {
            combine(taskRepository.getAllOpenTasksFlat(), dayClock.day) { tasks, day -> tasks to day }
                .collect { (tasks, day) ->
                    zone = day.zone
                    tasksByDay = groupTasksByDay(tasks, day.zone)
                    _uiState.update { state ->
                        val month = if (monthTouched) state.month else WhenLogic.monthStart(day.date)
                        val selected = state.selected
                        state.copy(month = month, today = day.date, selected = selected, isLoading = false)
                    }
                    publish()
                }
        }
        viewModelScope.launch { loadEvents() }
    }

    fun showMonth(offset: Int) {
        monthTouched = true
        _uiState.update { it.copy(month = WhenLogic.addMonths(it.month, offset)) }
        publish()
        viewModelScope.launch { loadEvents() }
    }

    fun select(date: LocalDate) {
        _uiState.update {
            it.copy(
                selected = date,
                // Picking a dimmed day of the neighbouring month moves the grid there.
                month = if (date.year == it.month.year && date.month == it.month.month) it.month else WhenLogic.monthStart(date),
            )
        }
        publish()
    }

    fun requestCalendarAccess() {
        viewModelScope.launch {
            val access = deviceCalendar.requestAccess()
            _uiState.update { it.copy(access = access) }
            if (access == CalendarAccess.Granted) loadEvents()
        }
    }

    fun toggleDone(task: Task) {
        viewModelScope.launch { taskRepository.toggleDone(task) }
    }

    fun refresh() {
        viewModelScope.launch { loadEvents() }
    }

    private suspend fun loadEvents() {
        if (deviceCalendar.access() != CalendarAccess.Granted) return
        val grid = WhenLogic.monthGrid(_uiState.value.month)
        val first = grid.first().first().date
        val last = grid.last().last().date
        val startMillis = first.atStartOfDayIn(zone).toEpochMilliseconds()
        val endMillis = WhenLogic.addMonths(last, 1).atStartOfDayIn(zone).toEpochMilliseconds()
        val events = deviceCalendar.events(startMillis, endMillis)
        eventsByDay = events.groupBy {
            Instant.fromEpochMilliseconds(it.startMillis).toLocalDateTime(zone).date
        }
        publish()
    }

    private fun publish() {
        _uiState.update { state ->
            val grid = WhenLogic.monthGrid(state.month)
            val cells = grid.flatten().map { day ->
                CalendarCell(
                    date = day.date,
                    inMonth = day.inMonth,
                    taskCount = tasksByDay[day.date]?.size ?: 0,
                    eventCount = eventsByDay[day.date]?.size ?: 0,
                )
            }
            state.copy(
                cells = cells,
                dayTasks = tasksByDay[state.selected].orEmpty(),
                dayEvents = eventsByDay[state.selected].orEmpty(),
            )
        }
    }

    private fun groupTasksByDay(tasks: List<Task>, zone: TimeZone): Map<LocalDate, List<Task>> =
        tasks
            .mapNotNull { task ->
                val due = DateUtils.parseIsoDate(task.dueDate) ?: return@mapNotNull null
                DueDates.localDateOf(due, zone) to task
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, list) -> list.sortedWith(compareBy({ it.position }, { it.id })) }
}
