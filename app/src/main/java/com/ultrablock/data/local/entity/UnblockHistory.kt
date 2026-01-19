package com.ultrablock.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unblock_history")
data class UnblockHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val timestamp: Long,
    val durationMinutes: Int,
    val costCents: Int, // Store in cents to avoid floating point issues
    val stripePaymentIntentId: String?
)
