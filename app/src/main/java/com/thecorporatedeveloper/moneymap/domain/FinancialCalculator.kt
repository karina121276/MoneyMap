package com.thecorporatedeveloper.moneymap.domain

import com.thecorporatedeveloper.moneymap.core.model.AccountType
import com.thecorporatedeveloper.moneymap.core.model.TransactionType
import com.thecorporatedeveloper.moneymap.data.entity.AccountEntity
import com.thecorporatedeveloper.moneymap.data.entity.BudgetEntity
import com.thecorporatedeveloper.moneymap.data.entity.TransactionEntity
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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

data class BudgetSpendingResult(
    val spentPaise: Long,
    val limitPaise: Long,
    val remainingPaise: Long,
    val usagePercentage: Int,
    val usageFraction: Float,
    val isOverBudget: Boolean,
    val isApproaching: Boolean,
    val status: String, // "UPCOMING", "ACTIVE", "COMPLETED"
    val dateRangeLabel: String
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
     * Single Global Balance Calculation Logic:
     * Total Balance = Sum of balances of all NON-SAVINGS, non-liability, active accounts.
     * Savings accounts (type == AccountType.SAVINGS or isSavings == true) are strictly excluded
     * and counted separately via [calculateSavingsBalance].
     */
    fun calculateTotalBalance(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>
    ): Long {
        return calculateTotalAssetBalance(accounts, transactions)
    }

    /**
     * Calculates Total Balance (Asset Balance):
     * Sum of all regular, non-savings, non-liability accounts.
     * Excludes accounts marked as Savings (which are tracked separately in Savings Balance)
     * and credit card liabilities.
     */
    fun calculateTotalAssetBalance(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>
    ): Long {
        return accounts
            .filter { !it.type.isLiability && !it.isSavingsAccount && !it.isArchived }
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
            .filter { it.isSavingsAccount && !it.isArchived }
            .sumOf { calculateAccountBalance(it, transactions) }
    }

    /**
     * Shared calculation logic for budget spending and status.
     * Spent = Sum of eligible expense transactions whose transaction dates are within
     * the budget's inclusive calendar date range (Start Date to End Date).
     */
    fun calculateBudgetSpending(
        budget: BudgetEntity,
        transactions: List<TransactionEntity>
    ): BudgetSpendingResult {
        val zone = ZoneId.systemDefault()
        val startLocalDate = if (budget.startDateEpochMillis > 0L) {
            Instant.ofEpochMilli(budget.startDateEpochMillis).atZone(zone).toLocalDate()
        } else {
            val ym = try { YearMonth.parse(budget.monthYear) } catch (e: Exception) { YearMonth.now() }
            ym.atDay(1)
        }
        val startInclusiveMillis = startLocalDate.atStartOfDay(zone).toInstant().toEpochMilli()

        val endLocalDate = if (budget.endDateEpochMillis > 0L) {
            Instant.ofEpochMilli(budget.endDateEpochMillis).atZone(zone).toLocalDate()
        } else {
            val ym = try { YearMonth.parse(budget.monthYear) } catch (e: Exception) { YearMonth.now() }
            ym.atEndOfMonth()
        }
        val endInclusiveMillis = endLocalDate.atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()

        val eligibleSpent = transactions.filter { tx ->
            tx.type == TransactionType.EXPENSE &&
                    tx.dateEpochMillis in startInclusiveMillis..endInclusiveMillis &&
                    when (budget.scope) {
                        "ACCOUNT" -> tx.accountId == budget.accountId
                        "CATEGORY" -> tx.categoryId == budget.categoryId
                        "ACCOUNT_CATEGORY" -> tx.accountId == budget.accountId && tx.categoryId == budget.categoryId
                        else -> true // "ALL_ACCOUNTS"
                    }
        }.sumOf { it.amountPaise }

        val limitPaise = budget.effectiveLimitPaise
        val remainingPaise = limitPaise - eligibleSpent
        val usageFraction = if (limitPaise > 0L) (eligibleSpent.toDouble() / limitPaise.toDouble()).toFloat().coerceAtLeast(0f) else 0f
        val usagePct = (usageFraction * 100).toInt()
        val isOver = eligibleSpent > limitPaise
        val isApproaching = usageFraction >= 0.8f && !isOver

        val nowMillis = System.currentTimeMillis()
        val status = when {
            nowMillis < startInclusiveMillis -> "UPCOMING"
            nowMillis > endInclusiveMillis -> "COMPLETED"
            else -> "ACTIVE"
        }

        val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
        val dateRangeLabel = "${startLocalDate.format(dateFormatter)} – ${endLocalDate.format(dateFormatter)}"

        return BudgetSpendingResult(
            spentPaise = eligibleSpent,
            limitPaise = limitPaise,
            remainingPaise = remainingPaise,
            usagePercentage = usagePct,
            usageFraction = usageFraction,
            isOverBudget = isOver,
            isApproaching = isApproaching,
            status = status,
            dateRangeLabel = dateRangeLabel
        )
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
        categoryMap: Map<Long, com.thecorporatedeveloper.moneymap.data.entity.CategoryEntity>
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
