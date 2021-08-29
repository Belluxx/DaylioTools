package com.pietrobellodi.dayliotools

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.pietrobellodi.dayliotools.utils.FirebaseTools
import com.pietrobellodi.dayliotools.utils.MoodTools
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val PICK_CSV_CODE = 10
    private val VERSION = 3

    private lateinit var languagesSpinnerOptions: Array<String>
    private lateinit var mt: MoodTools
    private lateinit var ft: FirebaseTools
    private lateinit var moods: Array<Float>
    private lateinit var dates: Array<String>
    private lateinit var lastUri: Uri

    private var textColor = -1
    private var mainLineColor = -1
    private var accentLineColor = -1
    private var avgRender = false
    private var avgWindow = 3
    private var smoothRender = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initAll()
        loadPrefs()
    }

    private fun initAll() {
        // Init variables
        languagesSpinnerOptions = resources.getStringArray(R.array.languages_array)
        avgRender = avg_swt.isChecked
        smoothRender = smooth_swt.isChecked
        mt = MoodTools(this, supportFragmentManager, contentResolver)
        mt.loadCustomMoods()
        ft = FirebaseTools(
            getPreferences(MODE_PRIVATE).getBoolean("firstLaunch", true),
            object : FirebaseTools.OnDataRetrievedListener {
                override fun onRetrieved(versionCode: Int, updateUrl: String) {
                    if (versionCode > VERSION) {
                        updateRequest(updateUrl)
                    }
                }
            })
        getPreferences(MODE_PRIVATE).edit().putBoolean("firstLaunch", false).apply()

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
            legend.isEnabled = true
            legend.textColor = textColor
            legend.textSize = 12f

            axisRight.isEnabled = false
            axisLeft.textColor = textColor
            axisLeft.setDrawGridLines(false)

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

        window_sb.progress = avgWindow - 3
        window_sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (::moods.isInitialized) {
                    avgWindow = progress + 3
                    if (moods.isNotEmpty()) reloadChart()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}

        })

        ArrayAdapter.createFromResource(
            this,
            R.array.languages_array,
            android.R.layout.simple_spinner_item
        )
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                language_spn.adapter = adapter
            }
        language_spn.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                adapter: AdapterView<*>?,
                view: View?,
                position: Int,
                p3: Long
            ) {
                val prefs = getPreferences(Context.MODE_PRIVATE)
                prefs.edit().putInt("selectedLanguage", position).apply()
            }

            override fun onNothingSelected(adapter: AdapterView<*>?) {}
        }

        choose_btn.setOnClickListener {
            if (language_spn.selectedItemPosition == 0) toast("Please choose a language")
            else chooseFile()
        }

        help_btn.setOnClickListener {
            val intent = Intent(this, HelpActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadPrefs() {
        val prefs = getPreferences(Context.MODE_PRIVATE)
        language_spn.setSelection(prefs.getInt("selectedLanguage", 0))
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
        mt.readCsv(lastUri, mt.LANGUAGES[language_spn.selectedItemPosition - 1])
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
                mt.readCsv(uri, mt.LANGUAGES[language_spn.selectedItemPosition - 1])
                if (mt.customMoodsQueue.isNotEmpty()) { // If the user was asked to define new custom moods
                    // Ask the user to reload the data
                    reloadDataRequest()
                }

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

    private fun reloadDataRequest() {
        val builder = AlertDialog.Builder(this)
        builder.apply {
            setTitle("Reload data")
            setMessage("You just added new custom moods, would you like to reload the chart to see the updated data?")
            setPositiveButton("Yes") { _, _ ->
                reloadChart()
                mt.saveCustomMoods()
            }
            setNegativeButton("No") { _, _ ->
                mt.saveCustomMoods()
            }
        }
        val dialog = builder.create()
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun updateRequest(updateUrl: String) {
        val builder = AlertDialog.Builder(this)
        builder.apply {
            setTitle("Update the app")
            setMessage("A new version of DaylioTools is available, would you like to download it?")
            setPositiveButton("Download") { _, _ ->
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl))
                startActivity(browserIntent)
            }
            setNegativeButton("Ignore") { _, _ ->
            }
        }
        val dialog = builder.create()
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}

private fun Context.isDarkModeOn(): Boolean {
    return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}
