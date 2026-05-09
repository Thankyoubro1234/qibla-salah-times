package com.prayerpilot.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PrayerLog::class, FastingLog::class, DuaEntry::class, DhikrEntry::class, FamilyMember::class],
    version = 1, exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun prayerLogDao(): PrayerLogDao
    abstract fun fastingDao(): FastingDao
    abstract fun duaDao(): DuaDao
    abstract fun dhikrDao(): DhikrDao
    abstract fun familyDao(): FamilyDao

    companion object {
        @Volatile private var inst: AppDatabase? = null
        fun get(ctx: Context): AppDatabase = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(
                ctx.applicationContext, AppDatabase::class.java, "prayerpilot.db"
            ).fallbackToDestructiveMigration().build().also { inst = it }
        }
    }
}
