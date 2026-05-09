package com.prayerpilot.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.prayerpilot.app.PrayerPilotApp
import com.prayerpilot.app.R
import com.prayerpilot.app.fragments.PrayerFragment
import com.prayerpilot.app.fragments.QiblaFragment
import com.prayerpilot.app.fragments.QuranFragment
import com.prayerpilot.app.fragments.SettingsFragment
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val app = applicationContext as PrayerPilotApp
        lifecycleScope.launch {
            if (app.repository.isFirstRun()) {
                startActivity(Intent(this@MainActivity, OnboardingActivity::class.java))
                finish()
                return@launch
            }
        }

        val nav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        nav.setOnItemSelectedListener { item ->
            val frag: Fragment = when (item.itemId) {
                R.id.tab_prayer -> PrayerFragment()
                R.id.tab_qibla -> QiblaFragment()
                R.id.tab_quran -> QuranFragment()
                R.id.tab_settings -> SettingsFragment()
                else -> PrayerFragment()
            }
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.fragment_container, frag).commit()
            true
        }
        if (savedInstanceState == null) nav.selectedItemId = R.id.tab_prayer
    }
}
