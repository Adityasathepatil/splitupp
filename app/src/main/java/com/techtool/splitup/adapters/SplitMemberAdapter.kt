package com.techtool.splitup.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.techtool.splitup.databinding.ItemSplitMemberBinding
import com.techtool.splitup.models.Member

class SplitMemberAdapter(
    private val members: List<Member>,
    private val selectedMembers: MutableSet<String>
) : RecyclerView.Adapter<SplitMemberAdapter.MemberViewHolder>() {

    private val colors = listOf(
        "#6366F1", "#8B5CF6", "#EC4899", "#F59E0B",
        "#10B981", "#3B82F6", "#EF4444", "#06B6D4"
    )

    inner class MemberViewHolder(val binding: ItemSplitMemberBinding) :
        RecyclerView.ViewHolder(binding.root) {

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

            binding.cbSelected.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedMembers.add(member.uid)
                } else {
                    selectedMembers.remove(member.uid)
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
