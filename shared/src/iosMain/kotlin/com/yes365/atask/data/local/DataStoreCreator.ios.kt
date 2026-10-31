package com.yes365.atask.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.yes365.atask.util.IosPaths
import okio.Path.Companion.toPath

private val dataStoreDir: String by lazy {
    IosPaths.subdirectory(IosPaths.applicationSupport, "datastore").also(IosPaths::excludeFromBackup)
}

actual fun createDataStore(context: PlatformContext, name: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { "$dataStoreDir/$name.preferences_pb".toPath() })
