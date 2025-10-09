package com.techtool.splitup

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.techtool.splitup.adapters.GroupSpendingAdapter
import com.techtool.splitup.adapters.TopContributorAdapter
import com.techtool.splitup.databinding.ActivityAnalyticsBinding
import com.techtool.splitup.models.Expense
import com.techtool.splitup.models.Group
import com.techtool.splitup.models.Member
import com.techtool.splitup.models.User

class AnalyticsActivity : BaseActivity() {
    private lateinit var binding: ActivityAnalyticsBinding
    private val database = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid ?: ""

    private val groups = mutableListOf<Group>()
    private val allExpenses = mutableMapOf<String, Expense>()
    private val members = mutableMapOf<String, Member>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadData()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Setup RecyclerViews
        binding.rvGroupSpending.layoutManager = LinearLayoutManager(this)
        binding.rvTopContributors.layoutManager = LinearLayoutManager(this)
    }

    private fun loadData() {
        // Load user's group IDs (same as HomeActivity)
        database.child("users").child(currentUserId).child("groupIds")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val groupIds = mutableListOf<String>()
                    snapshot.children.forEach { child ->
                        child.getValue(String::class.java)?.let { groupIds.add(it) }
                    }

                    if (groupIds.isEmpty()) {
                        updateUI()
                        return
                    }

                    loadGroups(groupIds)
                }

                override fun onCancelled(error: DatabaseError) {
                    updateUI()
                }
            })
    }

    private fun loadGroups(groupIds: List<String>) {
        groups.clear()
        var processedGroups = 0

        groupIds.forEach { groupId ->
            database.child("groups").child(groupId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val group = snapshot.getValue(Group::class.java)
                        if (group != null) {
                            groups.add(group)
                        }
                        processedGroups++
                        if (processedGroups == groupIds.size) {
                            loadAllExpenses()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        processedGroups++
                        if (processedGroups == groupIds.size) {
                            loadAllExpenses()
                        }
                    }
                })
        }
    }

    private fun loadAllExpenses() {
        val expenseIds = groups.flatMap { it.expenseIds }.toSet()

        if (expenseIds.isEmpty()) {
            loadMembers()
            return
        }

        var processedExpenses = 0
        expenseIds.forEach { expenseId ->
            database.child("expenses").child(expenseId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val expense = snapshot.getValue(Expense::class.java)
                        if (expense != null && !expense.isSettled) {
                            allExpenses[expenseId] = expense
                        }
                        processedExpenses++
                        if (processedExpenses == expenseIds.size) {
                            loadMembers()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        processedExpenses++
                        if (processedExpenses == expenseIds.size) {
                            loadMembers()
                        }
                    }
                })
        }
    }

    private fun loadMembers() {
        // Get unique member IDs
        val memberIds = mutableSetOf<String>()
        for (expense in allExpenses.values) {
            memberIds.add(expense.paidBy)
            memberIds.addAll(expense.splitAmong)
        }

        if (memberIds.isEmpty()) {
            updateUI()
            return
        }

        var membersLoaded = 0
        memberIds.forEach { memberId ->
            database.child("users").child(memberId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user = snapshot.getValue(User::class.java)
                        if (user != null) {
                            members[memberId] = Member(user.uid, user.name, user.email)
                        }
                        membersLoaded++
                        if (membersLoaded == memberIds.size) {
                            updateUI()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        membersLoaded++
                        if (membersLoaded == memberIds.size) {
                            updateUI()
                        }
                    }
                })
        }
    }

    private fun updateUI() {
        calculateAndDisplayStats()
    }

    private fun calculateAndDisplayStats() {
        // Calculate total expenses from all expenses
        val totalExpenses = allExpenses.values.sumOf { it.amount }
        binding.tvTotalExpenses.text = "₹${String.format("%.2f", totalExpenses)}"

        // Active groups count
        binding.tvActiveGroups.text = groups.size.toString()

        // Calculate group spending
        val groupSpending = mutableMapOf<String, Double>()
        for (expense in allExpenses.values) {
            val current = groupSpending[expense.groupId] ?: 0.0
            groupSpending[expense.groupId] = current + expense.amount
        }

        val groupSpendingList = groupSpending.map { entry ->
            val group = groups.find { it.id == entry.key }
            GroupSpendingData(
                groupId = entry.key,
                groupName = group?.name ?: "Unknown Group",
                amount = entry.value,
                percentage = if (totalExpenses > 0) (entry.value / totalExpenses * 100) else 0.0
            )
        }.sortedByDescending { it.amount }

        binding.rvGroupSpending.adapter = GroupSpendingAdapter(groupSpendingList)

        // Calculate top contributors (who paid the most)
        val contributorAmounts = mutableMapOf<String, Double>()
        val contributorExpenseCounts = mutableMapOf<String, Int>()

        for (expense in allExpenses.values) {
            val current = contributorAmounts[expense.paidBy] ?: 0.0
            contributorAmounts[expense.paidBy] = current + expense.amount

            val count = contributorExpenseCounts[expense.paidBy] ?: 0
            contributorExpenseCounts[expense.paidBy] = count + 1
        }

        val topContributors = contributorAmounts.map { entry ->
            ContributorData(
                userId = entry.key,
                userName = members[entry.key]?.name ?: "Unknown",
                amount = entry.value,
                expenseCount = contributorExpenseCounts[entry.key] ?: 0
            )
        }.sortedByDescending { it.amount }.take(5)

        binding.rvTopContributors.adapter = TopContributorAdapter(topContributors)

        // Calculate personal stats (same logic as HomeActivity)
        val youPaid = allExpenses.values.filter { it.paidBy == currentUserId }.sumOf { it.amount }
        binding.tvYouPaid.text = "₹${String.format("%.2f", youPaid)}"

        val yourShare = allExpenses.values.sumOf { expense ->
            expense.getSplitAmountForUser(currentUserId)
        }
        binding.tvYourShare.text = "₹${String.format("%.2f", yourShare)}"

        val yourExpenseCount = allExpenses.values.count {
            it.paidBy == currentUserId || it.splitAmong.contains(currentUserId)
        }
        binding.tvExpenseCount.text = yourExpenseCount.toString()
    }
}

// Data classes for adapters
data class GroupSpendingData(
    val groupId: String,
    val groupName: String,
    val amount: Double,
    val percentage: Double
)

data class ContributorData(
    val userId: String,
    val userName: String,
    val amount: Double,
    val expenseCount: Int
)
