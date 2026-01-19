package com.ultrablock.ui.screens.appselection

import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultrablock.data.repository.AppRepository
import com.ultrablock.domain.model.InstalledApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppSelectionUiState(
    val apps: List<AppItem> = emptyList(),
    val filteredApps: List<AppItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val selectedCount: Int = 0
)

data class AppItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isBlocked: Boolean
)

@HiltViewModel
class AppSelectionViewModel @Inject constructor(
    private val appRepository: AppRepository
) : ViewModel() {

    private val _apps = MutableStateFlow<List<AppItem>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(true)

    val uiState: StateFlow<AppSelectionUiState> = combine(
        _apps,
        _searchQuery,
        _isLoading,
        appRepository.getBlockedApps()
    ) { apps, query, isLoading, blockedApps ->
        val blockedPackages = blockedApps.map { it.packageName }.toSet()

        val updatedApps = apps.map { app ->
            app.copy(isBlocked = blockedPackages.contains(app.packageName))
        }

        val filteredApps = if (query.isBlank()) {
            updatedApps
        } else {
            updatedApps.filter {
                it.appName.contains(query, ignoreCase = true)
            }
        }

        AppSelectionUiState(
            apps = updatedApps,
            filteredApps = filteredApps,
            searchQuery = query,
            isLoading = isLoading,
            selectedCount = updatedApps.count { it.isBlocked }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSelectionUiState()
    )

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val installedApps = appRepository.getInstalledApps()
                _apps.value = installedApps.map { app ->
                    AppItem(
                        packageName = app.packageName,
                        appName = app.appName,
                        icon = app.icon,
                        isBlocked = app.isBlocked
                    )
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleAppBlocked(packageName: String, appName: String) {
        viewModelScope.launch {
            val currentApp = _apps.value.find { it.packageName == packageName }
            if (currentApp != null) {
                appRepository.setAppBlocked(packageName, appName, !currentApp.isBlocked)
            }
        }
    }
}
