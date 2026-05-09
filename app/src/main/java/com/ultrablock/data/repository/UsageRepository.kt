package com.ultrablock.data.repository

import com.ultrablock.data.local.dao.AppUsageDao
import com.ultrablock.data.local.entity.AppUsageSession
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageRepository @Inject constructor(
    private val appUsageDao: AppUsageDao
) {
    suspend fun recordBlockAttempt(packageName: String, appName: String, frictionLevel: String): Long {
        return appUsageDao.insert(
            AppUsageSession(
                packageName = packageName,
                appName = appName,
                blockedAtTimestamp = System.currentTimeMillis(),
                frictionLevel = frictionLevel
            )
        )
    }

    suspend fun recordUnblock(sessionId: Long, durationMinutes: Int) {
        appUsageDao.markUnblocked(sessionId, durationMinutes)
    }

    fun getTodayBlockAttempts(): Flow<Int> = appUsageDao.getBlockAttemptsSince(startOfToday())
    fun getTodaySuccessfulBlocks(): Flow<Int> = appUsageDao.getSuccessfulBlocksSince(startOfToday())
    fun getTodayUnblocks(): Flow<Int> = appUsageDao.getUnblocksSince(startOfToday())

    fun getWeekBlockAttempts(): Flow<Int> = appUsageDao.getBlockAttemptsSince(startOfToday() - 6 * DAY_MS)
    fun getWeekSuccessfulBlocks(): Flow<Int> = appUsageDao.getSuccessfulBlocksSince(startOfToday() - 6 * DAY_MS)

    fun getWeekSessions(): Flow<List<AppUsageSession>> = appUsageDao.getSessionsSince(startOfToday() - 6 * DAY_MS)

    private fun startOfToday(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    companion object {
        private const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
