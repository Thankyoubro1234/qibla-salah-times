package com.prayerpilot.app.fragments

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.prayerpilot.app.PrayerPilotApp
import com.prayerpilot.app.R
import com.prayerpilot.app.prayer.PrayTimes
import com.prayerpilot.app.services.PrayerScheduler
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PrayerFragment : Fragment() {
    private var countdown: CountDownTimer? = null
    private var times: PrayTimes.Times? = null
    private val prayedToday = mutableSetOf<String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View =
        inflater.inflate(R.layout.fragment_prayer, container, false)

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        val app = requireContext().applicationContext as PrayerPilotApp

        val today = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
        view.findViewById<TextView>(R.id.tv_today).text = today
        view.findViewById<TextView>(R.id.tv_hijri).text = HijriDate.todayHijriString()
        view.findViewById<TextView>(R.id.tv_location).text = "Locating you…"

        viewLifecycleOwner.lifecycleScope.launch {
            val t = app.repository.computeTimes() ?: return@launch
            times = t
            val (lat, lng) = app.repository.getLocation() ?: return@launch
            view.findViewById<TextView>(R.id.tv_location).text =
                "📍 ${"%.3f".format(lat)}°, ${"%.3f".format(lng)}°"
            renderCards(view, t)
            updateCountdown(view, t)
            PrayerScheduler.scheduleAll(requireContext(), t)
        }
    }

    private fun renderCards(root: View, t: PrayTimes.Times) {
        val container = root.findViewById<LinearLayout>(R.id.prayer_list)
        container.removeAllViews()
        val now = currentHours()
        val items = listOf(
            Triple("Fajr",    t.fajr,    R.color.g_blue),
            Triple("Sunrise", t.sunrise, R.color.neon_violet),
            Triple("Dhuhr",   t.dhuhr,   R.color.g_blue),
            Triple("Asr",     t.asr,     R.color.g_green),
            Triple("Maghrib", t.maghrib, R.color.g_green),
            Triple("Isha",    t.isha,    R.color.neon_violet)
        )
        val nextIdx = items.indexOfFirst { it.second > now }
        items.forEachIndexed { i, (name, hours, accent) ->
            val card = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_prayer_card, container, false) as MaterialCardView
            val isSunrise = name == "Sunrise"
            card.findViewById<View>(R.id.accent_bar).setBackgroundColor(
                requireContext().resources.getColor(accent, requireContext().theme))
            card.findViewById<TextView>(R.id.tv_p_name).text = name
            card.findViewById<TextView>(R.id.tv_p_time).text =
                PrayTimes.formatTime(hours, twentyFour = false)

            val pill = card.findViewById<TextView>(R.id.tv_p_pill)
            val rem = card.findViewById<TextView>(R.id.tv_p_remaining)
            val check = card.findViewById<ImageView>(R.id.iv_p_check)
            when {
                i == nextIdx -> {
                    pill.visibility = View.VISIBLE
                    pill.text = "NEXT"
                    pill.setBackgroundResource(R.drawable.pill_now)
                    rem.text = humanRemaining(hours - now) + " from now"
                }
                hours < now -> {
                    pill.visibility = View.VISIBLE
                    pill.text = "PASSED"
                    pill.setBackgroundResource(R.drawable.pill_passed)
                    rem.text = humanRemaining(now - hours) + " ago"
                }
                else -> {
                    pill.visibility = View.GONE
                    rem.text = humanRemaining(hours - now) + " from now"
                }
            }
            // Mark prayed
            if (prayedToday.contains(name)) check.alpha = 1f
            if (isSunrise) {
                check.visibility = View.GONE
            } else {
                check.setOnClickListener {
                    if (prayedToday.add(name)) check.alpha = 1f
                    else { prayedToday.remove(name); check.alpha = 0.35f }
                }
            }
            container.addView(card)
        }
    }

    private fun updateCountdown(v: View, t: PrayTimes.Times) {
        val pairs = listOf(
            "Fajr" to t.fajr, "Dhuhr" to t.dhuhr, "Asr" to t.asr,
            "Maghrib" to t.maghrib, "Isha" to t.isha
        )
        val now = currentHours()
        val next = pairs.firstOrNull { it.second > now } ?: ("Fajr (tomorrow)" to (t.fajr + 24))
        val msUntil = ((next.second - now) * 3_600_000L).toLong()
        v.findViewById<TextView>(R.id.tv_next_label).text = "NEXT: ${next.first.uppercase()}"
        v.findViewById<TextView>(R.id.tv_next_time).text = PrayTimes.formatTime(next.second % 24, false)
        countdown?.cancel()
        countdown = object : CountDownTimer(msUntil, 1000) {
            override fun onTick(ms: Long) {
                val h = ms / 3_600_000
                val m = (ms / 60_000) % 60
                val s = (ms / 1000) % 60
                v.findViewById<TextView>(R.id.tv_countdown).text =
                    String.format("%02d:%02d:%02d", h, m, s)
            }
            override fun onFinish() {
                // Reload times when a prayer passes
                val app = requireContext().applicationContext as PrayerPilotApp
                viewLifecycleOwner.lifecycleScope.launch {
                    app.repository.computeTimes()?.let {
                        times = it; renderCards(v, it); updateCountdown(v, it)
                    }
                }
            }
        }.start()
    }

    private fun currentHours(): Double {
        val c = Calendar.getInstance()
        return c.get(Calendar.HOUR_OF_DAY) + c.get(Calendar.MINUTE)/60.0 +
            c.get(Calendar.SECOND)/3600.0
    }

    private fun humanRemaining(diffHours: Double): String {
        val totalMin = (diffHours * 60).toInt()
        val h = totalMin / 60
        val m = totalMin % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }

    override fun onDestroyView() { super.onDestroyView(); countdown?.cancel() }
}
