package com.ultrablock.ui.screens.home

import android.app.Application
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ultrablock.data.preferences.UserPreferences
import com.ultrablock.data.repository.AppRepository
import com.ultrablock.data.repository.PaymentRepository
import com.ultrablock.data.repository.UsageRepository
import com.ultrablock.service.AppBlockerService
import com.ultrablock.service.AppMonitorAccessibilityService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val blockingEnabled: Boolean = false,
    val blockedAppCount: Int = 0,
    val totalSpent: Double = 0.0,
    val totalUnblockedMinutes: Int = 0,
    val unblockCount: Int = 0,
    val hasAllPermissions: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val hourlyRateDollars: Double = 20.0,
    // time-cost tracking
    val todayBlockAttempts: Int = 0,
    val todaySuccessfulBlocks: Int = 0,
    val weekBlockAttempts: Int = 0,
    val weekSuccessfulBlocks: Int = 0,
    val todayTimeSavedMinutes: Int = 0
) {
    val todaySuccessRate: Float
        get() = if (todayBlockAttempts == 0) 1f else todaySuccessfulBlocks.toFloat() / todayBlockAttempts

    val estimatedTimeSavedValue: Double
        get() = (todayTimeSavedMinutes / 60.0) * hourlyRateDollars
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val application: Application,
    private val userPreferences: UserPreferences,
    private val appRepository: AppRepository,
    private val paymentRepository: PaymentRepository,
    private val usageRepository: UsageRepository
) : AndroidViewModel(application) {

    private val _permissionCheckTrigger = MutableStateFlow(0)

    val uiState: StateFlow<HomeUiState> = combine(
        userPreferences.blockingEnabled,
        appRepository.getBlockedAppCount(),
        paymentRepository.getTotalSpent(),
        paymentRepository.getTotalUnblockedMinutes(),
        paymentRepository.getUnblockCount(),
        userPreferences.hourlyRateCents,
        usageRepository.getTodayBlockAttempts(),
        usageRepository.getTodaySuccessfulBlocks(),
        usageRepository.getWeekBlockAttempts(),
        usageRepository.getWeekSuccessfulBlocks(),
        _permissionCheckTrigger
    ) { values ->
        val blockingEnabled = values[0] as Boolean
        val blockedAppCount = values[1] as Int
        val totalSpent = values[2] as Double
        val totalUnblockedMinutes = values[3] as Int
        val unblockCount = values[4] as Int
        val hourlyRateCents = values[5] as Int
        val todayBlockAttempts = values[6] as Int
        val todaySuccessfulBlocks = values[7] as Int
        val weekBlockAttempts = values[8] as Int
        val weekSuccessfulBlocks = values[9] as Int

        // Estimate time saved: successful blocks × avg session length (assume 15 min each)
        val todayTimeSavedMinutes = todaySuccessfulBlocks * 15

        HomeUiState(
            blockingEnabled = blockingEnabled,
            blockedAppCount = blockedAppCount,
            totalSpent = totalSpent,
            totalUnblockedMinutes = totalUnblockedMinutes,
            unblockCount = unblockCount,
            hasAllPermissions = checkAllPermissions(),
            isAccessibilityEnabled = AppMonitorAccessibilityService.isServiceRunning,
            hourlyRateDollars = hourlyRateCents / 100.0,
            todayBlockAttempts = todayBlockAttempts,
            todaySuccessfulBlocks = todaySuccessfulBlocks,
            weekBlockAttempts = weekBlockAttempts,
            weekSuccessfulBlocks = weekSuccessfulBlocks,
            todayTimeSavedMinutes = todayTimeSavedMinutes
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun toggleBlocking(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setBlockingEnabled(enabled)
            if (enabled) startBlockerService() else stopBlockerService()
        }
    }

    fun refreshPermissions() {
        _permissionCheckTrigger.value++
    }

    private fun startBlockerService() {
        val intent = Intent(application, AppBlockerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            application.startForegroundService(intent)
        } else {
            application.startService(intent)
        }
    }

    private fun stopBlockerService() {
        application.stopService(Intent(application, AppBlockerService::class.java))
    }

    private fun checkAllPermissions() = hasOverlayPermission() && hasAccessibilityPermission()
    private fun hasOverlayPermission() = Settings.canDrawOverlays(application)
    private fun hasAccessibilityPermission() = AppMonitorAccessibilityService.isServiceRunning

    fun formatTime(minutes: Int): String = when {
        minutes < 60 -> "${minutes}m"
        else -> {
            val h = minutes / 60
            val m = minutes % 60
            if (m > 0) "${h}h ${m}m" else "${h}h"
        }
    }

    fun formatTimeSaved(minutes: Int): String = formatTime(minutes)
}
