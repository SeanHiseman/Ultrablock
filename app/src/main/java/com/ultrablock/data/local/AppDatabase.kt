package com.ultrablock.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ultrablock.data.local.dao.BlockedAppDao
import com.ultrablock.data.local.dao.ScheduleDao
import com.ultrablock.data.local.dao.UnblockHistoryDao
import com.ultrablock.data.local.entity.BlockedApp
import com.ultrablock.data.local.entity.Schedule
import com.ultrablock.data.local.entity.UnblockHistory

@Database(
    entities = [
        BlockedApp::class,
        Schedule::class,
        UnblockHistory::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun unblockHistoryDao(): UnblockHistoryDao
}
