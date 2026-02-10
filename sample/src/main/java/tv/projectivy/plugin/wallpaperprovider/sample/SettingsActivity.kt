package tv.projectivy.plugin.wallpaperprovider.sample

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.GuidedStepSupportFragment
import tv.projectivy.plugin.wallpaperprovider.api.WallpaperProviderContract
import com.butch708.projectivy.tvbgsuite.R

class SettingsActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d("SettingsActivity", "onCreate called")

        try {
            // Use application context to avoid leaks and ensure valid context
            PreferencesManager.init(applicationContext)
            Log.d("SettingsActivity", "PreferencesManager initialized")
        } catch (e: Exception) {
            Log.e("SettingsActivity", "Error initializing PreferencesManager", e)
        }

        if (!packageManager.isApplicationInstalled(PROJECTIVY_PACKAGE_ID)) {
            Toast.makeText(this, R.string.projectivy_not_installed, Toast.LENGTH_LONG).show()
        }

        if (savedInstanceState == null) {
            try {
                val fragment: GuidedStepSupportFragment = SettingsFragment()
                GuidedStepSupportFragment.addAsRoot(this, fragment, android.R.id.content)
                Log.d("SettingsActivity", "SettingsFragment added")
            } catch (e: Exception) {
                Log.e("SettingsActivity", "Error adding SettingsFragment", e)
                Toast.makeText(this, "Error loading settings: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }


    // Call this method to inform Projectivy that it needs to request new wallpapers because the settings have changed
    // Note : if the settings have been opened from Projectivy Settings>Appearance>Wallpaper, this call is not needed.
    fun requestWallpaperUpdate() {
        try {
            val intent = Intent(WallpaperProviderContract.ACTION_WALLPAPER_PROVIDER_UPDATED).apply {
                `package` = PROJECTIVY_PACKAGE_ID

                // Use this@SettingsActivity.getString to ensure we are calling getString on the Activity context
                putExtra(WallpaperProviderContract.EXTRA_PROVIDER_ID, this@SettingsActivity.getString(R.string.plugin_uuid))
                putExtra(WallpaperProviderContract.EXTRA_UPDATE_REASON, WallpaperProviderContract.UpdateReason.PREFS_CHANGED)
            }

            sendBroadcast(intent)
            Log.d("SettingsActivity", "Wallpaper update requested")
        } catch (e: Exception) {
            Log.e("SettingsActivity", "Error requesting wallpaper update", e)
        }
    }

    fun PackageManager.isApplicationInstalled(packageName: String): Boolean {
        return try {
            getApplicationInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private const val PROJECTIVY_PACKAGE_ID = "com.spocky.projengmenu"
    }
}