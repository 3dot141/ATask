package com.yes365.atask.di

import com.yes365.atask.voice.IosSpeechBridgeHolder
import com.yes365.atask.voice.IosSpeechInput
import com.yes365.atask.voice.SpeechInput
import com.yes365.atask.ai.AiKeyStore
import com.yes365.atask.ai.IosAiKeyStore
import com.yes365.atask.speech.AudioRecorder
import com.yes365.atask.speech.IosAudioRecorder
import com.yes365.atask.speech.IosSpeechEngineNative
import com.yes365.atask.speech.IosSpeechOutput
import com.yes365.atask.speech.IosSpeechStorage
import com.yes365.atask.speech.SpeechEngineNative
import com.yes365.atask.speech.SpeechOutput
import com.yes365.atask.speech.SpeechStorage
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.yes365.atask.auth.AuthManager
import com.yes365.atask.auth.IosAuthHooks
import com.yes365.atask.auth.IosKeychainTokenStorage
import com.yes365.atask.auth.PlatformAuthHooks
import com.yes365.atask.auth.TokenStorage
import com.yes365.atask.data.local.PlatformContext
import com.yes365.atask.data.remote.BaseUrlHolder
import com.yes365.atask.data.remote.VikunjaCoilInterceptor
import com.yes365.atask.data.repository.IosRepositoryHooks
import com.yes365.atask.domain.repository.PlatformRepositoryHooks
import com.yes365.atask.ios.BackgroundActivityLog
import com.yes365.atask.ios.IosAppLifecycle
import com.yes365.atask.ios.IosBackgroundTasks
import com.yes365.atask.location.DeviceLocation
import com.yes365.atask.location.IosDeviceLocation
import com.yes365.atask.location.IosGeofencer
import com.yes365.atask.location.IosLocationBridgeHolder
import com.yes365.atask.location.IosLocationNotifier
import com.yes365.atask.location.IosLocationPermissionPlatform
import com.yes365.atask.location.IosPlaceSearch
import com.yes365.atask.location.LocationNotifier
import com.yes365.atask.location.LocationPermissionPlatform
import com.yes365.atask.location.PlaceSearch
import com.yes365.atask.location.PlatformGeofencer
import com.yes365.atask.notification.IosNotificationScheduler
import com.yes365.atask.permission.IosNotificationPermissionPlatform
import com.yes365.atask.permission.NotificationPermissionPlatform
import com.yes365.atask.share.IosShareInbox
import com.yes365.atask.ui.AppInputs
import com.yes365.atask.ui.screens.settings.IosSettingsHooks
import com.yes365.atask.ui.screens.settings.PlatformSettingsHooks
import com.yes365.atask.util.IosCompletionSoundPlayer
import com.yes365.atask.util.IosNetworkMonitor
import com.yes365.atask.util.IosPaths
import com.yes365.atask.util.IosPlatformFiles
import com.yes365.atask.util.NetworkMonitor
import com.yes365.atask.util.PlatformFiles
import com.yes365.atask.util.TaskReminderNotifications
import com.yes365.atask.widget.IosWidgetSnapshots
import com.yes365.atask.worker.SyncEngine
import com.yes365.atask.worker.SyncRequester
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import okio.Path.Companion.toPath
import org.koin.dsl.module
import platform.Foundation.NSHTTPCookieAcceptPolicy

/**
 * The Darwin engine with NSURLSession's own cookie handling off: the refresh token arrives as a
 * cookie and AuthManager sends it itself, so the session must not store and resend it as well.
 */
internal fun darwinEngine(): HttpClientEngine = Darwin.create {
    configureSession {
        HTTPShouldSetCookies = false
        HTTPCookieStorage = null
        HTTPCookieAcceptPolicy = NSHTTPCookieAcceptPolicy.NSHTTPCookieAcceptPolicyNever
    }
}

/** The iOS implementations of the platform interfaces, next to [sharedModules]. */
val iosModule = module {
    single { PlatformContext() }
    single<HttpClientEngine> { darwinEngine() }
    single<TokenStorage> { IosKeychainTokenStorage() }
    single<PlatformAuthHooks> { IosAuthHooks(backgroundTasks = { get() }, widgets = { get() }) }
    single<PlatformFiles> { IosPlatformFiles() }
    single<NetworkMonitor> { IosNetworkMonitor() }
    single { AppInputs() }
    single {
        SyncRequester(
            scope = get(),
            sync = { get<SyncEngine>().performSync() },
            isSignedIn = {
                get<BaseUrlHolder>().ensureInitialized()
                !get<AuthManager>().getVikunjaUrl().isNullOrBlank()
            },
        )
    }
    single<PlatformSettingsHooks> {
        IosSettingsHooks(
            syncRequester = { get() },
            notifications = { get() },
            permission = get(),
            backgroundLog = get(),
            widgets = { get() },
        )
    }
    single { IosCompletionSoundPlayer(get()) }
    single<PlatformRepositoryHooks> {
        IosRepositoryHooks(
            syncRequester = { get() },
            soundPlayer = get(),
            notifications = { get() },
            backgroundTasks = { get() },
            widgets = { get() },
            shareInbox = { get() },
            appScope = get(),
        )
    }
    single {
        IosWidgetSnapshots(
            states = get(),
            customListStore = get(),
            routineRepository = get(),
            routinePrefs = get(),
            taskRepository = get(),
            dayClock = get(),
            syncRequester = { get() },
            appScope = get(),
        )
    }
    single {
        IosShareInbox(
            taskRepository = get(),
            attachmentRepository = get(),
            tokenStorage = get(),
            networkMonitor = get(),
            appMessages = get(),
            syncRequester = { get() },
        )
    }
    single {
        IosNotificationScheduler(
            taskDao = get(),
            taskMapper = get(),
            snoozeStore = get(),
            routineRepository = get(),
            routinePrefs = get(),
            notificationPrefs = get(),
            summaryReader = get(),
            authManager = get(),
            time = get(),
            appScope = get(),
        )
    }
    single<TaskReminderNotifications> { get<IosNotificationScheduler>() }
    single { BackgroundActivityLog(get()) }
    single {
        IosBackgroundTasks(
            tokenRefreshRun = get(),
            syncEngine = get(),
            notifications = get(),
            widgets = get(),
            shareInbox = get(),
            authManager = get(),
            baseUrlHolder = get(),
            pendingActionDao = get(),
            activityLog = get(),
            time = get(),
            appScope = get(),
            locations = get(),
        )
    }
    single { IosNotificationPermissionPlatform() }
    single { IosLocationBridgeHolder() }
    single { IosSpeechBridgeHolder() }
    single<SpeechInput> { IosSpeechInput(get()) }
    // Local speech (docs/ai-assistant-v1.md, section 3) and the AI provider keys (7.2). None of
    // them touches the audio session, the Keychain or the network until first used.
    single<SpeechEngineNative> { IosSpeechEngineNative() }
    single<AudioRecorder> { IosAudioRecorder() }
    single<SpeechOutput> { IosSpeechOutput() }
    single<SpeechStorage> { IosSpeechStorage() }
    single<AiKeyStore> { IosAiKeyStore() }
    single { IosLocationPermissionPlatform(get()) }
    single<LocationPermissionPlatform> { get<IosLocationPermissionPlatform>() }
    single<PlatformGeofencer> { IosGeofencer(get()) }
    single<DeviceLocation> { IosDeviceLocation(get()) }
    single<PlaceSearch> { IosPlaceSearch() }
    single<LocationNotifier> { IosLocationNotifier() }
    single<NotificationPermissionPlatform> { get<IosNotificationPermissionPlatform>() }
    single {
        IosAppLifecycle(
            authManager = get(),
            baseUrlHolder = get(),
            syncRequester = get(),
            syncEngine = get(),
            networkMonitor = get(),
            dayClock = get(),
            notificationPermission = get(),
            notifications = get(),
            backgroundTasks = get(),
            widgets = get(),
            shareInbox = get(),
            appScope = get(),
            locations = get(),
            locationPermission = get(),
        )
    }
    single {
        val baseUrlHolder: BaseUrlHolder = get()
        val authManager: AuthManager = get()
        ImageLoader.Builder(coil3.PlatformContext.INSTANCE)
            .components {
                add(
                    VikunjaCoilInterceptor(
                        fullBaseUrl = {
                            baseUrlHolder.ensureInitialized()
                            baseUrlHolder.getFullBaseUrl()
                        },
                        token = { authManager.getBestTokenSync() ?: authManager.ensureInitializedAndGetToken() },
                    ),
                )
                add(KtorNetworkFetcherFactory(httpClient = { HttpClient(darwinEngine()) }))
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(IosPaths.subdirectory(IosPaths.caches, "image_cache").toPath())
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .build()
    }
}
