package com.prayerpilot.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.prayerpilot.app.R

class PrayerDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prayer_detail)
        title = intent.getStringExtra("prayer_name") ?: "Prayer"
    }
}
