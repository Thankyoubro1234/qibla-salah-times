package com.prayerpilot.app.ui

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.prayerpilot.app.R
import com.prayerpilot.app.quran.QuranApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SurahReaderActivity : AppCompatActivity() {
    private var surahId = 1
    private var reciterId = 7
    private var verses: List<QuranApi.Verse> = emptyList()
    private var player: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_surah_reader)
        surahId = intent.getIntExtra("surah_id", 1)
        title = intent.getStringExtra("surah_name") ?: "Surah"

        val list = findViewById<RecyclerView>(R.id.list_verses)
        val progress = findViewById<ProgressBar>(R.id.progress)
        val playBtn = findViewById<Button>(R.id.btn_play)
        val reciterBtn = findViewById<Button>(R.id.btn_reciter)

        list.layoutManager = LinearLayoutManager(this)
        list.adapter = VerseAdapter(emptyList())

        loadVerses()

        playBtn.setOnClickListener {
            if (player?.isPlaying == true) {
                player?.pause()
                playBtn.text = getString(R.string.quran_play)
            } else if (player != null) {
                player?.start()
                playBtn.text = getString(R.string.quran_pause)
            } else {
                lifecycleScope.launch {
                    playBtn.isEnabled = false
                    progress.visibility = View.VISIBLE
                    val url = withContext(Dispatchers.IO) {
                        runCatching { QuranApi.chapterAudioUrl(surahId, reciterId) }.getOrNull()
                    }
                    progress.visibility = View.GONE
                    playBtn.isEnabled = true
                    if (url.isNullOrBlank()) {
                        Toast.makeText(this@SurahReaderActivity,
                            "Audio unavailable", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    player = MediaPlayer().apply {
                        setAudioAttributes(AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                        setOnPreparedListener { start(); playBtn.text = getString(R.string.quran_pause) }
                        setOnCompletionListener {
                            playBtn.text = getString(R.string.quran_play)
                            release(); player = null
                        }
                        setOnErrorListener { _, _, _ ->
                            Toast.makeText(this@SurahReaderActivity,
                                "Playback failed", Toast.LENGTH_SHORT).show()
                            release(); player = null; true
                        }
                        setDataSource(url)
                        prepareAsync()
                    }
                }
            }
        }

        reciterBtn.setOnClickListener {
            val items = QuranApi.RECITERS.map { it.name }.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle(R.string.quran_pick_reciter)
                .setSingleChoiceItems(items,
                    QuranApi.RECITERS.indexOfFirst { it.id == reciterId }) { dialog, which ->
                    reciterId = QuranApi.RECITERS[which].id
                    reciterBtn.text = QuranApi.RECITERS[which].name
                    // Reset player so next play uses the new reciter
                    player?.release(); player = null
                    findViewById<Button>(R.id.btn_play).text = getString(R.string.quran_play)
                    dialog.dismiss()
                }
                .show()
        }
        reciterBtn.text = QuranApi.RECITERS.first { it.id == reciterId }.name
    }

    private fun loadVerses() {
        val list = findViewById<RecyclerView>(R.id.list_verses)
        val progress = findViewById<ProgressBar>(R.id.progress)
        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val data = withContext(Dispatchers.IO) { QuranApi.getVerses(surahId) }
                verses = data
                list.adapter = VerseAdapter(data)
            } catch (e: Exception) {
                Toast.makeText(this@SurahReaderActivity,
                    "Failed to load: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progress.visibility = View.GONE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release(); player = null
    }
}

private class VerseAdapter(private val verses: List<QuranApi.Verse>) :
    RecyclerView.Adapter<VerseAdapter.VH>() {
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val number: TextView = v.findViewById(R.id.tv_v_number)
        val arabic: TextView = v.findViewById(R.id.tv_v_arabic)
        val english: TextView = v.findViewById(R.id.tv_v_english)
    }
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(
        LayoutInflater.from(p.context).inflate(R.layout.item_verse, p, false))
    override fun onBindViewHolder(h: VH, i: Int) {
        val v = verses[i]
        h.number.text = v.number.toString()
        h.arabic.text = v.arabic
        h.english.text = v.english
    }
    override fun getItemCount() = verses.size
}
