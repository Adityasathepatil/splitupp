package com.techtool.splitup

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.techtool.splitup.adapters.GroupAdapter
import com.techtool.splitup.databinding.ActivityHomeBinding
import com.techtool.splitup.models.Group
import com.techtool.splitup.models.Member
import com.techtool.splitup.models.User
import com.techtool.splitup.models.Expense
import com.techtool.splitup.dialogs.GroupActionDialog

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var groupAdapter: GroupAdapter

    private val groups = mutableListOf<Pair<Group, List<Member>>>()
    private val allExpenses = mutableMapOf<String, Expense>()
    private val expenseListeners = mutableMapOf<String, ValueEventListener>()
    private val groupListeners = mutableMapOf<String, ValueEventListener>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        setupUI()
        setupRecyclerView()
        loadUserData()
        loadUserGroups()
    }

    override fun onResume() {
        super.onResume()
        // Reload groups when returning to this activity
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
        val currentUser = auth.currentUser
        val userName = currentUser?.displayName ?: "User"
        val userInitial = userName.firstOrNull()?.uppercase() ?: "U"

        binding.tvGreeting.text = "Hi $userName 👋"
        binding.tvUserInitial.text = userInitial

        binding.cardCreateGroup.setOnClickListener {
            GroupActionDialog(this) {
                loadUserGroups()
            }.show()
        }

        binding.cardAddExpense.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }

        binding.cardSettleUp.setOnClickListener {
            startActivity(Intent(this, SettleUpActivity::class.java))
        }

        binding.tvViewAll.setOnClickListener {
            startActivity(Intent(this, AllGroupsActivity::class.java))
        }

        binding.tvUserInitial.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        groupAdapter = GroupAdapter(groups, allExpenses) { group ->
            // Open group details
            val intent = Intent(this, GroupDetailsActivity::class.java)
            intent.putExtra(GroupDetailsActivity.EXTRA_GROUP_ID, group.id)
            startActivity(intent)
        }

        binding.rvGroups.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = groupAdapter
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser ?: return

        // Create or update user in database
        val userRef = database.child("users").child(currentUser.uid)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    // Create new user
                    val user = User(
                        uid = currentUser.uid,
                        name = currentUser.displayName ?: "",
                        email = currentUser.email ?: ""
                    )
                    userRef.setValue(user.toMap())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@HomeActivity,
                    "Error loading user data: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun loadUserGroups() {
        val currentUser = auth.currentUser ?: return

        // Listen for user's groups
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
                        displayBalances(0.0, 0.0)
                        return
                    }

                    // Load groups
                    loadGroups(groupIds)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@HomeActivity,
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
                        this@HomeActivity,
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
            // Update or add group to list
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
        groupAdapter.updateGroups(groups.take(3), allExpenses) // Show only first 3
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
            calculateBalances()
            return
        }

        // Load expenses from all groups
        val expenseIds = groups.flatMap { it.first.expenseIds }.toSet()

        if (expenseIds.isEmpty()) {
            allExpenses.clear()
            calculateBalances()
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

                        // Recalculate balances on every expense change
                        calculateBalances()
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

    private fun calculateBalances() {
        val currentUserId = auth.currentUser?.uid ?: return
        var amountOwed = 0.0  // Others owe me
        var amountOwe = 0.0   // I owe others

        allExpenses.values.forEach { expense ->
            val splitAmount = expense.amount / expense.splitAmong.size

            if (expense.paidBy == currentUserId) {
                // I paid, so others owe me
                expense.splitAmong.forEach { memberId ->
                    if (memberId != currentUserId) {
                        amountOwed += splitAmount
                    }
                }
            } else if (expense.splitAmong.contains(currentUserId)) {
                // Someone else paid and I'm in the split
                amountOwe += splitAmount
            }
        }

        // Update group totals in real-time
        groupAdapter.updateGroups(groups.take(3), allExpenses)

        displayBalances(amountOwed, amountOwe)
    }

    private fun displayBalances(amountOwed: Double, amountOwe: Double) {
        val netBalance = amountOwed - amountOwe

        binding.tvAmountOwed.text = "₹%.2f".format(amountOwed)
        binding.tvAmountOwe.text = "₹%.2f".format(amountOwe)
        binding.tvNetBalance.text = "₹%.2f".format(kotlin.math.abs(netBalance))

        // Set color based on net balance
        binding.tvNetBalance.setTextColor(
            if (netBalance >= 0) {
                android.graphics.Color.parseColor("#10B981") // Green
            } else {
                android.graphics.Color.parseColor("#EF4444") // Red
            }
        )
    }
}
