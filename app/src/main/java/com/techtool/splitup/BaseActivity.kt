package com.techtool.splitup

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Window
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.techtool.splitup.utils.AppUpdateChecker
import com.techtool.splitup.utils.NetworkManager
import kotlinx.coroutines.launch

abstract class BaseActivity : AppCompatActivity() {

    private lateinit var networkManager: NetworkManager
    private var noInternetDialog: Dialog? = null
    private var updateDialog: AlertDialog? = null
    private val TAG = "BaseActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize network manager
        networkManager = NetworkManager(applicationContext)

        // Observe network connectivity
        networkManager.observe(this, Observer { isConnected ->
            if (isConnected) {
                // Internet is available, dismiss dialog
                dismissNoInternetDialog()
            } else {
                // No internet, show dialog
                showNoInternetDialog()
            }
        })

        // Check for app updates
        checkForAppUpdate()
    }

    private fun showNoInternetDialog() {
        // Don't show multiple dialogs
        if (noInternetDialog?.isShowing == true) {
            return
        }

        noInternetDialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_no_internet)
            setCancelable(false)

            // Make dialog background transparent to show custom background
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            show()
        }
    }

    private fun dismissNoInternetDialog() {
        noInternetDialog?.let {
            if (it.isShowing) {
                it.dismiss()
            }
        }
        noInternetDialog = null
    }

    private fun checkForAppUpdate() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Checking for app update...")
                val updateInfo = AppUpdateChecker.checkForUpdate(this@BaseActivity)

                Log.d(TAG, "Update check completed: shouldUpdate=${updateInfo.shouldUpdate}, isForceUpdate=${updateInfo.isForceUpdate}")

                if (updateInfo.shouldUpdate) {
                    showUpdateDialog(updateInfo)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for update", e)
            }
        }
    }

    private fun showUpdateDialog(updateInfo: AppUpdateChecker.UpdateInfo) {
        // Don't show multiple dialogs
        if (updateDialog?.isShowing == true) {
            return
        }

        val title = if (updateInfo.isForceUpdate) "Update Required" else "Update Available"

        Log.d(TAG, "Showing update dialog: $title")

        val builder = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(updateInfo.updateMessage)
            .setPositiveButton("Update Now") { _, _ ->
                openPlayStore(updateInfo.playStoreUrl)
                if (updateInfo.isForceUpdate) {
                    // Exit app after opening Play Store for force update
                    finishAffinity()
                }
            }
            .setCancelable(!updateInfo.isForceUpdate)

        if (!updateInfo.isForceUpdate) {
            builder.setNegativeButton("Later") { dialog, _ ->
                dialog.dismiss()
            }
        } else {
            builder.setOnCancelListener {
                // Force update - exit app if dialog is cancelled
                finishAffinity()
            }
        }

        updateDialog = builder.create()
        updateDialog?.show()
    }

    private fun openPlayStore(playStoreUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening Play Store", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissNoInternetDialog()
        updateDialog?.dismiss()
    }
}
