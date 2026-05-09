package com.prayerpilot.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.prayerpilot.app.R
import com.prayerpilot.app.fragments.SettingsFragment

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings_container, SettingsFragment()).commit()
        }
    }
}
