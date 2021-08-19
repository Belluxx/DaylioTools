package com.pietrobellodi.dayliotools

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val PICK_CSV_CODE = 10
    private val ERROR_CODE = -101f
    private val MOOD_MAPS = mapOf(
        "english" to mapOf( // English map
            "awful" to 2,
            "bad" to 4,
            "meh" to 6,
            "good" to 8,
            "rad" to 10
        ),
        "italian" to mapOf( // Italian map
            "terribile" to 2,
            "male" to 4,
            "così così" to 6,
            "buono" to 8,
            "ottimo" to 10
        ),
        "german" to mapOf( // German map
            "Einfach scheiße" to 2,
            "Schlecht" to 4,
            "Ok" to 6,
            "Gut" to 8,
            "Super" to 10
        )
    )

    private lateinit var rawData: List<String>

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
    
    private val MAX_GRAPH_POINTS = 100f
    private val MIN_GRAPH_POINTS = 7f
    private var avg_window = 3
    private var avg_render = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupColors()
        setupChart()

        choose_btn.setOnClickListener {
            chooseFile()
        }

        avg_swt.isEnabled = false
        avg_swt.setOnCheckedChangeListener { _, isChecked ->
            if (::rawData.isInitialized) {
                if (isChecked) {
                    window_lay.visibility = View.VISIBLE
                    avg_render = true
                    if (rawData.isNotEmpty()) updateGraph(rawData)
                } else {
                    window_lay.visibility = View.GONE
                    avg_render = false
                    updateGraph(rawData)
                }
            }
        }

        window_sb.isEnabled = false
        window_sb.progress = avg_window - 3
        window_sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (::rawData.isInitialized) {
                    avg_window = progress + 3
                    updateGraph(rawData)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}

        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, dataIntent: Intent?) {
        super.onActivityResult(requestCode, resultCode, dataIntent)
        if (requestCode == PICK_CSV_CODE && resultCode == Activity.RESULT_OK) {
            val uri = dataIntent?.data
            if (uri != null) {
                rawData = readTextFile(uri)
                updateGraph(rawData)

                window_sb.isEnabled = true
                avg_swt.isEnabled = true
            } else {
                toast("No file selected")
            }
        }
    }

    private fun updateGraph(rawData: List<String>) {
        val size = rawData.size - 1
        dates = Array(size) { "[null_date]" }
        moods = Array(size) { "[null_mood]" }

        // Extract data from file text
        var i: Int = -1
        rawData.forEach {
            if (i != -1) { // Skip first line (labels)
                val rawEntries = it.split(",")
                dates[i] = rawEntries[1]
                moods[i] = rawEntries[4]
            }
            i++
        }
        moods.reverse()
        dates.reverse()

        // Translate mood strings to numbers
        moodValues = convertMoods(moods)
        if (moodValues[0] == ERROR_CODE) return

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
            .toList().windowed(avg_window, 1) { it.average() }
            .map { it.toFloat() }
        val entriesMAArray = Array(moodMA.size) { Entry(0f, 0f) }
        for (ma in moodMA) {
            entriesMAArray[i] = Entry(i.toFloat(), ma)
            i++
        }
        entriesMA = entriesMAArray.toList()

        loadChartData()
    }

    private fun loadChartData() {
        if (entriesRaw.isNotEmpty()) {
            // Create mood dataset
            datasetRaw = LineDataSet(entriesRaw, "Mood")
            with(datasetRaw) {
                color = if (avg_render) mainLineColor else accentLineColor
                lineWidth = 0.8f
                mode = LineDataSet.Mode.CUBIC_BEZIER

                setDrawValues(false)
                setDrawHighlightIndicators(false)
                setDrawCircles(false)
            }

            // Create mood moving average dataset
            datasetMA = LineDataSet(entriesMA, "$avg_window Day average")
            with(datasetMA) {
                color = if (avg_render) accentLineColor else mainLineColor
                lineWidth = 2f
                mode = LineDataSet.Mode.CUBIC_BEZIER

                setDrawValues(false)
                setDrawHighlightIndicators(false)
                setDrawCircles(false)
            }

            // Setup the chart
            if (avg_render) chart.data = LineData(listOf(datasetRaw, datasetMA))
            else chart.data = LineData(datasetRaw)

            chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
            chart.xAxis.setDrawGridLines(false)
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(dates)
            chart.xAxis.textColor = textColor

            chart.invalidate()
            chart.setVisibleXRangeMinimum(MIN_GRAPH_POINTS)
            chart.setVisibleXRangeMaximum(MAX_GRAPH_POINTS)
        } else {
            toast("No data to create graph")
        }
    }

    private fun convertMoods(moods: Array<String>): Array<Float> {
        var map: Map<String, Int> = mapOf()

        var found = false
        MOOD_MAPS.forEach { (_, _map) ->
            if (_map.containsKey(moods[0])) {
                map = _map
                found = true
            }
        }
        if (!found) {
            toast("Your language is not supported")
            return arrayOf(ERROR_CODE)
        }

        return moods.mapNotNull {
            if (map.containsKey(it)) {
                return@mapNotNull map[it]!!.toFloat()
            } else {
                toast("CSV File corrupted")
                return@mapNotNull null
            }
        }.toTypedArray()
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

    private fun readTextFile(uri: Uri): List<String> =
        contentResolver.openInputStream(uri)?.bufferedReader()?.useLines { it.toList() }!!

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}

private fun Context.isDarkModeOn(): Boolean {
    return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}
