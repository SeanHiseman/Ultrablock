package com.ultrablock.ui.screens.social

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ultrablock.data.local.entity.AccountabilityPartner
import com.ultrablock.data.preferences.UserPreferences
import com.ultrablock.data.repository.SocialRepository
import com.ultrablock.data.repository.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SocialUiState(
    val userCode: String = "",
    val partners: List<AccountabilityPartner> = emptyList(),
    val todayBlockAttempts: Int = 0,
    val todaySuccessfulBlocks: Int = 0,
    val weekBlockAttempts: Int = 0,
    val weekSuccessfulBlocks: Int = 0,
    val showAddPartnerDialog: Boolean = false,
    val addPartnerCodeInput: String = "",
    val addPartnerNameInput: String = "",
    val addPartnerError: String? = null,
    val addPartnerSuccess: Boolean = false
) {
    val todaySuccessRate: Float
        get() = if (todayBlockAttempts == 0) 1f else todaySuccessfulBlocks.toFloat() / todayBlockAttempts

    val weekSuccessRate: Float
        get() = if (weekBlockAttempts == 0) 1f else weekSuccessfulBlocks.toFloat() / weekBlockAttempts
}

@HiltViewModel
class SocialViewModel @Inject constructor(
    private val application: Application,
    private val userPreferences: UserPreferences,
    private val socialRepository: SocialRepository,
    private val usageRepository: UsageRepository
) : AndroidViewModel(application) {

    private val _showAddPartnerDialog = MutableStateFlow(false)
    private val _addPartnerCodeInput = MutableStateFlow("")
    private val _addPartnerNameInput = MutableStateFlow("")
    private val _addPartnerError = MutableStateFlow<String?>(null)
    private val _addPartnerSuccess = MutableStateFlow(false)
    private val _userCode = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _userCode.value = userPreferences.getOrCreateSocialCode()
        }
    }

    val uiState: StateFlow<SocialUiState> = combine(
        _userCode,
        socialRepository.getAllPartners(),
        usageRepository.getTodayBlockAttempts(),
        usageRepository.getTodaySuccessfulBlocks(),
        usageRepository.getWeekBlockAttempts(),
        usageRepository.getWeekSuccessfulBlocks(),
        _showAddPartnerDialog,
        _addPartnerCodeInput,
        _addPartnerNameInput,
        _addPartnerError,
        _addPartnerSuccess
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val partners = values[1] as List<AccountabilityPartner>
        SocialUiState(
            userCode = values[0] as String,
            partners = partners,
            todayBlockAttempts = values[2] as Int,
            todaySuccessfulBlocks = values[3] as Int,
            weekBlockAttempts = values[4] as Int,
            weekSuccessfulBlocks = values[5] as Int,
            showAddPartnerDialog = values[6] as Boolean,
            addPartnerCodeInput = values[7] as String,
            addPartnerNameInput = values[8] as String,
            addPartnerError = values[9] as String?,
            addPartnerSuccess = values[10] as Boolean
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SocialUiState()
    )

    fun copyCodeToClipboard() {
        val clipboard = application.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Ultrablock code", _userCode.value))
    }

    fun shareStats() {
        val state = uiState.value
        val text = buildString {
            appendLine("My Ultrablock stats 📊")
            appendLine("Code: ${state.userCode}")
            appendLine()
            appendLine("Today: ${state.todaySuccessfulBlocks}/${state.todayBlockAttempts} blocks held (${(state.todaySuccessRate * 100).toInt()}%)")
            appendLine("This week: ${state.weekSuccessfulBlocks}/${state.weekBlockAttempts} blocks held (${(state.weekSuccessRate * 100).toInt()}%)")
            appendLine()
            appendLine("Join me on Ultrablock to track your screen time!")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, "My Ultrablock screen time stats")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        application.startActivity(Intent.createChooser(intent, "Share stats").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    fun showAddPartnerDialog() {
        _addPartnerCodeInput.value = ""
        _addPartnerNameInput.value = ""
        _addPartnerError.value = null
        _addPartnerSuccess.value = false
        _showAddPartnerDialog.value = true
    }

    fun hideAddPartnerDialog() {
        _showAddPartnerDialog.value = false
    }

    fun setPartnerCodeInput(code: String) {
        _addPartnerCodeInput.value = code.uppercase()
        _addPartnerError.value = null
    }

    fun setPartnerNameInput(name: String) {
        _addPartnerNameInput.value = name
        _addPartnerError.value = null
    }

    fun addPartner() {
        val code = _addPartnerCodeInput.value.trim()
        val name = _addPartnerNameInput.value.trim()
        if (code == _userCode.value) {
            _addPartnerError.value = "That's your own code!"
            return
        }
        viewModelScope.launch {
            val result = socialRepository.addPartner(code, name)
            result.fold(
                onSuccess = {
                    _addPartnerSuccess.value = true
                    hideAddPartnerDialog()
                },
                onFailure = { _addPartnerError.value = it.message }
            )
        }
    }

    fun removePartner(partner: AccountabilityPartner) {
        viewModelScope.launch { socialRepository.removePartner(partner) }
    }

    fun formatSuccessRate(rate: Float): String = "${(rate * 100).toInt()}%"
}
