package com.example

import com.example.core.model.TransactionType
import com.example.data.entity.TransactionEntity
import com.example.domain.FinancialCalculator
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class TransactionDateTimeTest {

    @Test
    fun testCreateTransactionWithCustomDateAndTime() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, 26)
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val tx = TransactionEntity(
            type = TransactionType.EXPENSE,
            amountPaise = 50000L, // ₹500
            accountId = 1L,
            categoryId = 2L,
            dateEpochMillis = cal.timeInMillis,
            merchantOrPayee = "Swiggy"
        )

        assertEquals(cal.timeInMillis, tx.dateEpochMillis)

        val verifyCal = Calendar.getInstance().apply { timeInMillis = tx.dateEpochMillis }
        assertEquals(2026, verifyCal.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, verifyCal.get(Calendar.MONTH))
        assertEquals(26, verifyCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(14, verifyCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, verifyCal.get(Calendar.MINUTE))
    }

    @Test
    fun testEditOnlyDatePreservesTime() {
        // Original: 26 Oct 2026 at 14:30
        val originalCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 26, 14, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val originalTx = TransactionEntity(
            type = TransactionType.EXPENSE,
            amountPaise = 25000L,
            accountId = 1L,
            dateEpochMillis = originalCal.timeInMillis,
            merchantOrPayee = "Groceries"
        )

        // Change ONLY date to 05 Nov 2026, keeping hour (14) and minute (30)
        val updatedCal = Calendar.getInstance().apply {
            timeInMillis = originalTx.dateEpochMillis
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 5)
        }

        val updatedTx = originalTx.copy(dateEpochMillis = updatedCal.timeInMillis)

        val verifyCal = Calendar.getInstance().apply { timeInMillis = updatedTx.dateEpochMillis }
        assertEquals(2026, verifyCal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, verifyCal.get(Calendar.MONTH))
        assertEquals(5, verifyCal.get(Calendar.DAY_OF_MONTH))
        // Time preserved!
        assertEquals(14, verifyCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, verifyCal.get(Calendar.MINUTE))
    }

    @Test
    fun testEditOnlyTimePreservesDate() {
        // Original: 26 Oct 2026 at 14:30
        val originalCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 26, 14, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val originalTx = TransactionEntity(
            type = TransactionType.EXPENSE,
            amountPaise = 25000L,
            accountId = 1L,
            dateEpochMillis = originalCal.timeInMillis,
            merchantOrPayee = "Groceries"
        )

        // Change ONLY time to 09:15 AM
        val updatedCal = Calendar.getInstance().apply {
            timeInMillis = originalTx.dateEpochMillis
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 15)
        }

        val updatedTx = originalTx.copy(dateEpochMillis = updatedCal.timeInMillis)

        val verifyCal = Calendar.getInstance().apply { timeInMillis = updatedTx.dateEpochMillis }
        // Date preserved!
        assertEquals(2026, verifyCal.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, verifyCal.get(Calendar.MONTH))
        assertEquals(26, verifyCal.get(Calendar.DAY_OF_MONTH))
        // Time updated!
        assertEquals(9, verifyCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(15, verifyCal.get(Calendar.MINUTE))
    }

    @Test
    fun testEditBothDateAndTime() {
        val originalCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 26, 14, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val originalTx = TransactionEntity(
            type = TransactionType.EXPENSE,
            amountPaise = 100000L,
            accountId = 1L,
            dateEpochMillis = originalCal.timeInMillis,
            merchantOrPayee = "Shopping"
        )

        val updatedCal = Calendar.getInstance().apply {
            set(2026, Calendar.DECEMBER, 15, 20, 45, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val updatedTx = originalTx.copy(dateEpochMillis = updatedCal.timeInMillis)

        val verifyCal = Calendar.getInstance().apply { timeInMillis = updatedTx.dateEpochMillis }
        assertEquals(2026, verifyCal.get(Calendar.YEAR))
        assertEquals(Calendar.DECEMBER, verifyCal.get(Calendar.MONTH))
        assertEquals(15, verifyCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(20, verifyCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(45, verifyCal.get(Calendar.MINUTE))
    }

    @Test
    fun testTransactionsNearMidnightAndSorting() {
        val nearMidnightBefore = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 26, 23, 59, 30)
        }.timeInMillis

        val nearMidnightAfter = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 27, 0, 0, 30)
        }.timeInMillis

        val tx1 = TransactionEntity(id = 1, type = TransactionType.EXPENSE, amountPaise = 1000L, accountId = 1L, dateEpochMillis = nearMidnightBefore, merchantOrPayee = "Late Snack")
        val tx2 = TransactionEntity(id = 2, type = TransactionType.EXPENSE, amountPaise = 2000L, accountId = 1L, dateEpochMillis = nearMidnightAfter, merchantOrPayee = "Early Coffee")

        assertTrue(tx2.dateEpochMillis > tx1.dateEpochMillis)

        val list = listOf(tx1, tx2).sortedByDescending { it.dateEpochMillis }
        assertEquals(2L, list[0].id)
        assertEquals(1L, list[1].id)
    }

    @Test
    fun testMonthAndYearBoundaryTransitions() {
        val dec31 = Calendar.getInstance().apply {
            set(2026, Calendar.DECEMBER, 31, 23, 59, 59)
        }.timeInMillis

        val jan01 = Calendar.getInstance().apply {
            set(2027, Calendar.JANUARY, 1, 0, 0, 1)
        }.timeInMillis

        val tx2026 = TransactionEntity(id = 1, type = TransactionType.EXPENSE, amountPaise = 50000L, accountId = 1L, dateEpochMillis = dec31, merchantOrPayee = "NYE Party")
        val tx2027 = TransactionEntity(id = 2, type = TransactionType.INCOME, amountPaise = 100000L, accountId = 1L, dateEpochMillis = jan01, merchantOrPayee = "New Year Gift")

        // 2026 Summary
        val summary2026 = FinancialCalculator.calculatePeriodSummary(listOf(tx2026))
        assertEquals(50000L, summary2026.totalExpensePaise)
        assertEquals(0L, summary2026.totalIncomePaise)

        // 2027 Summary
        val summary2027 = FinancialCalculator.calculatePeriodSummary(listOf(tx2027))
        assertEquals(0L, summary2027.totalExpensePaise)
        assertEquals(100000L, summary2027.totalIncomePaise)
    }

    @Test
    fun testReportTotalsAfterChangingTransactionDate() {
        val octDate = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 26, 12, 0, 0)
        }.timeInMillis

        val novDate = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 1, 12, 0, 0)
        }.timeInMillis

        val tx = TransactionEntity(id = 10, type = TransactionType.EXPENSE, amountPaise = 30000L, accountId = 1L, dateEpochMillis = octDate, merchantOrPayee = "Utility Bill")

        // Before move: October has tx
        val octTxBefore = listOf(tx).filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.dateEpochMillis }
            cal.get(Calendar.MONTH) == Calendar.OCTOBER
        }
        assertEquals(1, octTxBefore.size)

        // Move transaction from October to November
        val movedTx = tx.copy(dateEpochMillis = novDate)

        val octTxAfter = listOf(movedTx).filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.dateEpochMillis }
            cal.get(Calendar.MONTH) == Calendar.OCTOBER
        }
        val novTxAfter = listOf(movedTx).filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.dateEpochMillis }
            cal.get(Calendar.MONTH) == Calendar.NOVEMBER
        }

        assertEquals(0, octTxAfter.size)
        assertEquals(1, novTxAfter.size)
        assertEquals(30000L, novTxAfter[0].amountPaise)
    }
}
