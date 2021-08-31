package com.pietrobellodi.dayliotools.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.pietrobellodi.dayliotools.MainActivity
import com.pietrobellodi.dayliotools.R
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.new_mood_dialog_view.view.*

class MoodTools(
    private val activity: Activity,
    private val cr: ContentResolver
) {

    private var moodsMap = mutableMapOf<String, Float>()
    private var moodDialogsQueue = arrayListOf<String>()

    private lateinit var dates: Array<String>
    private lateinit var moods: Array<Float>

    init {
        loadMoods()
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

    fun saveMoods() {
        val prefs = activity.getSharedPreferences(
            activity.getString(R.string.shared_prefs),
            Context.MODE_PRIVATE
        )
        prefs.edit().putString("moodsMap", Gson().toJson(moodsMap)).apply()
    }

    fun loadMoods() {
        val prefs = activity.getSharedPreferences(
            activity.getString(R.string.shared_prefs),
            Context.MODE_PRIVATE
        )
        val data = prefs.getString("moodsMap", "")
        if (data == "") return
        moodsMap = Gson().fromJson(data, moodsMap.javaClass)
    }

    fun getSavedMoods(): MutableMap<String, Float> {
        val prefs = activity.getSharedPreferences(
            activity.getString(R.string.shared_prefs),
            Context.MODE_PRIVATE
        )
        val data = prefs.getString("moodsMap", "")
        return if (data == "") mutableMapOf() else Gson().fromJson(data, moodsMap.javaClass)
    }

    private fun convertMoods(moods: Array<String>): Array<Float> {
        return moods.mapNotNull {
            if (moodsMap.containsKey(it)) {
                return@mapNotNull moodsMap[it]!!.toFloat()
            } else { // Mood not contained in moodMap
                if (!moodDialogsQueue.contains(it)) askToDefineMood(it)
                return@mapNotNull null
            }
        }.toTypedArray()
    }

    private fun askToDefineMood(mood: String) {
        if (mood in moodDialogsQueue) return // Do not ask if already in queue
        val dialog = DefineMoodDialog(activity, mood, moodsMap, moodDialogsQueue)
        dialog.show()
        moodDialogsQueue.add(mood)
    }

    private fun readTextFile(uri: Uri): List<String> =
        cr.openInputStream(uri)?.bufferedReader()?.useLines { it.toList() }!!

    fun savedMoodsChanged(): Boolean {
        return moodsMap != getSavedMoods()
    }

    class DefineMoodDialog(
        private val activity: Activity,
        private val mood: String,
        private val moodsMap: MutableMap<String, Float>,
        private val customMoodsQueue: ArrayList<String>
    ) {

        @SuppressLint("InflateParams")
        fun show() {
            val inflater = activity.layoutInflater
            val view = inflater.inflate(R.layout.new_mood_dialog_view, null)
            view.mood_tv.text = mood
            MaterialAlertDialogBuilder(activity)
                .setView(view)
                .setTitle("Define new mood")
                .setMessage("The mood \"$mood\" is not recognized, please choose a value for that mood:")
                .setPositiveButton("Save") { _, _ ->
                    val value = view.mood_value_sb.value
                    moodsMap[mood] = value
                    customMoodsQueue.remove(mood)
                    if (customMoodsQueue.isEmpty()) {
                        (activity as MainActivity).applyNewMoods()
                    }
                }
                .setCancelable(false)
                .show()
        }
    }
}