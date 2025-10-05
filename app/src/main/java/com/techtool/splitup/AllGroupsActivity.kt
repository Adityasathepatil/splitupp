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
import com.techtool.splitup.models.Expense
import com.techtool.splitup.models.Group
import com.techtool.splitup.models.Member
import com.techtool.splitup.models.User

class AllGroupsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAllGroupsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var groupAdapter: AllGroupsAdapter

    private val groups = mutableListOf<Pair<Group, List<Member>>>()
    private val allExpenses = mutableMapOf<String, Expense>()
    private val expenseListeners = mutableMapOf<String, ValueEventListener>()
    private val groupListeners = mutableMapOf<String, ValueEventListener>()

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

    override fun onDestroy() {
        super.onDestroy()
        // Clean up listeners
        expenseListeners.forEach { (expenseId, listener) ->
            database.child("expenses").child(expenseId).removeEventListener(listener)
        }
        groupListeners.forEach { (groupId, listener) ->
            database.child("groups").child(groupId).removeEventListener(listener)
        }
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        groupAdapter = AllGroupsAdapter(groups, allExpenses) { group ->
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
        // Clean up old listeners
        groupListeners.forEach { (groupId, listener) ->
            if (groupId !in groupIds) {
                database.child("groups").child(groupId).removeEventListener(listener)
                groupListeners.remove(groupId)
            }
        }

        groups.clear()
        var processedGroups = 0

        groupIds.forEach { groupId ->
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val group = snapshot.getValue(Group::class.java)
                    if (group != null) {
                        loadGroupMembers(group) {
                            processedGroups++
                            if (processedGroups == groupIds.size) {
                                loadAllExpenses()
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@AllGroupsActivity,
                        "Error loading group: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            groupListeners[groupId] = listener
            database.child("groups").child(groupId).addValueEventListener(listener)
        }
    }

    private fun loadGroupMembers(group: Group, onComplete: () -> Unit) {
        val members = mutableListOf<Member>()

        if (group.memberIds.isEmpty()) {
            updateGroupInList(group, members)
            onComplete()
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
                            updateGroupInList(group, members)
                            onComplete()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        loadedCount++
                        if (loadedCount == group.memberIds.size) {
                            updateGroupInList(group, members)
                            onComplete()
                        }
                    }
                })
        }
    }

    private fun updateGroupInList(group: Group, members: List<Member>) {
        // Remove old group with same id
        groups.removeAll { it.first.id == group.id }

        // Add updated group
        groups.add(Pair(group, members))

        // Update UI with expenses
        groupAdapter.updateGroups(groups, allExpenses)
    }

    private fun loadAllExpenses() {
        // Clean up old expense listeners
        val allExpenseIds = groups.flatMap { it.first.expenseIds }.toSet()
        expenseListeners.keys.filter { it !in allExpenseIds }.forEach { expenseId ->
            expenseListeners[expenseId]?.let { listener ->
                database.child("expenses").child(expenseId).removeEventListener(listener)
            }
            expenseListeners.remove(expenseId)
            allExpenses.remove(expenseId)
        }

        if (groups.isEmpty()) {
            return
        }

        // Load expenses from all groups
        val expenseIds = groups.flatMap { it.first.expenseIds }.toSet()

        if (expenseIds.isEmpty()) {
            allExpenses.clear()
            groupAdapter.updateGroups(groups, allExpenses)
            return
        }

        expenseIds.forEach { expenseId ->
            if (!expenseListeners.containsKey(expenseId)) {
                val listener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val expense = snapshot.getValue(Expense::class.java)

                        if (expense != null && !expense.isSettled) {
                            allExpenses[expenseId] = expense
                        } else {
                            allExpenses.remove(expenseId)
                        }

                        // Update UI on every expense change
                        groupAdapter.updateGroups(groups, allExpenses)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle error
                    }
                }

                expenseListeners[expenseId] = listener
                database.child("expenses").child(expenseId).addValueEventListener(listener)
            }
        }
    }
}
