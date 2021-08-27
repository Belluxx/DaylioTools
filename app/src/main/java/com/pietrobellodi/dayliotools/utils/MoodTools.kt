package com.pietrobellodi.dayliotools.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.fragment.app.FragmentManager
import com.pietrobellodi.dayliotools.fragments.NewMoodDialogFragment
import kotlinx.android.synthetic.main.activity_main.*

class MoodTools(private val ctx: Context, private val fm: FragmentManager, private val cr: ContentResolver) {

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

    private var customMoods = mapOf(
        "english" to mutableMapOf<String, Int>( // English custom moods
        ),
        "italian" to mutableMapOf( // Italian custom moods
        ),
        "german" to mutableMapOf( // German custom moods
        )
    )
    private var customMoodsQueue = arrayListOf<String>()

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

    private fun convertMoods(moods: Array<String>, language: String): Array<Float> {
        val moodMap = MOOD_MAPS[language]!!
        val customMoodMap = customMoods[language]!!

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
        val dialog = NewMoodDialogFragment(language, mood, customMoods)
        dialog.show(fm, "NewMoodDialog")
        customMoodsQueue.add(mood)
    }

    private fun readTextFile(uri: Uri): List<String> =
        cr.openInputStream(uri)?.bufferedReader()?.useLines { it.toList() }!!
}