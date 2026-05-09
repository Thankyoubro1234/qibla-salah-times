package com.prayerpilot.app.fragments

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.prayerpilot.app.PrayerPilotApp
import com.prayerpilot.app.R
import com.prayerpilot.app.prayer.PrayTimes
import com.prayerpilot.app.services.PrayerScheduler
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PrayerFragment : Fragment() {
    private var countdown: CountDownTimer? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View =
        inflater.inflate(R.layout.fragment_prayer, container, false)

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        val app = requireContext().applicationContext as PrayerPilotApp
        val today = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
        view.findViewById<TextView>(R.id.tv_today).text = today
        view.findViewById<TextView>(R.id.tv_hijri).text = HijriDate.todayHijriString()

        viewLifecycleOwner.lifecycleScope.launch {
            val times = app.repository.computeTimes() ?: return@launch
            renderTimes(view, times)
            updateCountdown(view, times)
            PrayerScheduler.scheduleAll(requireContext(), times)
        }
    }

    private fun renderTimes(v: View, t: PrayTimes.Times) {
        v.findViewById<TextView>(R.id.tv_fajr).text = PrayTimes.formatTime(t.fajr)
        v.findViewById<TextView>(R.id.tv_sunrise).text = PrayTimes.formatTime(t.sunrise)
        v.findViewById<TextView>(R.id.tv_dhuhr).text = PrayTimes.formatTime(t.dhuhr)
        v.findViewById<TextView>(R.id.tv_asr).text = PrayTimes.formatTime(t.asr)
        v.findViewById<TextView>(R.id.tv_maghrib).text = PrayTimes.formatTime(t.maghrib)
        v.findViewById<TextView>(R.id.tv_isha).text = PrayTimes.formatTime(t.isha)
    }

    private fun updateCountdown(v: View, t: PrayTimes.Times) {
        val now = Calendar.getInstance()
        val nowHours = now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE)/60.0 + now.get(Calendar.SECOND)/3600.0
        val pairs = listOf(
            "Fajr" to t.fajr, "Dhuhr" to t.dhuhr, "Asr" to t.asr,
            "Maghrib" to t.maghrib, "Isha" to t.isha
        )
        val next = pairs.firstOrNull { it.second > nowHours } ?: ("Fajr (tomorrow)" to (t.fajr + 24))
        val msUntil = ((next.second - nowHours) * 3_600_000L).toLong()
        v.findViewById<TextView>(R.id.tv_next_label).text = "Next: ${next.first}"
        countdown?.cancel()
        countdown = object : CountDownTimer(msUntil, 1000) {
            override fun onTick(ms: Long) {
                val h = ms/3_600_000
                val m = (ms/60_000) % 60
                val s = (ms/1000) % 60
                v.findViewById<TextView>(R.id.tv_countdown).text = String.format("%02d:%02d:%02d", h, m, s)
            }
            override fun onFinish() {}
        }.start()
    }

    override fun onDestroyView() { super.onDestroyView(); countdown?.cancel() }
}
