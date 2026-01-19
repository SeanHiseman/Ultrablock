package com.ultrablock.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ultrablock.data.local.entity.BlockedApp
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedAppDao {
    @Query("SELECT * FROM blocked_apps WHERE isBlocked = 1")
    fun getAllBlockedApps(): Flow<List<BlockedApp>>

    @Query("SELECT * FROM blocked_apps")
    fun getAllApps(): Flow<List<BlockedApp>>

    @Query("SELECT * FROM blocked_apps WHERE packageName = :packageName")
    suspend fun getByPackageName(packageName: String): BlockedApp?

    @Query("SELECT COUNT(*) FROM blocked_apps WHERE isBlocked = 1")
    fun getBlockedAppCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: BlockedApp)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<BlockedApp>)

    @Update
    suspend fun update(app: BlockedApp)

    @Delete
    suspend fun delete(app: BlockedApp)

    @Query("UPDATE blocked_apps SET temporarilyUnblockedUntil = :until WHERE packageName = :packageName")
    suspend fun setTemporaryUnblock(packageName: String, until: Long?)

    @Query("UPDATE blocked_apps SET temporarilyUnblockedUntil = NULL WHERE temporarilyUnblockedUntil < :currentTime")
    suspend fun clearExpiredUnblocks(currentTime: Long)

    @Query("SELECT * FROM blocked_apps WHERE packageName = :packageName AND isBlocked = 1 AND (temporarilyUnblockedUntil IS NULL OR temporarilyUnblockedUntil < :currentTime)")
    suspend fun isAppCurrentlyBlocked(packageName: String, currentTime: Long): BlockedApp?
}
