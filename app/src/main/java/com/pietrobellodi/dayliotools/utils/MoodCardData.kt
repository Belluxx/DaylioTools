package com.pietrobellodi.dayliotools.utils

/**
 * Contains information about a mood
 *
 * @param mood the name of the mood
 * @param moodValue the numerical value of the mood: 1=worse, 5=best
 */
data class MoodCardData(val mood: String, val moodValue: Float)
