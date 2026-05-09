package com.prayerpilot.app.fragments

import android.os.Bundle
import android.os.CountDownTimer
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.prayerpilot.app.PrayerPilotApp
import com.prayerpilot.app.R
import com.prayerpilot.app.data.FastingLog
import com.prayerpilot.app.prayer.PrayTimes
import com.prayerpilot.app.ramadan.RamadanCalendar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class RamadanFragment : Fragment() {
    private var timer: CountDownTimer? = null
    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        inflater.inflate(R.layout.fragment_ramadan, c, false)

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        val app = requireContext().applicationContext as PrayerPilotApp
        view.findViewById<TextView>(R.id.tv_ramadan_day).text = RamadanCalendar.dayLabel()

        viewLifecycleOwner.lifecycleScope.launch {
            val times = app.repository.computeTimes() ?: return@launch
            view.findViewById<TextView>(R.id.tv_suhoor_time).text = "Suhoor ends at ${PrayTimes.formatTime(times.fajr)}"
            view.findViewById<TextView>(R.id.tv_iftar_time).text = "Iftar at ${PrayTimes.formatTime(times.maghrib)}"
            startCountdown(view, times)

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val log = app.repository.db.fastingDao().forDate(today) ?: FastingLog(date = today, fasted = false)
            view.findViewById<CheckBox>(R.id.cb_fasted).isChecked = log.fasted
            view.findViewById<CheckBox>(R.id.cb_sadaqah).isChecked = log.sadaqahGiven
            view.findViewById<EditText>(R.id.et_taraweeh).setText(log.taraweehRakaat.toString())
            view.findViewById<EditText>(R.id.et_quran_pages).setText(log.quranPagesRead.toString())

            view.findViewById<Button>(R.id.btn_save_ramadan).setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    app.repository.db.fastingDao().upsert(FastingLog(
                        date = today,
                        fasted = view.findViewById<CheckBox>(R.id.cb_fasted).isChecked,
                        sadaqahGiven = view.findViewById<CheckBox>(R.id.cb_sadaqah).isChecked,
                        taraweehRakaat = view.findViewById<EditText>(R.id.et_taraweeh).text.toString().toIntOrNull() ?: 0,
                        quranPagesRead = view.findViewById<EditText>(R.id.et_quran_pages).text.toString().toIntOrNull() ?: 0,
                        notes = ""
                    ))
                    Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startCountdown(v: View, t: PrayTimes.Times) {
        val now = Calendar.getInstance()
        val nowH = now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE)/60.0
        val target = if (nowH < t.maghrib) t.maghrib else t.fajr + 24
        val ms = ((target - nowH) * 3_600_000L).toLong()
        val label = if (nowH < t.maghrib) "Until Iftar" else "Until Suhoor ends"
        v.findViewById<TextView>(R.id.tv_countdown_label).text = label
        timer?.cancel()
        timer = object : CountDownTimer(ms, 1000) {
            override fun onTick(rem: Long) {
                val h = rem/3_600_000; val m = (rem/60_000)%60; val s = (rem/1000)%60
                v.findViewById<TextView>(R.id.tv_big_countdown).text = String.format("%02d:%02d:%02d", h, m, s)
            }
            override fun onFinish() {}
        }.start()
    }

    override fun onDestroyView() { super.onDestroyView(); timer?.cancel() }
}
