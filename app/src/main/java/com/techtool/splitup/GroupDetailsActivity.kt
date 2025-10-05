package com.techtool.splitup

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.techtool.splitup.adapters.MemberAdapter
import com.techtool.splitup.adapters.TransactionAdapter
import com.techtool.splitup.databinding.ActivityGroupDetailsBinding
import com.techtool.splitup.models.Expense
import com.techtool.splitup.models.Group
import com.techtool.splitup.models.Member
import com.techtool.splitup.models.SettlementRecord
import com.techtool.splitup.models.TransactionItem
import com.techtool.splitup.models.User

class GroupDetailsActivity : BaseActivity() {

    private lateinit var binding: ActivityGroupDetailsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private var group: Group? = null
    private val members = mutableListOf<Member>()
    private val expenses = mutableMapOf<String, Expense>()
    private val settlements = mutableMapOf<String, SettlementRecord>()
    private lateinit var transactionAdapter: TransactionAdapter
    private val expenseListeners = mutableMapOf<String, ValueEventListener>()
    private val settlementListeners = mutableMapOf<String, ValueEventListener>()
    private var groupListener: ValueEventListener? = null

    companion object {
        const val EXTRA_GROUP_ID = "group_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        val groupId = intent.getStringExtra(EXTRA_GROUP_ID)
        if (groupId == null) {
            Toast.makeText(this, "Error: Group not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        loadGroup(groupId)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up listeners
        val groupId = intent.getStringExtra(EXTRA_GROUP_ID)
        if (groupId != null && groupListener != null) {
            database.child("groups").child(groupId).removeEventListener(groupListener!!)
        }
        expenseListeners.forEach { (expenseId, listener) ->
            database.child("expenses").child(expenseId).removeEventListener(listener)
        }
        settlementListeners.forEach { (settlementId, listener) ->
            database.child("settlements").child(settlementId).removeEventListener(listener)
        }
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnCopyCode.setOnClickListener {
            val code = binding.tvGroupCode.text.toString()
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Group Code", code)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadGroup(groupId: String) {
        groupListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                group = snapshot.getValue(Group::class.java)
                if (group != null) {
                    displayGroupInfo(group!!)
                    loadMembers(group!!)
                    loadExpenses(group!!)
                    loadSettlements(group!!)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@GroupDetailsActivity,
                    "Error loading group: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        database.child("groups").child(groupId).addValueEventListener(groupListener!!)
    }

    private fun displayGroupInfo(group: Group) {
        binding.tvGroupName.text = group.name
        binding.tvGroupCode.text = group.inviteCode
    }

    private fun loadMembers(group: Group) {
        members.clear()

        if (group.memberIds.isEmpty()) {
            setupMembersRecyclerView()
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
                            setupMembersRecyclerView()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        loadedCount++
                        if (loadedCount == group.memberIds.size) {
                            setupMembersRecyclerView()
                        }
                    }
                })
        }
    }

    private fun setupMembersRecyclerView() {
        val currentUserId = auth.currentUser?.uid ?: ""
        val adapter = MemberAdapter(members, currentUserId) { }
        binding.rvMembers.apply {
            layoutManager = LinearLayoutManager(this@GroupDetailsActivity)
            this.adapter = adapter
        }
    }

    private fun loadExpenses(group: Group) {
        // Clean up old listeners
        val currentExpenseIds = group.expenseIds.toSet()
        expenseListeners.keys.filter { it !in currentExpenseIds }.forEach { expenseId ->
            expenseListeners[expenseId]?.let { listener ->
                database.child("expenses").child(expenseId).removeEventListener(listener)
            }
            expenseListeners.remove(expenseId)
            expenses.remove(expenseId)
        }

        if (group.expenseIds.isEmpty()) {
            expenses.clear()
            updateTransactionsView()
            return
        }

        group.expenseIds.forEach { expenseId ->
            if (!expenseListeners.containsKey(expenseId)) {
                val listener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val expense = snapshot.getValue(Expense::class.java)

                        if (expense != null && !expense.isSettled) {
                            expenses[expenseId] = expense
                        } else {
                            expenses.remove(expenseId)
                        }

                        // Update UI on every change
                        updateTransactionsView()
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

    private fun loadSettlements(group: Group) {
        // Clean up old listeners
        val currentSettlementIds = group.settlementIds.toSet()
        settlementListeners.keys.filter { it !in currentSettlementIds }.forEach { settlementId ->
            settlementListeners[settlementId]?.let { listener ->
                database.child("settlements").child(settlementId).removeEventListener(listener)
            }
            settlementListeners.remove(settlementId)
            settlements.remove(settlementId)
        }

        if (group.settlementIds.isEmpty()) {
            settlements.clear()
            updateTransactionsView()
            return
        }

        group.settlementIds.forEach { settlementId ->
            if (!settlementListeners.containsKey(settlementId)) {
                val listener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val settlement = snapshot.getValue(SettlementRecord::class.java)

                        if (settlement != null) {
                            settlements[settlementId] = settlement
                        } else {
                            settlements.remove(settlementId)
                        }

                        // Update UI on every change
                        updateTransactionsView()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle error
                    }
                }

                settlementListeners[settlementId] = listener
                database.child("settlements").child(settlementId).addValueEventListener(listener)
            }
        }
    }

    private fun updateTransactionsView() {
        val currentUserId = auth.currentUser?.uid ?: ""

        // Combine expenses and settlements into transaction items
        val transactions = mutableListOf<TransactionItem>()

        // Add expenses
        expenses.values.forEach { expense ->
            transactions.add(TransactionItem.ExpenseItem(expense))
        }

        // Add settlements
        settlements.values.forEach { settlement ->
            transactions.add(TransactionItem.SettlementItem(settlement))
        }

        // Sort by date (newest first)
        val sortedTransactions = transactions.sortedByDescending { it.timestamp }

        // Initialize or update adapter
        if (!::transactionAdapter.isInitialized) {
            transactionAdapter = TransactionAdapter(sortedTransactions, members, currentUserId)
            binding.rvExpenses.apply {
                layoutManager = LinearLayoutManager(this@GroupDetailsActivity)
                adapter = transactionAdapter
            }
        } else {
            transactionAdapter.updateTransactions(sortedTransactions)
        }

        // Calculate total unsettled expenses
        val total = expenses.values.sumOf { it.amount }
        binding.tvTotalExpenses.text = "Total: ₹%.2f".format(total)

        // Show or hide "no expenses" message
        if (transactions.isEmpty()) {
            binding.tvNoExpenses.visibility = View.VISIBLE
            binding.rvExpenses.visibility = View.GONE
        } else {
            binding.tvNoExpenses.visibility = View.GONE
            binding.rvExpenses.visibility = View.VISIBLE
        }

        // Update balances
        updateBalances()
    }

    private fun updateBalances() {
        val currentUserId = auth.currentUser?.uid ?: return

        // Calculate real-time balance from expenses and settlements
        var netBalance = 0.0

        // Process expenses
        expenses.values.forEach { expense ->
            val splitAmount = expense.amount / expense.splitAmong.size

            if (expense.paidBy == currentUserId) {
                // I paid, so others owe me (positive balance)
                expense.splitAmong.forEach { memberId ->
                    if (memberId != currentUserId) {
                        netBalance += splitAmount
                    }
                }
            } else if (expense.splitAmong.contains(currentUserId)) {
                // Someone else paid and I'm in the split (negative balance)
                netBalance -= splitAmount
            }
        }

        // Process settlements - they reduce balances
        settlements.values.forEach { settlement ->
            if (settlement.fromUserId == currentUserId) {
                // I paid someone, reduces what I owe (increases my balance)
                netBalance += settlement.amount
            } else if (settlement.toUserId == currentUserId) {
                // Someone paid me, reduces what they owe me (decreases my balance)
                netBalance -= settlement.amount
            }
        }

        // Positive balance means others owe me, negative means I owe others
        val amountOwed = if (netBalance > 0) netBalance else 0.0
        val amountOwe = if (netBalance < 0) -netBalance else 0.0

        binding.tvYouAreOwed.text = "₹%.2f".format(amountOwed)
        binding.tvYouOwe.text = "₹%.2f".format(amountOwe)
    }
}
