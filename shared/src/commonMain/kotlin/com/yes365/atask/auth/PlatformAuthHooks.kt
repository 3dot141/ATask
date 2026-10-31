package com.yes365.atask.auth

interface PlatformAuthHooks {
    fun cancelRefreshScheduler()
    fun scheduleRefresh()
    fun updateWidgets()
}
