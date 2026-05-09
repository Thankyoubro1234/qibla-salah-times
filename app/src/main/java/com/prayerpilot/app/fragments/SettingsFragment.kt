package com.prayerpilot.app.fragments

import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.setPadding
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.prayerpilot.app.BuildConfig
import com.prayerpilot.app.R

class SettingsFragment : PreferenceFragmentCompat() {
    private var preview: MediaPlayer? = null
    private var adView: AdView? = null

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
            startActivity(Intent.createChooser(
                Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text),
                "Share app"))
            true
        }
        findPreference<Preference>("rate_app")?.setOnPreferenceClickListener {
            val pkg = requireContext().packageName
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$pkg")))
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Wrap existing root with vertical LinearLayout so we can attach ad below.
        val root = view as? ViewGroup ?: return
        val parent = root.parent as? ViewGroup ?: return
        val index = parent.indexOfChild(root)

        val wrapper = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = root.layoutParams
        }
        parent.removeView(root)
        // Make recycler take all available space
        root.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        wrapper.addView(root)

        adView = AdView(requireContext()).apply {
            adUnitId = getString(R.string.admob_banner_id)
            setAdSize(AdSize.BANNER)
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(8)
        }
        val container = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(0)
            addView(adView)
        }
        wrapper.addView(container)
        parent.addView(wrapper, index)

        adView?.loadAd(AdRequest.Builder().build())
    }

    override fun onPause() {
        super.onPause()
        preview?.release(); preview = null
        adView?.pause()
    }

    override fun onResume() {
        super.onResume()
        adView?.resume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adView?.destroy(); adView = null
    }
}
