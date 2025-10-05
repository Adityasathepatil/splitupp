package com.techtool.splitup

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.techtool.splitup.adapters.SplitMemberAdapter
import com.techtool.splitup.databinding.ActivityAddExpenseBinding
import com.techtool.splitup.models.Expense
import com.techtool.splitup.models.Group
import com.techtool.splitup.models.Member
import com.techtool.splitup.models.User

class AddExpenseActivity : BaseActivity() {

    private lateinit var binding: ActivityAddExpenseBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private var selectedGroup: Group? = null
    private var selectedPaidBy: Member? = null
    private val groupMembers = mutableListOf<Member>()
    private val selectedSplitMembers = mutableSetOf<String>()
    private val userGroups = mutableListOf<Group>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Set current user as default payer
        val currentUser = auth.currentUser!!
        selectedPaidBy = Member(
            uid = currentUser.uid,
            name = currentUser.displayName ?: "You",
            email = currentUser.email ?: ""
        )

        setupUI()
        loadUserGroups()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSelectGroup.setOnClickListener {
            if (userGroups.isEmpty()) {
                Toast.makeText(this, "You don't have any groups yet", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showGroupSelectionDialog()
        }

        binding.btnSelectPaidBy.setOnClickListener {
            if (groupMembers.isEmpty()) {
                Toast.makeText(this, "Please select a group first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showPaidBySelectionDialog()
        }

        binding.btnAddExpense.setOnClickListener {
            addExpense()
        }
    }

    private fun loadUserGroups() {
        val currentUser = auth.currentUser ?: return

        database.child("users").child(currentUser.uid).child("groupIds")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val groupIds = mutableListOf<String>()
                    snapshot.children.forEach { child ->
                        child.getValue(String::class.java)?.let { groupIds.add(it) }
                    }

                    if (groupIds.isEmpty()) {
                        return
                    }

                    loadGroups(groupIds)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@AddExpenseActivity,
                        "Error loading groups: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun loadGroups(groupIds: List<String>) {
        userGroups.clear()

        var loadedCount = 0
        groupIds.forEach { groupId ->
            database.child("groups").child(groupId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val group = snapshot.getValue(Group::class.java)
                        if (group != null) {
                            userGroups.add(group)
                        }

                        loadedCount++
                        if (loadedCount == groupIds.size && userGroups.size == 1) {
                            // Auto-select if only one group
                            selectGroup(userGroups[0])
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        loadedCount++
                    }
                })
        }
    }

    private fun showGroupSelectionDialog() {
        val groupNames = userGroups.map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select Group")
            .setItems(groupNames) { _, which ->
                selectGroup(userGroups[which])
            }
            .show()
    }

    private fun selectGroup(group: Group) {
        selectedGroup = group
        binding.btnSelectGroup.text = group.name

        // Load group members
        loadGroupMembers(group)
    }

    private fun loadGroupMembers(group: Group) {
        groupMembers.clear()
        selectedSplitMembers.clear()

        var loadedCount = 0
        group.memberIds.forEach { memberId ->
            database.child("users").child(memberId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user = snapshot.getValue(User::class.java)
                        if (user != null) {
                            groupMembers.add(Member(user.uid, user.name, user.email))
                            // Auto-select all members
                            selectedSplitMembers.add(user.uid)
                        }

                        loadedCount++
                        if (loadedCount == group.memberIds.size) {
                            setupSplitMembersRecyclerView()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        loadedCount++
                        if (loadedCount == group.memberIds.size) {
                            setupSplitMembersRecyclerView()
                        }
                    }
                })
        }
    }

    private fun setupSplitMembersRecyclerView() {
        val adapter = SplitMemberAdapter(groupMembers, selectedSplitMembers)
        binding.rvSplitAmong.apply {
            layoutManager = LinearLayoutManager(this@AddExpenseActivity)
            this.adapter = adapter
        }
    }

    private fun showPaidBySelectionDialog() {
        val memberNames = groupMembers.map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Paid By")
            .setItems(memberNames) { _, which ->
                selectedPaidBy = groupMembers[which]
                binding.btnSelectPaidBy.text = selectedPaidBy?.name ?: "Select member"
            }
            .show()
    }

    private fun addExpense() {
        val description = binding.etDescription.text.toString().trim()
        val amountStr = binding.etAmount.text.toString().trim()

        if (description.isEmpty()) {
            binding.tilDescription.error = "Description is required"
            return
        }

        if (amountStr.isEmpty()) {
            binding.tilAmount.error = "Amount is required"
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            binding.tilAmount.error = "Please enter a valid amount"
            return
        }

        if (selectedGroup == null) {
            Toast.makeText(this, "Please select a group", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedPaidBy == null) {
            Toast.makeText(this, "Please select who paid", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedSplitMembers.isEmpty()) {
            Toast.makeText(this, "Please select at least one person to split with", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnAddExpense.isEnabled = false
        binding.btnAddExpense.text = "Adding..."

        val expenseId = database.child("expenses").push().key!!
        val expense = Expense(
            id = expenseId,
            groupId = selectedGroup!!.id,
            description = description,
            amount = amount,
            paidBy = selectedPaidBy!!.uid,
            splitAmong = selectedSplitMembers.toList()
        )

        database.child("expenses").child(expenseId).setValue(expense.toMap())
            .addOnSuccessListener {
                // Add expense to group's expense list
                val updatedExpenseIds = selectedGroup!!.expenseIds.toMutableList()
                updatedExpenseIds.add(expenseId)

                database.child("groups").child(selectedGroup!!.id).child("expenseIds")
                    .setValue(updatedExpenseIds)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Expense added successfully!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        binding.btnAddExpense.isEnabled = true
                        binding.btnAddExpense.text = "Add Expense"
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                binding.btnAddExpense.isEnabled = true
                binding.btnAddExpense.text = "Add Expense"
                Toast.makeText(this, "Error adding expense: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
