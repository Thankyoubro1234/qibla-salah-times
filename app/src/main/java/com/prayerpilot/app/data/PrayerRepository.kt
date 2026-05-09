package com.prayerpilot.app.data

import android.content.Context
import android.location.Location
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.prayerpilot.app.prayer.PrayTimes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar

private val Context.dataStore by preferencesDataStore("prayerpilot_prefs")

class PrayerRepository(private val ctx: Context) {
    val db = AppDatabase.get(ctx)
    private val K_LAT = doublePreferencesKey("lat")
    private val K_LNG = doublePreferencesKey("lng")
    private val K_METHOD = stringPreferencesKey("method")
    private val K_ASR = stringPreferencesKey("asr")
    private val K_HIGHLAT = stringPreferencesKey("highlat")
    private val K_ELEV = doublePreferencesKey("elev")
    private val K_OFFSETS = stringPreferencesKey("offsets")
    private val K_ADHAN_VOICE = stringPreferencesKey("adhan_voice")
    private val K_AUTO_DND = booleanPreferencesKey("auto_dnd")
    private val K_PRE_REMIND = intPreferencesKey("pre_remind_min")
    private val K_NOTIF_ENABLED = booleanPreferencesKey("notif_enabled")
    private val K_THEME = stringPreferencesKey("theme")
    private val K_FIRST_RUN = booleanPreferencesKey("first_run")

    suspend fun setLocation(loc: Location) {
        ctx.dataStore.edit { it[K_LAT] = loc.latitude; it[K_LNG] = loc.longitude; it[K_ELEV] = loc.altitude }
    }
    suspend fun getLocation(): Pair<Double, Double>? {
        val p = ctx.dataStore.data.first()
        val lat = p[K_LAT]; val lng = p[K_LNG]
        return if (lat != null && lng != null) lat to lng else null
    }
    val themeFlow: Flow<String> = ctx.dataStore.data.map { it[K_THEME] ?: "system" }

    suspend fun isFirstRun(): Boolean = ctx.dataStore.data.first()[K_FIRST_RUN] != false
    suspend fun completeFirstRun() = ctx.dataStore.edit { it[K_FIRST_RUN] = false }

    suspend fun computeTimes(date: Calendar = Calendar.getInstance()): PrayTimes.Times? {
        val (lat, lng) = getLocation() ?: return null
        val p = ctx.dataStore.data.first()
        val method = PrayTimes.Method.valueOf(p[K_METHOD] ?: "ISNA")
        val pt = PrayTimes(method).apply {
            asrJuristic = PrayTimes.AsrJuristic.valueOf(p[K_ASR] ?: "STANDARD")
            highLatRule = PrayTimes.HighLatRule.valueOf(p[K_HIGHLAT] ?: "ANGLE_BASED")
            elevation = p[K_ELEV] ?: 0.0
            (p[K_OFFSETS] ?: "0,0,0,0,0,0").split(",").mapIndexed { i, s ->
                manualOffsetsMinutes[i] = s.trim().toIntOrNull() ?: 0
            }
        }
        return pt.getTimes(date, lat, lng)
    }

    suspend fun setMethod(m: String) = ctx.dataStore.edit { it[K_METHOD] = m }
    suspend fun setAsr(a: String) = ctx.dataStore.edit { it[K_ASR] = a }
    suspend fun setHighLat(h: String) = ctx.dataStore.edit { it[K_HIGHLAT] = h }
    suspend fun setOffsets(s: String) = ctx.dataStore.edit { it[K_OFFSETS] = s }
    suspend fun setAdhanVoice(s: String) = ctx.dataStore.edit { it[K_ADHAN_VOICE] = s }
    suspend fun setAutoDnd(b: Boolean) = ctx.dataStore.edit { it[K_AUTO_DND] = b }
    suspend fun setPreRemind(min: Int) = ctx.dataStore.edit { it[K_PRE_REMIND] = min }
    suspend fun setNotificationsEnabled(b: Boolean) = ctx.dataStore.edit { it[K_NOTIF_ENABLED] = b }
    suspend fun setTheme(t: String) = ctx.dataStore.edit { it[K_THEME] = t }

    suspend fun getAdhanVoice(): String = ctx.dataStore.data.first()[K_ADHAN_VOICE] ?: "makkah"
    suspend fun getAutoDnd(): Boolean = ctx.dataStore.data.first()[K_AUTO_DND] ?: false
    suspend fun getPreRemind(): Int = ctx.dataStore.data.first()[K_PRE_REMIND] ?: 0
    suspend fun isNotifEnabled(): Boolean = ctx.dataStore.data.first()[K_NOTIF_ENABLED] != false
}
