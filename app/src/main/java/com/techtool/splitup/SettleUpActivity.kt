package com.techtool.splitup

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.techtool.splitup.adapters.SettlementAdapter
import com.techtool.splitup.databinding.ActivitySettleUpBinding
import com.techtool.splitup.models.*

class SettleUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettleUpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private val userGroups = mutableListOf<Group>()
    private var selectedGroup: Group? = null
    private val settlements = mutableListOf<Settlement>()
    private val groupMembers = mutableListOf<Member>()
    private val expenses = mutableListOf<Expense>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettleUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

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

        binding.btnSettleUp.setOnClickListener {
            settleUpGroup()
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
                        this@SettleUpActivity,
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

        loadGroupData(group)
    }

    private fun loadGroupData(group: Group) {
        groupMembers.clear()
        expenses.clear()

        // Load members
        var membersLoaded = 0
        group.memberIds.forEach { memberId ->
            database.child("users").child(memberId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user = snapshot.getValue(User::class.java)
                        if (user != null) {
                            groupMembers.add(Member(user.uid, user.name, user.email))
                        }

                        membersLoaded++
                        if (membersLoaded == group.memberIds.size) {
                            loadExpenses(group)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        membersLoaded++
                        if (membersLoaded == group.memberIds.size) {
                            loadExpenses(group)
                        }
                    }
                })
        }

        if (group.memberIds.isEmpty()) {
            calculateSettlements()
        }
    }

    private fun loadExpenses(group: Group) {
        if (group.expenseIds.isEmpty()) {
            calculateSettlements()
            return
        }

        var expensesLoaded = 0
        group.expenseIds.forEach { expenseId ->
            database.child("expenses").child(expenseId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val expense = snapshot.getValue(Expense::class.java)
                        if (expense != null && !expense.isSettled) {
                            expenses.add(expense)
                        }

                        expensesLoaded++
                        if (expensesLoaded == group.expenseIds.size) {
                            calculateSettlements()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        expensesLoaded++
                        if (expensesLoaded == group.expenseIds.size) {
                            calculateSettlements()
                        }
                    }
                })
        }
    }

    private fun calculateSettlements() {
        settlements.clear()

        // Calculate balances for each member
        val balances = mutableMapOf<String, Double>()
        groupMembers.forEach { member ->
            balances[member.uid] = 0.0
        }

        // Process expenses
        expenses.forEach { expense ->
            val splitAmount = expense.amount / expense.splitAmong.size

            // The payer should receive money
            balances[expense.paidBy] = (balances[expense.paidBy] ?: 0.0) + expense.amount

            // Each person in split should pay
            expense.splitAmong.forEach { memberId ->
                balances[memberId] = (balances[memberId] ?: 0.0) - splitAmount
            }
        }

        // Simplify settlements using greedy algorithm
        val debtors = balances.filter { it.value < -0.01 }.toMutableMap()
        val creditors = balances.filter { it.value > 0.01 }.toMutableMap()

        while (debtors.isNotEmpty() && creditors.isNotEmpty()) {
            val (debtorId, debtAmount) = debtors.entries.first()
            val (creditorId, creditAmount) = creditors.entries.first()

            val settleAmount = minOf(-debtAmount, creditAmount)

            val debtor = groupMembers.find { it.uid == debtorId }
            val creditor = groupMembers.find { it.uid == creditorId }

            if (debtor != null && creditor != null) {
                settlements.add(Settlement(debtor, creditor, settleAmount))
            }

            debtors[debtorId] = debtAmount + settleAmount
            creditors[creditorId] = creditAmount - settleAmount

            if (debtors[debtorId]!! >= -0.01) debtors.remove(debtorId)
            if (creditors[creditorId]!! <= 0.01) creditors.remove(creditorId)
        }

        displaySettlements()
    }

    private fun displaySettlements() {
        if (settlements.isEmpty()) {
            binding.tvSummaryTitle.visibility = View.VISIBLE
            binding.tvSummaryTitle.text = "All settled up! 🎉"
            binding.rvSettlements.visibility = View.GONE
            binding.btnSettleUp.visibility = View.GONE
        } else {
            binding.tvSummaryTitle.visibility = View.VISIBLE
            binding.tvSummaryTitle.text = "Who owes whom?"
            binding.rvSettlements.visibility = View.VISIBLE
            binding.btnSettleUp.visibility = View.VISIBLE

            val adapter = SettlementAdapter(settlements)
            binding.rvSettlements.apply {
                layoutManager = LinearLayoutManager(this@SettleUpActivity)
                this.adapter = adapter
            }
        }
    }

    private fun settleUpGroup() {
        if (expenses.isEmpty()) {
            Toast.makeText(this, "No expenses to settle", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSettleUp.isEnabled = false
        binding.btnSettleUp.text = "Settling..."

        // Mark all expenses as settled
        var settledCount = 0
        expenses.forEach { expense ->
            database.child("expenses").child(expense.id).child("isSettled")
                .setValue(true)
                .addOnSuccessListener {
                    settledCount++
                    if (settledCount == expenses.size) {
                        Toast.makeText(
                            this,
                            "Successfully settled all expenses!",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
                .addOnFailureListener { e ->
                    binding.btnSettleUp.isEnabled = true
                    binding.btnSettleUp.text = "Mark as Settled"
                    Toast.makeText(
                        this,
                        "Error settling: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }
}
