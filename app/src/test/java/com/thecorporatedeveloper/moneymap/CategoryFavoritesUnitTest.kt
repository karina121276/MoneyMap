package com.thecorporatedeveloper.moneymap

import com.thecorporatedeveloper.moneymap.core.model.TransactionType
import com.thecorporatedeveloper.moneymap.data.entity.CategoryEntity
import com.thecorporatedeveloper.moneymap.data.entity.TransactionEntity
import org.junit.Assert.*
import org.junit.Test

class CategoryFavoritesUnitTest {

    @Test
    fun testDefaultCategoryFavoriteState() {
        val cat = CategoryEntity(
            name = "Groceries",
            type = TransactionType.EXPENSE,
            iconName = "shopping_cart",
            colorHex = "#FF4081"
        )
        assertFalse(cat.isFavorite)
    }

    @Test
    fun testToggleCategoryFavorite() {
        val cat = CategoryEntity(
            id = 10L,
            name = "Dining Out",
            type = TransactionType.EXPENSE,
            iconName = "restaurant",
            colorHex = "#FF9800",
            isFavorite = false
        )

        val favoritedCat = cat.copy(isFavorite = true)
        assertTrue(favoritedCat.isFavorite)

        val unfavoritedCat = favoritedCat.copy(isFavorite = false)
        assertFalse(unfavoritedCat.isFavorite)
    }

    @Test
    fun testExpenseAndIncomeFavoritesSeparation() {
        val categories = listOf(
            CategoryEntity(id = 1L, name = "Food", type = TransactionType.EXPENSE, iconName = "fastfood", colorHex = "#FF0000", isFavorite = true),
            CategoryEntity(id = 2L, name = "Rent", type = TransactionType.EXPENSE, iconName = "home", colorHex = "#00FF00", isFavorite = false),
            CategoryEntity(id = 3L, name = "Salary", type = TransactionType.INCOME, iconName = "work", colorHex = "#0000FF", isFavorite = true),
            CategoryEntity(id = 4L, name = "Bonus", type = TransactionType.INCOME, iconName = "card_giftcard", colorHex = "#FFFF00", isFavorite = false)
        )

        val favoriteExpenses = categories.filter { it.type == TransactionType.EXPENSE && it.isFavorite }
        val favoriteIncomes = categories.filter { it.type == TransactionType.INCOME && it.isFavorite }

        assertEquals(1, favoriteExpenses.size)
        assertEquals("Food", favoriteExpenses[0].name)

        assertEquals(1, favoriteIncomes.size)
        assertEquals("Salary", favoriteIncomes[0].name)
    }

    @Test
    fun testAddTransactionShowsOnlyFavoriteCategoriesByDefault() {
        val categories = listOf(
            CategoryEntity(id = 1L, name = "Food", type = TransactionType.EXPENSE, iconName = "fastfood", colorHex = "#FF0000", isFavorite = true),
            CategoryEntity(id = 2L, name = "Transport", type = TransactionType.EXPENSE, iconName = "directions_car", colorHex = "#00FF00", isFavorite = true),
            CategoryEntity(id = 3L, name = "Rent", type = TransactionType.EXPENSE, iconName = "home", colorHex = "#0000FF", isFavorite = false)
        )

        // Add Transaction (initialTransaction == null)
        val filteredForExpense = categories.filter {
            it.type == TransactionType.EXPENSE && (!it.isArchived) && it.isFavorite
        }

        assertEquals(2, filteredForExpense.size)
        assertTrue(filteredForExpense.all { it.isFavorite })
        assertFalse(filteredForExpense.any { it.name == "Rent" })
    }

    @Test
    fun testEditTransactionPreservesNonFavoriteSelectedCategory() {
        val nonFavRentCategory = CategoryEntity(
            id = 3L,
            name = "Rent",
            type = TransactionType.EXPENSE,
            iconName = "home",
            colorHex = "#0000FF",
            isFavorite = false
        )
        val categories = listOf(
            CategoryEntity(id = 1L, name = "Food", type = TransactionType.EXPENSE, iconName = "fastfood", colorHex = "#FF0000", isFavorite = true),
            nonFavRentCategory
        )

        val existingTransaction = TransactionEntity(
            id = 100L,
            type = TransactionType.EXPENSE,
            amountPaise = 500000L,
            accountId = 1L,
            categoryId = nonFavRentCategory.id,
            dateEpochMillis = System.currentTimeMillis()
        )

        // When editing an existing transaction, non-favourite selected category must be included
        val filteredForEdit = categories.filter {
            it.type == TransactionType.EXPENSE && (!it.isArchived || it.id == existingTransaction.categoryId) && (it.isFavorite || it.id == existingTransaction.categoryId)
        }

        assertEquals(2, filteredForEdit.size)
        assertTrue(filteredForEdit.any { it.id == nonFavRentCategory.id })
    }

    @Test
    fun testCategoryManagementSortingFavoritesFirst() {
        val categories = listOf(
            CategoryEntity(id = 1L, name = "Shopping", type = TransactionType.EXPENSE, iconName = "shopping_bag", colorHex = "#FF0000", isFavorite = false),
            CategoryEntity(id = 2L, name = "Bills", type = TransactionType.EXPENSE, iconName = "receipt", colorHex = "#00FF00", isFavorite = true),
            CategoryEntity(id = 3L, name = "Auto", type = TransactionType.EXPENSE, iconName = "directions_car", colorHex = "#0000FF", isFavorite = true)
        )

        val sorted = categories.sortedWith(compareByDescending<CategoryEntity> { it.isFavorite }.thenBy { it.name })

        assertEquals("Auto", sorted[0].name)
        assertTrue(sorted[0].isFavorite)
        assertEquals("Bills", sorted[1].name)
        assertTrue(sorted[1].isFavorite)
        assertEquals("Shopping", sorted[2].name)
        assertFalse(sorted[2].isFavorite)
    }
}
