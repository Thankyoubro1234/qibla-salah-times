package com.prayerpilot.app.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.prayerpilot.app.PrayerPilotApp
import com.prayerpilot.app.R
import com.prayerpilot.app.quran.QuranApi
import com.prayerpilot.app.ui.SurahReaderActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuranFragment : Fragment() {
    private var allSurahs: List<QuranApi.Surah> = emptyList()
    private lateinit var adapter: SurahAdapter

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        inflater.inflate(R.layout.fragment_quran, c, false)

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        val list = view.findViewById<RecyclerView>(R.id.list_surahs)
        val progress = view.findViewById<ProgressBar>(R.id.progress)
        val empty = view.findViewById<TextView>(R.id.tv_empty)
        val search = view.findViewById<EditText>(R.id.et_search)
        val lastReadCard = view.findViewById<MaterialCardView>(R.id.card_last_read)
        val tvLastRead = view.findViewById<TextView>(R.id.tv_last_read)

        list.layoutManager = LinearLayoutManager(requireContext())
        adapter = SurahAdapter(emptyList()) { surah -> openReader(surah) }
        list.adapter = adapter

        // Last read banner
        val app = requireContext().applicationContext as PrayerPilotApp
        viewLifecycleOwner.lifecycleScope.launch {
            val last = app.repository.getLastReadSurah()
            if (last != null) {
                lastReadCard.visibility = View.VISIBLE
                tvLastRead.text = last.second
                lastReadCard.setOnClickListener {
                    val (id, _) = last
                    startActivity(Intent(requireContext(), SurahReaderActivity::class.java)
                        .putExtra("surah_id", id))
                }
            }
        }

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {
                val q = s?.toString()?.trim()?.lowercase().orEmpty()
                val filtered = if (q.isEmpty()) allSurahs
                    else allSurahs.filter {
                        it.nameSimple.lowercase().contains(q) ||
                        it.englishName.lowercase().contains(q) ||
                        it.id.toString() == q
                    }
                adapter.update(filtered)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                progress.visibility = View.VISIBLE
                empty.visibility = View.GONE
                val data = withContext(Dispatchers.IO) { QuranApi.getAllSurahs() }
                allSurahs = data
                adapter.update(data)
                progress.visibility = View.GONE
            } catch (e: Exception) {
                progress.visibility = View.GONE
                empty.visibility = View.VISIBLE
                empty.text = getString(R.string.quran_offline_warning) + "\n${e.message ?: ""}"
            }
        }
    }

    private fun openReader(surah: QuranApi.Surah) {
        val app = requireContext().applicationContext as PrayerPilotApp
        viewLifecycleOwner.lifecycleScope.launch {
            app.repository.setLastReadSurah(surah.id, "${surah.id}. ${surah.nameSimple}")
        }
        startActivity(Intent(requireContext(), SurahReaderActivity::class.java)
            .putExtra("surah_id", surah.id)
            .putExtra("surah_name", "${surah.id}. ${surah.nameSimple}")
            .putExtra("surah_english", surah.englishName))
    }
}

private class SurahAdapter(
    private var items: List<QuranApi.Surah>,
    private val onClick: (QuranApi.Surah) -> Unit
) : RecyclerView.Adapter<SurahAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val number: TextView = v.findViewById(R.id.tv_number)
        val name: TextView = v.findViewById(R.id.tv_name)
        val english: TextView = v.findViewById(R.id.tv_english)
        val arabic: TextView = v.findViewById(R.id.tv_arabic)
        val meta: TextView = v.findViewById(R.id.tv_meta)
    }

    fun update(newItems: List<QuranApi.Surah>) { items = newItems; notifyDataSetChanged() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_surah, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val s = items[pos]
        h.number.text = s.id.toString()
        h.name.text = s.nameSimple
        h.english.text = s.englishName
        h.arabic.text = s.nameArabic
        h.meta.text = "${s.versesCount} verses · ${s.revelationPlace.replaceFirstChar { it.titlecase() }}"
        h.itemView.setOnClickListener { onClick(s) }
    }

    override fun getItemCount() = items.size
}
