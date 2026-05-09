package com.ultrablock.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_usage_sessions")
data class AppUsageSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val blockedAtTimestamp: Long,
    val wasUnblocked: Boolean = false,
    val unblockDurationMinutes: Int = 0,
    val frictionLevel: String = "STRICT"
)
