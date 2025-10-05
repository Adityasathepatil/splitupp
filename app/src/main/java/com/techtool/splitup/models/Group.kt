package com.techtool.splitup.models

data class Group(
    val id: String = "",
    val name: String = "",
    val inviteCode: String = "",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val memberIds: List<String> = emptyList(),
    val expenseIds: List<String> = emptyList(),
    val settlementIds: List<String> = emptyList(),
    val memberBalances: Map<String, Double> = emptyMap()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "inviteCode" to inviteCode,
            "createdBy" to createdBy,
            "createdAt" to createdAt,
            "memberIds" to memberIds,
            "expenseIds" to expenseIds,
            "settlementIds" to settlementIds,
            "memberBalances" to memberBalances
        )
    }
}
