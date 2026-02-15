# TV Background Suite - Projectivy Wallpaper Plugin

This is a custom **Wallpaper Provider Plugin** for [Projectivy Launcher](https://play.google.com/store/apps/details?id=com.spocky.projengmenu). It is explicitly designed to work with the **TV Background Suite** backend, which can be found here:
**[https://github.com/z9m/androidtvbackgroundWebGui](https://github.com/z9m/androidtvbackgroundWebGui)**

It allows you to display metadata-driven wallpapers (e.g., Movies, TV Shows) on your Android TV home screen, fetched from this compatible backend server (e.g., a Jellyfin/Emby wrapper).

## Features

*   **Dynamic Wallpapers**: Fetches high-quality wallpapers based on your collection layout.
*   **Client Selection & Deep Linking**:
    *   **Preferred Client**: Select your preferred Jellyfin player from the settings.
    *   **Deep Link Support** (Opens directly to the media item):
        *   **Moonfin** (`org.moonfin.androidtv`)
        *   **Jellyfin** (`org.jellyfin.androidtv`)
    *   **App Launch Support** (Opens the player's main menu):
        *   **Kodi** (`org.xbmc.kodi`)
        *   **Fladder** (`nl.jknaapen.fladder`)
        *   **Wholphin** (`com.github.damontecres.wholphin`)
        *   **Void** (`com.hritwik.avoid`)
    *   *Note: Deep linking is only available for clients that plainly support external Android Intents for specific items.*
*   **Customizable Filters**:
    *   **Genre**: Filter content by genres (Action, Comedy, Drama, Sci-Fi, etc.), fetched dynamically from the server.
    *   **Age Rating**: Filter by age ratings (G, PG, R, FSK, etc.), fetched dynamically from the server.
    *   **Year**: Filter content by release year or range (e.g., 2005-2010), fetched dynamically from the server.
    *   **Layout/Collection**: Dynamically selectable from the server.
*   **Configurable Server**: Set your own backend URL directly from the settings interface.

## Installation & Setup

1.  **Install the APK**: Install the `app-debug.apk` (Package: `com.butch708.projectivy.tvbgsuite`) on your Android TV device.
2.  **Open Projectivy Launcher Settings**:
    *   Go to **Settings** > **Appearance** > **Wallpaper**.
3.  **Select Wallpaper Source**:
    *   Click on **Wallpaper Source**.
    *   Scroll down to **Plugins** and select **TV Background Suite** (or the name defined in the manifest).
4.  **Configure**:
    *   Click on the **Settings** (gear icon) next to the plugin name.
    *   **Server URL**: Enter the URL of your **TV Background Suite** backend (e.g., `http://192.168.1.x:5000`).
    *   **Collection / Layout**: Select which library/layout to fetch wallpapers from.
    *   **Preferred Client**: Choose your media player (e.g., Moonfin, Jellyfin) for opening content.
    *   **Filters**: Adjust Genre, Age Rating and Year as desired.
5.  **Enjoy**: Your background will now update based on your preferences.

## Backend Requirements

This plugin requires the **TV Background Suite** running on your network.
Repository: [https://github.com/z9m/androidtvbackgroundWebGui](https://github.com/z9m/androidtvbackgroundWebGui)

The backend provides the following REST API endpoints:

*   `GET /api/layouts/list`: Returns a JSON list of available layouts/collections (List<String>).
*   `GET /api/genres/list`: Returns a JSON list of available genres (List<String>).
*   `GET /api/ages/list`: Returns a JSON list of available age ratings (List<String>).
*   `GET /api/year/list`: Returns a JSON list of available years (List<String>).
*   `GET /api/wallpaper/status`: Returns a JSON object with:
    *   `imageUrl` (String): URL of the wallpaper image.
    *   `actionUrl` (String): Deep link URL (e.g., `jellyfin://items/xyz`).
    *   Query parameters supported: `layout`, `genre`, `age`, `year`.

## Development

*   **Package Name**: `com.butch708.projectivy.tvbgsuite`
*   **Architecture**:
    *   **Service**: `WallpaperProviderService` handles the communication with Projectivy and fetches data via Retrofit.
    *   **UI**: `SettingsActivity` and `SettingsFragment` (using Leanback `GuidedStepSupportFragment`) provide the TV-optimized configuration interface.
    *   **Deep Linking**: Custom logic in `WallpaperProviderService` parses Jellyfin URIs to support specific client intents.

## License

Based on the [Projectivy Plugin Sample](https://github.com/spocky/projectivy-plugin-wallpaper-provider).
