package com.techtool.splitup.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.techtool.splitup.databinding.ItemExpenseBinding
import com.techtool.splitup.models.Expense
import com.techtool.splitup.models.Member
import java.text.SimpleDateFormat
import java.util.*

class ExpenseAdapter(
    private var expenses: List<Expense>,
    private val members: List<Member>,
    private val currentUserId: String
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    inner class ExpenseViewHolder(private val binding: ItemExpenseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(expense: Expense) {
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

        private fun isSameDay(date1: Date, date2: Date): Boolean {
            val cal1 = Calendar.getInstance().apply { time = date1 }
            val cal2 = Calendar.getInstance().apply { time = date2 }
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(expenses[position])
    }

    override fun getItemCount() = expenses.size

    fun updateExpenses(newExpenses: List<Expense>) {
        expenses = newExpenses
        notifyDataSetChanged()
    }
}
