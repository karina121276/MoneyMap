package com.example

import com.example.core.model.AccountType
import com.example.core.model.TransactionType
import com.example.data.entity.AccountEntity
import com.example.data.entity.TransactionEntity
import com.example.domain.FinancialCalculator
import org.junit.Assert.*
import org.junit.Test

class FavoritesUnitTest {

    @Test
    fun testDefaultIsFavoriteIsFalse() {
        val tx = TransactionEntity(
            type = TransactionType.EXPENSE,
            amountPaise = 150000L, // ₹1,500
            accountId = 1L,
            dateEpochMillis = System.currentTimeMillis(),
            merchantOrPayee = "Scooty Servicing"
        )

        assertFalse(tx.isFavorite)
    }

    @Test
    fun testMarkAndUnmarkFavorite() {
        val tx = TransactionEntity(
            id = 1L,
            type = TransactionType.EXPENSE,
            amountPaise = 150000L,
            accountId = 1L,
            dateEpochMillis = System.currentTimeMillis(),
            merchantOrPayee = "Scooty Servicing",
            isFavorite = false
        )

        // Mark as favorite
        val favoritedTx = tx.copy(isFavorite = true)
        assertTrue(favoritedTx.isFavorite)

        // Unmark favorite
        val unfavoritedTx = favoritedTx.copy(isFavorite = false)
        assertFalse(unfavoritedTx.isFavorite)
    }

    @Test
    fun testFavoritesListOrderingNewestFirst() {
        val now = System.currentTimeMillis()
        val tx1 = TransactionEntity(
            id = 1L,
            type = TransactionType.EXPENSE,
            amountPaise = 50000L,
            accountId = 1L,
            dateEpochMillis = now - 86400000L, // 1 day ago
            merchantOrPayee = "Scooty Servicing",
            isFavorite = true
        )

        val tx2 = TransactionEntity(
            id = 2L,
            type = TransactionType.EXPENSE,
            amountPaise = 120000L,
            accountId = 1L,
            dateEpochMillis = now, // Today
            merchantOrPayee = "Laptop Repair",
            isFavorite = true
        )

        val favoritesList = listOf(tx1, tx2).filter { it.isFavorite }.sortedByDescending { it.dateEpochMillis }

        assertEquals(2, favoritesList.size)
        assertEquals(2L, favoritesList[0].id) // Newest first
        assertEquals(1L, favoritesList[1].id)
    }

    @Test
    fun testEditingFavoriteTransactionPreservesFavoriteState() {
        val favoriteTx = TransactionEntity(
            id = 5L,
            type = TransactionType.EXPENSE,
            amountPaise = 150000L,
            accountId = 1L,
            dateEpochMillis = System.currentTimeMillis(),
            merchantOrPayee = "Scooty Servicing",
            note = "Engine oil change",
            isFavorite = true
        )

        // Edit amount and note
        val editedTx = favoriteTx.copy(
            amountPaise = 180000L,
            note = "Engine oil & brake pad change"
        )

        assertTrue(editedTx.isFavorite)
        assertEquals(180000L, editedTx.amountPaise)
        assertEquals("Engine oil & brake pad change", editedTx.note)
    }

    @Test
    fun testDeletingFavoriteTransactionRemovesFromList() {
        val tx1 = TransactionEntity(id = 1L, type = TransactionType.EXPENSE, amountPaise = 1000L, accountId = 1L, dateEpochMillis = System.currentTimeMillis(), isFavorite = true)
        val tx2 = TransactionEntity(id = 2L, type = TransactionType.EXPENSE, amountPaise = 2000L, accountId = 1L, dateEpochMillis = System.currentTimeMillis(), isFavorite = true)

        val initialList = mutableListOf(tx1, tx2)
        // Delete tx1
        initialList.removeIf { it.id == 1L }

        assertEquals(1, initialList.size)
        assertEquals(2L, initialList[0].id)
    }

    @Test
    fun testTogglingFavoriteDoesNotAffectAccountBalancesOrReports() {
        val account = AccountEntity(id = 1L, name = "HDFC Bank", type = AccountType.BANK, openingBalancePaise = 500000L)

        val tx1 = TransactionEntity(id = 1L, type = TransactionType.EXPENSE, amountPaise = 150000L, accountId = 1L, dateEpochMillis = System.currentTimeMillis(), isFavorite = false)

        val balanceBefore = FinancialCalculator.calculateAccountBalance(account, listOf(tx1))
        val summaryBefore = FinancialCalculator.calculatePeriodSummary(listOf(tx1))

        // Toggle star to true
        val favoritedTx = tx1.copy(isFavorite = true)

        val balanceAfter = FinancialCalculator.calculateAccountBalance(account, listOf(favoritedTx))
        val summaryAfter = FinancialCalculator.calculatePeriodSummary(listOf(favoritedTx))

        assertEquals(balanceBefore, balanceAfter)
        assertEquals(summaryBefore.totalExpensePaise, summaryAfter.totalExpensePaise)
        assertEquals(summaryBefore.totalIncomePaise, summaryAfter.totalIncomePaise)
    }

    @Test
    fun testFavoritesSearchFiltering() {
        val tx1 = TransactionEntity(id = 1L, type = TransactionType.EXPENSE, amountPaise = 150000L, accountId = 1L, dateEpochMillis = System.currentTimeMillis(), merchantOrPayee = "Scooty Servicing", isFavorite = true)
        val tx2 = TransactionEntity(id = 2L, type = TransactionType.EXPENSE, amountPaise = 300000L, accountId = 1L, dateEpochMillis = System.currentTimeMillis(), merchantOrPayee = "Laptop Repair", isFavorite = true)

        val favorites = listOf(tx1, tx2)

        val query = "scooty"
        val filtered = favorites.filter { it.merchantOrPayee.contains(query, ignoreCase = true) }

        assertEquals(1, filtered.size)
        assertEquals("Scooty Servicing", filtered[0].merchantOrPayee)
    }
}
