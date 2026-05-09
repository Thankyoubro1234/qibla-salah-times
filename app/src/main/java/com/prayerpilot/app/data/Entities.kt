package com.prayerpilot.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prayer_log")
data class PrayerLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, val name: String,
    val onTime: Boolean, val congregation: Boolean, val timestamp: Long
)

@Entity(tableName = "fasting_log")
data class FastingLog(
    @PrimaryKey val date: String,
    val fasted: Boolean,
    val taraweehRakaat: Int = 0,
    val quranPagesRead: Int = 0,
    val sadaqahGiven: Boolean = false,
    val notes: String = ""
)

@Entity(tableName = "dua_journal")
data class DuaEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, val title: String, val text: String,
    val answered: Boolean = false, val gratitude: String = ""
)

@Entity(tableName = "dhikr_count")
data class DhikrEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, val phrase: String, val count: Int
)

@Entity(tableName = "family_member")
data class FamilyMember(
    @PrimaryKey val userId: String, val displayName: String,
    val streakDays: Int = 0, val lastSeen: Long = 0
)
