package com.techtool.splitup

import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.techtool.splitup.databinding.ActivitySignInBinding

class SignInActivity : BaseActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest
    private lateinit var googleSignInClient: GoogleSignInClient

    // One Tap Sign In launcher
    private val oneTapSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        hideGoogleSignInLoading()
        try {
            val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
            val idToken = credential.googleIdToken
            if (idToken != null) {
                firebaseAuthWithGoogle(idToken)
            } else {
                android.util.Log.e("GoogleSignIn", "No ID token from One Tap")
                // Fall back to traditional sign in
                signInWithGoogleTraditional()
            }
        } catch (e: ApiException) {
            android.util.Log.e("GoogleSignIn", "One Tap failed: ${e.statusCode} - ${e.message}")
            // Fall back to traditional sign in if One Tap fails
            signInWithGoogleTraditional()
        } catch (e: Exception) {
            android.util.Log.e("GoogleSignIn", "Unexpected error in One Tap: ${e.message}", e)
            hideGoogleSignInLoading()
            Toast.makeText(this, "Sign in failed. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    // Traditional Google Sign In launcher (fallback)
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        hideGoogleSignInLoading()
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                val idToken = account.idToken
                if (idToken != null) {
                    firebaseAuthWithGoogle(idToken)
                } else {
                    Toast.makeText(this, "Failed to get authentication token. Please try again.", Toast.LENGTH_LONG).show()
                    android.util.Log.e("GoogleSignIn", "ID Token is null")
                }
            } else {
                Toast.makeText(this, "Failed to get account information. Please try again.", Toast.LENGTH_LONG).show()
                android.util.Log.e("GoogleSignIn", "Account is null")
            }
        } catch (e: ApiException) {
            handleGoogleSignInError(e)
        } catch (e: Exception) {
            android.util.Log.e("GoogleSignIn", "Unexpected error: ${e.message}", e)
            Toast.makeText(this, "An unexpected error occurred: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Check if user is already signed in
        if (auth.currentUser != null) {
            navigateToMain()
            return
        }

        // Initialize One Tap Sign In client
        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false) // Show all accounts, not just previously authorized
                    .build()
            )
            .setAutoSelectEnabled(true) // Auto-select if only one account
            .build()

        // Configure traditional Google Sign In (fallback)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setupUI()
        setupValidation()
    }

    private fun setupUI() {
        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        binding.btnSignIn.setOnClickListener {
            if (validateForm()) {
                signIn()
            }
        }

        binding.tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }

        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }
    }

    private fun setupValidation() {
        binding.etEmail.doAfterTextChanged { validateEmail() }
        binding.etPassword.doAfterTextChanged { validatePassword() }
    }

    private fun validateEmail(): Boolean {
        val email = binding.etEmail.text.toString().trim()
        return when {
            email.isEmpty() -> {
                binding.tilEmail.error = "Email is required"
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.tilEmail.error = "Please enter a valid email"
                false
            }
            else -> {
                binding.tilEmail.error = null
                true
            }
        }
    }

    private fun validatePassword(): Boolean {
        val password = binding.etPassword.text.toString()
        return when {
            password.isEmpty() -> {
                binding.tilPassword.error = "Password is required"
                false
            }
            password.length < 6 -> {
                binding.tilPassword.error = "Password must be at least 6 characters"
                false
            }
            else -> {
                binding.tilPassword.error = null
                true
            }
        }
    }

    private fun validateForm(): Boolean {
        val isEmailValid = validateEmail()
        val isPasswordValid = validatePassword()

        return isEmailValid && isPasswordValid
    }

    private fun signIn() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        binding.btnSignIn.isEnabled = false
        binding.btnSignIn.text = "Signing In..."

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.btnSignIn.isEnabled = true
                binding.btnSignIn.text = "Sign In"

                if (task.isSuccessful) {
                    Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    val errorMessage = when {
                        task.exception?.message?.contains("no user record") == true ||
                        task.exception?.message?.contains("INVALID_LOGIN_CREDENTIALS") == true ->
                            "Invalid email or password"
                        task.exception?.message?.contains("network error") == true ->
                            "Network error. Please check your connection"
                        task.exception?.message?.contains("too many requests") == true ->
                            "Too many failed attempts. Please try again later"
                        else -> task.exception?.message ?: "Sign in failed"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun signInWithGoogle() {
        showGoogleSignInLoading()

        // Try One Tap Sign In first
        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener { result ->
                try {
                    val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                    oneTapSignInLauncher.launch(intentSenderRequest)
                } catch (e: IntentSender.SendIntentException) {
                    android.util.Log.e("GoogleSignIn", "Couldn't start One Tap UI: ${e.message}")
                    // Fall back to traditional sign in
                    signInWithGoogleTraditional()
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("GoogleSignIn", "One Tap Sign In failed: ${e.message}")
                // Fall back to traditional sign in
                signInWithGoogleTraditional()
            }
    }

    private fun signInWithGoogleTraditional() {
        android.util.Log.d("GoogleSignIn", "Using traditional Google Sign In")
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun showGoogleSignInLoading() {
        binding.btnGoogleSignIn.isEnabled = false
        binding.btnGoogleSignIn.text = "Signing in..."
    }

    private fun hideGoogleSignInLoading() {
        binding.btnGoogleSignIn.isEnabled = true
        binding.btnGoogleSignIn.text = "Sign in with Google"
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        showGoogleSignInLoading()
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                hideGoogleSignInLoading()
                if (task.isSuccessful) {
                    Toast.makeText(this, "Welcome!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    val errorMessage = when {
                        task.exception?.message?.contains("network error", ignoreCase = true) == true ->
                            "Network error. Please check your connection"
                        task.exception?.message?.contains("account exists", ignoreCase = true) == true ->
                            "An account with this email already exists with a different sign-in method"
                        else -> "Authentication failed. Please try again"
                    }
                    android.util.Log.e("GoogleSignIn", "Firebase auth failed: ${task.exception?.message}", task.exception)
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun showForgotPasswordDialog() {
        val email = binding.etEmail.text.toString().trim()

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address first", Toast.LENGTH_SHORT).show()
            return
        }

        android.app.AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setMessage("Send password reset email to $email?")
            .setPositiveButton("Send") { _, _ ->
                sendPasswordResetEmail(email)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Password reset email sent! Check your inbox.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val errorMessage = when {
                        task.exception?.message?.contains("no user record") == true ->
                            "No account found with this email"
                        else -> task.exception?.message ?: "Failed to send reset email"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun handleGoogleSignInError(e: ApiException) {
        val errorMessage = when (e.statusCode) {
            CommonStatusCodes.DEVELOPER_ERROR, 10 -> {
                // DEVELOPER_ERROR - Most common cause: SHA-1 certificate not registered in Firebase Console
                android.util.Log.e("GoogleSignIn", "Error 10: DEVELOPER_ERROR - Status: ${e.statusCode}, Message: ${e.message}")
                "Configuration Error: Please ensure your SHA-1 certificate is added to Firebase Console"
            }
            CommonStatusCodes.NETWORK_ERROR, 7 -> {
                // NETWORK_ERROR
                "Network error. Please check your internet connection"
            }
            CommonStatusCodes.CANCELED, 12501 -> {
                // SIGN_IN_CANCELLED - User closed the dialog
                android.util.Log.d("GoogleSignIn", "Sign in cancelled by user")
                null // Don't show error for user cancellation
            }
            12502 -> {
                // SIGN_IN_CURRENTLY_IN_PROGRESS
                "Sign in already in progress. Please wait."
            }
            CommonStatusCodes.INTERNAL_ERROR, 16 -> {
                // INTERNAL_ERROR
                "An internal error occurred. Please try again"
            }
            CommonStatusCodes.API_NOT_CONNECTED, 17 -> {
                // API_NOT_CONNECTED
                "Google Play Services is not available. Please update Google Play Services"
            }
            else -> {
                android.util.Log.e("GoogleSignIn", "Error: ${e.statusCode} - ${e.message}")
                "Sign in failed. Please try again"
            }
        }

        errorMessage?.let {
            Toast.makeText(this, it, Toast.LENGTH_LONG).show()
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
