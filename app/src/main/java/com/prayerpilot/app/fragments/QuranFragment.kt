package com.prayerpilot.app.fragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.prayerpilot.app.R
import com.prayerpilot.app.quran.QuranApi
import com.prayerpilot.app.ui.SurahReaderActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuranFragment : Fragment() {
    private var surahs: List<QuranApi.Surah> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        inflater.inflate(R.layout.fragment_quran, c, false)

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        val list = view.findViewById<RecyclerView>(R.id.list_surahs)
        val progress = view.findViewById<ProgressBar>(R.id.progress)
        val empty = view.findViewById<TextView>(R.id.tv_empty)

        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = SurahAdapter(emptyList()) { surah ->
            val intent = Intent(requireContext(), SurahReaderActivity::class.java)
                .putExtra("surah_id", surah.id)
                .putExtra("surah_name", "${surah.id}. ${surah.nameSimple}")
                .putExtra("surah_english", surah.englishName)
            startActivity(intent)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                progress.visibility = View.VISIBLE
                empty.visibility = View.GONE
                val data = withContext(Dispatchers.IO) { QuranApi.getAllSurahs() }
                surahs = data
                list.adapter = SurahAdapter(data) { surah ->
                    val intent = Intent(requireContext(), SurahReaderActivity::class.java)
                        .putExtra("surah_id", surah.id)
                        .putExtra("surah_name", "${surah.id}. ${surah.nameSimple}")
                        .putExtra("surah_english", surah.englishName)
                    startActivity(intent)
                }
                progress.visibility = View.GONE
            } catch (e: Exception) {
                progress.visibility = View.GONE
                empty.visibility = View.VISIBLE
                empty.text = getString(R.string.quran_offline_warning) + "\n${e.message ?: ""}"
            }
        }
    }
}

private class SurahAdapter(
    private val items: List<QuranApi.Surah>,
    private val onClick: (QuranApi.Surah) -> Unit
) : RecyclerView.Adapter<SurahAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val number: TextView = v.findViewById(R.id.tv_number)
        val name: TextView = v.findViewById(R.id.tv_name)
        val english: TextView = v.findViewById(R.id.tv_english)
        val arabic: TextView = v.findViewById(R.id.tv_arabic)
        val meta: TextView = v.findViewById(R.id.tv_meta)
    }

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
