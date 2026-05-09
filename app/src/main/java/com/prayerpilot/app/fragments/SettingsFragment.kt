package com.prayerpilot.app.fragments

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.prayerpilot.app.BuildConfig
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

        findPreference<Preference>("share_app")?.setOnPreferenceClickListener {
            val text = "Try Qibla Salah Times — accurate prayer times, the full Holy Quran with audio, and a hyper accurate Qibla compass.\n\nhttps://coworkster.com/qibla-salah-times/qibla-salah-times.apk"
            val intent = Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, text)
            startActivity(Intent.createChooser(intent, "Share app"))
            true
        }

        findPreference<Preference>("rate_app")?.setOnPreferenceClickListener {
            val pkg = requireContext().packageName
            val intent = Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$pkg"))
            startActivity(intent)
            true
        }

        findPreference<Preference>("version")?.summary =
            "${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})"

        findPreference<Preference>("open_source")?.setOnPreferenceClickListener {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://github.com/Thankyoubro1234/qibla-salah-times")))
            true
        }
    }

    override fun onPause() {
        super.onPause()
        preview?.release(); preview = null
    }
}
