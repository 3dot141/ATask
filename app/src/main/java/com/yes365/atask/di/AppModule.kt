package com.yes365.atask.di

import com.yes365.atask.voice.AndroidSpeechInput
import com.yes365.atask.voice.SpeechInput
import com.yes365.atask.ai.AiKeyStore
import com.yes365.atask.ai.AndroidAiKeyStore
import com.yes365.atask.speech.AndroidAudioRecorder
import com.yes365.atask.speech.AndroidSpeechEngineNative
import com.yes365.atask.speech.AndroidSpeechOutput
import com.yes365.atask.speech.AndroidSpeechStorage
import com.yes365.atask.speech.AudioRecorder
import com.yes365.atask.speech.SpeechEngineNative
import com.yes365.atask.speech.SpeechOutput
import com.yes365.atask.speech.SpeechStorage
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import android.content.pm.ApplicationInfo
import java.util.concurrent.TimeUnit
import com.yes365.atask.auth.AndroidAuthHooks
import com.yes365.atask.auth.AndroidSecureTokenStorage
import com.yes365.atask.auth.AuthManager
import com.yes365.atask.auth.PlatformAuthHooks
import com.yes365.atask.auth.TokenStorage
import com.yes365.atask.data.local.PlatformContext
import com.yes365.atask.data.remote.BaseUrlHolder
import com.yes365.atask.data.remote.VikunjaImageInterceptor
import com.yes365.atask.data.repository.AndroidRepositoryHooks
import com.yes365.atask.domain.repository.PlatformRepositoryHooks
import com.yes365.atask.notification.AlarmScheduler
import com.yes365.atask.util.TaskReminderNotifications
import com.yes365.atask.notification.DailySummaryScheduler
import com.yes365.atask.notification.NotificationChannelManager
import com.yes365.atask.notification.RoutineAlarmScheduler
import com.yes365.atask.util.CompletionSoundPlayer
import com.yes365.atask.util.AndroidNetworkMonitor
import com.yes365.atask.util.NetworkMonitor
import com.yes365.atask.util.PlatformFiles
import com.yes365.atask.util.AndroidPlatformFiles
import com.yes365.atask.BuildConfig
import com.yes365.atask.location.AndroidDeviceLocation
import com.yes365.atask.location.AndroidGeofencer
import com.yes365.atask.location.AndroidLocationNotifier
import com.yes365.atask.location.AndroidLocationPermissionPlatform
import com.yes365.atask.location.DeviceLocation
import com.yes365.atask.location.LocationNotifier
import com.yes365.atask.location.LocationPermissionPlatform
import com.yes365.atask.location.PhotonPlaceSearch
import com.yes365.atask.location.PlaceSearch
import com.yes365.atask.location.PlatformGeofencer
import com.yes365.atask.permission.AndroidNotificationPermissionPlatform
import com.yes365.atask.permission.NotificationPermissionPlatform
import com.yes365.atask.ui.screens.settings.PlatformSettingsHooks
import com.yes365.atask.ui.screens.settings.AndroidSettingsHooks
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.androidx.workmanager.dsl.workerOf
import com.yes365.atask.worker.SyncWorker
import com.yes365.atask.worker.DailySummaryWorker
import com.yes365.atask.worker.PeriodicSyncWorker
import com.yes365.atask.worker.TokenRefreshWorker
import com.yes365.atask.worker.RoutineMaintenanceWorker
import com.yes365.atask.widget.RoutineWidgetActionWorker
import com.yes365.atask.widget.TaskWidgetWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toOkioPath


val appModule = module {
    single { PlatformContext(androidContext()) }
    single<TokenStorage> { AndroidSecureTokenStorage(androidContext()) }
    single<PlatformAuthHooks> { AndroidAuthHooks(androidContext()) }
    single<PlatformRepositoryHooks> {
        AndroidRepositoryHooks(
            context = androidContext(),
            alarmScheduler = get(),
            routineAlarmSchedulerProvider = { get() },
            completionSoundPlayer = get(),
            appScope = get(),
            dailySummaryScheduler = get(),
        )
    }
    single<NetworkMonitor> { AndroidNetworkMonitor(androidContext()) }
    single<HttpClientEngine> { OkHttp.create() }
    single<PlatformFiles> { AndroidPlatformFiles(get()) }
    single<PlatformSettingsHooks> { AndroidSettingsHooks(androidContext(), get()) }
    single { AndroidNotificationPermissionPlatform(androidContext()) }
    single<NotificationPermissionPlatform> { get<AndroidNotificationPermissionPlatform>() }
    single { AndroidLocationPermissionPlatform(androidContext()) }
    single<LocationPermissionPlatform> { get<AndroidLocationPermissionPlatform>() }
    single { AndroidSpeechInput(androidContext()) }
    single<SpeechInput> { get<AndroidSpeechInput>() }
    // Local speech (docs/ai-assistant-v1.md, section 3) and the AI provider keys (7.2). The
    // recorder asks for the microphone through the same prompt as holding +.
    single<SpeechEngineNative> { AndroidSpeechEngineNative() }
    single<AudioRecorder> { AndroidAudioRecorder(get()) }
    single<SpeechOutput> { AndroidSpeechOutput(androidContext()) }
    single<SpeechStorage> { AndroidSpeechStorage(androidContext()) }
    single<AiKeyStore> { AndroidAiKeyStore(androidContext()) }
    single<PlatformGeofencer> { AndroidGeofencer(androidContext()) }
    single<DeviceLocation> { AndroidDeviceLocation(androidContext()) }
    single<LocationNotifier> { AndroidLocationNotifier(androidContext()) }
    single<PlaceSearch> {
        PhotonPlaceSearch(
            engine = get(),
            userAgent = "ATask/${BuildConfig.VERSION_NAME} (Android; https://github.com/3dot141/ATask)",
        )
    }

    single { AlarmScheduler(androidContext(), get(), get(), get(), get(), get()) }
    single<TaskReminderNotifications> { get<AlarmScheduler>() }
    single { RoutineAlarmScheduler(androidContext(), get(), get(), get()) }
    single { DailySummaryScheduler(androidContext(), get()) }
    single { CompletionSoundPlayer(androidContext(), get()) }
    single { NotificationChannelManager(androidContext()) }

    single {
        val baseUrlHolder = get<BaseUrlHolder>()
        val authManager = get<AuthManager>()
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(
                VikunjaImageInterceptor(
                    getFullBaseUrl = baseUrlHolder::getFullBaseUrl,
                    initializeBaseUrl = baseUrlHolder::ensureInitializedBlocking,
                    getCachedToken = authManager::getBestTokenSync,
                    initializeAuth = {
                        runBlocking {
                            authManager.ensureInitializedAndGetToken()
                        }
                    },
                )
            )

        val context = androidContext()
        val isDebug = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (isDebug) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.HEADERS
                    redactHeader("Authorization")
                    redactHeader("Cookie")
                    redactHeader("Set-Cookie")
                }
            )
        }
        builder.build()
    }

    single {
        ImageLoader.Builder(androidContext())
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { get<OkHttpClient>() }))
            }
            .diskCache(
                DiskCache.Builder()
                    .directory(androidContext().cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            )
            .build()
    }
}

val workerModule = module {
    workerOf(::SyncWorker)
    workerOf(::PeriodicSyncWorker)
    workerOf(::DailySummaryWorker)
    workerOf(::TokenRefreshWorker)
    workerOf(::TaskWidgetWorker)
    workerOf(::RoutineMaintenanceWorker)
    workerOf(::RoutineWidgetActionWorker)
}

val androidAppModules = listOf(
    appModule,
    workerModule
)
