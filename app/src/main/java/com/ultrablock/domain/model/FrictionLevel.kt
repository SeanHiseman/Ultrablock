package com.ultrablock.domain.model

enum class FrictionLevel(val displayName: String, val description: String) {
    GENTLE("Gentle", "5-second pause with a motivational reminder — free to continue"),
    MODERATE("Moderate", "30-second reflection with a written reason — free to continue"),
    STRICT("Strict", "Payment required to unblock"),
    EXTREME("Extreme", "Locked — no unblocking until you change settings")
}
