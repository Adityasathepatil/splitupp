package com.techtool.splitup.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.techtool.splitup.databinding.ItemSettlementBinding
import com.techtool.splitup.models.Settlement
import java.text.DecimalFormat

class SettlementAdapter(
    private val settlements: List<Settlement>
) : RecyclerView.Adapter<SettlementAdapter.SettlementViewHolder>() {

    private val decimalFormat = DecimalFormat("#,##0.00")

    inner class SettlementViewHolder(val binding: ItemSettlementBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(settlement: Settlement) {
            binding.tvFromName.text = settlement.from.name
            binding.tvToName.text = settlement.to.name
            binding.tvAmount.text = "₹${decimalFormat.format(settlement.amount)}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettlementViewHolder {
        val binding = ItemSettlementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SettlementViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SettlementViewHolder, position: Int) {
        holder.bind(settlements[position])
    }

    override fun getItemCount() = settlements.size
}
