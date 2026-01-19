package com.ultrablock.ui.screens.blocker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultrablock.data.preferences.UserPreferences
import com.ultrablock.data.repository.AppRepository
import com.ultrablock.data.repository.PaymentRepository
import com.ultrablock.service.StripePaymentService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlockerUiState(
    val packageName: String = "",
    val appName: String = "",
    val selectedDuration: Int = 15,
    val costCents: Int = 0,
    val hasPaymentMethod: Boolean = false,
    val isProcessingPayment: Boolean = false,
    val paymentError: String? = null,
    val paymentSuccess: Boolean = false,
    val hourlyRateCents: Int = 2000
)

@HiltViewModel
class BlockerViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val appRepository: AppRepository,
    private val paymentRepository: PaymentRepository,
    private val stripePaymentService: StripePaymentService
) : ViewModel() {

    private val _packageName = MutableStateFlow("")
    private val _appName = MutableStateFlow("")
    private val _selectedDuration = MutableStateFlow(15)
    private val _isProcessingPayment = MutableStateFlow(false)
    private val _paymentError = MutableStateFlow<String?>(null)
    private val _paymentSuccess = MutableStateFlow(false)

    val uiState: StateFlow<BlockerUiState> = combine(
        _packageName,
        _appName,
        _selectedDuration,
        userPreferences.hourlyRateCents,
        userPreferences.stripePaymentMethodId,
        _isProcessingPayment,
        _paymentError,
        _paymentSuccess
    ) { values ->
        val packageName = values[0] as String
        val appName = values[1] as String
        val selectedDuration = values[2] as Int
        val hourlyRateCents = values[3] as Int
        val paymentMethodId = values[4] as String?
        val isProcessing = values[5] as Boolean
        val paymentError = values[6] as String?
        val paymentSuccess = values[7] as Boolean

        val costCents = calculateCost(hourlyRateCents, selectedDuration)

        BlockerUiState(
            packageName = packageName,
            appName = appName,
            selectedDuration = selectedDuration,
            costCents = costCents,
            hasPaymentMethod = paymentMethodId != null,
            isProcessingPayment = isProcessing,
            paymentError = paymentError,
            paymentSuccess = paymentSuccess,
            hourlyRateCents = hourlyRateCents
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BlockerUiState()
    )

    fun setBlockedApp(packageName: String, appName: String) {
        _packageName.value = packageName
        _appName.value = appName
    }

    fun setDuration(minutes: Int) {
        _selectedDuration.value = minutes
        _paymentError.value = null
    }

    fun processUnblock() {
        val currentState = uiState.value
        if (!currentState.hasPaymentMethod) {
            _paymentError.value = "Please add a payment method in settings first"
            return
        }

        viewModelScope.launch {
            _isProcessingPayment.value = true
            _paymentError.value = null

            val result = stripePaymentService.processPayment(
                amountCents = currentState.costCents,
                description = "Unblock ${currentState.appName} for ${currentState.selectedDuration} minutes"
            )

            when (result) {
                is StripePaymentService.PaymentResult.Success -> {
                    // Record the unblock in history
                    paymentRepository.recordUnblock(
                        packageName = currentState.packageName,
                        appName = currentState.appName,
                        durationMinutes = currentState.selectedDuration,
                        costCents = currentState.costCents,
                        paymentIntentId = result.paymentIntentId
                    )

                    // Temporarily unblock the app
                    appRepository.temporarilyUnblockApp(
                        currentState.packageName,
                        currentState.selectedDuration
                    )

                    _paymentSuccess.value = true
                }
                is StripePaymentService.PaymentResult.Error -> {
                    _paymentError.value = result.message
                }
            }

            _isProcessingPayment.value = false
        }
    }

    fun clearError() {
        _paymentError.value = null
    }

    private fun calculateCost(hourlyRateCents: Int, durationMinutes: Int): Int {
        return (hourlyRateCents.toDouble() / 60 * durationMinutes).toInt()
    }

    fun formatCost(cents: Int): String {
        return "$%.2f".format(cents / 100.0)
    }

    val durationOptions = listOf(5, 10, 15, 20, 30, 45, 60)
}
