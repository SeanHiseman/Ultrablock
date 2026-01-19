package com.ultrablock.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.ultrablock.data.preferences.UserPreferences
import com.ultrablock.data.repository.AppRepository
import com.ultrablock.data.repository.ScheduleRepository
import com.ultrablock.ui.screens.blocker.BlockerOverlayActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AppMonitorAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var appRepository: AppRepository

    @Inject
    lateinit var scheduleRepository: ScheduleRepository

    @Inject
    lateinit var userPreferences: UserPreferences

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var lastBlockedPackage: String? = null
    private var lastBlockTime: Long = 0

    companion object {
        private const val BLOCK_COOLDOWN_MS = 1000L // Prevent rapid re-blocking
        var isServiceRunning = false
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // Skip our own app and system UI
        if (packageName == this.packageName ||
            packageName == "com.android.systemui" ||
            packageName == "com.android.launcher" ||
            packageName.contains("launcher")) {
            return
        }

        // Prevent rapid re-blocking of same app
        val currentTime = System.currentTimeMillis()
        if (packageName == lastBlockedPackage && currentTime - lastBlockTime < BLOCK_COOLDOWN_MS) {
            return
        }

        serviceScope.launch {
            checkAndBlockApp(packageName)
        }
    }

    private suspend fun checkAndBlockApp(packageName: String) {
        // Check if blocking is enabled
        val blockingEnabled = userPreferences.blockingEnabled.first()
        if (!blockingEnabled) return

        // Check if we're within a blocking schedule
        val withinSchedule = scheduleRepository.isWithinBlockingSchedule()
        if (!withinSchedule) return

        // Clear any expired temporary unblocks first
        appRepository.clearExpiredUnblocks()

        // Check if this app is blocked and not temporarily unblocked
        val isBlocked = appRepository.isAppCurrentlyBlocked(packageName)
        if (!isBlocked) return

        // App should be blocked - launch blocker overlay
        lastBlockedPackage = packageName
        lastBlockTime = System.currentTimeMillis()

        val appName = appRepository.getAppName(packageName)

        val intent = Intent(this@AppMonitorAccessibilityService, BlockerOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(BlockerOverlayActivity.EXTRA_PACKAGE_NAME, packageName)
            putExtra(BlockerOverlayActivity.EXTRA_APP_NAME, appName)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        // Required override
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        serviceScope.cancel()
    }
}
