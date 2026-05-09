package com.prayerpilot.app.fragments

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.prayerpilot.app.PrayerPilotApp
import com.prayerpilot.app.R
import com.prayerpilot.app.data.DhikrEntry
import com.prayerpilot.app.data.DuaEntry
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CommunityFragment : Fragment() {
    private var dhikrCount = 0

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        inflater.inflate(R.layout.fragment_community, c, false)

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        val app = requireContext().applicationContext as PrayerPilotApp
        val tvCount = view.findViewById<TextView>(R.id.tv_dhikr_count)

        view.findViewById<Button>(R.id.btn_dhikr_tap).setOnClickListener {
            dhikrCount++; tvCount.text = dhikrCount.toString()
            requireContext().getSystemService(android.os.Vibrator::class.java)?.let {
                if (it.hasVibrator()) it.vibrate(android.os.VibrationEffect.createOneShot(20, 90))
            }
        }
        view.findViewById<Button>(R.id.btn_dhikr_reset).setOnClickListener {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val phrase = (view.findViewById<Spinner>(R.id.spinner_phrase).selectedItem as String?) ?: "SubhanAllah"
            viewLifecycleOwner.lifecycleScope.launch {
                if (dhikrCount > 0) app.repository.db.dhikrDao().insert(DhikrEntry(date = today, phrase = phrase, count = dhikrCount))
                dhikrCount = 0; tvCount.text = "0"
            }
        }
        view.findViewById<Spinner>(R.id.spinner_phrase).adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item,
            listOf("SubhanAllah", "Alhamdulillah", "Allahu Akbar", "La ilaha illa Allah", "Astaghfirullah")
        )

        view.findViewById<Button>(R.id.btn_save_dua).setOnClickListener {
            val title = view.findViewById<EditText>(R.id.et_dua_title).text.toString()
            val text = view.findViewById<EditText>(R.id.et_dua_text).text.toString()
            if (title.isBlank() || text.isBlank()) {
                Toast.makeText(requireContext(), "Add a title and text", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            viewLifecycleOwner.lifecycleScope.launch {
                app.repository.db.duaDao().insert(DuaEntry(date = today, title = title, text = text))
                view.findViewById<EditText>(R.id.et_dua_title).setText("")
                view.findViewById<EditText>(R.id.et_dua_text).setText("")
                Toast.makeText(requireContext(), "Dua saved ✨", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
