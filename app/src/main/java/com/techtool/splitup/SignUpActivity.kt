package com.techtool.splitup

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.techtool.splitup.databinding.ActivitySignUpBinding

class SignUpActivity : BaseActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Configure Google Sign In
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

        binding.btnCreateAccount.setOnClickListener {
            if (validateForm()) {
                createAccount()
            }
        }

        binding.tvSignIn.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }
    }

    private fun setupValidation() {
        binding.etFullName.doAfterTextChanged { validateFullName() }
        binding.etEmail.doAfterTextChanged { validateEmail() }
        binding.etPassword.doAfterTextChanged { validatePassword() }
        binding.etConfirmPassword.doAfterTextChanged { validateConfirmPassword() }
    }

    private fun validateFullName(): Boolean {
        val name = binding.etFullName.text.toString().trim()
        return when {
            name.isEmpty() -> {
                binding.tilFullName.error = "Full name is required"
                false
            }
            name.length < 2 -> {
                binding.tilFullName.error = "Name must be at least 2 characters"
                false
            }
            else -> {
                binding.tilFullName.error = null
                true
            }
        }
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
            !password.matches(Regex(".*[A-Z].*")) -> {
                binding.tilPassword.error = "Password must contain at least one uppercase letter"
                false
            }
            !password.matches(Regex(".*[a-z].*")) -> {
                binding.tilPassword.error = "Password must contain at least one lowercase letter"
                false
            }
            !password.matches(Regex(".*[0-9].*")) -> {
                binding.tilPassword.error = "Password must contain at least one digit"
                false
            }
            else -> {
                binding.tilPassword.error = null
                true
            }
        }
    }

    private fun validateConfirmPassword(): Boolean {
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()
        return when {
            confirmPassword.isEmpty() -> {
                binding.tilConfirmPassword.error = "Please confirm your password"
                false
            }
            confirmPassword != password -> {
                binding.tilConfirmPassword.error = "Passwords do not match"
                false
            }
            else -> {
                binding.tilConfirmPassword.error = null
                true
            }
        }
    }

    private fun validateForm(): Boolean {
        val isNameValid = validateFullName()
        val isEmailValid = validateEmail()
        val isPasswordValid = validatePassword()
        val isConfirmPasswordValid = validateConfirmPassword()

        return isNameValid && isEmailValid && isPasswordValid && isConfirmPasswordValid
    }

    private fun createAccount() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        binding.btnCreateAccount.isEnabled = false
        binding.btnCreateAccount.text = "Creating Account..."

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.btnCreateAccount.isEnabled = true
                binding.btnCreateAccount.text = "Create Account"

                if (task.isSuccessful) {
                    // Update display name
                    val user = auth.currentUser
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(binding.etFullName.text.toString().trim())
                        .build()

                    user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                        Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                        navigateToMain()
                    }
                } else {
                    val errorMessage = when {
                        task.exception?.message?.contains("email address is already in use") == true ->
                            "This email is already registered"
                        task.exception?.message?.contains("network error") == true ->
                            "Network error. Please check your connection"
                        else -> task.exception?.message ?: "Registration failed"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Google sign in successful!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
