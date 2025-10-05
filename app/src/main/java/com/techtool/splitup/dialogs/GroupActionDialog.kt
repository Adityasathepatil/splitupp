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
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.techtool.splitup.databinding.DialogGroupActionBinding
import com.techtool.splitup.models.Group
import java.util.*

class GroupActionDialog(
    context: Context,
    private val onGroupAction: () -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogGroupActionBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogGroupActionBinding.inflate(layoutInflater)
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

        // Tab switching
        binding.btnTabCreate.setOnClickListener {
            showCreateTab()
        }

        binding.btnTabJoin.setOnClickListener {
            showJoinTab()
        }

        // Create group actions
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

        // Join group actions
        binding.btnJoinGroup.setOnClickListener {
            joinGroup()
        }
    }

    private fun showCreateTab() {
        // Update tab UI
        binding.btnTabCreate.setCardBackgroundColor(context.getColor(android.R.color.transparent).let {
            android.graphics.Color.parseColor("#6366F1")
        })
        binding.btnTabJoin.setCardBackgroundColor(android.graphics.Color.TRANSPARENT)

        // Show/hide content
        binding.layoutCreateContent.visibility = View.VISIBLE
        binding.layoutJoinContent.visibility = View.GONE
    }

    private fun showJoinTab() {
        // Update tab UI
        binding.btnTabCreate.setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
        binding.btnTabJoin.setCardBackgroundColor(context.getColor(android.R.color.transparent).let {
            android.graphics.Color.parseColor("#6366F1")
        })

        // Show/hide content
        binding.layoutCreateContent.visibility = View.GONE
        binding.layoutJoinContent.visibility = View.VISIBLE
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
            onGroupAction()
            dismiss()
        }
    }

    private fun joinGroup() {
        val inviteCode = binding.etInviteCode.text.toString().trim().uppercase()

        if (inviteCode.isEmpty()) {
            binding.tilInviteCode.error = "Invite code is required"
            return
        }

        if (inviteCode.length != 6) {
            binding.tilInviteCode.error = "Invite code must be 6 characters"
            return
        }

        binding.btnJoinGroup.isEnabled = false
        binding.btnJoinGroup.text = "Joining..."

        // Search for group with this invite code
        database.child("groups")
            .orderByChild("inviteCode")
            .equalTo(inviteCode)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        binding.btnJoinGroup.isEnabled = true
                        binding.btnJoinGroup.text = "Join Group"
                        binding.tilInviteCode.error = "Invalid invite code"
                        return
                    }

                    // Get the group
                    val groupSnapshot = snapshot.children.first()
                    val group = groupSnapshot.getValue(Group::class.java)

                    if (group == null) {
                        binding.btnJoinGroup.isEnabled = true
                        binding.btnJoinGroup.text = "Join Group"
                        Toast.makeText(context, "Error loading group", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val currentUser = auth.currentUser!!

                    // Check if user is already a member
                    if (group.memberIds.contains(currentUser.uid)) {
                        binding.btnJoinGroup.isEnabled = true
                        binding.btnJoinGroup.text = "Join Group"
                        Toast.makeText(context, "You are already in this group", Toast.LENGTH_SHORT)
                            .show()
                        dismiss()
                        return
                    }

                    // Add user to group
                    val updatedMemberIds = group.memberIds.toMutableList()
                    updatedMemberIds.add(currentUser.uid)

                    database.child("groups").child(group.id).child("memberIds")
                        .setValue(updatedMemberIds)
                        .addOnSuccessListener {
                            // Add group to user's groups
                            database.child("users").child(currentUser.uid).child("groupIds")
                                .get()
                                .addOnSuccessListener { userSnapshot ->
                                    val groupIds = mutableListOf<String>()
                                    userSnapshot.children.forEach { child ->
                                        child.getValue(String::class.java)?.let { groupIds.add(it) }
                                    }
                                    groupIds.add(group.id)

                                    database.child("users").child(currentUser.uid)
                                        .child("groupIds")
                                        .setValue(groupIds)
                                        .addOnSuccessListener {
                                            Toast.makeText(
                                                context,
                                                "Joined ${group.name} successfully!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            onGroupAction()
                                            dismiss()
                                        }
                                        .addOnFailureListener { e ->
                                            binding.btnJoinGroup.isEnabled = true
                                            binding.btnJoinGroup.text = "Join Group"
                                            Toast.makeText(
                                                context,
                                                "Error: ${e.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }
                        }
                        .addOnFailureListener { e ->
                            binding.btnJoinGroup.isEnabled = true
                            binding.btnJoinGroup.text = "Join Group"
                            Toast.makeText(
                                context,
                                "Error joining group: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.btnJoinGroup.isEnabled = true
                    binding.btnJoinGroup.text = "Join Group"
                    Toast.makeText(
                        context,
                        "Error: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}
