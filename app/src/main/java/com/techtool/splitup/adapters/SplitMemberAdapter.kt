package com.techtool.splitup.adapters

import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.techtool.splitup.databinding.ItemSplitMemberBinding
import com.techtool.splitup.models.Member

class SplitMemberAdapter(
    private val members: List<Member>,
    private val selectedMembers: MutableSet<String>,
    private val splitAmounts: MutableMap<String, Double> = mutableMapOf(),
    private var splitMode: String = "EQUAL", // EQUAL, UNEQUAL, PERCENTAGE, EXACT
    private val onAmountChanged: ((Double) -> Unit)? = null
) : RecyclerView.Adapter<SplitMemberAdapter.MemberViewHolder>() {

    private val colors = listOf(
        "#6366F1", "#8B5CF6", "#EC4899", "#F59E0B",
        "#10B981", "#3B82F6", "#EF4444", "#06B6D4"
    )

    fun updateSplitMode(mode: String) {
        splitMode = mode
        notifyDataSetChanged()
    }

    fun getSplitAmounts(): Map<String, Double> {
        return splitAmounts.toMap()
    }

    fun getTotalAmount(): Double {
        return splitAmounts.values.sum()
    }

    inner class MemberViewHolder(val binding: ItemSplitMemberBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var textWatcher: TextWatcher? = null

        fun bind(member: Member, position: Int) {
            binding.tvMemberName.text = member.name
            binding.tvMemberInitial.text = member.name.firstOrNull()?.uppercase() ?: "?"

            // Set avatar color
            val colorIndex = position % colors.size
            binding.tvMemberInitial.parent.let {
                if (it is com.google.android.material.card.MaterialCardView) {
                    it.setCardBackgroundColor(Color.parseColor(colors[colorIndex]))
                }
            }

            // Set checkbox state
            binding.cbSelected.isChecked = selectedMembers.contains(member.uid)

            // Show/hide amount input based on split mode
            if (splitMode == "EQUAL") {
                binding.tilAmount.visibility = View.GONE
            } else {
                binding.tilAmount.visibility = if (selectedMembers.contains(member.uid)) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

                // Update hint based on split mode
                binding.etAmount.hint = when (splitMode) {
                    "PERCENTAGE" -> "%"
                    else -> "₹0"
                }

                // Set current amount if exists
                val currentAmount = splitAmounts[member.uid]
                if (currentAmount != null && currentAmount > 0) {
                    binding.etAmount.setText(currentAmount.toString())
                }

                // Remove old text watcher
                textWatcher?.let { binding.etAmount.removeTextChangedListener(it) }

                // Add text watcher for amount changes
                textWatcher = object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        val amountStr = s?.toString() ?: ""
                        val amount = amountStr.toDoubleOrNull() ?: 0.0
                        splitAmounts[member.uid] = amount
                        onAmountChanged?.invoke(getTotalAmount())
                    }
                }
                binding.etAmount.addTextChangedListener(textWatcher)
            }

            binding.cbSelected.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedMembers.add(member.uid)
                    if (splitMode != "EQUAL") {
                        binding.tilAmount.visibility = View.VISIBLE
                        splitAmounts[member.uid] = 0.0
                    }
                } else {
                    selectedMembers.remove(member.uid)
                    binding.tilAmount.visibility = View.GONE
                    splitAmounts.remove(member.uid)
                    onAmountChanged?.invoke(getTotalAmount())
                }
            }

            binding.root.setOnClickListener {
                binding.cbSelected.isChecked = !binding.cbSelected.isChecked
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemSplitMemberBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(members[position], position)
    }

    override fun getItemCount() = members.size
}
