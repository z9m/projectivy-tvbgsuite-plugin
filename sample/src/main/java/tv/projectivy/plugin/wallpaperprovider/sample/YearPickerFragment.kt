package tv.projectivy.plugin.wallpaperprovider.sample

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.leanback.widget.VerticalGridPresenter
import com.butch708.projectivy.tvbgsuite.R

class YearPickerFragment : VerticalGridSupportFragment() {

    private lateinit var mAdapter: ArrayObjectAdapter
    private var years: ArrayList<String> = ArrayList()
    private var pendingStartYear: Int? = null

    companion object {
        private const val COLUMNS = 10
        private const val TAG = "YearPickerFragment"

        fun newInstance(years: ArrayList<String>): YearPickerFragment {
            val fragment = YearPickerFragment()
            val args = Bundle()
            args.putStringArrayList("YEARS", years)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate called")
        try {
            super.onCreate(savedInstanceState)
            // Ensure PreferencesManager is initialized
            try {
                PreferencesManager.init(requireContext())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to init PreferencesManager", e)
            }
            
            title = "Select Year(s)"

            val gridPresenter = VerticalGridPresenter().apply {
                numberOfColumns = COLUMNS
                shadowEnabled = false
            }
            setGridPresenter(gridPresenter)

            mAdapter = ArrayObjectAdapter(YearPresenter())
            adapter = mAdapter

            arguments?.getStringArrayList("YEARS")?.let {
                years = it
                updateAdapter()
            }

            // Fix: Use SAM conversion for OnItemViewClickedListener to avoid signature mismatch
            onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
                 if (item is String) {
                    handleYearClick(item)
                    // Refresh visual state of all items to show selection changes
                    mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size())
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(context, "Error initializing YearPicker: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateAdapter() {
        mAdapter.clear()
        mAdapter.add("Any")
        years.forEach { mAdapter.add(it) }
    }

    private fun handleYearClick(yearStr: String) {
        try {
            if (yearStr == "Any") {
                PreferencesManager.yearFilter = ""
                pendingStartYear = null
                Toast.makeText(requireContext(), "Year filter cleared", Toast.LENGTH_SHORT).show()
                notifySettingsChanged()
                requireActivity().supportFragmentManager.popBackStack()
                return
            }

            val clickedYear = yearStr.toIntOrNull()
            if (clickedYear != null) {
                if (pendingStartYear == null) {
                    // Start selection
                    pendingStartYear = clickedYear
                    Toast.makeText(requireContext(), "Select end year (or click again for single year)", Toast.LENGTH_LONG).show()
                } else {
                    // End selection
                    val start = pendingStartYear!!
                    val end = clickedYear
                    
                    val min = minOf(start, end)
                    val max = maxOf(start, end)
                    
                    val newFilter = if (min == max) "$min" else "$min-$max"
                    PreferencesManager.yearFilter = newFilter
                    pendingStartYear = null
                    
                    Toast.makeText(requireContext(), "Selected: $newFilter", Toast.LENGTH_SHORT).show()
                    notifySettingsChanged()
                    
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling click", e)
        }
    }

    private fun notifySettingsChanged() {
        (requireActivity() as? SettingsActivity)?.requestWallpaperUpdate()
    }

    fun isYearChecked(yearStr: String): Boolean {
        try {
             val yearFilter = PreferencesManager.yearFilter

            if (yearStr == "Any") return yearFilter.isEmpty() && pendingStartYear == null
            
            if (pendingStartYear != null) {
                return yearStr.toIntOrNull() == pendingStartYear
            }

            if (yearFilter.isEmpty()) return false
            if (yearFilter.contains("-")) {
                val parts = yearFilter.split("-")
                if (parts.size == 2) {
                    val start = parts[0].toIntOrNull() ?: 0
                    val end = parts[1].toIntOrNull() ?: 0
                    val c = yearStr.toIntOrNull() ?: return false
                    return c in minOf(start, end)..maxOf(start, end)
                }
            }
            return yearFilter == yearStr
        } catch (e: Exception) {
            Log.e(TAG, "Error checking year", e)
            return false
        }
    }

    inner class YearPresenter : Presenter() {
        override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
            // Inflate XML layout instead of creating view programmatically
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_year, parent, false)
            
            // Calculate fixed width based on screen width and columns to ensure uniform size
            val screenWidth = parent.context.resources.displayMetrics.widthPixels
            // Subtract safe horizontal padding for TV
            val gridPadding = (128 * parent.context.resources.displayMetrics.density).toInt()
            val itemWidth = (screenWidth - gridPadding) / COLUMNS

            // Preserve existing layout params (with margins!) by modifying width directly
            view.layoutParams.width = itemWidth
            view.requestLayout()
            
            // Set focus change listener to update scaling animation
            // Note: Background color changes are handled by XML selector (state_focused/state_selected)
            view.setOnFocusChangeListener { _, hasFocus ->
                val scale = if (hasFocus) 1.1f else 1.0f
                view.animate().scaleX(scale).scaleY(scale).setDuration(150).start()
            }
            
            return ViewHolder(view)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
            val year = item as? String ?: return
            val tv = viewHolder.view.findViewById<TextView>(R.id.year_text)
            tv.text = year
            
            // Set selected state - this triggers the XML selector's state_selected on the background (now on FrameLayout)
            viewHolder.view.isSelected = isYearChecked(year)
            
            // Apply initial scale based on current focus state
            val scale = if (viewHolder.view.hasFocus()) 1.1f else 1.0f
            viewHolder.view.scaleX = scale
            viewHolder.view.scaleY = scale
        }

        override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        }
    }
}