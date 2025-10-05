package com.techtool.splitup.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

object AppUpdateChecker {
    private const val TAG = "AppUpdateChecker"

    /**
     * Data class to hold update information
     */
    data class UpdateInfo(
        val shouldUpdate: Boolean,
        val isForceUpdate: Boolean,
        val currentVersion: Long,
        val latestVersion: Long,
        val minVersion: Long,
        val playStoreUrl: String,
        val updateMessage: String
    )

    /**
     * Gets the current app version code
     */
    fun getCurrentVersionCode(context: Context): Long {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }

            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }

            Log.d(TAG, "Current app version code: $versionCode")
            versionCode
        } catch (e: Exception) {
            Log.e(TAG, "Error getting version code", e)
            e.printStackTrace()
            1L
        }
    }

    /**
     * Checks if app update is available and returns update info
     * @return UpdateInfo with details about available update
     */
    suspend fun checkForUpdate(context: Context): UpdateInfo {
        Log.d(TAG, "Starting update check...")

        // Fetch latest remote config values
        val fetchSuccess = RemoteConfigManager.fetchAndActivate()
        Log.d(TAG, "Remote config fetch success: $fetchSuccess")

        val currentVersion = getCurrentVersionCode(context)
        val latestVersion = RemoteConfigManager.getLatestVersion()
        val minVersion = RemoteConfigManager.getMinVersion()
        val isForceUpdate = RemoteConfigManager.isForceUpdateEnabled()
        val playStoreUrl = RemoteConfigManager.getPlayStoreUrl()
        val updateMessage = RemoteConfigManager.getUpdateMessage()

        Log.d(TAG, "=== Version Comparison ===")
        Log.d(TAG, "Current Version: $currentVersion")
        Log.d(TAG, "Latest Version: $latestVersion")
        Log.d(TAG, "Min Version: $minVersion")
        Log.d(TAG, "Force Update Flag: $isForceUpdate")
        Log.d(TAG, "========================")

        // Determine if update is needed
        val shouldUpdate = when {
            // Current version is below minimum version - force update required
            currentVersion < minVersion -> {
                Log.d(TAG, "Update needed: Current version ($currentVersion) < Min version ($minVersion)")
                true
            }
            // Current version is below latest version and force update is enabled
            currentVersion < latestVersion && isForceUpdate -> {
                Log.d(TAG, "Update needed: Current version ($currentVersion) < Latest version ($latestVersion) AND force update enabled")
                true
            }
            // Current version is below latest version (optional update)
            currentVersion < latestVersion -> {
                Log.d(TAG, "Update available: Current version ($currentVersion) < Latest version ($latestVersion)")
                true
            }
            else -> {
                Log.d(TAG, "No update needed: App is up to date")
                false
            }
        }

        // Determine if it's a force update
        val forceUpdate = currentVersion < minVersion ||
                         (currentVersion < latestVersion && isForceUpdate)

        Log.d(TAG, "Should Update: $shouldUpdate")
        Log.d(TAG, "Force Update: $forceUpdate")

        val updateInfo = UpdateInfo(
            shouldUpdate = shouldUpdate,
            isForceUpdate = forceUpdate,
            currentVersion = currentVersion,
            latestVersion = latestVersion,
            minVersion = minVersion,
            playStoreUrl = playStoreUrl,
            updateMessage = updateMessage
        )

        Log.d(TAG, "Update check completed: $updateInfo")
        return updateInfo
    }

    /**
     * Gets appropriate title for update dialog
     */
    fun getUpdateMessage(updateInfo: UpdateInfo): Pair<String, String> {
        val title = if (updateInfo.isForceUpdate) {
            "Update Required"
        } else {
            "Update Available"
        }

        Log.d(TAG, "Update title: $title")
        return Pair(title, updateInfo.updateMessage)
    }
}
