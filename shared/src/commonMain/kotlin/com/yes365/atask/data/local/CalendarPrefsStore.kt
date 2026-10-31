package com.yes365.atask.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Which of the phone's own calendars the Calendar screen shows, as the identifiers the person
 * turned off. Storing the *hidden* ones rather than the visible ones keeps a calendar that is
 * added on the phone later visible without touching this setting.
 *
 * A device setting: the calendars belong to this phone, so it never syncs.
 */
class CalendarPrefsStore(
    private val dataStore: DataStore<Preferences>,
) {
    private companion object {
        val KEY_HIDDEN = stringSetPreferencesKey("hidden_calendar_ids")
    }

    fun hiddenCalendarIds(): Flow<Set<String>> = dataStore.data.map { it[KEY_HIDDEN] ?: emptySet() }

    suspend fun setHidden(ids: Set<String>) {
        dataStore.edit { it[KEY_HIDDEN] = ids }
    }
}
