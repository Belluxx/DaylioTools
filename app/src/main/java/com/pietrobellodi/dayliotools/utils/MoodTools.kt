package com.pietrobellodi.dayliotools.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.SeekBar
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.gson.Gson
import com.pietrobellodi.dayliotools.R
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.new_mood_dialog_view.view.*

class MoodTools(private val activity: Activity, private val fm: FragmentManager, private val cr: ContentResolver) {

    val LANGUAGES = arrayOf("english", "italian", "german")
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

    private var customMoodMaps = mapOf(
        "english" to mutableMapOf<String, Int>( // English custom moods
        ),
        "italian" to mutableMapOf( // Italian custom moods
        ),
        "german" to mutableMapOf( // German custom moods
        )
    )
    var customMoodsQueue = arrayListOf<String>()

    private lateinit var dates: Array<String>
    private lateinit var moods: Array<Float>

    fun readCsv(uri: Uri, language: String) {
        val rawEntries = readTextFile(uri)
        val size = rawEntries.size - 1
        dates = Array(size) { "[null_date]" }
        val moodsRaw = Array(size) { "[null_mood]" }

        // Extract data from file text
        var i: Int = -1
        rawEntries.forEach {
            if (i != -1) { // Skip first line (labels)
                val entries = it.split(",")
                dates[i] = entries[1]
                moodsRaw[i] = entries[4]
            }
            i++
        }
        moodsRaw.reverse()
        dates.reverse()

        // Convert moods
        moods = convertMoods(moodsRaw, language)
    }

    fun getResults(): Pair<Array<Float>, Array<String>> {
        return Pair(moods, dates)
    }

    fun saveCustomMoods() {
        val prefs = activity.getPreferences(Context.MODE_PRIVATE)
        prefs.edit().putString("customMoodMap", Gson().toJson(customMoodMaps)).apply()
    }

    fun loadCustomMoods() {
        val prefs = activity.getPreferences(Context.MODE_PRIVATE)
        val data = prefs.getString("customMoodMap", "")
        if (data == "") return
        customMoodMaps = Gson().fromJson(data, customMoodMaps.javaClass)
    }

    private fun convertMoods(moods: Array<String>, language: String): Array<Float> {
        val moodMap = MOOD_MAPS[language]!!
        val customMoodMap = customMoodMaps[language]!!

        return moods.mapNotNull {
            if (moodMap.containsKey(it)) {
                return@mapNotNull moodMap[it]!!.toFloat()
            } else { // Mood not contained in moodMap
                if (customMoodMap.containsKey(it)) {
                    return@mapNotNull customMoodMap[it]!!.toFloat()
                } else { // Mood unknown
                    askNewCustomMood(language, it)
                    return@mapNotNull null
                }
            }
        }.toTypedArray()
    }

    private fun askNewCustomMood(language: String, mood: String) {
        if (mood in customMoodsQueue) return // Do not ask if already in queue
        val dialog = NewMoodDialogFragment(language, mood, customMoodMaps, customMoodsQueue)
        dialog.isCancelable = false
        dialog.show(fm, "NewMoodDialog")
        customMoodsQueue.add(mood)
    }

    private fun readTextFile(uri: Uri): List<String> =
        cr.openInputStream(uri)?.bufferedReader()?.useLines { it.toList() }!!

    class NewMoodDialogFragment(private val language: String, private val mood: String, private val customMoods: Map<String, MutableMap<String, Int>>, private val customMoodsQueue: ArrayList<String>) : DialogFragment() {

        @SuppressLint("InflateParams")
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val builder = AlertDialog.Builder(activity)
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.new_mood_dialog_view, null)
            view.body_tv.text = "The mood \"$mood\" is not recognized, please choose a value between 0-10 to be associated with that mood"
            view.mood_value_sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekbar: SeekBar?, value: Int, p2: Boolean) {
                    view.mood_value_tv.text = value.toString()
                }

                override fun onStartTrackingTouch(seekbar: SeekBar?) {}

                override fun onStopTrackingTouch(seekbar: SeekBar?) {}

            })
            builder
                .setTitle("Define custom mood")
                .setView(view)
                .setPositiveButton("Create mood") { dialog, id ->
                    val value = view.mood_value_sb.progress
                    customMoods[language]!![mood] = value
                    customMoodsQueue.remove(mood)
                }
            return builder.create()
        }
    }
}