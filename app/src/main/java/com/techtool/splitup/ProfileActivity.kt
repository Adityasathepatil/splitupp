package com.techtool.splitup

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.techtool.splitup.databinding.ActivityProfileBinding
import com.techtool.splitup.databinding.DialogAddPhoneBinding
import com.techtool.splitup.models.User

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        setupUI()
        loadUserData()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.cardPhone.setOnClickListener {
            showAddPhoneDialog()
        }

        binding.cardAppSettings.setOnClickListener {
            Toast.makeText(this, "App Settings", Toast.LENGTH_SHORT).show()
        }

        binding.cardLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun loadUserData() {
        val firebaseUser = auth.currentUser ?: return

        database.child("users").child(firebaseUser.uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    currentUser = snapshot.getValue(User::class.java)

                    if (currentUser != null) {
                        updateUI(currentUser!!)
                    } else {
                        // User doesn't exist in database, create one
                        val newUser = User(
                            uid = firebaseUser.uid,
                            name = firebaseUser.displayName ?: "",
                            email = firebaseUser.email ?: "",
                            phone = ""
                        )
                        database.child("users").child(firebaseUser.uid).setValue(newUser.toMap())
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Error loading user data: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })

        // Load total groups count
        database.child("users").child(firebaseUser.uid).child("groupIds")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val groupCount = snapshot.childrenCount.toInt()
                    binding.tvGroupsValue.text = "$groupCount Active Groups"
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    private fun updateUI(user: User) {
        // Update user initial
        val userInitial = user.name.firstOrNull()?.uppercase() ?: "U"
        binding.tvUserInitial.text = userInitial

        // Update user name
        binding.tvUserName.text = user.name

        // Update email
        binding.tvUserEmail.text = user.email
        binding.tvEmailValue.text = user.email

        // Update phone
        if (user.phone.isNotEmpty()) {
            binding.tvPhoneValue.text = user.phone
        } else {
            binding.tvPhoneValue.text = "Add phone number"
            binding.tvPhoneValue.setTextColor(getColor(R.color.profile_text_secondary))
        }
    }

    private fun showAddPhoneDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val dialogBinding = DialogAddPhoneBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        // Pre-fill current phone number if exists
        currentUser?.phone?.let {
            if (it.isNotEmpty()) {
                dialogBinding.etPhone.setText(it)
            }
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSave.setOnClickListener {
            val phoneNumber = dialogBinding.etPhone.text.toString().trim()

            if (phoneNumber.isEmpty()) {
                Toast.makeText(this, "Please enter a phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (phoneNumber.length < 10) {
                Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Update phone number in database
            val firebaseUser = auth.currentUser ?: return@setOnClickListener
            database.child("users").child(firebaseUser.uid).child("phone")
                .setValue(phoneNumber)
                .addOnSuccessListener {
                    Toast.makeText(this, "Phone number updated successfully", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .addOnFailureListener { error ->
                    Toast.makeText(
                        this,
                        "Error updating phone number: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        dialog.show()
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        // Sign out from Firebase
        auth.signOut()

        // Sign out from Google
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInClient.signOut().addOnCompleteListener {
            // Navigate to SignIn activity
            val intent = Intent(this, SignInActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
