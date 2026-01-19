package com.ultrablock.domain.model

data class UnblockRequest(
    val packageName: String,
    val appName: String,
    val durationMinutes: Int,
    val costCents: Int
)
