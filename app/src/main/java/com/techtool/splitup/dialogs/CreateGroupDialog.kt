package com.techtool.splitup.dialogs

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.techtool.splitup.databinding.DialogCreateGroupBinding
import com.techtool.splitup.models.Group
import java.util.*

class CreateGroupDialog(
    context: Context,
    private val onGroupCreated: () -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogCreateGroupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogCreateGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window?.setBackgroundDrawableResource(android.R.color.transparent)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        setupUI()
    }

    private fun setupUI() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.btnCreateGroup.setOnClickListener {
            createGroup()
        }

        binding.btnCopyCode.setOnClickListener {
            val code = binding.tvGroupCode.text.toString()
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Group Code", code)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createGroup() {
        val groupName = binding.etGroupName.text.toString().trim()

        if (groupName.isEmpty()) {
            binding.etGroupName.error = "Group name is required"
            return
        }

        binding.btnCreateGroup.isEnabled = false
        binding.btnCreateGroup.text = "Creating..."

        val currentUser = auth.currentUser!!
        val groupId = database.child("groups").push().key!!
        val inviteCode = generateInviteCode()

        // Get all member IDs (only current user for now, others can join via invite code)
        val memberIds = listOf(currentUser.uid)

        val group = Group(
            id = groupId,
            name = groupName,
            inviteCode = inviteCode,
            createdBy = currentUser.uid,
            memberIds = memberIds
        )

        // Save group to database
        database.child("groups").child(groupId).setValue(group.toMap())
            .addOnSuccessListener {
                // Add group to user's groups
                database.child("users").child(currentUser.uid).child("groupIds")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val groupIds = mutableListOf<String>()
                        snapshot.children.forEach { child ->
                            child.getValue(String::class.java)?.let { groupIds.add(it) }
                        }
                        groupIds.add(groupId)

                        database.child("users").child(currentUser.uid).child("groupIds")
                            .setValue(groupIds)
                            .addOnSuccessListener {
                                // Show generated code
                                showGeneratedCode(inviteCode)
                            }
                            .addOnFailureListener { e ->
                                binding.btnCreateGroup.isEnabled = true
                                binding.btnCreateGroup.text = "Create Group"
                                Toast.makeText(
                                    context,
                                    "Error: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
            }
            .addOnFailureListener { e ->
                binding.btnCreateGroup.isEnabled = true
                binding.btnCreateGroup.text = "Create Group"
                Toast.makeText(context, "Error creating group: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6)
            .map { chars.random() }
            .joinToString("")
    }

    private fun showGeneratedCode(code: String) {
        binding.tvGroupCode.text = code
        binding.cvGeneratedCode.visibility = View.VISIBLE
        binding.btnCreateGroup.text = "Done"
        binding.btnCreateGroup.setOnClickListener {
            onGroupCreated()
            dismiss()
        }
    }
}
