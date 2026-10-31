package com.yes365.atask.location

import com.yes365.atask.data.remote.createApiJson
import com.yes365.atask.data.repository.InMemoryPreferencesDataStore
import com.yes365.atask.data.repository.InMemorySyncedEntries
import kotlinx.coroutines.flow.MutableStateFlow

/** Location access a test sets; counts the trips to the system settings. */
class FakeLocationPermission(initial: LocationAccess = LocationAccess.ALWAYS) : LocationPermissionPlatform {
    override val access = MutableStateFlow(initial)
    var settingsOpened = 0
        private set

    override fun refresh() {}

    override fun request() = true

    override fun openSettings() {
        settingsOpened++
    }
}

/** A location repository over in-memory synced entries and stores. */
fun testLocations(synced: InMemorySyncedEntries = InMemorySyncedEntries()) = LocationRepository(
    synced = synced,
    positions = SavedPlacesStore(InMemoryPreferencesDataStore(), createApiJson()),
    local = LocationReminderStore(InMemoryPreferencesDataStore(), createApiJson()),
)
