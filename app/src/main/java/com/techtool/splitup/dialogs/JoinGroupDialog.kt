package com.techtool.splitup.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.techtool.splitup.databinding.DialogJoinGroupBinding
import com.techtool.splitup.models.Group

class JoinGroupDialog(
    context: Context,
    private val onGroupJoined: () -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogJoinGroupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogJoinGroupBinding.inflate(layoutInflater)
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

        binding.btnJoinGroup.setOnClickListener {
            joinGroup()
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
                                            onGroupJoined()
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
