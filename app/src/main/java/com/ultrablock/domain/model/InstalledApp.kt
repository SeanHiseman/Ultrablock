package com.ultrablock.domain.model

import android.graphics.drawable.Drawable

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isBlocked: Boolean = false
)
