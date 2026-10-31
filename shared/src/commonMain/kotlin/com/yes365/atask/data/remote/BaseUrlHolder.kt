package com.yes365.atask.data.remote

import com.yes365.atask.auth.TokenStorage
import com.yes365.atask.util.Logger
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.Volatile

class BaseUrlHolder(
    private val tokenStorage: TokenStorage,
) {
    companion object {
        private const val TAG = "BaseUrlHolder"
    }

    @Volatile
    var baseUrl: String = ""

    fun getFullBaseUrl(): String {
        if (baseUrl.isEmpty()) return ""
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return "${normalized}api/v2/"
    }

    suspend fun ensureInitialized() {
        if (baseUrl.isEmpty()) {
            val url = tokenStorage.getVikunjaUrl()
            if (!url.isNullOrBlank()) {
                baseUrl = url
                Logger.d(TAG, "Lazy-initialized baseUrl from DataStore")
            }
        }
    }

    fun ensureInitializedBlocking() {
        if (baseUrl.isEmpty()) {
            runBlocking {
                ensureInitialized()
            }
        }
    }
}
