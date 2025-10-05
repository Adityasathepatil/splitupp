package com.techtool.splitup.models

data class SettlementRecord(
    val id: String = "",
    val groupId: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val amount: Double = 0.0,
    val settledAt: Long = System.currentTimeMillis(),
    val expenseIds: List<String> = emptyList() // Expenses that were settled
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "groupId" to groupId,
            "fromUserId" to fromUserId,
            "toUserId" to toUserId,
            "amount" to amount,
            "settledAt" to settledAt,
            "expenseIds" to expenseIds
        )
    }
}
