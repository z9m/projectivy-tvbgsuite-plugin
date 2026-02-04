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
import tv.projectivy.plugin.wallpaperprovider.api.WallpaperType
import tv.projectivy.plugin.wallpaperprovider.sample.ApiService

class WallpaperProviderService: Service() {

    override fun onCreate() {
        super.onCreate()
        PreferencesManager.init(this)
    }

    override fun onBind(intent: Intent): IBinder {
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
            Log.d("WallpaperService", "getWallpapers called with event: $event")
            
            // Handle both TimeElapsed and LauncherIdleModeChanged (or any other relevant event)
            if (event is Event.TimeElapsed || event is Event.LauncherIdleModeChanged) {
                val serverUrl = PreferencesManager.serverUrl
                val selectedLayout = PreferencesManager.selectedLayout
                val genreFilter = PreferencesManager.genreFilter.ifEmpty { null }
                val sortOrder = PreferencesManager.sortOrder.lowercase().ifEmpty { null }
                val ageFilter = PreferencesManager.ageFilter.ifEmpty { null }

                if (serverUrl.isBlank()) {
                    Log.w("WallpaperService", "Server URL is blank")
                    return emptyList()
                }

                try {
                    val retrofit = Retrofit.Builder()
                        .baseUrl(serverUrl)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()

                    val apiService = retrofit.create(ApiService::class.java)
                    val response = apiService.getWallpaperStatus(
                        layout = selectedLayout,
                        genre = genreFilter,
                        sort = sortOrder,
                        age = ageFilter
                    ).execute()

                    if (response.isSuccessful) {
                        val status = response.body()
                        if (status != null) {
                            var finalActionUri = status.actionUrl
                            
                            if (!finalActionUri.isNullOrBlank() && finalActionUri.startsWith("jellyfin://items/")) {
                                val itemId = finalActionUri.substringAfter("jellyfin://items/")
                                if (isPackageInstalled("org.moonfin.androidtv")) {
                                    finalActionUri = "intent:#Intent;component=org.moonfin.androidtv/org.jellyfin.androidtv.ui.startup.StartupActivity;action=android.intent.action.VIEW;S.ItemId=$itemId;S.id=$itemId;end"
                                }
                            }

                            if (finalActionUri.isNullOrBlank()) {
                                finalActionUri = null
                            }

                            Log.d("WallpaperService", "Fetched wallpaper: ${status.imageUrl}, Action: $finalActionUri")
                            
                            return listOf(
                                Wallpaper(
                                    uri = status.imageUrl,
                                    type = WallpaperType.IMAGE,
                                    author = selectedLayout,
                                    actionUri = finalActionUri
                                )
                            )
                        }
                    } else {
                        Log.e("WallpaperService", "API Error: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e("WallpaperService", "Exception fetching wallpaper", e)
                }
            }
            return emptyList()
        }

        override fun getPreferences(): String {
            return PreferencesManager.export()
        }

        override fun setPreferences(params: String) {
            PreferencesManager.import(params)
        }
    }
}