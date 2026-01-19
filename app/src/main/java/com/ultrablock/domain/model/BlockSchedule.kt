package com.ultrablock.domain.model

import java.time.DayOfWeek
import java.time.LocalTime

data class BlockSchedule(
    val id: Long = 0,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val activeDays: Set<DayOfWeek>,
    val isEnabled: Boolean = true
)
