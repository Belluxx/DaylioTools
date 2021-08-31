package com.pietrobellodi.dayliotools.utils

import android.content.ContentResolver
import android.net.Uri

class ActivitiesTools(private val cr: ContentResolver) {

    private lateinit var dates: Array<String>
    private lateinit var activities: Array<String>
    private var activityTypes = arrayListOf<String>()
    private lateinit var activitiesCount: Array<Float>

    fun readCsv(uri: Uri) {
        val rawEntries = readTextFile(uri)
        val size = rawEntries.size - 1
        dates = Array(size) { "[null_date]" }
        activities = Array(size) { "[null_activity]" }

        // Extract data from CSV
        var i = -1
        rawEntries.forEach {
            if (i != -1) { // Skip first line (labels)
                val entries = it.split(",")
                dates[i] = entries[1]
                activities[i] = entries[5]
                if (!activityTypes.contains(activities[i])) activityTypes.add(activities[i])
            }
            i++
        }
        activities.reverse()
        dates.reverse()

        // Count activities
        activitiesCount = Array(activityTypes.size) { 0f }
        for (activity in activities) {
            activitiesCount[activityTypes.indexOf(activity)] += 1f
        }
    }

    fun getResults(): Pair<Array<Float>, Array<String>> {
        return Pair(activitiesCount, activityTypes.toTypedArray())
    }

    private fun readTextFile(uri: Uri): List<String> =
        cr.openInputStream(uri)?.bufferedReader()?.useLines { it.toList() }!!

}