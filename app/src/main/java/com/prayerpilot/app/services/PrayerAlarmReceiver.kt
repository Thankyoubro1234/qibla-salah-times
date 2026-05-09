package com.prayerpilot.app.services

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.prayerpilot.app.R

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val name = intent.getStringExtra("prayer") ?: "Prayer"
        val isReminder = intent.getBooleanExtra("isReminder", false)
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val builder = NotificationCompat.Builder(ctx, if (isReminder) "reminder" else "adhan")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(if (isReminder) "$name in a few minutes" else "$name time")
            .setContentText(if (isReminder) "Get ready for $name" else "It is time for $name. Allahu Akbar.")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        nm.notify(name.hashCode(), builder.build())
        if (!isReminder) {
            val svc = Intent(ctx, AdhanPlaybackService::class.java).putExtra("prayer", name)
            ctx.startForegroundService(svc)
        }
    }
}
