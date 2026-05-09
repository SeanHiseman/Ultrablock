package com.ultrablock.ui.screens.blocker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultrablock.data.preferences.UserPreferences
import com.ultrablock.data.repository.AppRepository
import com.ultrablock.data.repository.PaymentRepository
import com.ultrablock.data.repository.UsageRepository
import com.ultrablock.domain.model.FrictionLevel
import com.ultrablock.service.StripePaymentService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlockerUiState(
    val packageName: String = "",
    val appName: String = "",
    val frictionLevel: FrictionLevel = FrictionLevel.STRICT,
    // GENTLE / MODERATE countdown
    val countdownSeconds: Int = 0,
    val countdownComplete: Boolean = false,
    val reflectionReason: String = "",
    // STRICT payment
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
    private val usageRepository: UsageRepository,
    private val stripePaymentService: StripePaymentService
) : ViewModel() {

    private val _packageName = MutableStateFlow("")
    private val _appName = MutableStateFlow("")
    private val _frictionLevel = MutableStateFlow(FrictionLevel.STRICT)
    private val _countdownSeconds = MutableStateFlow(0)
    private val _countdownComplete = MutableStateFlow(false)
    private val _reflectionReason = MutableStateFlow("")
    private val _selectedDuration = MutableStateFlow(15)
    private val _isProcessingPayment = MutableStateFlow(false)
    private val _paymentError = MutableStateFlow<String?>(null)
    private val _paymentSuccess = MutableStateFlow(false)

    private var currentSessionId: Long = -1L
    private var countdownJob: Job? = null

    val uiState: StateFlow<BlockerUiState> = combine(
        _packageName,
        _appName,
        _frictionLevel,
        _countdownSeconds,
        _countdownComplete,
        _reflectionReason,
        _selectedDuration,
        userPreferences.hourlyRateCents,
        userPreferences.stripePaymentMethodId,
        _isProcessingPayment,
        _paymentError,
        _paymentSuccess
    ) { values ->
        val packageName = values[0] as String
        val appName = values[1] as String
        val frictionLevel = values[2] as FrictionLevel
        val countdownSeconds = values[3] as Int
        val countdownComplete = values[4] as Boolean
        val reflectionReason = values[5] as String
        val selectedDuration = values[6] as Int
        val hourlyRateCents = values[7] as Int
        val paymentMethodId = values[8] as String?
        val isProcessing = values[9] as Boolean
        val paymentError = values[10] as String?
        val paymentSuccess = values[11] as Boolean

        BlockerUiState(
            packageName = packageName,
            appName = appName,
            frictionLevel = frictionLevel,
            countdownSeconds = countdownSeconds,
            countdownComplete = countdownComplete,
            reflectionReason = reflectionReason,
            selectedDuration = selectedDuration,
            costCents = calculateCost(hourlyRateCents, selectedDuration),
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

        viewModelScope.launch {
            val resolved = resolvedFrictionLevel(packageName)
            _frictionLevel.value = resolved
            startCountdownIfNeeded(resolved)
            currentSessionId = usageRepository.recordBlockAttempt(packageName, appName, resolved.name)
        }
    }

    private suspend fun resolvedFrictionLevel(packageName: String): FrictionLevel {
        val app = appRepository.getAppByPackage(packageName)
        val perApp = app?.frictionLevel
        val levelName = perApp ?: userPreferences.globalFrictionLevel.first()
        return runCatching { FrictionLevel.valueOf(levelName) }.getOrDefault(FrictionLevel.STRICT)
    }

    private fun startCountdownIfNeeded(level: FrictionLevel) {
        countdownJob?.cancel()
        val seconds = when (level) {
            FrictionLevel.GENTLE -> 5
            FrictionLevel.MODERATE -> 30
            else -> return
        }
        _countdownSeconds.value = seconds
        _countdownComplete.value = false

        countdownJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                _countdownSeconds.value = remaining
            }
            _countdownComplete.value = true
        }
    }

    fun setReflectionReason(reason: String) {
        _reflectionReason.value = reason
    }

    /** Called when the user bypasses via GENTLE or MODERATE (free, no payment). */
    fun bypassFree() {
        viewModelScope.launch {
            if (currentSessionId >= 0) {
                usageRepository.recordUnblock(currentSessionId, durationMinutes = 0)
            }
            appRepository.temporarilyUnblockApp(_packageName.value, durationMinutes = 5)
            _paymentSuccess.value = true
        }
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
                    paymentRepository.recordUnblock(
                        packageName = currentState.packageName,
                        appName = currentState.appName,
                        durationMinutes = currentState.selectedDuration,
                        costCents = currentState.costCents,
                        paymentIntentId = result.paymentIntentId
                    )
                    appRepository.temporarilyUnblockApp(currentState.packageName, currentState.selectedDuration)
                    if (currentSessionId >= 0) {
                        usageRepository.recordUnblock(currentSessionId, currentState.selectedDuration)
                    }
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

    fun formatCost(cents: Int): String = "$%.2f".format(cents / 100.0)

    val durationOptions = listOf(5, 10, 15, 20, 30, 45, 60)
}
