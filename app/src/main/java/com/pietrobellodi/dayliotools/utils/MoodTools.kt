package com.pietrobellodi.dayliotools.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.gson.Gson
import com.pietrobellodi.dayliotools.R
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.new_mood_dialog_view.view.*

class MoodTools(
    private val activity: Activity,
    private val fm: FragmentManager,
    private val cr: ContentResolver
) {

    private var moodsMap = mutableMapOf<String, Float>()
    var customMoodsQueue = arrayListOf<String>()

    private lateinit var dates: Array<String>
    private lateinit var moods: Array<Float>

    init {
        loadCustomMoods()
    }

    fun readCsv(uri: Uri) {
        val rawEntries = readTextFile(uri)
        val size = rawEntries.size - 1
        dates = Array(size) { "[null_date]" }
        val moodsRaw = Array(size) { "[null_mood]" }

        // Extract data from CSV
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

        // Convert moods from string to number
        moods = convertMoods(moodsRaw)
    }

    fun getResults(): Pair<Array<Float>, Array<String>> {
        return Pair(moods, dates)
    }

    fun saveCustomMoods() {
        val prefs = activity.getPreferences(Context.MODE_PRIVATE)
        prefs.edit().putString("moodsMap", Gson().toJson(moodsMap)).apply()
    }

    private fun loadCustomMoods() {
        val prefs = activity.getPreferences(Context.MODE_PRIVATE)
        val data = prefs.getString("moodsMap", "")
        if (data == "") return
        moodsMap = Gson().fromJson(data, moodsMap.javaClass)
    }

    private fun convertMoods(moods: Array<String>): Array<Float> {
        return moods.mapNotNull {
            if (moodsMap.containsKey(it)) {
                return@mapNotNull moodsMap[it]!!.toFloat()
            } else { // Mood not contained in moodMap
                if (!customMoodsQueue.contains(it)) askNewCustomMood(it)
                return@mapNotNull null
            }
        }.toTypedArray()
    }

    private fun askNewCustomMood(mood: String) {
        if (mood in customMoodsQueue) return // Do not ask if already in queue
        val dialog = NewMoodDialogFragment(mood, moodsMap, customMoodsQueue)
        dialog.isCancelable = false
        dialog.show(fm, "DefineMoodDialog")
        customMoodsQueue.add(mood)
    }

    private fun readTextFile(uri: Uri): List<String> =
        cr.openInputStream(uri)?.bufferedReader()?.useLines { it.toList() }!!

    class NewMoodDialogFragment(
        private val mood: String,
        private val moodsMap: MutableMap<String, Float>,
        private val customMoodsQueue: ArrayList<String>
    ) : DialogFragment() {

        @SuppressLint("InflateParams")
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val builder = AlertDialog.Builder(activity)
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.new_mood_dialog_view, null)
            view.body_tv.text =
                "The mood \"$mood\" is not recognized, please choose a value for that mood:\n\n1: Awful mood\n5: Rad mood"
            view.mood_tv.text = mood
            builder
                .setTitle("Define custom mood")
                .setView(view)
                .setPositiveButton("Create mood") { _, _ ->
                    val value = view.mood_value_sb.value
                    moodsMap[mood] = value
                    customMoodsQueue.remove(mood)
                }
            return builder.create()
        }
    }
}