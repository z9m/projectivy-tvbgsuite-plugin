package tv.projectivy.plugin.wallpaperprovider.sample

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

data class WallpaperStatus(
    val imageUrl: String,
    val actionUrl: String?
)

interface ApiService {
    @GET("/api/wallpaper/status")
    fun getWallpaperStatus(
        @Query("layout") layout: String,
        @Query("genre") genre: String? = null,
        @Query("sort") sort: String? = null,
        @Query("age") age: String? = null
    ): Call<WallpaperStatus>

    @GET("/api/layouts/list")
    fun getLayouts(): Call<List<String>>
}