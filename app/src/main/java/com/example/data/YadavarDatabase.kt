package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AlarmEntity::class, BirthdayEntity::class, FinancialEntity::class],
    version = 6,
    exportSchema = false
)
abstract class YadavarDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun birthdayDao(): BirthdayDao
    abstract fun financialDao(): FinancialDao

    companion object {
        @Volatile
        private var INSTANCE: YadavarDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN hasAlarm INTEGER NOT NULL DEFAULT 1")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS financial_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        amount INTEGER NOT NULL,
                        jalaliYear INTEGER NOT NULL,
                        jalaliMonth INTEGER NOT NULL,
                        jalaliDay INTEGER NOT NULL,
                        isMonthly INTEGER NOT NULL DEFAULT 0,
                        endJalaliYear INTEGER,
                        endJalaliMonth INTEGER,
                        endJalaliDay INTEGER,
                        notes TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN hasDate INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE financial_items ADD COLUMN paidDates TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Keep every existing reminder visible in the matrix after update.
                // Newly-created reminders can opt in via the UI.
                db.execSQL("ALTER TABLE alarms ADD COLUMN showInMatrix INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Preserve the existing matrix choice: reminders previously hidden from
                // the matrix become "no priority"; all others keep their current priority.
                db.execSQL("UPDATE alarms SET prio = 'none' WHERE showInMatrix = 0 AND tag != 'تولد'")
                db.execSQL("UPDATE alarms SET showInMatrix = 0 WHERE prio = 'none' AND tag != 'تولد'")
                db.execSQL("UPDATE alarms SET showInMatrix = 1 WHERE prio != 'none' AND tag != 'تولد'")
            }
        }

        fun getInstance(context: Context): YadavarDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    YadavarDatabase::class.java,
                    "yadavar_alarms.db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
