package com.ultrablock.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ultrablock.data.local.entity.AccountabilityPartner
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountabilityPartnerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(partner: AccountabilityPartner)

    @Delete
    suspend fun delete(partner: AccountabilityPartner)

    @Query("SELECT * FROM accountability_partners ORDER BY addedAt DESC")
    fun getAllPartners(): Flow<List<AccountabilityPartner>>

    @Query("SELECT * FROM accountability_partners WHERE partnerCode = :code LIMIT 1")
    suspend fun getByCode(code: String): AccountabilityPartner?

    @Query("""
        UPDATE accountability_partners
        SET lastKnownBlockCount = :blockCount, lastKnownSuccessRate = :successRate, lastSyncAt = :syncAt
        WHERE partnerCode = :code
    """)
    suspend fun updatePartnerStats(code: String, blockCount: Int, successRate: Float, syncAt: Long)
}
