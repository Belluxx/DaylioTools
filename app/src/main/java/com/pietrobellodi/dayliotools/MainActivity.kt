package com.pietrobellodi.dayliotools

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.pietrobellodi.dayliotools.utils.FirebaseTools
import com.pietrobellodi.dayliotools.utils.MoodTools
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val PICK_CSV_CODE = 10
    private val VERSION = 3
    private val MINIMUM_MOOD = 1f
    private val MAXIMUM_MOOD = 5f

    private lateinit var mt: MoodTools
    private lateinit var ft: FirebaseTools
    private lateinit var moods: Array<Float>
    private lateinit var dates: Array<String>
    private lateinit var lastUri: Uri

    private var textColor = -1
    private var mainLineColor = -1
    private var accentLineColor = -1
    private var avgRender = false
    private var avgWindow = -1
    private var smoothRender = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initAll()
    }

    private fun initAll() {
        // Check if first launch
        val firstLaunch =
            getSharedPreferences(getString(R.string.shared_prefs), Context.MODE_PRIVATE).getBoolean(
                "firstLaunch",
                true
            )

        // Init variables
        avgRender = avg_swt.isChecked
        avgWindow = window_sb.value.toInt()
        smoothRender = smooth_swt.isChecked
        mt = MoodTools(this, contentResolver)
        ft = FirebaseTools(
            firstLaunch,
            object : FirebaseTools.OnDataRetrievedListener {
                override fun onRetrieved(versionCode: Int, updateUrl: String) {
                    if (versionCode > VERSION) {
                        updateRequest(updateUrl)
                    }
                }
            })
        getSharedPreferences(getString(R.string.shared_prefs), Context.MODE_PRIVATE).edit()
            .putBoolean("firstLaunch", false).apply()

        // Show tutorial if first launch
        if (firstLaunch) {
            // val intent = Intent(this, HelpActivity::class.java)
            // startActivity(intent)
        }

        // Init colors
        if (isDarkModeOn()) {
            textColor = Color.parseColor("#FFFFFF")
            mainLineColor = Color.parseColor("#555555")
            accentLineColor = Color.parseColor("#FFAA00")
        } else {
            textColor = Color.parseColor("#000000")
            mainLineColor = Color.parseColor("#AAAAAA")
            accentLineColor = Color.parseColor("#FFAA00")
        }

        // Setup chart
        with(mood_chart) {
            legend.isEnabled = false
            legend.textColor = textColor
            legend.textSize = 12f

            axisRight.isEnabled = false
            axisLeft.textColor = textColor
            axisLeft.setDrawGridLines(false)
            axisLeft.setDrawLabels(false)
            axisLeft.axisMinimum = 0f
            val upperLimit = LimitLine(MAXIMUM_MOOD, "Rad").apply {
                lineColor = Color.parseColor("#00BB00")
                lineWidth = 1f
                enableDashedLine(20f, 16f, 0f)
            }
            val lowerLimit = LimitLine(MINIMUM_MOOD, "Awful").apply {
                lineColor = Color.parseColor("#FF0000")
                lineWidth = 1f
                enableDashedLine(20f, 16f, 0f)
            }
            axisLeft.addLimitLine(upperLimit)
            axisLeft.addLimitLine(lowerLimit)

            description.isEnabled = false
            isScaleYEnabled = false
        }

        // Init widgets
        avg_swt.isEnabled = false
        avg_swt.isChecked = false
        smooth_swt.isEnabled = false
        smooth_swt.isChecked = false

        if (avg_swt.isChecked) window_lay.visibility = View.VISIBLE
        else window_lay.visibility = View.GONE
        avg_swt.setOnCheckedChangeListener { _, isChecked ->
            if (::moods.isInitialized) {
                if (isChecked) {
                    window_lay.visibility = View.VISIBLE
                    avgRender = true
                    if (moods.isNotEmpty()) reloadChart()
                } else {
                    window_lay.visibility = View.GONE
                    avgRender = false
                    if (moods.isNotEmpty()) reloadChart()
                }
            }
        }

        smooth_swt.setOnCheckedChangeListener { _, isChecked ->
            smoothRender = isChecked
            reloadChart()
        }

        window_sb.value = avgWindow.toFloat()
        window_sb.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                if (::moods.isInitialized) {
                    avgWindow = slider.value.toInt()
                    if (moods.isNotEmpty()) reloadChart()
                }
            }

            override fun onStopTrackingTouch(slider: Slider) {}

        })
        window_sb.addOnChangeListener { _, value, _ ->
            if (::moods.isInitialized) {
                avgWindow = value.toInt()
                if (moods.isNotEmpty()) reloadChart()
            }
        }

        choose_btn.setOnClickListener {
            chooseFile()
        }

        manage_btn.setOnClickListener {
            val intent = Intent(this, ManageMoodsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadChartData(moodEntries: List<Entry>, maEntries: List<Entry>) {
        if (moodEntries.isNotEmpty()) {
            // Create mood dataset
            val moodDataset = LineDataSet(moodEntries, "Mood")
            with(moodDataset) {
                color = if (avgRender) mainLineColor else accentLineColor
                lineWidth = 0.8f
                mode = if (smoothRender) LineDataSet.Mode.CUBIC_BEZIER else LineDataSet.Mode.LINEAR

                setDrawValues(false)
                setDrawHighlightIndicators(false)
                setDrawCircles(false)
            }

            // Create moving average dataset
            val maDataset = LineDataSet(maEntries, "Moving average $avgWindow days")
            with(maDataset) {
                color = accentLineColor
                lineWidth = 2f
                mode = if (smoothRender) LineDataSet.Mode.CUBIC_BEZIER else LineDataSet.Mode.LINEAR

                setDrawValues(false)
                setDrawHighlightIndicators(false)
                setDrawCircles(false)
            }

            // Tweak the chart
            if (avgRender) mood_chart.data = LineData(listOf(moodDataset, maDataset))
            else mood_chart.data = LineData(moodDataset)
            mood_chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
            mood_chart.xAxis.setDrawGridLines(false)
            mood_chart.xAxis.valueFormatter = IndexAxisValueFormatter(dates)
            mood_chart.xAxis.textColor = textColor
            mood_chart.invalidate()
            mood_chart.setVisibleXRangeMinimum(7f)
            mood_chart.setVisibleXRangeMaximum(100f)

            avg_swt.isEnabled = true
            smooth_swt.isEnabled = true
        } else {
            toast("No data to create graph")
        }
    }

    private fun reloadChart() {
        if (!::lastUri.isInitialized) {
            toast("Cannot reload chart")
            return
        }
        mt.readCsv(lastUri)
        val results = mt.getResults()
        moods = results.first
        dates = results.second

        // Create mood entries list
        val moodEntries = Array(moods.size) { Entry(0f, 0f) }
        for ((i, mood) in moods.withIndex()) {
            moodEntries[i] = Entry(i.toFloat(), mood)
        }

        // Create moving average entries list
        val moodMA = moods
            .toList().windowed(avgWindow, 1) { it.average() }
            .map { it.toFloat() }
        val maEntries = Array(moodMA.size) { Entry(0f, 0f) }
        for ((i, ma) in moodMA.withIndex()) {
            maEntries[i] = Entry(i.toFloat(), ma)
        }

        // Load chart data
        loadChartData(moodEntries.toList(), maEntries.toList())
    }

    private fun chooseFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/*"
        }
        startActivityForResult(intent, PICK_CSV_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, dataIntent: Intent?) {
        super.onActivityResult(requestCode, resultCode, dataIntent)
        if (requestCode == PICK_CSV_CODE && resultCode == Activity.RESULT_OK) {
            val uri = dataIntent?.data
            if (uri != null) {
                lastUri = uri
                mt.readCsv(uri)

                // Get chart data
                val results = mt.getResults()
                moods = results.first
                dates = results.second

                // Create mood entries list
                val moodEntries = Array(moods.size) { Entry(0f, 0f) }
                for ((i, mood) in moods.withIndex()) {
                    moodEntries[i] = Entry(i.toFloat(), mood)
                }

                // Create mood moving average entries list
                val moodMA = moods
                    .toList().windowed(avgWindow, 1) { it.average() }
                    .map { it.toFloat() }
                val maEntries = Array(moodMA.size) { Entry(0f, 0f) }
                for ((i, ma) in moodMA.withIndex()) {
                    maEntries[i] = Entry(i.toFloat(), ma)
                }

                // Load chart data
                loadChartData(moodEntries.toList(), maEntries.toList())
            } else {
                toast("No file selected")
            }
        }
    }

    fun applyNewMoods() {
        reloadChart()
        mt.saveMoods()
    }

    private fun updateRequest(updateUrl: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("New version available")
            .setMessage("An update of DaylioTools is available, would you like to download the update APK?")
            .setPositiveButton("Download APK") { _, _ ->
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl))
                startActivity(browserIntent)
            }
            .setNegativeButton("Ignore") { _, _ ->
            }
            .show()
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}

private fun Context.isDarkModeOn(): Boolean {
    return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}
