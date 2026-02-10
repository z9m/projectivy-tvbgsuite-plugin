package tv.projectivy.plugin.wallpaperprovider.sample

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import tv.projectivy.plugin.wallpaperprovider.api.Event
import tv.projectivy.plugin.wallpaperprovider.api.IWallpaperProviderService
import tv.projectivy.plugin.wallpaperprovider.api.Wallpaper
import tv.projectivy.plugin.wallpaperprovider.api.WallpaperDisplayMode
import tv.projectivy.plugin.wallpaperprovider.api.WallpaperType
import tv.projectivy.plugin.wallpaperprovider.sample.ApiService

class WallpaperProviderService: Service() {

    override fun onCreate() {
        super.onCreate()
        Log.e("WallpaperService", "PROJECTIVY_LOG: Service onCreate")
        PreferencesManager.init(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        Log.e("WallpaperService", "PROJECTIVY_LOG: Service onBind")
        return binder
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    private val binder = object : IWallpaperProviderService.Stub() {
        override fun getWallpapers(event: Event?): List<Wallpaper> {
            Log.e("WallpaperService", "PROJECTIVY_LOG: getWallpapers | Event: ${event?.eventType} (${event?.javaClass?.simpleName}) ")

            var forceRefresh = false
            
            if (event is Event.LauncherIdleModeChanged) {
                Log.e("WallpaperService", "PROJECTIVY_LOG: AIDL Idle Changed | isIdle: ${event.isIdle}")

                // If exiting idle mode (screensaver stops)
                if (!event.isIdle) {
                    if (PreferencesManager.refreshOnIdleExit) {
                        Log.e("WallpaperService", "PROJECTIVY_LOG: Idle exit detected and preference is ON. Triggering refresh.")
                        forceRefresh = true
                    } else {
                        // Preference is OFF, return last saved wallpaper
                        val lastUri = PreferencesManager.lastWallpaperUri
                        val lastAuthor = PreferencesManager.lastWallpaperAuthor
                        if (lastUri.isNotBlank()) {
                            Log.e("WallpaperService", "PROJECTIVY_LOG: Idle exit detected but preference is OFF. Returning last wallpaper: $lastUri")
                            return listOf(Wallpaper(uri = lastUri, type = WallpaperType.IMAGE, displayMode = WallpaperDisplayMode.CROP, author = lastAuthor.ifBlank { null }, actionUri = null))
                        } else {
                            Log.e("WallpaperService", "PROJECTIVY_LOG: Idle exit detected but preference is OFF and no last wallpaper saved. Returning empty list.")
                            return emptyList()
                        }
                    }
                } else {
                    // If entering idle mode (screensaver starts), we don't need to refresh
                    Log.e("WallpaperService", "PROJECTIVY_LOG: Idle enter detected. No refresh needed.")
                    return emptyList()
                }
            }

            // Standard refresh logic (TimeElapsed, or confirmed forceRefresh)
            if (event is Event.TimeElapsed || forceRefresh) {
                Log.e("WallpaperService", "PROJECTIVY_LOG: Executing API refresh...")
                val serverUrl = PreferencesManager.serverUrl
                val selectedLayout = PreferencesManager.selectedLayout
                val genreFilter = PreferencesManager.genreFilter.ifEmpty { null }
                val ageFilter = PreferencesManager.ageFilter.ifEmpty { null }
                val yearFilter = PreferencesManager.yearFilter.ifEmpty { null }
                val minRating = PreferencesManager.minRating
                val maxRating = PreferencesManager.maxRating

                if (serverUrl.isBlank()) return emptyList()

                try {
                    val apiService = Retrofit.Builder()
                        .baseUrl(serverUrl)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                        .create(ApiService::class.java)

                    val response = apiService.getWallpaperStatus(
                        layout = selectedLayout,
                        genre = genreFilter,
                        age = ageFilter,
                        year = yearFilter,
                        minRating = if (minRating > 0.0f) minRating else null,
                        maxRating = if (maxRating < 10.0f) maxRating else null
                    ).execute()

                    if (response.isSuccessful) {
                        val status = response.body()
                        if (status != null) {
                            var action = status.actionUrl
                            if (!action.isNullOrBlank() && action.startsWith("jellyfin://items/")) {
                                val id = action.substringAfter("jellyfin://items/")
                                if (isPackageInstalled("org.moonfin.androidtv")) {
                                    action = "intent:#Intent;component=org.moonfin.androidtv/org.jellyfin.androidtv.ui.startup.StartupActivity;action=android.intent.action.VIEW;S.ItemId=$id;S.id=$id;end"
                                }
                            }
                            Log.e("WallpaperService", "PROJECTIVY_LOG: API Success: ${status.imageUrl}")
                            
                            // Save last successfully loaded wallpaper
                            PreferencesManager.lastWallpaperUri = status.imageUrl
                            // Use selectedLayout as author, or a default if selectedLayout is blank
                            PreferencesManager.lastWallpaperAuthor = selectedLayout.ifBlank { "" }

                            return listOf(
                                Wallpaper(
                                    uri = status.imageUrl,
                                    type = WallpaperType.IMAGE,
                                    displayMode = WallpaperDisplayMode.CROP,
                                    author = selectedLayout,
                                    actionUri = action
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WallpaperService", "PROJECTIVY_LOG: AIDL Error", e)
                }
            } else {
                Log.e("WallpaperService", "PROJECTIVY_LOG: Event type ${event?.javaClass?.simpleName} not handled for refresh or forceRefresh is false.")
            }
            return emptyList()
        }

        override fun getPreferences(): String = PreferencesManager.export()
        override fun setPreferences(params: String) { PreferencesManager.import(params) }
    }
}