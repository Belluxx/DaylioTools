package com.pietrobellodi.dayliotools.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.SeekBar
import androidx.fragment.app.DialogFragment
import com.pietrobellodi.dayliotools.R
import com.pietrobellodi.dayliotools.utils.MoodTools
import kotlinx.android.synthetic.main.new_mood_dialog_view.*
import kotlinx.android.synthetic.main.new_mood_dialog_view.view.*

class NewMoodDialogFragment(private val language: String, private val mood: String, private val customMoods: Map<String, MutableMap<String, Int>>) : DialogFragment() {

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
            }
            .setCancelable(false)
        return builder.create()
    }
}