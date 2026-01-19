package com.ultrablock.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class Schedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTimeMinutes: Int, // Minutes from midnight (0-1439)
    val endTimeMinutes: Int,   // Minutes from midnight (0-1439)
    val monday: Boolean = true,
    val tuesday: Boolean = true,
    val wednesday: Boolean = true,
    val thursday: Boolean = true,
    val friday: Boolean = true,
    val saturday: Boolean = false,
    val sunday: Boolean = false,
    val isEnabled: Boolean = true
)
