package com.techtool.splitup

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // Check if user is already logged in
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // User is logged in, go to home
            startActivity(Intent(this, HomeActivity::class.java))
        } else {
            // User is not logged in, go to sign up
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        finish()
    }
}
