package com.stella.core.data

object FinanceCategories {
    val ingress = listOf("Salary", "Gift", "Refund", "Other Income")
    val egress = listOf(
        "Food",
        "Rent",
        "Transport",
        "Subscriptions",
        "Fine-Penalty",
        "Penalty",
        "Other Expense",
    )

    const val PENALTY = "Penalty"

    fun forType(type: TransactionType): List<String> = when (type) {
        TransactionType.INGRESS -> ingress
        TransactionType.EGRESS -> egress
    }

    fun defaultFor(type: TransactionType): String = forType(type).first()

    fun matchesType(category: String, type: TransactionType): Boolean =
        category in forType(type)
}

enum class TransactionType(val wire: String) {
    INGRESS("ingress"),
    EGRESS("egress"),
}

enum class DebtDirection(val wire: String) {
    OWED_TO_ME("owed_to_me"),
    OWED_BY_ME("owed_by_me"),
}

data class FinanceMonthSummary(
    val ingress: Double,
    val egress: Double,
    val netBalance: Double,
    val owedToMe: Double,
    val owedByMe: Double,
)
