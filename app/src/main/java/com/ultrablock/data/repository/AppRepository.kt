package com.ultrablock.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.ultrablock.data.local.dao.BlockedAppDao
import com.ultrablock.data.local.entity.BlockedApp
import com.ultrablock.domain.model.InstalledApp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val blockedAppDao: BlockedAppDao
) {
    fun getBlockedApps(): Flow<List<BlockedApp>> = blockedAppDao.getAllBlockedApps()

    fun getBlockedAppCount(): Flow<Int> = blockedAppDao.getBlockedAppCount()

    suspend fun getInstalledApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val blockedApps = blockedAppDao.getAllBlockedApps().let { flow ->
            val apps = mutableListOf<BlockedApp>()
            // Get current blocked apps synchronously
            blockedAppDao.getByPackageName("") // warm up
            apps
        }

        val blockedPackages = mutableSetOf<String>()

        packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .filter { it.activityInfo.packageName != context.packageName }
            .map { resolveInfo ->
                val appInfo = resolveInfo.activityInfo.applicationInfo
                InstalledApp(
                    packageName = appInfo.packageName,
                    appName = packageManager.getApplicationLabel(appInfo).toString(),
                    icon = try {
                        packageManager.getApplicationIcon(appInfo.packageName)
                    } catch (e: Exception) {
                        null
                    },
                    isBlocked = blockedPackages.contains(appInfo.packageName)
                )
            }
            .sortedBy { it.appName.lowercase() }
    }

    suspend fun getInstalledAppsWithBlockStatus(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        // Get all blocked apps from database
        val blockedAppsMap = mutableMapOf<String, BlockedApp>()
        blockedAppDao.getAllBlockedApps().collect { apps ->
            apps.forEach { blockedAppsMap[it.packageName] = it }
        }

        packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .filter { it.activityInfo.packageName != context.packageName }
            .map { resolveInfo ->
                val appInfo = resolveInfo.activityInfo.applicationInfo
                val packageName = appInfo.packageName
                InstalledApp(
                    packageName = packageName,
                    appName = packageManager.getApplicationLabel(appInfo).toString(),
                    icon = try {
                        packageManager.getApplicationIcon(packageName)
                    } catch (e: Exception) {
                        null
                    },
                    isBlocked = blockedAppsMap[packageName]?.isBlocked ?: false
                )
            }
            .sortedBy { it.appName.lowercase() }
    }

    suspend fun setAppBlocked(packageName: String, appName: String, blocked: Boolean) {
        val existing = blockedAppDao.getByPackageName(packageName)
        if (existing != null) {
            blockedAppDao.update(existing.copy(isBlocked = blocked))
        } else if (blocked) {
            blockedAppDao.insert(BlockedApp(packageName = packageName, appName = appName, isBlocked = true))
        }
    }

    suspend fun isAppCurrentlyBlocked(packageName: String): Boolean {
        val currentTime = System.currentTimeMillis()
        return blockedAppDao.isAppCurrentlyBlocked(packageName, currentTime) != null
    }

    suspend fun temporarilyUnblockApp(packageName: String, durationMinutes: Int) {
        val until = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        blockedAppDao.setTemporaryUnblock(packageName, until)
    }

    suspend fun clearExpiredUnblocks() {
        blockedAppDao.clearExpiredUnblocks(System.currentTimeMillis())
    }

    suspend fun getAppByPackage(packageName: String): BlockedApp? {
        return blockedAppDao.getByPackageName(packageName)
    }

    fun getAppName(packageName: String): String {
        return try {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}
