package com.ultrablock.data.repository

import com.ultrablock.data.local.dao.AccountabilityPartnerDao
import com.ultrablock.data.local.entity.AccountabilityPartner
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialRepository @Inject constructor(
    private val partnerDao: AccountabilityPartnerDao
) {
    fun getAllPartners(): Flow<List<AccountabilityPartner>> = partnerDao.getAllPartners()

    suspend fun addPartner(code: String, displayName: String): Result<Unit> {
        if (code.isBlank()) return Result.failure(IllegalArgumentException("Code cannot be empty"))
        if (displayName.isBlank()) return Result.failure(IllegalArgumentException("Name cannot be empty"))
        val existing = partnerDao.getByCode(code)
        if (existing != null) return Result.failure(IllegalStateException("Partner already added"))
        partnerDao.insert(AccountabilityPartner(partnerCode = code.uppercase(), displayName = displayName.trim()))
        return Result.success(Unit)
    }

    suspend fun removePartner(partner: AccountabilityPartner) {
        partnerDao.delete(partner)
    }
}
