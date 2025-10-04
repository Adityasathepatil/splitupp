package com.techtool.splitup.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.techtool.splitup.databinding.ItemMemberBinding
import com.techtool.splitup.models.Member

class MemberAdapter(
    private val members: MutableList<Member>,
    private val currentUserId: String,
    private val onRemoveMember: (Member) -> Unit
) : RecyclerView.Adapter<MemberAdapter.MemberViewHolder>() {

    private val colors = listOf(
        "#6366F1", "#8B5CF6", "#EC4899", "#F59E0B",
        "#10B981", "#3B82F6", "#EF4444", "#06B6D4"
    )

    inner class MemberViewHolder(val binding: ItemMemberBinding) :
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

            // Show "You" badge for current user
            if (member.uid == currentUserId) {
                binding.tvYouBadge.visibility = View.VISIBLE
                binding.btnRemoveMember.visibility = View.GONE
            } else {
                binding.tvYouBadge.visibility = View.GONE
                binding.btnRemoveMember.visibility = View.VISIBLE
            }

            binding.btnRemoveMember.setOnClickListener {
                onRemoveMember(member)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemMemberBinding.inflate(
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

    fun addMember(member: Member) {
        members.add(member)
        notifyItemInserted(members.size - 1)
    }

    fun removeMember(member: Member) {
        val index = members.indexOf(member)
        if (index != -1) {
            members.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun getMembers(): List<Member> = members
}
