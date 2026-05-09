package com.prayerpilot.app.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.prayerpilot.app.PrayerPilotApp
import com.prayerpilot.app.prayer.PrayTimes
import kotlinx.coroutines.runBlocking
import java.util.Calendar

object PrayerScheduler {
    fun scheduleAll(ctx: Context, t: PrayTimes.Times) {
        val app = ctx.applicationContext as PrayerPilotApp
        val enabled = runBlocking { app.repository.isNotifEnabled() }
        if (!enabled) return
        val preMin = runBlocking { app.repository.getPreRemind() }
        val pairs = listOf("Fajr" to t.fajr, "Dhuhr" to t.dhuhr, "Asr" to t.asr,
            "Maghrib" to t.maghrib, "Isha" to t.isha)
        val am = ctx.getSystemService(AlarmManager::class.java)
        pairs.forEach { (name, h) ->
            schedule(ctx, am, name, h, false)
            if (preMin > 0) schedule(ctx, am, name, h - preMin/60.0, true)
        }
    }

    private fun schedule(ctx: Context, am: AlarmManager, name: String, hours: Double, reminder: Boolean) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hours.toInt())
            set(Calendar.MINUTE, ((hours - hours.toInt()) * 60).toInt())
            set(Calendar.SECOND, 0)
            if (timeInMillis < System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val intent = Intent(ctx, PrayerAlarmReceiver::class.java).apply {
            putExtra("prayer", name); putExtra("isReminder", reminder)
        }
        val req = (name + reminder.toString()).hashCode()
        val pi = PendingIntent.getBroadcast(ctx, req, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && am.canScheduleExactAlarms())
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        } catch (_: SecurityException) {
            am.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        }
    }
}
