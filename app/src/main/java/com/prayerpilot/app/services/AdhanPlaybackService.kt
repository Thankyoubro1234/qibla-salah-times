package com.prayerpilot.app.services

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.prayerpilot.app.PrayerPilotApp
import com.prayerpilot.app.R
import kotlinx.coroutines.runBlocking

/**
 * Plays the Adhan or alternative tone for a prayer. Honors the user's
 * notification mode preference: full Adhan, silent (visual only),
 * vibration only, or soft beep.
 */
class AdhanPlaybackService : Service() {
    private var mp: MediaPlayer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val name = intent?.getStringExtra("prayer") ?: "Prayer"
        val app = applicationContext as PrayerPilotApp
        val mode = runBlocking { app.repository.getNotifMode() }
        val voice = runBlocking { app.repository.getAdhanVoice() }

        val notif = NotificationCompat.Builder(this, channelForMode(mode))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$name time")
            .setContentText("It is time for $name")
            .setOngoing(false)
            .build()
        startForeground(2024, notif)

        when (mode) {
            "silent" -> { stopSelf(); return START_NOT_STICKY }
            "vibrate" -> {
                vibrateFor(2000)
                stopSelf()
                return START_NOT_STICKY
            }
            "beep" -> playRaw(R.raw.adhan_beep)
            "full" -> playRaw(resourceForVoice(voice))
            else -> playRaw(resourceForVoice(voice))
        }
        return START_NOT_STICKY
    }

    private fun resourceForVoice(voice: String): Int = when (voice) {
        "makkah" -> R.raw.adhan_makkah
        "madinah" -> R.raw.adhan_madinah
        "mishary" -> R.raw.adhan_mishary
        "beep" -> R.raw.adhan_beep
        else -> R.raw.adhan_makkah
    }

    private fun channelForMode(mode: String): String = when (mode) {
        "silent" -> "reminder"
        else -> "adhan"
    }

    private fun vibrateFor(ms: Long) {
        val v = getSystemService(Vibrator::class.java) ?: return
        if (!v.hasVibrator()) return
        v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400, 200, 800), -1))
    }

    private fun playRaw(resId: Int) {
        try {
            mp = MediaPlayer.create(this, resId).apply {
                setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                setOnCompletionListener { stopSelf() }
                start()
            }
        } catch (_: Exception) { stopSelf() }
    }

    override fun onDestroy() { super.onDestroy(); mp?.release(); mp = null }
    override fun onBind(intent: Intent?): IBinder? = null
}
