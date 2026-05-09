package com.ultrablock.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ultrablock.data.local.dao.AccountabilityPartnerDao
import com.ultrablock.data.local.dao.AppUsageDao
import com.ultrablock.data.local.dao.BlockedAppDao
import com.ultrablock.data.local.dao.ScheduleDao
import com.ultrablock.data.local.dao.UnblockHistoryDao
import com.ultrablock.data.local.entity.AccountabilityPartner
import com.ultrablock.data.local.entity.AppUsageSession
import com.ultrablock.data.local.entity.BlockedApp
import com.ultrablock.data.local.entity.Schedule
import com.ultrablock.data.local.entity.UnblockHistory

@Database(
    entities = [
        BlockedApp::class,
        Schedule::class,
        UnblockHistory::class,
        AppUsageSession::class,
        AccountabilityPartner::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun unblockHistoryDao(): UnblockHistoryDao
    abstract fun appUsageDao(): AppUsageDao
    abstract fun accountabilityPartnerDao(): AccountabilityPartnerDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE blocked_apps ADD COLUMN frictionLevel TEXT")
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `app_usage_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `packageName` TEXT NOT NULL,
                        `appName` TEXT NOT NULL,
                        `blockedAtTimestamp` INTEGER NOT NULL,
                        `wasUnblocked` INTEGER NOT NULL DEFAULT 0,
                        `unblockDurationMinutes` INTEGER NOT NULL DEFAULT 0,
                        `frictionLevel` TEXT NOT NULL DEFAULT 'STRICT'
                    )
                """)
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `accountability_partners` (
                        `partnerCode` TEXT PRIMARY KEY NOT NULL,
                        `displayName` TEXT NOT NULL,
                        `addedAt` INTEGER NOT NULL,
                        `lastKnownBlockCount` INTEGER NOT NULL DEFAULT 0,
                        `lastKnownSuccessRate` REAL NOT NULL DEFAULT 0.0,
                        `lastSyncAt` INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }
    }
}
