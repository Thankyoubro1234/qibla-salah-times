package com.prayerpilot.app.services

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.prayerpilot.app.R

class AdhanPlaybackService : Service() {
    private var mp: MediaPlayer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val name = intent?.getStringExtra("prayer") ?: "Prayer"
        val notif = NotificationCompat.Builder(this, "adhan")
            .setSmallIcon(R.mipmap.ic_launcher).setContentTitle("Adhan playing")
            .setContentText(name).setOngoing(true).build()
        startForeground(2024, notif)

        try {
            mp = MediaPlayer.create(this, R.raw.adhan).apply {
                setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                setOnCompletionListener { stopSelf() }
                start()
            }
        } catch (_: Exception) { stopSelf() }
        return START_NOT_STICKY
    }

    override fun onDestroy() { super.onDestroy(); mp?.release(); mp = null }
    override fun onBind(intent: Intent?): IBinder? = null
}
