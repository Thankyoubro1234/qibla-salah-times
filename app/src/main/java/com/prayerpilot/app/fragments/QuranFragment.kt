package com.prayerpilot.app.fragments

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.prayerpilot.app.R

class QuranFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        inflater.inflate(R.layout.fragment_quran, c, false)

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        val list = listOf(
            "Al-Fatihah" to "The Opening", "Al-Baqarah" to "The Cow",
            "Aal Imran" to "The Family of Imran", "An-Nisa" to "The Women",
            "Al-Maidah" to "The Table Spread", "Al-Anam" to "The Cattle",
            "Al-Araf" to "The Heights", "Al-Anfal" to "The Spoils of War",
            "At-Tawbah" to "The Repentance", "Yunus" to "Jonah"
        )
        val lv = view.findViewById<ListView>(R.id.list_surahs)
        lv.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_2, android.R.id.text1,
            list.mapIndexed { i, p -> "${i+1}. ${p.first}" })
    }
}
