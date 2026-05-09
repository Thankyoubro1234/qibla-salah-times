package com.prayerpilot.app.fragments

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.prayerpilot.app.R

class SettingsFragment : PreferenceFragmentCompat() {
    private var preview: MediaPlayer? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        findPreference<Preference>("adhan_preview")?.setOnPreferenceClickListener {
            val voiceKey = preferenceManager.sharedPreferences?.getString("adhan_voice", "makkah") ?: "makkah"
            val resId = when (voiceKey) {
                "makkah" -> R.raw.adhan_makkah
                "madinah" -> R.raw.adhan_madinah
                "mishary" -> R.raw.adhan_mishary
                else -> R.raw.adhan_makkah
            }
            preview?.release()
            preview = MediaPlayer.create(requireContext(), resId)?.apply {
                setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                setOnCompletionListener { release(); preview = null }
                start()
            }
            true
        }
    }

    override fun onPause() {
        super.onPause()
        preview?.release(); preview = null
    }
}
