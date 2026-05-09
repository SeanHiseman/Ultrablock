package com.ultrablock.ui.screens.settings

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ultrablock.data.preferences.UserPreferences
import com.ultrablock.domain.model.FrictionLevel
import com.ultrablock.service.AppMonitorAccessibilityService
import com.ultrablock.service.StripePaymentService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val hourlyRateDollars: Int = 20,
    val paymentMethodLastFour: String? = null,
    val hasPaymentMethod: Boolean = false,
    val defaultUnblockMinutes: Int = 15,
    val globalFrictionLevel: FrictionLevel = FrictionLevel.STRICT,
    val hasOverlayPermission: Boolean = false,
    val hasAccessibilityPermission: Boolean = false,
    val showRateDialog: Boolean = false,
    val showUnblockDurationDialog: Boolean = false,
    val showFrictionDialog: Boolean = false,
    val isAddingCard: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val application: Application,
    private val userPreferences: UserPreferences,
    private val stripePaymentService: StripePaymentService
) : AndroidViewModel(application) {

    private val _showRateDialog = MutableStateFlow(false)
    private val _showUnblockDurationDialog = MutableStateFlow(false)
    private val _showFrictionDialog = MutableStateFlow(false)
    private val _isAddingCard = MutableStateFlow(false)
    private val _permissionRefresh = MutableStateFlow(0)

    val uiState: StateFlow<SettingsUiState> = combine(
        userPreferences.hourlyRateCents,
        userPreferences.paymentMethodLastFour,
        userPreferences.defaultUnblockDuration,
        userPreferences.globalFrictionLevel,
        _showRateDialog,
        _showUnblockDurationDialog,
        _showFrictionDialog,
        _isAddingCard,
        _permissionRefresh
    ) { values ->
        val hourlyRateCents = values[0] as Int
        val paymentMethodLastFour = values[1] as String?
        val defaultUnblockMinutes = values[2] as Int
        val frictionLevelName = values[3] as String
        val frictionLevel = runCatching { FrictionLevel.valueOf(frictionLevelName) }.getOrDefault(FrictionLevel.STRICT)

        SettingsUiState(
            hourlyRateDollars = hourlyRateCents / 100,
            paymentMethodLastFour = paymentMethodLastFour,
            hasPaymentMethod = paymentMethodLastFour != null,
            defaultUnblockMinutes = defaultUnblockMinutes,
            globalFrictionLevel = frictionLevel,
            hasOverlayPermission = Settings.canDrawOverlays(application),
            hasAccessibilityPermission = AppMonitorAccessibilityService.isServiceRunning,
            showRateDialog = values[4] as Boolean,
            showUnblockDurationDialog = values[5] as Boolean,
            showFrictionDialog = values[6] as Boolean,
            isAddingCard = values[7] as Boolean
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun showRateDialog() { _showRateDialog.value = true }
    fun hideRateDialog() { _showRateDialog.value = false }
    fun setHourlyRate(dollars: Int) {
        viewModelScope.launch { userPreferences.setHourlyRateCents(dollars * 100); hideRateDialog() }
    }

    fun showUnblockDurationDialog() { _showUnblockDurationDialog.value = true }
    fun hideUnblockDurationDialog() { _showUnblockDurationDialog.value = false }
    fun setDefaultUnblockDuration(minutes: Int) {
        viewModelScope.launch { userPreferences.setDefaultUnblockDuration(minutes); hideUnblockDurationDialog() }
    }

    fun showFrictionDialog() { _showFrictionDialog.value = true }
    fun hideFrictionDialog() { _showFrictionDialog.value = false }
    fun setFrictionLevel(level: FrictionLevel) {
        viewModelScope.launch { userPreferences.setGlobalFrictionLevel(level.name); hideFrictionDialog() }
    }

    fun addPaymentMethod() {
        viewModelScope.launch {
            _isAddingCard.value = true
            stripePaymentService.addDemoCard()
            _isAddingCard.value = false
        }
    }

    fun removePaymentMethod() {
        viewModelScope.launch { stripePaymentService.removePaymentMethod() }
    }

    fun refreshPermissions() { _permissionRefresh.value++ }

    val rateOptions = listOf(5, 10, 15, 20, 25, 30, 40, 50, 75, 100)
    val durationOptions = listOf(5, 10, 15, 20, 30, 45, 60)
    val frictionOptions = FrictionLevel.entries
}
