package com.pietrobellodi.dayliotools

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * This activity provides an in-app tutorial about how to export
 * a CSV file from the Daylio app
 */
class HelpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)
    }
}