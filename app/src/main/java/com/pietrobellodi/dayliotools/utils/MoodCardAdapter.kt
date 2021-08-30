package com.pietrobellodi.dayliotools.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.pietrobellodi.dayliotools.R

class MoodCardAdapter(private val moodsCardsData: ArrayList<MoodCardData>) :
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

        // TODO Define buttons behaviour
    }

    override fun getItemCount(): Int {
        return moodsCardsData.size
    }

    class MoodCardHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.mood_card)
        val name_tv: TextView = view.findViewById(R.id.mood_card_name_tv)
        val edit_btn: MaterialButton = view.findViewById(R.id.mood_card_edit_btn)
        val delete_btn: MaterialButton = view.findViewById(R.id.mood_card_delete_btn)
    }
}