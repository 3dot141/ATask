package com.yes365.atask.auth

import android.content.Context
import com.yes365.atask.worker.TokenRefreshScheduler
import com.yes365.atask.widget.WidgetUpdateScheduler

class AndroidAuthHooks(private val context: Context) : PlatformAuthHooks {
    override fun cancelRefreshScheduler() {
        TokenRefreshScheduler.cancel(context)
    }

    override fun scheduleRefresh() {
        TokenRefreshScheduler.schedule(context)
    }

    override fun updateWidgets() {
        WidgetUpdateScheduler.enqueueImmediateUpdateAll(context)
    }
}
