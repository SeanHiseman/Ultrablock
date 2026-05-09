package com.ultrablock.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ultrablock.data.local.entity.AppUsageSession
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUsageDao {
    @Insert
    suspend fun insert(session: AppUsageSession): Long

    @Query("UPDATE app_usage_sessions SET wasUnblocked = 1, unblockDurationMinutes = :minutes WHERE id = :id")
    suspend fun markUnblocked(id: Long, minutes: Int)

    @Query("SELECT COUNT(*) FROM app_usage_sessions WHERE blockedAtTimestamp >= :since")
    fun getBlockAttemptsSince(since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM app_usage_sessions WHERE wasUnblocked = 0 AND blockedAtTimestamp >= :since")
    fun getSuccessfulBlocksSince(since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM app_usage_sessions WHERE wasUnblocked = 1 AND blockedAtTimestamp >= :since")
    fun getUnblocksSince(since: Long): Flow<Int>

    @Query("SELECT * FROM app_usage_sessions WHERE blockedAtTimestamp >= :since ORDER BY blockedAtTimestamp DESC")
    fun getSessionsSince(since: Long): Flow<List<AppUsageSession>>

    @Query("SELECT SUM(unblockDurationMinutes) FROM app_usage_sessions WHERE wasUnblocked = 1 AND blockedAtTimestamp >= :since")
    fun getTotalUnblockedMinutesSince(since: Long): Flow<Int?>
}
