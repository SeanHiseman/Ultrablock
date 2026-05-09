package com.ultrablock.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accountability_partners")
data class AccountabilityPartner(
    @PrimaryKey
    val partnerCode: String,
    val displayName: String,
    val addedAt: Long = System.currentTimeMillis(),
    val lastKnownBlockCount: Int = 0,
    val lastKnownSuccessRate: Float = 0f,
    val lastSyncAt: Long = 0
)
