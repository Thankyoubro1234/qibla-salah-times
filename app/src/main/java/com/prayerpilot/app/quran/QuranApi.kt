package com.prayerpilot.app.quran

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Lightweight client for the public Quran.com API v4.
 * https://api.quran.com/api/v4/
 *
 * No retrofit/gson dependency — uses org.json for tiny APK size.
 */
object QuranApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private const val BASE = "https://api.quran.com/api/v4"

    data class Surah(
        val id: Int,
        val nameArabic: String,
        val nameSimple: String,
        val englishName: String,
        val versesCount: Int,
        val revelationPlace: String
    )

    data class Verse(
        val number: Int,
        val arabic: String,
        val english: String,
        val audioUrl: String?
    )

    data class Reciter(val id: Int, val name: String)

    val RECITERS = listOf(
        Reciter(7, "Mishary Rashid Alafasy"),
        Reciter(1, "AbdulBaset AbdulSamad (Mujawwad)"),
        Reciter(2, "AbdulBaset AbdulSamad (Murattal)"),
        Reciter(3, "Abdur Rahman as Sudais"),
        Reciter(4, "Abu Bakr al Shatri"),
        Reciter(5, "Hani ar Rifai"),
        Reciter(6, "Mahmoud Khalil al Husari"),
        Reciter(9, "Mohamed Siddiq al Minshawi"),
        Reciter(10, "Sa'ud ash Shuraym")
    )

    /** Returns all 114 surahs with metadata. */
    fun getAllSurahs(): List<Surah> {
        val req = Request.Builder().url("$BASE/chapters?language=en").build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw RuntimeException("Surahs fetch failed: ${resp.code}")
            val arr = JSONObject(resp.body!!.string()).getJSONArray("chapters")
            return (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Surah(
                    id = o.getInt("id"),
                    nameArabic = o.getString("name_arabic"),
                    nameSimple = o.getString("name_simple"),
                    englishName = o.optJSONObject("translated_name")?.optString("name") ?: "",
                    versesCount = o.getInt("verses_count"),
                    revelationPlace = o.getString("revelation_place")
                )
            }
        }
    }

    /** Returns all verses of a chapter with Arabic, Mustafa Khattab translation, and audio URL. */
    fun getVerses(chapterId: Int, reciterId: Int = 7): List<Verse> {
        // translation 131 = Mustafa Khattab The Clear Quran
        val url = "$BASE/verses/by_chapter/$chapterId" +
            "?language=en&translations=131&audio=$reciterId&fields=text_uthmani&per_page=300"
        val req = Request.Builder().url(url).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw RuntimeException("Verses fetch failed: ${resp.code}")
            val root = JSONObject(resp.body!!.string())
            val verses = root.getJSONArray("verses")
            val out = mutableListOf<Verse>()
            for (i in 0 until verses.length()) {
                val v = verses.getJSONObject(i)
                val arabic = v.optString("text_uthmani", "")
                val translations = v.optJSONArray("translations")
                val english = if (translations != null && translations.length() > 0)
                    translations.getJSONObject(0).optString("text", "")
                        .replace(Regex("<[^>]+>"), "") else ""
                val audio = v.optJSONObject("audio")?.optString("url")
                val audioUrl = if (audio != null && audio.isNotBlank())
                    "https://audio.qurancdn.com/$audio" else null
                val keyParts = v.getString("verse_key").split(":")
                val number = keyParts.getOrNull(1)?.toIntOrNull() ?: (i + 1)
                out.add(Verse(number, arabic, english, audioUrl))
            }
            return out
        }
    }

    /** Returns the URL of a continuous full-chapter audio stream for a reciter. */
    fun chapterAudioUrl(chapterId: Int, reciterId: Int = 7): String {
        // Quran.com hosts full-chapter recitations per reciter
        val url = "$BASE/chapter_recitations/$reciterId/$chapterId"
        val req = Request.Builder().url(url).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw RuntimeException("Audio fetch failed: ${resp.code}")
            val obj = JSONObject(resp.body!!.string()).optJSONObject("audio_file")
            return obj?.optString("audio_url") ?: ""
        }
    }
}
