package com.pietrobellodi.dayliotools

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val PICK_CSV_CODE = 10
    private val MA_WINDOW = 4
    private val MOOD_MAP = mapOf(
        "terribile" to 2,
        "male"      to 4,
        "così così" to 6,
        "buono"     to 8,
        "ottimo"    to 10
    )

    private lateinit var datasetRaw: LineDataSet
    private lateinit var datasetMA: LineDataSet
    private lateinit var dates: Array<String>
    private lateinit var moods: Array<String>
    private lateinit var moodValues: Array<Float>

    private lateinit var entriesRaw: List<Entry>
    private lateinit var entriesMA: List<Entry>

    private var textColor = -1
    private var mainLineColor = -1
    private var accentLineColor = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupColors()
        setupChart()

        choose_btn.setOnClickListener {
            chooseFile()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, dataIntent: Intent?) {
        super.onActivityResult(requestCode, resultCode, dataIntent)
        if (requestCode == PICK_CSV_CODE && resultCode == Activity.RESULT_OK) {
            val uri = dataIntent?.data
            if (uri != null) {
                val lines = readTextFile(uri)
                extractData(lines)
                loadChartData()
            } else {
                toast("No file selected")
            }
        }
    }

    private fun extractData(rawData: List<String>) {
        val size = rawData.size - 1
        dates = Array(size) { "[null_date]" }
        moods = Array(size) { "[null_mood]" }

        // Extract data from file text
        var i: Int = -1
        rawData.forEach {
            if (i != -1) { // Skip first line (labels)
                val rawEntries = it.split(",")
                dates[i] = rawEntries[0]
                moods[i] = rawEntries[4]
            }
            i++
        }
        moods.reverse()
        dates.reverse()

        // Translate mood strings to numbers
        moodValues = moods.map {
            MOOD_MAP[it]!!.toFloat()
        }.toTypedArray()

        // Create mood entries list
        i = 0
        val entriesRawArray = Array(size) { Entry(0f, 0f) }
        for (mood in moodValues) {
            entriesRawArray[i] = Entry(i.toFloat(), mood)
            i++
        }
        entriesRaw = entriesRawArray.toList()

        // Create mood moving average entries list
        i = 0
        val moodMA = moodValues
            .toList().windowed(MA_WINDOW, 1) { it.average() }
            .map { it.toFloat() }
        val entriesMAArray = Array(moodMA.size) { Entry(0f, 0f) }
        for (ma in moodMA) {
            entriesMAArray[i] = Entry(i.toFloat()+MA_WINDOW/2f, ma)
            i++
        }
        entriesMA = entriesMAArray.toList()
    }

    private fun loadChartData() {
        if (entriesRaw.isNotEmpty()) {
            // Create mood dataset
            datasetRaw = LineDataSet(entriesRaw, "Mood")
            with(datasetRaw) {
                color = mainLineColor
                lineWidth = 0.8f
                mode = LineDataSet.Mode.CUBIC_BEZIER

                setDrawValues(false)
                setDrawHighlightIndicators(false)
                setDrawCircles(false)
            }

            // Create mood moving average dataset
            datasetMA = LineDataSet(entriesMA, "$MA_WINDOW Day average")
            with(datasetMA) {
                color = accentLineColor
                lineWidth = 2f
                mode = LineDataSet.Mode.CUBIC_BEZIER

                setDrawValues(false)
                setDrawHighlightIndicators(false)
                setDrawCircles(false)
            }

            // Setup the chart
            chart.data = LineData(listOf(datasetRaw, datasetMA))

            chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
            chart.xAxis.setDrawGridLines(false)
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(dates)
            chart.xAxis.textColor = textColor

            chart.invalidate()
            chart.setVisibleXRangeMinimum(10f)
            chart.setVisibleXRangeMaximum(30f)
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

    private fun readTextFile(uri: Uri): List<String> = contentResolver.openInputStream(uri)?.bufferedReader()?.useLines { it.toList() }!!

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}

private fun Context.isDarkModeOn(): Boolean {
    return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}
