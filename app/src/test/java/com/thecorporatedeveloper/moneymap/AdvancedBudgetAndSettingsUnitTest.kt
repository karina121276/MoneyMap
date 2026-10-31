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

    @Test
    fun testBudgetStartDateAndEndDateInclusiveCalculation() {
        val zone = ZoneId.systemDefault()
        val startDate = java.time.LocalDate.of(2026, 9, 2)
        val endDate = java.time.LocalDate.of(2026, 9, 23)
        val startMillis = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = endDate.atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()

        val budget = BudgetEntity(
            id = 5L,
            name = "Trip Budget",
            monthYear = "2026-09",
            amountLimitPaise = 3000000L, // ₹30,000
            startDateEpochMillis = startMillis,
            endDateEpochMillis = endMillis,
            scope = "ALL_ACCOUNTS"
        )

        val beforeBudget = startDate.minusDays(1).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val onFirstDay = startDate.atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
        val onMiddleDay = java.time.LocalDate.of(2026, 9, 15).atTime(14, 0).atZone(zone).toInstant().toEpochMilli()
        val onLastDay = endDate.atTime(22, 30).atZone(zone).toInstant().toEpochMilli()
        val afterBudget = endDate.plusDays(1).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()

        val txs = listOf(
            TransactionEntity(id = 1L, type = TransactionType.EXPENSE, amountPaise = 500000L, accountId = 1L, dateEpochMillis = beforeBudget), // ₹5,000 (excluded)
            TransactionEntity(id = 2L, type = TransactionType.EXPENSE, amountPaise = 300000L, accountId = 1L, dateEpochMillis = onFirstDay),    // ₹3,000 (included)
            TransactionEntity(id = 3L, type = TransactionType.EXPENSE, amountPaise = 1000000L, accountId = 1L, dateEpochMillis = onMiddleDay), // ₹10,000 (included)
            TransactionEntity(id = 4L, type = TransactionType.EXPENSE, amountPaise = 200000L, accountId = 1L, dateEpochMillis = onLastDay),    // ₹2,000 (included)
            TransactionEntity(id = 5L, type = TransactionType.EXPENSE, amountPaise = 400000L, accountId = 1L, dateEpochMillis = afterBudget)    // ₹4,000 (excluded)
        )

        val result = com.thecorporatedeveloper.moneymap.domain.FinancialCalculator.calculateBudgetSpending(budget, txs)
        assertEquals(1500000L, result.spentPaise) // 3,000 + 10,000 + 2,000 = 15,000
        assertEquals(3000000L, result.limitPaise)
        assertEquals(1500000L, result.remainingPaise)
        assertEquals(0.5f, result.usageFraction, 0.001f)
        assertEquals(50, result.usagePercentage)
        assertFalse(result.isOverBudget)
    }

    @Test
    fun testBudgetStatusUpcomingActiveCompleted() {
        val now = System.currentTimeMillis()
        val dayMillis = 24 * 60 * 60 * 1000L

        // Upcoming: starts in future
        val upcomingBudget = BudgetEntity(
            id = 10L,
            monthYear = "2026-10",
            startDateEpochMillis = now + 5 * dayMillis,
            endDateEpochMillis = now + 15 * dayMillis,
            amountLimitPaise = 100000L
        )
        val upcomingResult = com.thecorporatedeveloper.moneymap.domain.FinancialCalculator.calculateBudgetSpending(upcomingBudget, emptyList())
        assertEquals("UPCOMING", upcomingResult.status)

        // Active: spans across now
        val activeBudget = BudgetEntity(
            id = 11L,
            monthYear = "2026-10",
            startDateEpochMillis = now - 2 * dayMillis,
            endDateEpochMillis = now + 5 * dayMillis,
            amountLimitPaise = 100000L
        )
        val activeResult = com.thecorporatedeveloper.moneymap.domain.FinancialCalculator.calculateBudgetSpending(activeBudget, emptyList())
        assertEquals("ACTIVE", activeResult.status)

        // Completed: ended in the past
        val completedBudget = BudgetEntity(
            id = 12L,
            monthYear = "2026-10",
            startDateEpochMillis = now - 15 * dayMillis,
            endDateEpochMillis = now - 2 * dayMillis,
            amountLimitPaise = 100000L
        )
        val completedResult = com.thecorporatedeveloper.moneymap.domain.FinancialCalculator.calculateBudgetSpending(completedBudget, emptyList())
        assertEquals("COMPLETED", completedResult.status)
    }

    @Test
    fun testCustomQuickAmountsListParsingAndFallback() {
        val defaultPrefs = UserPreferences()
        assertEquals(listOf(100L, 500L, 1000L, 2000L, 5000L), defaultPrefs.quickAmountsList)

        val customPrefs = UserPreferences(quickAmounts = "250, 750, 1500, 3000")
        assertEquals(listOf(250L, 750L, 1500L, 3000L), customPrefs.quickAmountsList)

        val invalidPrefs = UserPreferences(quickAmounts = "abc, 0, -50")
        assertEquals(listOf(100L, 500L, 1000L, 2000L, 5000L), invalidPrefs.quickAmountsList)
    }
}
