package com.prayerpilot.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.prayerpilot.app.PrayerPilotApp
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
    private var fontSize = 22f
    private lateinit var verseAdapter: VerseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_surah_reader)
        surahId = intent.getIntExtra("surah_id", 1)
        val title = intent.getStringExtra("surah_name") ?: "Surah $surahId"

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = title
        toolbar.setNavigationOnClickListener { finish() }

        val list = findViewById<RecyclerView>(R.id.list_verses)
        val progress = findViewById<ProgressBar>(R.id.progress)
        val playBtn = findViewById<MaterialButton>(R.id.btn_play)
        val reciterBtn = findViewById<MaterialButton>(R.id.btn_reciter)
        val slider = findViewById<Slider>(R.id.slider_font)
        val fontLabel = findViewById<TextView>(R.id.tv_font_size_label)

        val app = applicationContext as PrayerPilotApp
        lifecycleScope.launch {
            fontSize = app.repository.getQuranFontSize().toFloat()
            slider.value = fontSize.coerceIn(16f, 36f)
            fontLabel.text = "${fontSize.toInt()}sp"
        }

        list.layoutManager = LinearLayoutManager(this)
        verseAdapter = VerseAdapter(emptyList(), { verse, action ->
            when (action) {
                "copy" -> {
                    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val text = "${verse.arabic}\n\n${verse.english}\n\n— Quran $surahId:${verse.number}"
                    cm.setPrimaryClip(ClipData.newPlainText("Verse", text))
                    Toast.makeText(this, "Copied verse $surahId:${verse.number}",
                        Toast.LENGTH_SHORT).show()
                }
                "share" -> {
                    val text = "${verse.arabic}\n\n${verse.english}\n\n— Quran $surahId:${verse.number}\nvia Qibla Salah Times"
                    startActivity(Intent.createChooser(
                        Intent(Intent.ACTION_SEND).setType("text/plain")
                            .putExtra(Intent.EXTRA_TEXT, text), "Share verse"))
                }
            }
        }, fontSize)
        list.adapter = verseAdapter

        slider.addOnChangeListener { _, value, _ ->
            fontSize = value
            fontLabel.text = "${value.toInt()}sp"
            verseAdapter.setFontSize(value)
            lifecycleScope.launch { app.repository.setQuranFontSize(value.toInt()) }
        }

        loadVerses()

        playBtn.setOnClickListener {
            if (player?.isPlaying == true) {
                player?.pause()
                playBtn.text = getString(R.string.quran_play)
                playBtn.setIconResource(R.drawable.ic_play_arrow)
            } else if (player != null) {
                player?.start()
                playBtn.text = getString(R.string.quran_pause)
                playBtn.setIconResource(R.drawable.ic_pause)
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
                        setOnPreparedListener {
                            start()
                            playBtn.text = getString(R.string.quran_pause)
                            playBtn.setIconResource(R.drawable.ic_pause)
                        }
                        setOnCompletionListener {
                            playBtn.text = getString(R.string.quran_play)
                            playBtn.setIconResource(R.drawable.ic_play_arrow)
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
                    player?.release(); player = null
                    playBtn.text = getString(R.string.quran_play)
                    playBtn.setIconResource(R.drawable.ic_play_arrow)
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
                verseAdapter.update(data)
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

private class VerseAdapter(
    private var verses: List<QuranApi.Verse>,
    private val onAction: (QuranApi.Verse, String) -> Unit,
    private var arabicSize: Float
) : RecyclerView.Adapter<VerseAdapter.VH>() {
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val number: TextView = v.findViewById(R.id.tv_v_number)
        val arabic: TextView = v.findViewById(R.id.tv_v_arabic)
        val english: TextView = v.findViewById(R.id.tv_v_english)
        val copy: ImageView = v.findViewById(R.id.btn_copy)
        val share: ImageView = v.findViewById(R.id.btn_share)
    }
    fun update(d: List<QuranApi.Verse>) { verses = d; notifyDataSetChanged() }
    fun setFontSize(sp: Float) { arabicSize = sp; notifyDataSetChanged() }
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(
        LayoutInflater.from(p.context).inflate(R.layout.item_verse, p, false))
    override fun onBindViewHolder(h: VH, i: Int) {
        val v = verses[i]
        h.number.text = v.number.toString()
        h.arabic.text = v.arabic
        h.arabic.textSize = arabicSize
        h.english.text = v.english
        h.copy.setOnClickListener { onAction(v, "copy") }
        h.share.setOnClickListener { onAction(v, "share") }
    }
    override fun getItemCount() = verses.size
}
