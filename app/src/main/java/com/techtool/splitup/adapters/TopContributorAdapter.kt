package com.techtool.splitup.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.techtool.splitup.ContributorData
import com.techtool.splitup.R

class TopContributorAdapter(
    private val items: List<ContributorData>
) : RecyclerView.Adapter<TopContributorAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardRank: CardView = view.findViewById(R.id.cardRank)
        val tvRank: TextView = view.findViewById(R.id.tvRank)
        val tvUserInitial: TextView = view.findViewById(R.id.tvUserInitial)
        val tvUserName: TextView = view.findViewById(R.id.tvUserName)
        val tvExpenseCount: TextView = view.findViewById(R.id.tvExpenseCount)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contributor, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val rank = position + 1

        // Set rank
        holder.tvRank.text = rank.toString()

        // Set rank badge color based on position
        val rankColor = when (rank) {
            1 -> ContextCompat.getColor(holder.itemView.context, android.R.color.holo_orange_light)
            2 -> ContextCompat.getColor(holder.itemView.context, android.R.color.darker_gray)
            3 -> ContextCompat.getColor(holder.itemView.context, android.R.color.holo_orange_dark)
            else -> ContextCompat.getColor(holder.itemView.context, android.R.color.darker_gray)
        }
        holder.cardRank.setCardBackgroundColor(rankColor)

        // Set user initial
        holder.tvUserInitial.text = item.userName.firstOrNull()?.uppercase() ?: "U"

        // Set user name
        holder.tvUserName.text = item.userName

        // Set expense count
        val expenseText = if (item.expenseCount == 1) "1 expense" else "${item.expenseCount} expenses"
        holder.tvExpenseCount.text = expenseText

        // Set amount
        holder.tvAmount.text = "₹${String.format("%.2f", item.amount)}"
    }

    override fun getItemCount() = items.size
}
