package com.techtool.splitup.dialogs

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private const val TAG = "AppUpdateDialog"

@Composable
fun AppUpdateDialog(
    title: String,
    message: String,
    isForceUpdate: Boolean,
    playStoreUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Log.d(TAG, "AppUpdateDialog composed - title: $title, isForceUpdate: $isForceUpdate, url: $playStoreUrl")

    Dialog(
        onDismissRequest = {
            Log.d(TAG, "Dialog dismiss requested")
            if (!isForceUpdate) {
                onDismiss()
            } else {
                Log.d(TAG, "Force update enabled - dismiss blocked")
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isForceUpdate,
            dismissOnClickOutside = !isForceUpdate
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    if (!isForceUpdate) {
                        TextButton(onClick = {
                            Log.d(TAG, "Later button clicked")
                            onDismiss()
                        }) {
                            Text("Later")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Button(
                        onClick = {
                            Log.d(TAG, "Update button clicked - opening Play Store: $playStoreUrl")
                            openPlayStore(context, playStoreUrl)
                            if (isForceUpdate) {
                                Log.d(TAG, "Force update - killing app process")
                                // Exit app after opening Play Store for force update
                                android.os.Process.killProcess(android.os.Process.myPid())
                            }
                        }
                    ) {
                        Text(if (isForceUpdate) "Update Now" else "Update")
                    }
                }
            }
        }
    }
}

private fun openPlayStore(context: Context, playStoreUrl: String) {
    try {
        Log.d(TAG, "Opening Play Store with URL: $playStoreUrl")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUrl))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        Log.d(TAG, "Play Store intent launched successfully")
    } catch (e: Exception) {
        Log.e(TAG, "Error opening Play Store", e)
        e.printStackTrace()
    }
}
