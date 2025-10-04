package com.techtool.splitup

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.techtool.splitup.adapters.AllGroupsAdapter
import com.techtool.splitup.databinding.ActivityAllGroupsBinding
import com.techtool.splitup.models.Group
import com.techtool.splitup.models.Member
import com.techtool.splitup.models.User

class AllGroupsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAllGroupsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var groupAdapter: AllGroupsAdapter

    private val groups = mutableListOf<Pair<Group, List<Member>>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllGroupsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        setupUI()
        setupRecyclerView()
        loadUserGroups()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        groupAdapter = AllGroupsAdapter(groups) { group ->
            // Open group details
            val intent = Intent(this, GroupDetailsActivity::class.java)
            intent.putExtra(GroupDetailsActivity.EXTRA_GROUP_ID, group.id)
            startActivity(intent)
        }

        binding.rvAllGroups.apply {
            layoutManager = GridLayoutManager(this@AllGroupsActivity, 2)
            adapter = groupAdapter
        }
    }

    private fun loadUserGroups() {
        val currentUser = auth.currentUser ?: return

        database.child("users").child(currentUser.uid).child("groupIds")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val groupIds = mutableListOf<String>()
                    snapshot.children.forEach { child ->
                        child.getValue(String::class.java)?.let { groupIds.add(it) }
                    }

                    if (groupIds.isEmpty()) {
                        groups.clear()
                        groupAdapter.updateGroups(groups)
                        return
                    }

                    loadGroups(groupIds)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@AllGroupsActivity,
                        "Error loading groups: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun loadGroups(groupIds: List<String>) {
        groups.clear()

        groupIds.forEach { groupId ->
            database.child("groups").child(groupId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val group = snapshot.getValue(Group::class.java)
                        if (group != null) {
                            loadGroupMembers(group)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(
                            this@AllGroupsActivity,
                            "Error loading group: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }
    }

    private fun loadGroupMembers(group: Group) {
        val members = mutableListOf<Member>()

        if (group.memberIds.isEmpty()) {
            groups.add(Pair(group, members))
            groupAdapter.updateGroups(groups)
            return
        }

        var loadedCount = 0
        group.memberIds.forEach { memberId ->
            database.child("users").child(memberId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user = snapshot.getValue(User::class.java)
                        if (user != null) {
                            members.add(Member(user.uid, user.name, user.email))
                        }

                        loadedCount++
                        if (loadedCount == group.memberIds.size) {
                            groups.add(Pair(group, members))
                            groupAdapter.updateGroups(groups)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        loadedCount++
                        if (loadedCount == group.memberIds.size) {
                            groups.add(Pair(group, members))
                            groupAdapter.updateGroups(groups)
                        }
                    }
                })
        }
    }
}
