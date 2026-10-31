package com.yes365.atask.data.local

import com.yes365.atask.data.repository.InMemoryPreferencesDataStore
import com.yes365.atask.domain.model.CustomList
import com.yes365.atask.domain.model.CustomListFilter
import com.yes365.atask.domain.model.CustomListSyncLocalState
import com.yes365.atask.domain.model.toWire
import com.yes365.atask.util.CustomListEnvelope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** When custom lists hold changes that sign-out would lose. */
class CustomListUnsyncedTest {

    private val list = CustomList(id = "a", name = "Errands", filter = CustomListFilter())

    private fun state(dirty: Boolean, withList: Boolean = true) = CustomListSyncLocalState(
        deviceId = "device",
        document = if (withList) {
            CustomListEnvelope.fromLists(listOf(list.toWire()), "device", 1_000L)
        } else {
            CustomListEnvelope.empty("device", 0L)
        },
        dirty = dirty,
    )

    @Test
    fun `nothing to lose without custom lists`() = runTest {
        val store = CustomListStore(InMemoryPreferencesDataStore())
        assertFalse(store.hasUnsyncedChanges.first())

        store.saveSyncState(state(dirty = true, withList = false))
        assertFalse(store.hasUnsyncedChanges.first(), "an empty document has nothing to lose")
    }

    @Test
    fun `a change not written to the server yet counts`() = runTest {
        val store = CustomListStore(InMemoryPreferencesDataStore())

        store.saveSyncState(state(dirty = true))
        assertTrue(store.hasUnsyncedChanges.first())

        store.saveSyncState(state(dirty = false))
        assertFalse(store.hasUnsyncedChanges.first(), "synced")
    }

    @Test
    fun `lists from before custom lists synced count until they are uploaded`() = runTest {
        val store = CustomListStore(InMemoryPreferencesDataStore())

        store.save(list)

        assertTrue(store.hasUnsyncedChanges.first())
    }
}
