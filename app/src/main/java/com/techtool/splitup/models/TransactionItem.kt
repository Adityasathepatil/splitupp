package com.techtool.splitup.models

sealed class TransactionItem {
    abstract val timestamp: Long

    data class ExpenseItem(
        val expense: Expense
    ) : TransactionItem() {
        override val timestamp: Long = expense.createdAt
    }

    data class SettlementItem(
        val settlement: SettlementRecord
    ) : TransactionItem() {
        override val timestamp: Long = settlement.settledAt
    }
}
