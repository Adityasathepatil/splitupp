package com.techtool.splitup

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.techtool.splitup.utils.NetworkManager

abstract class BaseActivity : AppCompatActivity() {

    private lateinit var networkManager: NetworkManager
    private var noInternetDialog: Dialog? = null

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

    override fun onDestroy() {
        super.onDestroy()
        dismissNoInternetDialog()
    }
}
