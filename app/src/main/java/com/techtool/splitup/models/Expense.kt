package com.techtool.splitup.models

data class Expense(
    val id: String = "",
    val groupId: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val paidBy: String = "", // User ID
    val splitAmong: List<String> = emptyList(), // List of User IDs
    val splitType: String = "EQUAL", // EQUAL, UNEQUAL, PERCENTAGE, EXACT
    val splitDetails: Map<String, Double> = emptyMap(), // userId -> amount they owe
    val createdAt: Long = System.currentTimeMillis(),
    val isSettled: Boolean = false
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "groupId" to groupId,
            "description" to description,
            "amount" to amount,
            "paidBy" to paidBy,
            "splitAmong" to splitAmong,
            "splitType" to splitType,
            "splitDetails" to splitDetails,
            "createdAt" to createdAt,
            "isSettled" to isSettled
        )
    }

    // Helper function to get split amount for a user
    fun getSplitAmountForUser(userId: String): Double {
        return when (splitType) {
            "EQUAL" -> {
                if (splitAmong.contains(userId)) {
                    amount / splitAmong.size
                } else {
                    0.0
                }
            }
            else -> {
                // For UNEQUAL, PERCENTAGE, EXACT - use splitDetails
                splitDetails[userId] ?: 0.0
            }
        }
    }
}
