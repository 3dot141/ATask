package com.yes365.atask.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

expect fun createDataStore(context: PlatformContext, name: String): DataStore<Preferences>
