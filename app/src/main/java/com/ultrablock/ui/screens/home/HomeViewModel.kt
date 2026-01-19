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
import com.ultrablock.service.AppBlockerService
import com.ultrablock.service.AppMonitorAccessibilityService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val hourlyRateDollars: Double = 20.0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val application: Application,
    private val userPreferences: UserPreferences,
    private val appRepository: AppRepository,
    private val paymentRepository: PaymentRepository
) : AndroidViewModel(application) {

    private val _permissionCheckTrigger = MutableStateFlow(0)

    val uiState: StateFlow<HomeUiState> = combine(
        userPreferences.blockingEnabled,
        appRepository.getBlockedAppCount(),
        paymentRepository.getTotalSpent(),
        paymentRepository.getTotalUnblockedMinutes(),
        paymentRepository.getUnblockCount(),
        userPreferences.hourlyRateCents,
        _permissionCheckTrigger
    ) { values ->
        val blockingEnabled = values[0] as Boolean
        val blockedAppCount = values[1] as Int
        val totalSpent = values[2] as Double
        val totalUnblockedMinutes = values[3] as Int
        val unblockCount = values[4] as Int
        val hourlyRateCents = values[5] as Int

        HomeUiState(
            blockingEnabled = blockingEnabled,
            blockedAppCount = blockedAppCount,
            totalSpent = totalSpent,
            totalUnblockedMinutes = totalUnblockedMinutes,
            unblockCount = unblockCount,
            hasAllPermissions = checkAllPermissions(),
            isAccessibilityEnabled = AppMonitorAccessibilityService.isServiceRunning,
            hourlyRateDollars = hourlyRateCents / 100.0
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun toggleBlocking(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setBlockingEnabled(enabled)
            if (enabled) {
                startBlockerService()
            } else {
                stopBlockerService()
            }
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

    private fun checkAllPermissions(): Boolean {
        return hasOverlayPermission() && hasAccessibilityPermission()
    }

    private fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(application)
    }

    private fun hasAccessibilityPermission(): Boolean {
        return AppMonitorAccessibilityService.isServiceRunning
    }

    fun formatTimeSaved(minutes: Int): String {
        return when {
            minutes < 60 -> "$minutes min"
            else -> {
                val hours = minutes / 60
                val mins = minutes % 60
                if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
            }
        }
    }
}
