package tv.projectivy.plugin.wallpaperprovider.sample

import android.os.Bundle
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

class SettingsFragment : GuidedStepSupportFragment() {

    companion object {
        private const val ACTION_ID_SERVER_URL = 1L
        private const val ACTION_ID_LAYOUT = 2L
        private const val ACTION_ID_GENRE = 3L
        private const val ACTION_ID_SORT = 4L
        private const val ACTION_ID_AGE = 5L
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
        PreferencesManager.init(requireContext())

        val serverUrl = PreferencesManager.serverUrl
        val selectedLayout = PreferencesManager.selectedLayout
        val genreFilter = PreferencesManager.genreFilter
        val sortOrder = PreferencesManager.sortOrder
        val ageFilter = PreferencesManager.ageFilter

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
        val genreOptions = listOf(
            "All", "Action", "Adventure", "Animation", "Comedy", "Crime",
            "Documentary", "Drama", "Family", "Fantasy", "History", "Horror",
            "Music", "Mystery", "Romance", "Science Fiction", "TV Movie",
            "Thriller", "War", "Western"
        )
        val genreSubActions = genreOptions.mapIndexed { index, option ->
            GuidedAction.Builder(context)
                .id(2000L + index)
                .title(option)
                .checkSetId(3)
                .checked(option == genreFilter || (option == "All" && genreFilter.isEmpty()))
                .build()
        }

        actions.add(GuidedAction.Builder(context)
            .id(ACTION_ID_GENRE)
            .title("Genre Filter")
            .description(if (genreFilter.isNotEmpty()) genreFilter else "All")
            .subActions(genreSubActions)
            .build())

        // Sort Order
        val sortOptions = listOf("Random", "Newest (Year)", "Best Rated")
        val sortSubActions = sortOptions.mapIndexed { index, option ->
             GuidedAction.Builder(context)
                .id(1000L + index)
                .title(option)
                .checkSetId(2)
                .checked(option == sortOrder)
                .build()
        }

        actions.add(GuidedAction.Builder(context)
            .id(ACTION_ID_SORT)
            .title("Sort Order")
            .description(sortOrder)
            .subActions(sortSubActions)
            .build())

        // Age Rating
        val ageOptions = listOf(
            "Any", "G", "PG", "PG-13", "R", "NC-17",
            "TV-Y", "TV-Y7", "TV-G", "TV-PG", "TV-14", "TV-MA",
            "FSK-0", "FSK-6", "FSK-12", "FSK-16", "FSK-18"
        )
        val ageSubActions = ageOptions.mapIndexed { index, option ->
            GuidedAction.Builder(context)
                .id(3000L + index)
                .title(option)
                .checkSetId(4)
                .checked(option == ageFilter || (option == "Any" && ageFilter.isEmpty()))
                .build()
        }

        actions.add(GuidedAction.Builder(context)
            .id(ACTION_ID_AGE)
            .title("Age Rating")
            .description(if (ageFilter.isNotEmpty()) ageFilter else "Any")
            .subActions(ageSubActions)
            .build())
    }

    override fun onResume() {
        super.onResume()
        refreshLayouts()
    }

    private fun refreshLayouts() {
        val serverUrl = PreferencesManager.serverUrl
        if (serverUrl.isBlank()) return

        try {
            val retrofit = Retrofit.Builder()
                .baseUrl(serverUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val apiService = retrofit.create(ApiService::class.java)
            apiService.getLayouts().enqueue(object : Callback<List<String>> {
                override fun onResponse(call: Call<List<String>>, response: Response<List<String>>) {
                    if (response.isSuccessful) {
                        val layouts = response.body() ?: emptyList()
                        updateLayoutAction(layouts)
                    }
                }

                override fun onFailure(call: Call<List<String>>, t: Throwable) {
                    // Ignore
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
        }
        // Sort Order selection
        if (action.checkSetId == 2) {
             val sortName = action.title.toString()
             PreferencesManager.sortOrder = sortName

             val sortActionIndex = actions.indexOfFirst { it.id == ACTION_ID_SORT }
             if (sortActionIndex != -1) {
                 actions[sortActionIndex].description = sortName
                 notifyActionChanged(sortActionIndex)
             }
             notifySettingsChanged()
             return true
        }
        // Genre selection
        if (action.checkSetId == 3) {
            val genreName = action.title.toString()
            val storedGenre = if (genreName == "All") "" else genreName
            PreferencesManager.genreFilter = storedGenre

            val genreActionIndex = actions.indexOfFirst { it.id == ACTION_ID_GENRE }
            if (genreActionIndex != -1) {
                actions[genreActionIndex].description = genreName
                notifyActionChanged(genreActionIndex)
            }
            notifySettingsChanged()
            return true
        }
        // Age selection
        if (action.checkSetId == 4) {
            val ageName = action.title.toString()
            val storedAge = if (ageName == "Any") "" else ageName
            PreferencesManager.ageFilter = storedAge

            val ageActionIndex = actions.indexOfFirst { it.id == ACTION_ID_AGE }
            if (ageActionIndex != -1) {
                actions[ageActionIndex].description = ageName
                notifyActionChanged(ageActionIndex)
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
                refreshLayouts()
                notifySettingsChanged()
            }
            ACTION_ID_LAYOUT -> {
                if (action.subActions.isNullOrEmpty()) {
                    refreshLayouts()
                    Toast.makeText(context, "Fetching layouts...", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}