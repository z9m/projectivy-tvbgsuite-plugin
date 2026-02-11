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
        @Query("age_rating") ageRating: String? = null,
        @Query("min_year") minYear: String? = null,
        @Query("max_year") maxYear: String? = null,
        @Query("min_rating") minRating: Float? = null,
        @Query("max_rating") maxRating: Float? = null
    ): Call<WallpaperStatus>

    @GET("/api/layouts/list")
    fun getLayouts(): Call<List<String>>

    @GET("/api/genres/list")
    fun getGenres(): Call<List<String>>

    @GET("/api/ages/list")
    fun getAgeRatings(): Call<List<String>>

    @GET("/api/year/list")
    fun getYears(): Call<List<String>>
}