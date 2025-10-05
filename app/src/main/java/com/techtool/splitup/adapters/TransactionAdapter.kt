package com.techtool.splitup.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.techtool.splitup.databinding.ItemExpenseBinding
import com.techtool.splitup.databinding.ItemSettlementBinding
import com.techtool.splitup.models.Member
import com.techtool.splitup.models.TransactionItem
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private var transactions: List<TransactionItem>,
    private val members: List<Member>,
    private val currentUserId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_EXPENSE = 0
        private const val TYPE_SETTLEMENT = 1
    }

    inner class ExpenseViewHolder(private val binding: ItemExpenseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(expenseItem: TransactionItem.ExpenseItem) {
            val expense = expenseItem.expense
            binding.tvExpenseDescription.text = expense.description
            binding.tvExpenseAmount.text = "₹%.2f".format(expense.amount)

            // Find who paid
            val paidByMember = members.find { it.uid == expense.paidBy }
            val paidByName = if (expense.paidBy == currentUserId) "You" else (paidByMember?.name ?: "Unknown")
            binding.tvExpensePaidBy.text = "Paid by $paidByName"

            // Calculate your share
            val yourShare = if (expense.splitAmong.contains(currentUserId)) {
                expense.amount / expense.splitAmong.size
            } else {
                0.0
            }
            binding.tvExpenseYourShare.text = "Your share: ₹%.2f".format(yourShare)

            // Split among info
            binding.tvExpenseSplitAmong.text = "Split among: ${expense.splitAmong.size} people"

            // Date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val date = Date(expense.createdAt)
            val today = Date()
            val yesterday = Date(today.time - 24 * 60 * 60 * 1000)

            binding.tvExpenseDate.text = when {
                isSameDay(date, today) -> "Today"
                isSameDay(date, yesterday) -> "Yesterday"
                else -> dateFormat.format(date)
            }
        }
    }

    inner class SettlementViewHolder(private val binding: ItemSettlementBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(settlementItem: TransactionItem.SettlementItem) {
            val settlement = settlementItem.settlement

            // Find member names
            val fromMember = members.find { it.uid == settlement.fromUserId }
            val toMember = members.find { it.uid == settlement.toUserId }

            val fromName = if (settlement.fromUserId == currentUserId) "You" else (fromMember?.name ?: "Unknown")
            val toName = if (settlement.toUserId == currentUserId) "You" else (toMember?.name ?: "Unknown")

            binding.tvFromName.text = fromName
            binding.tvToName.text = toName
            binding.tvAmount.text = "₹%.2f".format(settlement.amount)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (transactions[position]) {
            is TransactionItem.ExpenseItem -> TYPE_EXPENSE
            is TransactionItem.SettlementItem -> TYPE_SETTLEMENT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_EXPENSE -> {
                val binding = ItemExpenseBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ExpenseViewHolder(binding)
            }
            TYPE_SETTLEMENT -> {
                val binding = ItemSettlementBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                SettlementViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val transaction = transactions[position]) {
            is TransactionItem.ExpenseItem -> (holder as ExpenseViewHolder).bind(transaction)
            is TransactionItem.SettlementItem -> (holder as SettlementViewHolder).bind(transaction)
        }
    }

    override fun getItemCount() = transactions.size

    fun updateTransactions(newTransactions: List<TransactionItem>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
