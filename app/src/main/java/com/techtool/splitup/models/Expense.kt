package com.techtool.splitup.models

data class Expense(
    val id: String = "",
    val groupId: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val paidBy: String = "", // User ID
    val splitAmong: List<String> = emptyList(), // List of User IDs
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
            "createdAt" to createdAt,
            "isSettled" to isSettled
        )
    }
}
