package com.prayerpilot.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerLogDao {
    @Query("SELECT * FROM prayer_log WHERE date = :date") fun forDate(date: String): Flow<List<PrayerLog>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(log: PrayerLog)
    @Query("SELECT COUNT(DISTINCT date) FROM prayer_log WHERE onTime = 1") suspend fun streakDays(): Int
}

@Dao
interface FastingDao {
    @Query("SELECT * FROM fasting_log ORDER BY date DESC") fun all(): Flow<List<FastingLog>>
    @Query("SELECT * FROM fasting_log WHERE date = :date") suspend fun forDate(date: String): FastingLog?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(log: FastingLog)
}

@Dao
interface DuaDao {
    @Query("SELECT * FROM dua_journal ORDER BY id DESC") fun all(): Flow<List<DuaEntry>>
    @Insert suspend fun insert(d: DuaEntry): Long
    @Update suspend fun update(d: DuaEntry)
    @Delete suspend fun delete(d: DuaEntry)
}

@Dao
interface DhikrDao {
    @Query("SELECT * FROM dhikr_count WHERE date = :date") fun forDate(date: String): Flow<List<DhikrEntry>>
    @Insert suspend fun insert(e: DhikrEntry): Long
    @Update suspend fun update(e: DhikrEntry)
}

@Dao
interface FamilyDao {
    @Query("SELECT * FROM family_member ORDER BY streakDays DESC") fun all(): Flow<List<FamilyMember>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(m: FamilyMember)
}
