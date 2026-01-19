package com.ultrablock.data.repository

import com.ultrablock.data.local.dao.UnblockHistoryDao
import com.ultrablock.data.local.entity.UnblockHistory
import com.ultrablock.data.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepository @Inject constructor(
    private val unblockHistoryDao: UnblockHistoryDao,
    private val userPreferences: UserPreferences
) {
    fun getTotalSpent(): Flow<Double> = unblockHistoryDao.getTotalSpentCents().map { cents ->
        (cents ?: 0) / 100.0
    }

    fun getTotalUnblockedMinutes(): Flow<Int> = unblockHistoryDao.getTotalUnblockedMinutes().map { it ?: 0 }

    fun getUnblockCount(): Flow<Int> = unblockHistoryDao.getUnblockCount()

    fun getRecentHistory(limit: Int = 10): Flow<List<UnblockHistory>> =
        unblockHistoryDao.getRecentHistory(limit)

    suspend fun recordUnblock(
        packageName: String,
        appName: String,
        durationMinutes: Int,
        costCents: Int,
        paymentIntentId: String?
    ) {
        unblockHistoryDao.insert(
            UnblockHistory(
                packageName = packageName,
                appName = appName,
                timestamp = System.currentTimeMillis(),
                durationMinutes = durationMinutes,
                costCents = costCents,
                stripePaymentIntentId = paymentIntentId
            )
        )
    }

    fun calculateCostCents(durationMinutes: Int): Flow<Int> = userPreferences.hourlyRateCents.map { hourlyRateCents ->
        (hourlyRateCents.toDouble() / 60 * durationMinutes).toInt()
    }

    suspend fun calculateCostCentsSync(durationMinutes: Int): Int {
        val hourlyRateCents = userPreferences.hourlyRateCents.first()
        return (hourlyRateCents.toDouble() / 60 * durationMinutes).toInt()
    }

    suspend fun hasPaymentMethod(): Boolean {
        return userPreferences.stripePaymentMethodId.first() != null
    }
}
