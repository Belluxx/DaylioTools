package com.pietrobellodi.dayliotools.utils

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.pietrobellodi.dayliotools.R
import kotlinx.android.synthetic.main.new_mood_dialog_view.view.*

/**
 * Adapter for mood cards
 *
 * @param activity the context activity
 * @param moodsCardsData list containing data about all moods that
 * will be managed by this adapter
 * @param moodsMap map that will be modified by the user
 */
class MoodCardAdapter(
    private val activity: Activity,
    private val moodsCardsData: ArrayList<MoodCardData>,
    private val moodsMap: MutableMap<String, Float>
) :
    RecyclerView.Adapter<MoodCardAdapter.MoodCardHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MoodCardHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.mood_item, parent, false)
        return MoodCardHolder(view)
    }

    override fun onBindViewHolder(holder: MoodCardHolder, position: Int) {
        val data = moodsCardsData[position]

        with(holder.name_tv) {
            text = data.mood
        }

        holder.edit_btn.setOnClickListener {
            val inflater = activity.layoutInflater
            val view = inflater.inflate(R.layout.new_mood_dialog_view, null)
            view.mood_tv.text = data.mood
            view.mood_value_sb.value = data.moodValue
            MaterialAlertDialogBuilder(activity)
                .setView(view)
                .setTitle("Edit mood")
                .setMessage("Choose a new value for the mood")
                .setPositiveButton("Save") { _, _ ->
                    val value = view.mood_value_sb.value
                    editMood(data.mood, value)
                }
                .setNegativeButton("Cancel") { _, _ ->
                }
                .show()
        }

        holder.delete_btn.setOnClickListener {
            MaterialAlertDialogBuilder(activity)
                .setTitle("Delete mood")
                .setMessage("Do you really want to delete this mood: ${data.mood}?")
                .setPositiveButton("Delete") { _, _ ->
                    deleteMood(data.mood)
                }
                .setNegativeButton("Keep") { _, _ ->
                }
                .show()
        }
    }

    override fun getItemCount(): Int {
        return moodsCardsData.size
    }

    /**
     * Deletes a mood from the moods map and from the recyclerview
     *
     * @param mood the name of the mood that needs to be removed
     */
    private fun deleteMood(mood: String) {
        val moodIndex = moodsMap.keys.indexOf(mood)
        moodsCardsData.remove(moodsCardsData[moodIndex])
        notifyItemRemoved(moodIndex)

        moodsMap.remove(mood)
        val prefs = activity.getSharedPreferences(
            activity.getString(R.string.shared_prefs),
            Context.MODE_PRIVATE
        )
        prefs.edit().putString("moodsMap", Gson().toJson(moodsMap)).apply()
    }

    /**
     * Changes the value of a mood
     *
     * @param mood the name of the mood that needs to be modified
     * @param value the new value for the mood
     */
    private fun editMood(mood: String, value: Float) {
        moodsMap[mood] = value
        val prefs = activity.getSharedPreferences(
            activity.getString(R.string.shared_prefs),
            Context.MODE_PRIVATE
        )
        prefs.edit().putString("moodsMap", Gson().toJson(moodsMap)).apply()
    }

    /**
     * Holds all the widgets of the mood card
     *
     * @param view the root view
     */
    class MoodCardHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.mood_card)
        val name_tv: TextView = view.findViewById(R.id.mood_card_name_tv)
        val edit_btn: MaterialButton = view.findViewById(R.id.mood_card_edit_btn)
        val delete_btn: MaterialButton = view.findViewById(R.id.mood_card_delete_btn)
    }
}