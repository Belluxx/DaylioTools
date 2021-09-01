package com.pietrobellodi.dayliotools

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.pietrobellodi.dayliotools.utils.MoodCardAdapter
import com.pietrobellodi.dayliotools.utils.MoodCardData
import kotlinx.android.synthetic.main.activity_manage_moods.*

/**
 * This activity allows the user to edit or delete user-defined moods
 */
class ManageMoodsActivity : AppCompatActivity() {

    private lateinit var moodsData: ArrayList<MoodCardData>
    private var moodsMap = mutableMapOf<String, Float>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_moods)

        loadMoods()
        updateList()
    }

    private fun updateList() {
        moodsData = arrayListOf()
        moodsMap.forEach { (mood, value) ->
            moodsData.add(MoodCardData(mood, value))
        }

        moods_rv.adapter = MoodCardAdapter(this, moodsData, moodsMap)
        moods_rv.layoutManager = LinearLayoutManager(this)
        moods_rv.setHasFixedSize(true)
    }

    private fun loadMoods() {
        val prefs = getSharedPreferences(getString(R.string.shared_prefs), Context.MODE_PRIVATE)
        val data = prefs.getString("moodsMap", "")
        if (data == "") return
        moodsMap = Gson().fromJson(data, moodsMap.javaClass)
    }
}