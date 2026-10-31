package com.rendyhd.vicu.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import com.rendyhd.vicu.util.deviceLocaleTag
import kotlinx.datetime.LocalDate

/**
 * The app's user-facing text, one instance per language.
 *
 * The language follows the device: [resolveStrings] reads [deviceLocaleTag] on every start, so a
 * Chinese phone opens in Chinese and an English one in English without a setting. A tag that names
 * no language we ship falls back to English, which is also what the rest of the app is written in.
 *
 * Dates are phrased here too: the shared [com.rendyhd.vicu.util.DateDisplay] builds its month and
 * weekday names in English by contract, so a localized screen must not use it for a month header.
 */
interface Strings {
    val languageTag: String

    // Shell
    val inbox: String
    val today: String
    val calendar: String
    val anytime: String
    val logbook: String
    val overdue: String
    val routines: String
    val review: String
    val ask: String
    val search: String
    val settings: String
    val projects: String

    // Shared actions
    val retry: String
    val cancel: String
    val done: String
    val delete: String

    // Lists
    val nothingUpcoming: String
    val upcomingSubtitle: String
    val nothingToday: String
    val todaySubtitle: String
    val nothingInbox: String
    val inboxSubtitle: String
    val nothingAnytime: String
    val anytimeSubtitle: String
    val nothingLogged: String
    val logbookSubtitle: String

    // Calendar
    val calendarTitle: String
    val previousMonth: String
    val nextMonth: String
    val todayLabel: String
    val eventsSection: String
    val tasksSection: String
    val noEventsToday: String
    val noTasksToday: String
    val calendarPermissionTitle: String
    val calendarPermissionBody: String
    val allowCalendarAccess: String
    val calendarPermissionDenied: String
    val allDay: String
    val chooseCalendars: String
    val calendarsHint: String

    // Setup
    val welcomeTitle: String
    val welcomeSubtitle: String
    val serverUrlLabel: String
    val cloudHint: String
    val continueLabel: String
    val signInTitle: String
    val signInSubtitle: String
    val signInWithPassword: String
    val signInPasswordSubtitle: String
    val useApiToken: String
    val apiTokenSubtitle: String
    val usernameLabel: String
    val passwordLabel: String
    val totpLabel: String
    val noAccount: String
    val newTask: String
    val signUp: String

    /** Monday-first short weekday names, seven entries. */
    val weekdaysShort: List<String>

    /** "October 2026" in English, "2026年10月" in Chinese. */
    fun monthTitle(year: Int, month: Int): String

    /** "Saturday, October 31" in English, "10月31日 星期六" in Chinese. */
    fun dayTitle(date: LocalDate): String

    /** "31" — the number drawn in a day cell. */
    fun dayNumber(date: LocalDate): String = date.day.toString()
}

/** The language a locale tag asks for: Chinese for `zh…`, English for everything else. */
fun languageFor(tag: String): String = if (tag.lowercase().startsWith("zh")) "zh-Hans" else "en"

/** The strings for a locale tag. */
fun stringsFor(tag: String): Strings = if (languageFor(tag) == "zh-Hans") ChineseStrings else EnglishStrings

/** The strings for this device. */
fun deviceStrings(): Strings = stringsFor(deviceLocaleTag())

val LocalStrings = staticCompositionLocalOf<Strings> { EnglishStrings }

private val EN_MONTHS = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)
private val EN_WEEKDAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val EN_WEEKDAYS_LONG = listOf(
    "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday",
)
private val ZH_WEEKDAYS = listOf("一", "二", "三", "四", "五", "六", "日")
private val ZH_WEEKDAYS_LONG = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")

object EnglishStrings : Strings {
    override val languageTag = "en"
    override val inbox = "Inbox"
    override val today = "Today"
    override val calendar = "Calendar"
    override val anytime = "Anytime"
    override val logbook = "Logbook"
    override val overdue = "Overdue"
    override val routines = "Routines"
    override val review = "Review"
    override val ask = "Ask"
    override val search = "Search"
    override val settings = "Settings"
    override val projects = "Projects"
    override val retry = "Retry"
    override val cancel = "Cancel"
    override val done = "Done"
    override val delete = "Delete"
    override val nothingUpcoming = "Nothing upcoming"
    override val upcomingSubtitle = "Tasks with future due dates appear here"
    override val nothingToday = "Nothing for today"
    override val todaySubtitle = "Tasks due today appear here"
    override val nothingInbox = "Inbox is empty"
    override val inboxSubtitle = "New tasks land here first"
    override val nothingAnytime = "Nothing to do"
    override val anytimeSubtitle = "Tasks outside the Inbox appear here"
    override val nothingLogged = "Nothing logged yet"
    override val logbookSubtitle = "Completed tasks appear here"
    override val calendarTitle = "Calendar"
    override val previousMonth = "Previous month"
    override val nextMonth = "Next month"
    override val todayLabel = "Today"
    override val eventsSection = "Events"
    override val tasksSection = "Tasks"
    override val noEventsToday = "No events"
    override val noTasksToday = "No tasks"
    override val calendarPermissionTitle = "Show your calendar"
    override val calendarPermissionBody =
        "Vicu can show the events from your device calendar next to your tasks. Nothing leaves your phone."
    override val allowCalendarAccess = "Allow calendar access"
    override val calendarPermissionDenied = "Calendar access is off. Turn it on in Settings to see events."
    override val allDay = "All day"
    override val chooseCalendars = "Calendars"
    override val calendarsHint = "Only the calendars you tick are shown here."
    override val welcomeTitle = "Welcome to Vicu"
    override val welcomeSubtitle = "Enter your Vikunja server URL to get started."
    override val serverUrlLabel = "Server URL"
    override val cloudHint = "Leave empty to use Vikunja Cloud"
    override val continueLabel = "Continue"
    override val signInTitle = "Sign In"
    override val signInSubtitle = "Choose how to authenticate with your server."
    override val signInWithPassword = "Sign in with password"
    override val signInPasswordSubtitle = "Username and password"
    override val useApiToken = "Use API token"
    override val apiTokenSubtitle = "Manual token entry"
    override val usernameLabel = "Username"
    override val passwordLabel = "Password"
    override val totpLabel = "TOTP Code"
    override val noAccount = "Don't have an account? "
    override val newTask = "New task"
    override val signUp = "Sign up"
    override val weekdaysShort = EN_WEEKDAYS

    override fun monthTitle(year: Int, month: Int): String = "${EN_MONTHS[month - 1]} $year"

    override fun dayTitle(date: LocalDate): String {
        val weekday = EN_WEEKDAYS_LONG[date.dayOfWeek.ordinal]
        return "$weekday, ${EN_MONTHS[date.monthNumber - 1]} ${date.day}"
    }
}

object ChineseStrings : Strings {
    override val languageTag = "zh-Hans"
    override val inbox = "收件箱"
    override val today = "今天"
    override val calendar = "日历"
    override val anytime = "随时"
    override val logbook = "日志"
    override val overdue = "逾期"
    override val routines = "习惯"
    override val review = "回顾"
    override val ask = "助手"
    override val search = "搜索"
    override val settings = "设置"
    override val projects = "项目"
    override val retry = "重试"
    override val cancel = "取消"
    override val done = "完成"
    override val delete = "删除"
    override val nothingUpcoming = "暂无安排"
    override val upcomingSubtitle = "未来有截止日期的任务会显示在这里"
    override val nothingToday = "今天没有安排"
    override val todaySubtitle = "今天到期的任务会显示在这里"
    override val nothingInbox = "收件箱是空的"
    override val inboxSubtitle = "新任务会先进入这里"
    override val nothingAnytime = "暂无待办"
    override val anytimeSubtitle = "收件箱以外的任务会显示在这里"
    override val nothingLogged = "还没有记录"
    override val logbookSubtitle = "已完成的任务会显示在这里"
    override val calendarTitle = "日历"
    override val previousMonth = "上个月"
    override val nextMonth = "下个月"
    override val todayLabel = "今天"
    override val eventsSection = "日程"
    override val tasksSection = "任务"
    override val noEventsToday = "没有日程"
    override val noTasksToday = "没有任务"
    override val calendarPermissionTitle = "显示你的日历"
    override val calendarPermissionBody = "Vicu 可以把设备日历中的日程和任务一起显示。数据不会离开手机。"
    override val allowCalendarAccess = "允许访问日历"
    override val calendarPermissionDenied = "日历权限已关闭。可在系统设置中开启后查看日程。"
    override val allDay = "全天"
    override val chooseCalendars = "选择日历"
    override val calendarsHint = "只显示勾选的日历中的日程"
    override val welcomeTitle = "欢迎使用 Vicu"
    override val welcomeSubtitle = "填写你的 Vikunja 服务器地址即可开始。"
    override val serverUrlLabel = "服务器地址"
    override val cloudHint = "留空则使用 Vikunja Cloud"
    override val continueLabel = "继续"
    override val signInTitle = "登录"
    override val signInSubtitle = "选择与服务器认证的方式。"
    override val signInWithPassword = "使用密码登录"
    override val signInPasswordSubtitle = "用户名和密码"
    override val useApiToken = "使用 API Token"
    override val apiTokenSubtitle = "手动填写 Token"
    override val usernameLabel = "用户名"
    override val passwordLabel = "密码"
    override val totpLabel = "动态验证码"
    override val noAccount = "还没有账号？"
    override val newTask = "新建任务"
    override val signUp = "注册"
    override val weekdaysShort = ZH_WEEKDAYS

    override fun monthTitle(year: Int, month: Int): String = "${year}年${month}月"

    override fun dayTitle(date: LocalDate): String =
        "${date.monthNumber}月${date.day}日 ${ZH_WEEKDAYS_LONG[date.dayOfWeek.ordinal]}"
}
