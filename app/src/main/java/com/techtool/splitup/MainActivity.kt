package com.techtool.splitup

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.techtool.splitup.dialogs.AppUpdateDialog
import com.techtool.splitup.ui.theme.SplitupTheme
import com.techtool.splitup.utils.AppUpdateChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity onCreate")
        enableEdgeToEdge()
        setContent {
            SplitupTheme {
                var showUpdateDialog by remember { mutableStateOf(false) }
                var updateInfo by remember { mutableStateOf<AppUpdateChecker.UpdateInfo?>(null) }

                LaunchedEffect(Unit) {
                    Log.d(TAG, "LaunchedEffect triggered - checking for updates")
                    try {
                        val info = AppUpdateChecker.checkForUpdate(this@MainActivity)
                        Log.d(TAG, "Update check result: shouldUpdate=${info.shouldUpdate}, isForceUpdate=${info.isForceUpdate}")

                        if (info.shouldUpdate) {
                            updateInfo = info
                            showUpdateDialog = true
                            Log.d(TAG, "Showing update dialog")
                        } else {
                            Log.d(TAG, "No update needed, dialog will not be shown")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error checking for update", e)
                    }
                }

                // Show update dialog if needed
                if (showUpdateDialog && updateInfo != null) {
                    Log.d(TAG, "Rendering AppUpdateDialog")
                    val (title, message) = AppUpdateChecker.getUpdateMessage(updateInfo!!)
                    AppUpdateDialog(
                        title = title,
                        message = updateInfo!!.updateMessage,
                        isForceUpdate = updateInfo!!.isForceUpdate,
                        playStoreUrl = updateInfo!!.playStoreUrl,
                        onDismiss = {
                            Log.d(TAG, "Update dialog dismissed")
                            showUpdateDialog = false
                        }
                    )
                }
            }
        }
    }
}

