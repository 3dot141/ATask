package com.yes365.atask

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.yes365.atask.auth.AuthDebugLog
import com.yes365.atask.auth.AuthManager
import com.yes365.atask.location.LocationReminderCoordinator
import com.yes365.atask.notification.NotificationChannelManager
import com.yes365.atask.util.BuildInfo
import com.yes365.atask.util.DayChangeReceiver
import com.yes365.atask.util.DayClock
import com.yes365.atask.util.NetworkMonitor
import com.yes365.atask.widget.WidgetUpdateScheduler
import com.yes365.atask.worker.RoutineMaintenanceScheduler
import com.yes365.atask.worker.SyncScheduler
import com.yes365.atask.util.reconnections
import com.yes365.atask.voice.LauncherShortcuts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.androidx.workmanager.koin.workManagerFactory
import com.yes365.atask.di.androidAppModules
import com.yes365.atask.di.sharedModules

class VicuApplication : Application(), SingletonImageLoader.Factory, KoinComponent {

    private val notificationChannelManager: NotificationChannelManager by inject()
    private val imageLoader: ImageLoader by inject()
    private val dayClock: DayClock by inject()
    private val networkMonitor: NetworkMonitor by inject()
    private val authManager: AuthManager by inject()
    private val appScope: CoroutineScope by inject()
    private val locationReminders: LocationReminderCoordinator by inject()

    override fun onCreate() {
        super.onCreate()
        // Tell the shared module which build this is before anything logs or builds the HTTP
        // client: release builds get no request logging, no debug logs and no auth log file.
        BuildInfo.configure(isDebug = BuildConfig.DEBUG, versionName = BuildConfig.VERSION_NAME)
        AuthDebugLog.init(this)
        startKoin {
            androidContext(this@VicuApplication)
            workManagerFactory()
            modules(sharedModules + androidAppModules)
        }
        notificationChannelManager.createChannels()
        LauncherShortcuts.publish(this)
        DayChangeReceiver.register(this, dayClock)
        // Location reminders: follow changes from now on, and hand the regions over again (Play
        // services forgets them after a reboot and when location was off).
        locationReminders.start()
        appScope.launch { locationReminders.replan(force = true) }
        appScope.launch {
            // Only for an account: sign-out cancels the account's background work, and scheduling
            // it again at every start would undo that. Signing in schedules it (MainActivity).
            if (!authManager.getVikunjaUrl().isNullOrBlank()) {
                WidgetUpdateScheduler.schedulePeriodicRefresh(this@VicuApplication)
                RoutineMaintenanceScheduler.schedule(this@VicuApplication)
            }
        }
        // A sync that failed while the device was offline waits out its backoff, minutes to hours,
        // even after the network is back. Start it as soon as the network returns instead.
        appScope.launch {
            networkMonitor.isOnline.reconnections().collect {
                if (!authManager.getVikunjaUrl().isNullOrBlank()) {
                    SyncScheduler.enqueueWhenOnline(this@VicuApplication)
                }
            }
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader
}
