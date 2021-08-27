package com.pietrobellodi.dayliotools

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.pietrobellodi.dayliotools.utils.MoodTools
import kotlinx.android.synthetic.main.activity_main.*

// TODO Save custom moods
// TODO Allow management of custom moods

class MainActivity : AppCompatActivity() {

    private val PICK_CSV_CODE = 10

    private lateinit var LANGUAGES: Array<String>
    private lateinit var mt: MoodTools

    private lateinit var moods: Array<Float>
    private lateinit var dates: Array<String>

    private var textColor = -1
    private var mainLineColor = -1
    private var accentLineColor = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initVars()
        initWidgets()
    }

    private fun initVars() {
        LANGUAGES = resources.getStringArray(R.array.languages_array)
        mt = MoodTools(this, supportFragmentManager, contentResolver)
        setupColors()
    }

    private fun initWidgets() {
        setupChart()

        window_sb.isEnabled = false
        avg_swt.isEnabled = false

        choose_btn.setOnClickListener {
            chooseFile()
        }

        ArrayAdapter.createFromResource(this, R.array.languages_array, android.R.layout.simple_spinner_item)
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                language_spn.adapter = adapter
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, dataIntent: Intent?) {
        super.onActivityResult(requestCode, resultCode, dataIntent)
        if (requestCode == PICK_CSV_CODE && resultCode == Activity.RESULT_OK) {
            val uri = dataIntent?.data
            if (uri != null) {
                mt.readCsv(uri, mt.LANGUAGES[language_spn.selectedItemPosition])
                if (mt.customMoodsQueue.isNotEmpty()) { // If the user was asked to define new custom moods
                    // Ask the user to reload the data
                    val alertDialog = AlertDialog.Builder(this)

                    alertDialog.apply {
                        setTitle("Reload data")
                        setMessage("You just added new custom moods, would you like to reload the chart to see the updated data?")
                        setPositiveButton("Yes") { _, _ ->
                            reloadChart(uri)
                        }
                        setNegativeButton("No") { _, _ ->
                        }
                    }.create().show()
                }
                val results = mt.getResults()
                moods = results.first
                dates = results.second

                // Create mood entries list
                val moodEntries = Array(moods.size) { Entry(0f, 0f) }
                for ((i, mood) in moods.withIndex()) {
                    moodEntries[i] = Entry(i.toFloat(), mood)
                }
                loadChartData(moodEntries.toList())

                window_sb.isEnabled = true
                avg_swt.isEnabled = true
            } else {
                toast("No file selected")
            }
        }
    }

    private fun reloadChart(uri: Uri) {
        mt.readCsv(uri, mt.LANGUAGES[language_spn.selectedItemPosition])
        val results = mt.getResults()
        moods = results.first
        dates = results.second

        // Create mood entries list
        val moodEntries = Array(moods.size) { Entry(0f, 0f) }
        for ((i, mood) in moods.withIndex()) {
            moodEntries[i] = Entry(i.toFloat(), mood)
        }
        loadChartData(moodEntries.toList())

        window_sb.isEnabled = true
        avg_swt.isEnabled = true
    }

    private fun loadChartData(moodEntries: List<Entry>) {
        if (moodEntries.isNotEmpty()) {
            // Create mood dataset
            val dataset = LineDataSet(moodEntries, "Mood")
            with(dataset) {
                color = accentLineColor
                lineWidth = 0.8f
                mode = LineDataSet.Mode.CUBIC_BEZIER

                setDrawValues(false)
                setDrawHighlightIndicators(false)
                setDrawCircles(false)
            }

            // Setup the chart
            chart.data = LineData(dataset)

            chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
            chart.xAxis.setDrawGridLines(false)
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(dates)
            chart.xAxis.textColor = textColor

            chart.invalidate()
            chart.setVisibleXRangeMinimum(70f)
            chart.setVisibleXRangeMaximum(7f)
        } else {
            toast("No data to create graph")
        }
    }

    private fun setupColors() {
        if (isDarkModeOn()) {
            textColor = Color.parseColor("#FFFFFF")
            mainLineColor = Color.parseColor("#555555")
            accentLineColor = Color.parseColor("#FFAA00")
        } else {
            textColor = Color.parseColor("#000000")
            mainLineColor = Color.parseColor("#AAAAAA")
            accentLineColor = Color.parseColor("#FFAA00")
        }
    }

    private fun setupChart() {
        with(chart) {
            legend.isEnabled = true
            legend.textColor = textColor
            legend.textSize = 12f

            axisRight.isEnabled = false
            axisLeft.textColor = textColor
            axisLeft.setDrawGridLines(false)

            description.isEnabled = false
            isScaleYEnabled = false
        }
    }

    private fun chooseFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/*"
        }
        startActivityForResult(intent, PICK_CSV_CODE)
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}

private fun Context.isDarkModeOn(): Boolean {
    return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}
