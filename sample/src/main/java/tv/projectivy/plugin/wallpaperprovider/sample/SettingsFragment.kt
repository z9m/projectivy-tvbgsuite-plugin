package tv.projectivy.plugin.wallpaperprovider.sample

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist.Guidance
import androidx.leanback.widget.GuidedAction
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import tv.projectivy.plugin.wallpaperprovider.sample.ApiService
import com.butch708.projectivy.tvbgsuite.R
import kotlin.math.roundToInt

class SettingsFragment : GuidedStepSupportFragment() {

    private var availableYears: ArrayList<String> = ArrayList()
    private var availableGenres: ArrayList<String> = ArrayList()
    private var availableAges: ArrayList<String> = ArrayList()

    companion object {
        private const val TAG = "SettingsFragment"
        private const val ACTION_ID_SERVER_URL = 1L
        private const val ACTION_ID_LAYOUT = 2L
        private const val ACTION_ID_GENRE = 3L
        private const val ACTION_ID_AGE = 5L
        private const val ACTION_ID_YEAR = 6L
        private const val ACTION_ID_RATING = 7L
        private const val ACTION_ID_MAX_RATING = 8L
        private const val ACTION_ID_EVENT_IDLE = 9L
        
        private val DEFAULT_GENRES = listOf(
            "Action", "Adventure", "Animation", "Comedy", "Crime",
            "Documentary", "Drama", "Family", "Fantasy", "History", "Horror",
            "Music", "Mystery", "Romance", "Science Fiction", "TV Movie",
            "Thriller", "War", "Western"
        )

        private val DEFAULT_AGE_RATINGS = listOf(
            "G", "PG", "PG-13", "R", "NC-17",
            "TV-Y", "TV-Y7", "TV-G", "TV-PG", "TV-14", "TV-MA",
            "FSK-0", "FSK-6", "FSK-12", "FSK-16", "FSK-18"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        availableGenres.addAll(DEFAULT_GENRES)
        availableAges.addAll(DEFAULT_AGE_RATINGS)
    }

    override fun onCreateGuidance(savedInstanceState: Bundle?): Guidance {
        return Guidance(
            getString(R.string.plugin_name),
            "Metadata-Driven Provider Configuration",
            getString(R.string.settings),
            AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_plugin)
        )
    }

    override fun onCreateActions(actions: MutableList<GuidedAction>, savedInstanceState: Bundle?) {
        try {
            PreferencesManager.init(requireContext())

            val serverUrl = PreferencesManager.serverUrl
            val selectedLayout = PreferencesManager.selectedLayout
            val genreFilter = PreferencesManager.genreFilter
            val ageFilter = PreferencesManager.ageFilter
            val yearFilter = PreferencesManager.yearFilter
            val minRating = PreferencesManager.minRating
            val maxRating = PreferencesManager.maxRating
            val refreshOnIdle = PreferencesManager.refreshOnIdleExit

            // Server URL
            actions.add(GuidedAction.Builder(context)
                .id(ACTION_ID_SERVER_URL)
                .title("Server URL")
                .description(if (serverUrl.isNotEmpty()) serverUrl else "http://...")
                .editDescription(serverUrl)
                .descriptionEditable(true)
                .build())

            // Layout / Collection
            actions.add(GuidedAction.Builder(context)
                .id(ACTION_ID_LAYOUT)
                .title("Collection / Layout")
                .description(selectedLayout)
                .subActions(emptyList()) 
                .build())

            // Genre Filter
            actions.add(GuidedAction.Builder(context)
                .id(ACTION_ID_GENRE)
                .title("Genre Filter")
                .description(if (genreFilter.isNotEmpty()) genreFilter else "All")
                .build())

            // Age Rating
            actions.add(GuidedAction.Builder(context)
                .id(ACTION_ID_AGE)
                .title("Age Rating")
                .description(if (ageFilter.isNotEmpty()) ageFilter else "Any")
                .build())

            // Year Filter
            actions.add(GuidedAction.Builder(context)
                .id(ACTION_ID_YEAR)
                .title("Year Filter")
                .description(if (yearFilter.isNotEmpty()) yearFilter else "Any")
                .build())

            // Rating Filter
            actions.add(GuidedAction.Builder(context)
                .id(ACTION_ID_RATING)
                .title("Minimum Rating")
                .description(getStarsString(minRating))
                .subActions(createRatingSubActions(minRating, 2))
                .build())

            // Max Rating Filter
            actions.add(GuidedAction.Builder(context)
                .id(ACTION_ID_MAX_RATING)
                .title("Maximum Rating")
                .description(getStarsString(maxRating))
                .subActions(createRatingSubActions(maxRating, 3))
                .build())

            // Events Section
            actions.add(GuidedAction.Builder(context)
                .id(ACTION_ID_EVENT_IDLE)
                .title("Refresh on idle exit")
                .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
                .checked(refreshOnIdle)
                .build())

        } catch (e: Exception) {
            Log.e(TAG, "Error creating actions", e)
        }
    }

    override fun onResume() {
        super.onResume()
        // Update descriptions dynamically on resume
        updateGenreAction(availableGenres)
        updateAgeRatingAction(availableAges)
        updateYearAction(availableYears)
        refreshAllData()
    }

    private fun refreshAllData() {
        val serverUrl = PreferencesManager.serverUrl
        if (serverUrl.isBlank()) return
        
        val apiService = try {
            Retrofit.Builder()
                .baseUrl(serverUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return

        apiService.getLayouts().enqueue(object : Callback<List<String>> {
            override fun onResponse(call: Call<List<String>>, response: Response<List<String>>) {
                if (response.isSuccessful) {
                    val layouts = response.body() ?: emptyList()
                    updateLayoutAction(layouts)
                }
            }
            override fun onFailure(call: Call<List<String>>, t: Throwable) {
                Log.e(TAG, "Failed to fetch layouts", t)
            }
        })

        apiService.getGenres().enqueue(object : Callback<List<String>> {
            override fun onResponse(call: Call<List<String>>, response: Response<List<String>>) {
                if (response.isSuccessful) {
                    val genres = response.body() ?: emptyList()
                    if (genres.isNotEmpty()) {
                        updateGenreAction(genres)
                    } else {
                        Log.w(TAG, "Fetched empty genre list, keeping defaults")
                    }
                }
            }
            override fun onFailure(call: Call<List<String>>, t: Throwable) {
                Log.e(TAG, "Failed to fetch genres", t)
            }
        })

        apiService.getAgeRatings().enqueue(object : Callback<List<String>> {
            override fun onResponse(call: Call<List<String>>, response: Response<List<String>>) {
                if (response.isSuccessful) {
                    val ages = response.body() ?: emptyList()
                    if (ages.isNotEmpty()) {
                        updateAgeRatingAction(ages)
                    } else {
                         Log.w(TAG, "Fetched empty age list, keeping defaults")
                    }
                }
            }
            override fun onFailure(call: Call<List<String>>, t: Throwable) {
                Log.e(TAG, "Failed to fetch age ratings", t)
            }
        })

        apiService.getYears().enqueue(object : Callback<List<String>> {
             override fun onResponse(call: Call<List<String>>, response: Response<List<String>>) {
                 if (response.isSuccessful) {
                     val years = response.body() ?: emptyList()
                     if (years.isNotEmpty()) {
                         updateYearAction(years)
                     }
                 }
             }
             override fun onFailure(call: Call<List<String>>, t: Throwable) {
                 Log.e(TAG, "Failed to fetch years", t)
             }
        })
    }

    private fun updateLayoutAction(layouts: List<String>) {
        val actions = actions
        val layoutActionIndex = actions.indexOfFirst { it.id == ACTION_ID_LAYOUT }
        if (layoutActionIndex != -1) {
            val layoutAction = actions[layoutActionIndex]
            val subActions = layouts.mapIndexed { index, name ->
                GuidedAction.Builder(context)
                    .id(100L + index)
                    .title(name)
                    .checkSetId(1)
                    .checked(name == PreferencesManager.selectedLayout)
                    .build()
            }
            layoutAction.subActions = subActions
            notifyActionChanged(layoutActionIndex)
        }
    }

    private fun updateGenreAction(genres: List<String>) {
        availableGenres.clear()
        availableGenres.addAll(genres)
        
        val actions = actions
        val genreActionIndex = actions.indexOfFirst { it.id == ACTION_ID_GENRE }
        if (genreActionIndex != -1) {
            val genreAction = actions[genreActionIndex]
            val genreFilterString = PreferencesManager.genreFilter
            
            val descriptionText = if (genreFilterString.isEmpty()) {
                "All"
            } else {
                val selectedCount = genreFilterString.split(",").filter { it.isNotBlank() }.size
                if (selectedCount == availableGenres.size) {
                    "All"
                } else {
                    "$selectedCount selected"
                }
            }
            genreAction.description = descriptionText
            genreAction.subActions = null 
            notifyActionChanged(genreActionIndex)
        }
    }

    private fun updateAgeRatingAction(ages: List<String>) {
        availableAges.clear()
        availableAges.addAll(ages)
        
        val actions = actions
        val ageActionIndex = actions.indexOfFirst { it.id == ACTION_ID_AGE }
        if (ageActionIndex != -1) {
            val ageAction = actions[ageActionIndex]
            val ageFilterString = PreferencesManager.ageFilter
            
            val descriptionText = if (ageFilterString.isEmpty()) {
                "Any"
            } else {
                val selectedCount = ageFilterString.split(",").filter { it.isNotBlank() }.size
                if (selectedCount == availableAges.size) {
                    "Any"
                } else {
                    "$selectedCount selected"
                }
            }
            ageAction.description = descriptionText
            ageAction.subActions = null 
            notifyActionChanged(ageActionIndex)
        }
    }

    private fun updateYearAction(years: List<String>) {
        availableYears.clear()
        availableYears.addAll(years)
        
        val actions = actions
        val yearActionIndex = actions.indexOfFirst { it.id == ACTION_ID_YEAR }
        if (yearActionIndex != -1) {
            val yearAction = actions[yearActionIndex]
            val yearFilter = PreferencesManager.yearFilter
            val descriptionText = if (yearFilter.isEmpty()) {
                "Any"
            } else {
                yearFilter // Already formatted as YYYY or YYYY-YYYY
            }
            yearAction.description = descriptionText
            yearAction.subActions = null
            notifyActionChanged(yearActionIndex)
        }
    }

    private fun notifySettingsChanged() {
        (requireActivity() as? SettingsActivity)?.requestWallpaperUpdate()
    }

    override fun onSubGuidedActionClicked(action: GuidedAction): Boolean {
        // Layout selection
        if (action.checkSetId == 1) {
            val layoutName = action.title.toString()
            PreferencesManager.selectedLayout = layoutName
            
            val layoutActionIndex = actions.indexOfFirst { it.id == ACTION_ID_LAYOUT }
            if (layoutActionIndex != -1) {
                actions[layoutActionIndex].description = layoutName
                notifyActionChanged(layoutActionIndex)
            }
            notifySettingsChanged()
            return true
        } else if (action.checkSetId == 2) {
            val rating = action.title.toString().toFloatOrNull() ?: 7.0f
            PreferencesManager.minRating = rating
            
            // Validation: if min > max, adjust max
            if (rating > PreferencesManager.maxRating) {
                PreferencesManager.maxRating = rating
                val maxActionIndex = actions.indexOfFirst { it.id == ACTION_ID_MAX_RATING }
                if (maxActionIndex != -1) {
                    actions[maxActionIndex].description = getStarsString(rating)
                    actions[maxActionIndex].subActions = createRatingSubActions(rating, 3)
                    notifyActionChanged(maxActionIndex)
                }
            }
            
            val ratingActionIndex = actions.indexOfFirst { it.id == ACTION_ID_RATING }
            if (ratingActionIndex != -1) {
                actions[ratingActionIndex].description = getStarsString(rating)
                actions[ratingActionIndex].subActions = createRatingSubActions(rating, 2)
                notifyActionChanged(ratingActionIndex)
            }
            notifySettingsChanged()
            return true
        } else if (action.checkSetId == 3) {
            val rating = action.title.toString().toFloatOrNull() ?: 10.0f
            PreferencesManager.maxRating = rating

            // Validation: if max < min, adjust min
            if (rating < PreferencesManager.minRating) {
                PreferencesManager.minRating = rating
                val minActionIndex = actions.indexOfFirst { it.id == ACTION_ID_RATING }
                if (minActionIndex != -1) {
                    actions[minActionIndex].description = getStarsString(rating)
                    actions[minActionIndex].subActions = createRatingSubActions(rating, 2)
                    notifyActionChanged(minActionIndex)
                }
            }
            
            val maxActionIndex = actions.indexOfFirst { it.id == ACTION_ID_MAX_RATING }
            if (maxActionIndex != -1) {
                actions[maxActionIndex].description = getStarsString(rating)
                actions[maxActionIndex].subActions = createRatingSubActions(rating, 3)
                notifyActionChanged(maxActionIndex)
            }
            notifySettingsChanged()
            return true
        }
        return super.onSubGuidedActionClicked(action)
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        when (action.id) {
            ACTION_ID_SERVER_URL -> {
                val params = action.editDescription
                PreferencesManager.serverUrl = params.toString()
                findActionById(ACTION_ID_SERVER_URL)?.description = params
                notifyActionChanged(findActionPositionById(ACTION_ID_SERVER_URL))
                refreshAllData()
                notifySettingsChanged()
            }
            ACTION_ID_LAYOUT -> {
                if (action.subActions.isNullOrEmpty()) {
                    refreshAllData()
                    Toast.makeText(context, "Fetching layouts...", Toast.LENGTH_SHORT).show()
                }
            }
            ACTION_ID_GENRE -> {
                 if (availableGenres.isEmpty()) {
                    refreshAllData()
                    Toast.makeText(context, "Fetching genres...", Toast.LENGTH_SHORT).show()
                } else {
                    val fragment = MultiSelectFragment.newInstance(MultiSelectFragment.TYPE_GENRE, availableGenres)
                    parentFragmentManager.beginTransaction()
                        .replace(android.R.id.content, fragment)
                        .addToBackStack(null)
                        .commit()
                }
            }
            ACTION_ID_AGE -> {
                 if (availableAges.isEmpty()) {
                    refreshAllData()
                    Toast.makeText(context, "Fetching age ratings...", Toast.LENGTH_SHORT).show()
                } else {
                    val fragment = MultiSelectFragment.newInstance(MultiSelectFragment.TYPE_AGE, availableAges)
                    parentFragmentManager.beginTransaction()
                        .replace(android.R.id.content, fragment)
                        .addToBackStack(null)
                        .commit()
                }
            }
            ACTION_ID_YEAR -> {
                 if (availableYears.isEmpty()) {
                    refreshAllData()
                    Toast.makeText(context, "Fetching years...", Toast.LENGTH_SHORT).show()
                } else {
                    val fragment = YearPickerFragment.newInstance(availableYears)
                    parentFragmentManager.beginTransaction()
                        .replace(android.R.id.content, fragment)
                        .addToBackStack(null)
                        .commit()
                }
            }
            ACTION_ID_EVENT_IDLE -> {
                val newState = !PreferencesManager.refreshOnIdleExit
                PreferencesManager.refreshOnIdleExit = newState
                action.isChecked = newState
                notifyActionChanged(findActionPositionById(ACTION_ID_EVENT_IDLE))
            }
        }
    }

    private fun getStarsString(rating: Float): String {
        val stars = rating.roundToInt()
        val sb = StringBuilder()
        for (i in 1..10) {
            if (i <= stars) sb.append("★") else sb.append("☆")
        }
        return "$sb ($rating / 10)"
    }

    private fun createRatingSubActions(currentRating: Float, checkSetId: Int): List<GuidedAction> {
        val actions = ArrayList<GuidedAction>()
        for (i in 1..10) {
            actions.add(GuidedAction.Builder(context)
                .id(200L + i + (checkSetId * 100)) // Unique ID based on checkSetId to avoid collision
                .title("$i")
                .checkSetId(checkSetId)
                .checked(i.toFloat() == currentRating)
                .build())
        }
        return actions
    }
}