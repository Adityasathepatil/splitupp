package com.techtool.splitup.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.techtool.splitup.GroupSpendingData
import com.techtool.splitup.R

class GroupSpendingAdapter(
    private val items: List<GroupSpendingData>
) : RecyclerView.Adapter<GroupSpendingAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvGroupInitial: TextView = view.findViewById(R.id.tvGroupInitial)
        val tvGroupName: TextView = view.findViewById(R.id.tvGroupName)
        val tvPercentage: TextView = view.findViewById(R.id.tvPercentage)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group_spending, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        // Set group initial (first letter of group name)
        holder.tvGroupInitial.text = item.groupName.firstOrNull()?.uppercase() ?: "G"

        // Set group name
        holder.tvGroupName.text = item.groupName

        // Set percentage
        holder.tvPercentage.text = "${String.format("%.1f", item.percentage)}% of total"

        // Set amount
        holder.tvAmount.text = "₹${String.format("%.2f", item.amount)}"

        // Set progress bar
        holder.progressBar.progress = item.percentage.toInt()
    }

    override fun getItemCount() = items.size
}
