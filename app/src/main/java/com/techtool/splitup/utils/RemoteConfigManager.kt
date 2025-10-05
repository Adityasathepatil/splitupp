package com.techtool.splitup.utils

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.tasks.await

object RemoteConfigManager {
    private const val TAG = "RemoteConfigManager"
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    // Remote config keys
    private const val KEY_FORCE_UPDATE = "force_update"
    private const val KEY_LATEST_VERSION = "latest_version"
    private const val KEY_MIN_VERSION = "min_version"
    private const val KEY_PLAYSTORE_URL = "playstore_url"
    private const val KEY_UPDATE_MESSAGE = "update_message"

    // Default values
    private const val DEFAULT_FORCE_UPDATE = false
    private const val DEFAULT_LATEST_VERSION = 1L
    private const val DEFAULT_MIN_VERSION = 1L
    private const val DEFAULT_PLAYSTORE_URL = "https://play.google.com/store/apps/details?id=com.techtool.splitup"
    private const val DEFAULT_UPDATE_MESSAGE = "A new version is available! Update now for the best experience."

    init {
        Log.d(TAG, "Initializing RemoteConfigManager")
        // Configure Remote Config settings
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600) // 0 for development, use 3600 for production
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        Log.d(TAG, "Config settings applied: fetch interval = 0 seconds")

        // Set default values
        val defaults = mapOf(
            KEY_FORCE_UPDATE to DEFAULT_FORCE_UPDATE,
            KEY_LATEST_VERSION to DEFAULT_LATEST_VERSION,
            KEY_MIN_VERSION to DEFAULT_MIN_VERSION,
            KEY_PLAYSTORE_URL to DEFAULT_PLAYSTORE_URL,
            KEY_UPDATE_MESSAGE to DEFAULT_UPDATE_MESSAGE
        )
        remoteConfig.setDefaultsAsync(defaults)
        Log.d(TAG, "Default values set: force_update=$DEFAULT_FORCE_UPDATE, latest_version=$DEFAULT_LATEST_VERSION, min_version=$DEFAULT_MIN_VERSION")
    }

    /**
     * Fetches and activates remote config values
     * @return true if fetch and activate succeeded
     */
    suspend fun fetchAndActivate(): Boolean {
        return try {
            Log.d(TAG, "Starting fetchAndActivate...")
            val result = remoteConfig.fetchAndActivate().await()
            Log.d(TAG, "fetchAndActivate result: $result")

            // Log all current values
            logAllConfigValues()

            result
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching remote config", e)
            e.printStackTrace()
            false
        }
    }

    /**
     * Logs all current config values for debugging
     */
    private fun logAllConfigValues() {
        val forceUpdate = remoteConfig.getBoolean(KEY_FORCE_UPDATE)
        val latestVersion = remoteConfig.getLong(KEY_LATEST_VERSION)
        val minVersion = remoteConfig.getLong(KEY_MIN_VERSION)
        val playStoreUrl = remoteConfig.getString(KEY_PLAYSTORE_URL)
        val updateMessage = remoteConfig.getString(KEY_UPDATE_MESSAGE)

        Log.d(TAG, "=== Remote Config Values ===")
        Log.d(TAG, "force_update: $forceUpdate")
        Log.d(TAG, "latest_version: $latestVersion")
        Log.d(TAG, "min_version: $minVersion")
        Log.d(TAG, "playstore_url: $playStoreUrl")
        Log.d(TAG, "update_message: $updateMessage")
        Log.d(TAG, "===========================")

        // Check value sources
        Log.d(TAG, "Value sources:")
        Log.d(TAG, "force_update source: ${remoteConfig.getValue(KEY_FORCE_UPDATE).source}")
        Log.d(TAG, "latest_version source: ${remoteConfig.getValue(KEY_LATEST_VERSION).source}")
        Log.d(TAG, "min_version source: ${remoteConfig.getValue(KEY_MIN_VERSION).source}")
        Log.d(TAG, "playstore_url source: ${remoteConfig.getValue(KEY_PLAYSTORE_URL).source}")
        Log.d(TAG, "update_message source: ${remoteConfig.getValue(KEY_UPDATE_MESSAGE).source}")
    }

    /**
     * Gets the force update flag
     */
    fun isForceUpdateEnabled(): Boolean {
        val value = remoteConfig.getBoolean(KEY_FORCE_UPDATE)
        Log.d(TAG, "Getting force_update: $value")
        return value
    }

    /**
     * Gets the latest version from remote config
     */
    fun getLatestVersion(): Long {
        val value = remoteConfig.getLong(KEY_LATEST_VERSION)
        Log.d(TAG, "Getting latest_version: $value")
        return value
    }

    /**
     * Gets the minimum supported version from remote config
     */
    fun getMinVersion(): Long {
        val value = remoteConfig.getLong(KEY_MIN_VERSION)
        Log.d(TAG, "Getting min_version: $value")
        return value
    }

    /**
     * Gets the Play Store URL from remote config
     */
    fun getPlayStoreUrl(): String {
        val value = remoteConfig.getString(KEY_PLAYSTORE_URL)
        Log.d(TAG, "Getting playstore_url: $value")
        return value
    }

    /**
     * Gets the update message from remote config
     */
    fun getUpdateMessage(): String {
        val value = remoteConfig.getString(KEY_UPDATE_MESSAGE)
        Log.d(TAG, "Getting update_message: $value")
        return value
    }
}
