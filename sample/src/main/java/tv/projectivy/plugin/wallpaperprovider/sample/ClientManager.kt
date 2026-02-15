package tv.projectivy.plugin.wallpaperprovider.sample

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

data class ClientProfile(
    val name: String,
    val packageName: String,
    val type: ClientType
)

enum class ClientType {
    DEEP_LINK,
    LAUNCH
}

object ClientManager {
    private val SUPPORTED_CLIENTS = listOf(
        ClientProfile("Moonfin", "org.moonfin.androidtv", ClientType.DEEP_LINK),
        ClientProfile("Jellyfin", "org.jellyfin.androidtv", ClientType.DEEP_LINK),
        ClientProfile("Fladder", "nl.jknaapen.fladder", ClientType.LAUNCH),
        ClientProfile("Kodi", "org.xbmc.kodi", ClientType.LAUNCH),
        ClientProfile("Wholphin", "com.github.damontecres.wholphin", ClientType.LAUNCH),
        ClientProfile("Void", "com.hritwik.avoid", ClientType.LAUNCH)
    )

    fun getInstalledClients(context: Context): List<ClientProfile> {
        val pm = context.packageManager
        return SUPPORTED_CLIENTS.filter { isPackageInstalled(pm, it.packageName) }
    }

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun getClientActionUri(context: Context, packageName: String, itemId: String): String? {
        val client = SUPPORTED_CLIENTS.find { it.packageName == packageName } ?: return null
        
        return when (client.type) {
            ClientType.DEEP_LINK -> {
               val component = when (packageName) {
                   "org.moonfin.androidtv" -> "org.moonfin.androidtv/org.jellyfin.androidtv.ui.startup.StartupActivity"
                   "org.jellyfin.androidtv" -> "org.jellyfin.androidtv/org.jellyfin.androidtv.ui.startup.StartupActivity"
                   "nl.jknaapen.fladder" -> "nl.jknaapen.fladder/nl.jknaapen.fladder.MainActivity"
                   "org.xbmc.kodi" -> "org.xbmc.kodi/org.xbmc.kodi.Splash"
                   "com.github.damontecres.wholphin" -> "com.github.damontecres.wholphin/com.github.damontecres.wholphin.MainActivity" // Best guess
                   "com.hritwik.avoid" -> "com.hritwik.avoid/com.hritwik.avoid.LeanbackLauncher"
                   else -> return null
               }
               "intent:#Intent;component=$component;action=android.intent.action.VIEW;S.ItemId=$itemId;S.id=$itemId;end"
            }
            ClientType.LAUNCH -> {
                if (packageName == "com.hritwik.avoid") {
                    return "intent:#Intent;component=com.hritwik.avoid/com.hritwik.avoid.LeanbackLauncher;action=android.intent.action.MAIN;category=android.intent.category.LEANBACK_LAUNCHER;end"
                }
                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                launchIntent?.toUri(Intent.URI_INTENT_SCHEME)
            }
        }
    }
}
