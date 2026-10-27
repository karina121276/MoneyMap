package com.thecorporatedeveloper.moneymap

import com.thecorporatedeveloper.moneymap.core.model.TransactionType
import com.thecorporatedeveloper.moneymap.data.entity.BudgetEntity
import com.thecorporatedeveloper.moneymap.data.entity.TransactionEntity
import com.thecorporatedeveloper.moneymap.data.preferences.UserPreferences
import org.junit.Assert.*
import org.junit.Test
import java.time.YearMonth
import java.time.ZoneId

class AdvancedBudgetAndSettingsUnitTest {

    @Test
    fun testAllAccountsBudgetSpentCalculation() {
        val ym = YearMonth.of(2026, 10)
        val zone = ZoneId.systemDefault()
        val monthStart = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val budget = BudgetEntity(
            id = 1L,
            name = "Overall October Budget",
            monthYear = "2026-10",
            amountLimitPaise = 5000000L, // ₹50,000
            scope = "ALL_ACCOUNTS"
        )

        val transactions = listOf(
            // Expense in Oct 2026
            TransactionEntity(id = 101L, type = TransactionType.EXPENSE, amountPaise = 1500000L, accountId = 1L, dateEpochMillis = monthStart + 10000L),
            TransactionEntity(id = 102L, type = TransactionType.EXPENSE, amountPaise = 1000000L, accountId = 2L, dateEpochMillis = monthStart + 20000L),
            // Income in Oct 2026 (must be excluded)
            TransactionEntity(id = 103L, type = TransactionType.INCOME, amountPaise = 3000000L, accountId = 1L, dateEpochMillis = monthStart + 30000L),
            // Transfer in Oct 2026 (must be excluded)
            TransactionEntity(id = 104L, type = TransactionType.TRANSFER, amountPaise = 500000L, accountId = 1L, toAccountId = 2L, dateEpochMillis = monthStart + 40000L)
        )

        val start = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = ym.atEndOfMonth().atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

        val spentPaise = transactions.filter { tx ->
            tx.type == TransactionType.EXPENSE &&
                    tx.dateEpochMillis in start..end
        }.sumOf { it.amountPaise }

        assertEquals(2500000L, spentPaise) // ₹25,000 spent
        val remainingPaise = budget.effectiveLimitPaise - spentPaise
        assertEquals(2500000L, remainingPaise) // ₹25,000 remaining
    }

    @Test
    fun testBankWiseAccountBudgetCalculation() {
        val ym = YearMonth.of(2026, 10)
        val zone = ZoneId.systemDefault()
        val monthStart = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val hdfcAccount = 10L
        val sbiAccount = 20L

        val hdfcBudget = BudgetEntity(
            id = 2L,
            name = "HDFC Monthly Budget",
            monthYear = "2026-10",
            amountLimitPaise = 2000000L, // ₹20,000
            scope = "ACCOUNT",
            accountId = hdfcAccount
        )

        val transactions = listOf(
            TransactionEntity(id = 201L, type = TransactionType.EXPENSE, amountPaise = 500000L, accountId = hdfcAccount, dateEpochMillis = monthStart + 1000L),
            TransactionEntity(id = 202L, type = TransactionType.EXPENSE, amountPaise = 800000L, accountId = sbiAccount, dateEpochMillis = monthStart + 2000L),
            TransactionEntity(id = 203L, type = TransactionType.EXPENSE, amountPaise = 300000L, accountId = hdfcAccount, dateEpochMillis = monthStart + 3000L)
        )

        val start = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = ym.atEndOfMonth().atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

        val hdfcSpent = transactions.filter { tx ->
            tx.type == TransactionType.EXPENSE &&
                    tx.dateEpochMillis in start..end &&
                    tx.accountId == hdfcBudget.accountId
        }.sumOf { it.amountPaise }

        assertEquals(800000L, hdfcSpent) // ₹8,000 spent on HDFC
        val remaining = hdfcBudget.effectiveLimitPaise - hdfcSpent
        assertEquals(1200000L, remaining) // ₹12,000 remaining
    }

    @Test
    fun testCategorySpecificBudgetCalculation() {
        val ym = YearMonth.of(2026, 10)
        val zone = ZoneId.systemDefault()
        val monthStart = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val foodCategoryId = 5L
        val shoppingCategoryId = 6L

        val foodBudget = BudgetEntity(
            id = 3L,
            name = "Dining & Food Budget",
            monthYear = "2026-10",
            amountLimitPaise = 1000000L, // ₹10,000
            scope = "CATEGORY",
            categoryId = foodCategoryId
        )

        val transactions = listOf(
            TransactionEntity(id = 301L, type = TransactionType.EXPENSE, amountPaise = 400000L, accountId = 1L, categoryId = foodCategoryId, dateEpochMillis = monthStart + 100L),
            TransactionEntity(id = 302L, type = TransactionType.EXPENSE, amountPaise = 700000L, accountId = 1L, categoryId = shoppingCategoryId, dateEpochMillis = monthStart + 200L),
            TransactionEntity(id = 303L, type = TransactionType.EXPENSE, amountPaise = 300000L, accountId = 2L, categoryId = foodCategoryId, dateEpochMillis = monthStart + 300L)
        )

        val start = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = ym.atEndOfMonth().atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

        val foodSpent = transactions.filter { tx ->
            tx.type == TransactionType.EXPENSE &&
                    tx.dateEpochMillis in start..end &&
                    tx.categoryId == foodBudget.categoryId
        }.sumOf { it.amountPaise }

        assertEquals(700000L, foodSpent) // ₹7,000 spent on food
        val fraction = foodSpent.toDouble() / foodBudget.effectiveLimitPaise.toDouble()
        assertEquals(0.70, fraction, 0.001)
    }

    @Test
    fun testAccentColorsSelection() {
        val prefs = UserPreferences(accentColorHex = "#00897B")
        assertEquals("#00897B", prefs.accentColorHex)
    }
}
