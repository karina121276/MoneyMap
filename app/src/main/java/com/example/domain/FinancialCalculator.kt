package com.example.domain

import com.example.core.model.AccountType
import com.example.core.model.TransactionType
import com.example.data.entity.AccountEntity
import com.example.data.entity.TransactionEntity

data class AccountBalance(
    val account: AccountEntity,
    val calculatedBalancePaise: Long
)

data class PeriodSummary(
    val totalIncomePaise: Long,
    val totalExpensePaise: Long,
    val netCashFlowPaise: Long,
    val savingsRatePercentage: Double,
    val transactionCount: Int
)

data class CategorySpending(
    val categoryId: Long?,
    val categoryName: String,
    val iconName: String,
    val colorHex: String,
    val totalSpentPaise: Long,
    val percentage: Double
)

object FinancialCalculator {

    /**
     * Calculates the balance of a single account from its opening balance and transactions.
     * Invariants:
     * - Income to this account increases balance.
     * - Expense from this account decreases balance.
     * - Transfer from this account (as source) decreases balance.
     * - Transfer to this account (as destination) increases balance.
     * - For CREDIT_CARD (liability): positive balance represents amount owed.
     */
    fun calculateAccountBalance(
        account: AccountEntity,
        transactions: List<TransactionEntity>
    ): Long {
        var balance = account.openingBalancePaise

        for (tx in transactions) {
            when (tx.type) {
                TransactionType.INCOME -> {
                    if (tx.accountId == account.id) {
                        if (account.type.isLiability) {
                            // Payment/credit to credit card reduces amount owed
                            balance -= tx.amountPaise
                        } else {
                            balance += tx.amountPaise
                        }
                    }
                }
                TransactionType.EXPENSE -> {
                    if (tx.accountId == account.id) {
                        if (account.type.isLiability) {
                            // Purchase on credit card increases amount owed
                            balance += tx.amountPaise
                        } else {
                            balance -= tx.amountPaise
                        }
                    }
                }
                TransactionType.TRANSFER -> {
                    if (tx.accountId == account.id) {
                        // Outgoing transfer
                        balance -= tx.amountPaise
                    }
                    if (tx.toAccountId == account.id) {
                        // Incoming transfer
                        balance += tx.amountPaise
                    }
                }
            }
        }
        return balance
    }

    /**
     * Calculates Net Worth:
     * Total Asset Balances (Bank + Cash + Wallet) - Total Credit Card Liabilities
     */
    fun calculateNetWorth(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>
    ): Long {
        var totalAssets = 0L
        var totalLiabilities = 0L

        for (account in accounts) {
            val balance = calculateAccountBalance(account, transactions)
            if (account.type.isLiability) {
                // If positive, it's debt owed
                if (balance > 0) {
                    totalLiabilities += balance
                }
            } else {
                totalAssets += balance
            }
        }
        return totalAssets - totalLiabilities
    }

    /**
     * Calculates total tracked assets across all non-liability accounts.
     */
    fun calculateTotalAssetBalance(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>
    ): Long {
        return accounts
            .filter { !it.type.isLiability }
            .sumOf { calculateAccountBalance(it, transactions) }
    }

    /**
     * Calculates total savings balance as the sum of current balances of
     * all accounts marked as Savings Accounts (isSavings == true or type == AccountType.SAVINGS).
     * Excludes non-savings accounts and archived accounts.
     */
    fun calculateSavingsBalance(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>
    ): Long {
        return accounts
            .filter { (it.isSavings || it.type == AccountType.SAVINGS) && !it.isArchived }
            .sumOf { calculateAccountBalance(it, transactions) }
    }

    /**
     * Calculates income, expense, cash flow, and savings rate for a set of transactions.
     * Transfers are strictly excluded.
     */
    fun calculatePeriodSummary(transactions: List<TransactionEntity>): PeriodSummary {
        var income = 0L
        var expense = 0L
        var count = 0

        for (tx in transactions) {
            count++
            when (tx.type) {
                TransactionType.INCOME -> income += tx.amountPaise
                TransactionType.EXPENSE -> expense += tx.amountPaise
                TransactionType.TRANSFER -> {
                    // Strictly excluded from income & expenses
                }
            }
        }

        val netFlow = income - expense
        val savingsRate = if (income > 0) {
            (netFlow.toDouble() / income.toDouble()) * 100.0
        } else {
            0.0
        }

        return PeriodSummary(
            totalIncomePaise = income,
            totalExpensePaise = expense,
            netCashFlowPaise = netFlow,
            savingsRatePercentage = savingsRate.coerceIn(-100.0, 100.0),
            transactionCount = count
        )
    }

    /**
     * Calculates category spending breakdown.
     */
    fun calculateCategoryBreakdown(
        transactions: List<TransactionEntity>,
        categoryMap: Map<Long, com.example.data.entity.CategoryEntity>
    ): List<CategorySpending> {
        val expenseTx = transactions.filter { it.type == TransactionType.EXPENSE }
        val totalExpense = expenseTx.sumOf { it.amountPaise }
        if (totalExpense <= 0L) return emptyList()

        val grouped = expenseTx.groupBy { it.categoryId }

        return grouped.map { (catId, txList) ->
            val spent = txList.sumOf { it.amountPaise }
            val cat = catId?.let { categoryMap[it] }
            val name = cat?.name ?: "Others"
            val icon = cat?.iconName ?: "more_horiz"
            val color = cat?.colorHex ?: "#64748B"
            val percentage = (spent.toDouble() / totalExpense.toDouble()) * 100.0

            CategorySpending(
                categoryId = catId,
                categoryName = name,
                iconName = icon,
                colorHex = color,
                totalSpentPaise = spent,
                percentage = percentage
            )
        }.sortedByDescending { it.totalSpentPaise }
    }
}
