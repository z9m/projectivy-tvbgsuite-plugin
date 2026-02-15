package tv.projectivy.plugin.wallpaperprovider.sample

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class WallpaperEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.e(TAG, "PROJECTIVY_LOG: WallpaperEventReceiver received action: $action")

        when (action) {
            "tv.projectivy.launcher.action.REFRESH_WALLPAPER" -> {
                Log.e(TAG, "PROJECTIVY_LOG: Projectivy Refresh Wallpaper action received. Forcing refresh.")
                // Ruft die Helferfunktion im WallpaperProvider auf
                WallpaperProvider.forceRefresh(context)
            }
            // Weitere Aktionen könnten hier hinzugefügt werden, falls Projectivy andere sendet
        }
    }

    companion object {
        private const val TAG = "WallpaperEventReceiver"
    }
}