package com.prayerpilot.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.prayerpilot.app.data.PrayerRepository
import com.google.android.gms.ads.MobileAds

class PrayerPilotApp : Application() {
    val repository by lazy { PrayerRepository(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        MobileAds.initialize(this) { }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java)
        listOf(
            NotificationChannel("adhan", "Adhan Calls", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Plays the Adhan when prayer time arrives"
                enableVibration(true)
            },
            NotificationChannel("reminder", "Pre-prayer reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Gentle reminder before prayer"
            },
            NotificationChannel("ramadan", "Ramadan Companion", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Iftar, Suhoor and Ramadan reminders"
            },
            NotificationChannel("streak", "Streaks & Family", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Streak updates and family notifications"
            }
        ).forEach { nm.createNotificationChannel(it) }
    }
}
