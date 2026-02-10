package tv.projectivy.plugin.wallpaperprovider.sample

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.butch708.projectivy.tvbgsuite.R
import tv.projectivy.plugin.wallpaperprovider.api.WallpaperProviderContract

class WallpaperProvider : ContentProvider() {

    // Moved from WallpaperEventReceiver
    companion object {
        private const val TAG_RECEIVER = "WallpaperEventReceiver"

        fun forceRefresh(context: Context) {
            try {
                val uuid = context.getString(R.string.plugin_uuid)
                val intent = Intent(WallpaperProviderContract.ACTION_WALLPAPER_PROVIDER_UPDATED).apply {
                    `package` = "com.spocky.projengmenu"
                    putExtra(WallpaperProviderContract.EXTRA_PROVIDER_ID, uuid)
                    putExtra(WallpaperProviderContract.EXTRA_UPDATE_REASON, WallpaperProviderContract.UpdateReason.DATA_CHANGED)
                }
                context.sendBroadcast(intent)
                Log.e(TAG_RECEIVER, "PROJECTIVY_LOG: Force refresh broadcast sent to Projectivy (UUID: $uuid)")
            } catch (e: Exception) {
                Log.e(TAG_RECEIVER, "PROJECTIVY_LOG: Error sending refresh broadcast", e)
            }
        }
    }

    override fun onCreate(): Boolean {
        Log.e("MY_WALLPAPER_PROVIDER", "PROJECTIVY_LOG: ContentProvider created")
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        Log.d("MY_WALLPAPER_PROVIDER", "Query called!")
        try {
            val context = context ?: return null
            PreferencesManager.init(context.applicationContext)
            
            val event = uri.getQueryParameter("event")
            val isIdle = uri.getBooleanQueryParameter("isIdle", false)
            
            Log.e("MY_WALLPAPER_PROVIDER", "PROJECTIVY_LOG: Query | URI: $uri | Event: $event | isIdle: $isIdle | Selection: $selection")
            
            // Trigger refresh if exiting idle mode
            if (event == "LAUNCHER_IDLE_MODE_CHANGED") {
                if (!isIdle) {
                    // If exiting idle mode
                    if (PreferencesManager.refreshOnIdleExit) {
                        Log.e("MY_WALLPAPER_PROVIDER", "PROJECTIVY_LOG: Idle Exit detected via Provider. Preference ON. Triggering refresh broadcast.")
                        // Der Provider triggert den Service zum Neuladen
                        forceRefresh(context)
                    } else {
                        // Preference is OFF, return null to tell Projectivy to keep the current wallpaper
                        Log.e("MY_WALLPAPER_PROVIDER", "PROJECTIVY_LOG: Idle Exit detected via Provider. Preference OFF. Returning null to keep current.")
                        return null
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MY_WALLPAPER_PROVIDER", "PROJECTIVY_LOG: Query failed", e)
        }
        
        return null
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}