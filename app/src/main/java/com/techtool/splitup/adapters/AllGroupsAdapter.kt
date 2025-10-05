package com.techtool.splitup.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.techtool.splitup.databinding.ItemGroupGridBinding
import com.techtool.splitup.models.Expense
import com.techtool.splitup.models.Group
import com.techtool.splitup.models.Member

class AllGroupsAdapter(
    private var groups: List<Pair<Group, List<Member>>>,
    private var expenses: Map<String, Expense> = emptyMap(),
    private val onGroupClick: (Group) -> Unit
) : RecyclerView.Adapter<AllGroupsAdapter.GroupViewHolder>() {

    private val colors = listOf(
        "#6366F1", "#8B5CF6", "#EC4899", "#F59E0B",
        "#10B981", "#3B82F6", "#EF4444", "#06B6D4"
    )

    inner class GroupViewHolder(val binding: ItemGroupGridBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(groupWithMembers: Pair<Group, List<Member>>) {
            val (group, members) = groupWithMembers

            binding.tvGroupName.text = group.name

            // Calculate total from expenses
            val groupTotal = group.expenseIds
                .mapNotNull { expenseId -> expenses[expenseId] }
                .filter { !it.isSettled }
                .sumOf { it.amount }

            binding.tvGroupTotal.text = "₹%.2f".format(groupTotal)

            // Clear previous member avatars
            binding.memberAvatarsContainer.removeAllViews()

            // Add member avatars (max 3, then show +N)
            val maxAvatars = 3
            val displayMembers = members.take(maxAvatars)

            displayMembers.forEachIndexed { index, member ->
                val avatar = createMemberAvatar(member, index)
                binding.memberAvatarsContainer.addView(avatar)
            }

            // Add "+N" indicator if there are more members
            if (members.size > maxAvatars) {
                val remaining = members.size - maxAvatars
                val extraAvatar = createExtraCountAvatar(remaining, maxAvatars)
                binding.memberAvatarsContainer.addView(extraAvatar)
            }

            binding.groupCard.setOnClickListener {
                onGroupClick(group)
            }
        }

        private fun createMemberAvatar(member: Member, index: Int): MaterialCardView {
            val size = 32 // dp
            val margin = if (index > 0) -8 else 0 // Overlap effect

            return MaterialCardView(binding.root.context).apply {
                layoutParams = ViewGroup.MarginLayoutParams(
                    (size * binding.root.context.resources.displayMetrics.density).toInt(),
                    (size * binding.root.context.resources.displayMetrics.density).toInt()
                ).apply {
                    marginStart = (margin * binding.root.context.resources.displayMetrics.density).toInt()
                }
                radius = (size / 2 * binding.root.context.resources.displayMetrics.density)
                cardElevation = 0f
                setCardBackgroundColor(Color.parseColor(colors[index % colors.size]))

                addView(TextView(binding.root.context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    text = member.name.firstOrNull()?.uppercase() ?: "?"
                    textSize = 14f
                    setTextColor(Color.WHITE)
                    gravity = android.view.Gravity.CENTER
                })
            }
        }

        private fun createExtraCountAvatar(count: Int, index: Int): MaterialCardView {
            val size = 32
            val margin = -8

            return MaterialCardView(binding.root.context).apply {
                layoutParams = ViewGroup.MarginLayoutParams(
                    (size * binding.root.context.resources.displayMetrics.density).toInt(),
                    (size * binding.root.context.resources.displayMetrics.density).toInt()
                ).apply {
                    marginStart = (margin * binding.root.context.resources.displayMetrics.density).toInt()
                }
                radius = (size / 2 * binding.root.context.resources.displayMetrics.density)
                cardElevation = 0f
                setCardBackgroundColor(Color.parseColor("#475569"))

                addView(TextView(binding.root.context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    text = "+$count"
                    textSize = 12f
                    setTextColor(Color.WHITE)
                    gravity = android.view.Gravity.CENTER
                })
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemGroupGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(groups[position])
    }

    override fun getItemCount() = groups.size

    fun updateGroups(newGroups: List<Pair<Group, List<Member>>>, newExpenses: Map<String, Expense> = expenses) {
        groups = newGroups
        expenses = newExpenses
        notifyDataSetChanged()
    }
}
