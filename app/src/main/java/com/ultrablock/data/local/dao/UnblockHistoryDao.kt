package com.ultrablock.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ultrablock.data.local.entity.UnblockHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface UnblockHistoryDao {
    @Query("SELECT * FROM unblock_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<UnblockHistory>>

    @Query("SELECT * FROM unblock_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHistory(limit: Int): Flow<List<UnblockHistory>>

    @Query("SELECT SUM(costCents) FROM unblock_history")
    fun getTotalSpentCents(): Flow<Int?>

    @Query("SELECT SUM(durationMinutes) FROM unblock_history")
    fun getTotalUnblockedMinutes(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM unblock_history")
    fun getUnblockCount(): Flow<Int>

    @Insert
    suspend fun insert(history: UnblockHistory): Long
}
