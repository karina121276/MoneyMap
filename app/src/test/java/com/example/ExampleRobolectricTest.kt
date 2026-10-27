package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.model.AccountType
import com.example.core.model.TransactionType
import com.example.data.database.MoneyMapDatabase
import com.example.data.database.populateInitialData
import com.example.data.entity.AccountEntity
import com.example.data.entity.CategoryEntity
import com.example.data.entity.TransactionEntity
import com.example.data.repository.MoneyMapRepository
import com.example.domain.FinancialCalculator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: MoneyMapDatabase
    private lateinit var repo: MoneyMapRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MoneyMapDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = MoneyMapRepository(db, context)
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MoneyMap", appName)
    }

    @Test
    fun `fresh installation contains no financial transactions or fake balances`() {
        val totalAssets = FinancialCalculator.calculateTotalAssetBalance(emptyList(), emptyList())
        val netWorth = FinancialCalculator.calculateNetWorth(emptyList(), emptyList())
        val periodSummary = FinancialCalculator.calculatePeriodSummary(emptyList())

        assertEquals(0L, totalAssets)
        assertEquals(0L, netWorth)
        assertEquals(0L, periodSummary.totalIncomePaise)
        assertEquals(0L, periodSummary.totalExpensePaise)
        assertEquals(0L, periodSummary.netCashFlowPaise)
        assertEquals(0, periodSummary.transactionCount)
    }

    @Test
    fun `database initial data only seeds categories and leaves accounts and transactions empty`() = runBlocking {
        populateInitialData(db)

        val categories = db.categoryDao().getAllCategoriesList()
        val accounts = db.accountDao().getAllAccountsList()
        val transactions = db.transactionDao().getAllTransactionsList()

        assertEquals(25, categories.size)
        assertTrue("Accounts must be empty on fresh install", accounts.isEmpty())
        assertTrue("Transactions must be empty on fresh install", transactions.isEmpty())
    }

    @Test
    fun `user can add custom Expense and Income categories`() = runBlocking {
        populateInitialData(db)

        val customExpense = CategoryEntity(
            name = "Pet Care",
            type = TransactionType.EXPENSE,
            iconName = "pets",
            colorHex = "#10B981",
            isCustom = true
        )
        val expenseId = repo.insertCategory(customExpense)
        assertTrue(expenseId > 0)

        val customIncome = CategoryEntity(
            name = "Tutoring",
            type = TransactionType.INCOME,
            iconName = "school",
            colorHex = "#3B82F6",
            isCustom = true
        )
        val incomeId = repo.insertCategory(customIncome)
        assertTrue(incomeId > 0)

        val allCategories = repo.allCategories.first()
        val foundExp = allCategories.find { it.id == expenseId }
        val foundInc = allCategories.find { it.id == incomeId }

        assertNotNull(foundExp)
        assertEquals("Pet Care", foundExp?.name)
        assertEquals(TransactionType.EXPENSE, foundExp?.type)
        assertTrue(foundExp?.isCustom == true)

        assertNotNull(foundInc)
        assertEquals("Tutoring", foundInc?.name)
        assertEquals(TransactionType.INCOME, foundInc?.type)
        assertTrue(foundInc?.isCustom == true)
    }

    @Test
    fun `duplicate category names are rejected case-insensitively within same type`() = runBlocking {
        populateInitialData(db)

        // "Food & Dining" already exists as EXPENSE
        val isDupSameType = repo.isDuplicateCategoryName("food & dining", TransactionType.EXPENSE)
        assertTrue("Should detect duplicate for same type", isDupSameType)

        val isDupWithWhitespace = repo.isDuplicateCategoryName("  FOOD & DINING  ", TransactionType.EXPENSE)
        assertTrue("Should detect duplicate ignoring whitespace and casing", isDupWithWhitespace)

        // However, "Food & Dining" does not exist under INCOME
        val isDupDiffType = repo.isDuplicateCategoryName("Food & Dining", TransactionType.INCOME)
        assertFalse("Different type should not collide", isDupDiffType)
    }

    @Test
    fun `user can edit category name, icon, and color`() = runBlocking {
        populateInitialData(db)

        val customCat = CategoryEntity(
            name = "Gaming",
            type = TransactionType.EXPENSE,
            iconName = "sports_esports",
            colorHex = "#8B5CF6",
            isCustom = true
        )
        val id = repo.insertCategory(customCat)
        val saved = db.categoryDao().getCategoryById(id)!!

        val updated = saved.copy(
            name = "Video Games & Streaming",
            iconName = "devices",
            colorHex = "#06B6D4"
        )
        repo.updateCategory(updated)

        val reloaded = db.categoryDao().getCategoryById(id)
        assertNotNull(reloaded)
        assertEquals("Video Games & Streaming", reloaded?.name)
        assertEquals("devices", reloaded?.iconName)
        assertEquals("#06B6D4", reloaded?.colorHex)
    }

    @Test
    fun `unused category can be safely deleted`() = runBlocking {
        populateInitialData(db)

        val cat = CategoryEntity(
            name = "Temporary Category",
            type = TransactionType.EXPENSE,
            iconName = "more_horiz",
            colorHex = "#94A3B8",
            isCustom = true
        )
        val catId = repo.insertCategory(cat)
        val saved = db.categoryDao().getCategoryById(catId)!!

        // Check reference count
        val count = repo.getTransactionCountForCategory(catId)
        assertEquals(0, count)

        repo.deleteCategory(saved, reassignToId = null)

        val check = db.categoryDao().getCategoryById(catId)
        assertNull("Category must be deleted", check)
    }

    @Test
    fun `category in use reassignment preserves all transaction data and calculations`() = runBlocking {
        populateInitialData(db)

        // Create an account
        val accId = repo.insertAccount(
            AccountEntity(name = "Checking Bank", type = AccountType.BANK, openingBalancePaise = 500000L)
        )

        // Create 2 categories
        val catOldId = repo.insertCategory(
            CategoryEntity(name = "Old Category", type = TransactionType.EXPENSE, iconName = "restaurant", colorHex = "#EF4444", isCustom = true)
        )
        val catNewId = repo.insertCategory(
            CategoryEntity(name = "New Category", type = TransactionType.EXPENSE, iconName = "shopping_cart", colorHex = "#10B981", isCustom = true)
        )

        // Create transaction linked to old category
        val txId = repo.insertTransaction(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amountPaise = 15000L,
                categoryId = catOldId,
                accountId = accId,
                dateEpochMillis = 1716000000000L,
                merchantOrPayee = "Swiggy Dinner",
                note = "Family dinner"
            )
        )

        // Verify count
        val inUseCount = repo.getTransactionCountForCategory(catOldId)
        assertEquals(1, inUseCount)

        // Delete old category and reassign to new category
        val oldCat = db.categoryDao().getCategoryById(catOldId)!!
        repo.deleteCategory(oldCat, reassignToId = catNewId)

        // Old category is deleted
        assertNull(db.categoryDao().getCategoryById(catOldId))

        // Transaction still exists with same amount, account, date, note, but updated categoryId
        val updatedTx = repo.getTransactionById(txId)
        assertNotNull(updatedTx)
        assertEquals(catNewId, updatedTx?.categoryId)
        assertEquals(15000L, updatedTx?.amountPaise)
        assertEquals(accId, updatedTx?.accountId)
        assertEquals("Swiggy Dinner", updatedTx?.merchantOrPayee)
        assertEquals("Family dinner", updatedTx?.note)

        // Calculation check
        val accounts = repo.allAccounts.first()
        val transactions = repo.allTransactions.first()
        val totalAssets = FinancialCalculator.calculateTotalAssetBalance(accounts, transactions)
        // 500000 opening - 15000 expense = 485000
        assertEquals(485000L, totalAssets)
    }

    @Test
    fun `archiving category hides it from active list while preserving in historical transactions`() = runBlocking {
        populateInitialData(db)

        val catId = repo.insertCategory(
            CategoryEntity(name = "Seasonal Subscriptions", type = TransactionType.EXPENSE, iconName = "subscriptions", colorHex = "#8B5CF6", isCustom = true)
        )

        // Active list includes it
        val activeBefore = repo.allActiveCategories.first()
        assertTrue(activeBefore.any { it.id == catId })

        // Archive it
        repo.archiveCategory(catId)

        // Active list excludes it
        val activeAfter = repo.allActiveCategories.first()
        assertFalse(activeAfter.any { it.id == catId })

        // But full categories list still contains it (for historical display)
        val allCats = repo.allCategories.first()
        val archivedCat = allCats.find { it.id == catId }
        assertNotNull(archivedCat)
        assertTrue(archivedCat?.isArchived == true)
    }

    @Test
    fun `expense and income selectors show only compatible categories`() = runBlocking {
        populateInitialData(db)

        val expenseCategories = repo.getActiveCategoriesByType(TransactionType.EXPENSE).first()
        val incomeCategories = repo.getActiveCategoriesByType(TransactionType.INCOME).first()

        assertTrue(expenseCategories.all { it.type == TransactionType.EXPENSE })
        assertTrue(incomeCategories.all { it.type == TransactionType.INCOME })
        assertTrue(expenseCategories.isNotEmpty())
        assertTrue(incomeCategories.isNotEmpty())
    }

    // =========================================================================
    // SAVINGS ACCOUNT MODEL AND FINANCIAL CALCULATIONS TESTS (Requirement 9)
    // =========================================================================

    @Test
    fun `1 no accounts - savings balance is zero`() {
        val savings = FinancialCalculator.calculateSavingsBalance(emptyList(), emptyList())
        assertEquals(0L, savings)
    }

    @Test
    fun `2 no savings accounts - savings balance is zero even if regular accounts have balances`() {
        val regBank = AccountEntity(id = 1, name = "Checking", type = AccountType.BANK, openingBalancePaise = 1500000L, isSavings = false)
        val cash = AccountEntity(id = 2, name = "Cash", type = AccountType.CASH, openingBalancePaise = 200000L, isSavings = false)
        val accounts = listOf(regBank, cash)

        val savings = FinancialCalculator.calculateSavingsBalance(accounts, emptyList())
        val totalAssets = FinancialCalculator.calculateTotalAssetBalance(accounts, emptyList())

        assertEquals(0L, savings)
        assertEquals(1700000L, totalAssets)
    }

    @Test
    fun `3 one savings account with opening balance reflects opening balance`() {
        val savingsAcc = AccountEntity(id = 1, name = "Savings Bank", type = AccountType.SAVINGS, openingBalancePaise = 5000000L)
        val savings = FinancialCalculator.calculateSavingsBalance(listOf(savingsAcc), emptyList())
        assertEquals(5000000L, savings)
    }

    @Test
    fun `4 multiple savings accounts correctly summed and exclude regular accounts`() {
        val emergency = AccountEntity(id = 1, name = "Emergency Fund", type = AccountType.BANK, isSavings = true, openingBalancePaise = 5000000L)
        val savingsBank = AccountEntity(id = 2, name = "Savings Bank", type = AccountType.SAVINGS, openingBalancePaise = 2500000L)
        val regularBank = AccountEntity(id = 3, name = "Regular Bank Account", type = AccountType.BANK, isSavings = false, openingBalancePaise = 1500000L)
        val cash = AccountEntity(id = 4, name = "Cash Wallet", type = AccountType.CASH, isSavings = false, openingBalancePaise = 200000L)

        val accounts = listOf(emergency, savingsBank, regularBank, cash)

        val savingsBalance = FinancialCalculator.calculateSavingsBalance(accounts, emptyList())
        val totalAssets = FinancialCalculator.calculateTotalAssetBalance(accounts, emptyList())

        // Emergency Fund (₹50,000) + Savings Bank (₹25,000) = ₹75,000 (7500000 paise)
        assertEquals(7500000L, savingsBalance)
        // Total Assets = ₹50,000 + ₹25,000 + ₹15,000 + ₹2,000 = ₹92,000 (9200000 paise)
        assertEquals(9200000L, totalAssets)
    }

    @Test
    fun `5 income transaction to savings account increases savings balance`() {
        val emergency = AccountEntity(id = 1, name = "Emergency Fund", type = AccountType.BANK, isSavings = true, openingBalancePaise = 5000000L)
        val txIncome = TransactionEntity(
            id = 1,
            type = TransactionType.INCOME,
            accountId = 1,
            amountPaise = 1000000L,
            merchantOrPayee = "Interest Credit",
            dateEpochMillis = 1716000000000L
        )

        val savingsBalance = FinancialCalculator.calculateSavingsBalance(listOf(emergency), listOf(txIncome))
        assertEquals(6000000L, savingsBalance) // ₹60,000
    }

    @Test
    fun `6 expense transaction from savings account decreases savings balance`() {
        val emergency = AccountEntity(id = 1, name = "Emergency Fund", type = AccountType.BANK, isSavings = true, openingBalancePaise = 5000000L)
        val txExpense = TransactionEntity(
            id = 1,
            type = TransactionType.EXPENSE,
            accountId = 1,
            amountPaise = 500000L,
            merchantOrPayee = "Emergency Repair",
            dateEpochMillis = 1716000000000L
        )

        val savingsBalance = FinancialCalculator.calculateSavingsBalance(listOf(emergency), listOf(txExpense))
        assertEquals(4500000L, savingsBalance) // ₹45,000
    }

    @Test
    fun `7 transfer from regular account to savings account increases savings balance while combined balance is unchanged`() {
        val regular = AccountEntity(id = 1, name = "Regular Bank", type = AccountType.BANK, isSavings = false, openingBalancePaise = 5000000L)
        val savings = AccountEntity(id = 2, name = "Savings Account", type = AccountType.SAVINGS, openingBalancePaise = 2000000L)
        val accounts = listOf(regular, savings)

        // Transfer ₹10,000 (1000000 paise) from regular (1) to savings (2)
        val transferTx = TransactionEntity(
            id = 1,
            type = TransactionType.TRANSFER,
            accountId = 1,
            toAccountId = 2,
            amountPaise = 1000000L,
            merchantOrPayee = "Self Transfer to Savings",
            dateEpochMillis = 1716000000000L
        )

        val savingsBal = FinancialCalculator.calculateSavingsBalance(accounts, listOf(transferTx))
        val totalAssets = FinancialCalculator.calculateTotalAssetBalance(accounts, listOf(transferTx))
        val periodSummary = FinancialCalculator.calculatePeriodSummary(listOf(transferTx))

        assertEquals(3000000L, savingsBal) // ₹30,000
        assertEquals(7000000L, totalAssets) // ₹70,000 combined remains unchanged
        assertEquals(0L, periodSummary.totalIncomePaise) // Transfer does NOT count as income
        assertEquals(0L, periodSummary.totalExpensePaise) // Transfer does NOT count as expense
        assertEquals(0L, periodSummary.netCashFlowPaise)
    }

    @Test
    fun `8 transfer from savings account to regular account decreases savings balance while combined balance is unchanged`() {
        val regular = AccountEntity(id = 1, name = "Regular Bank", type = AccountType.BANK, isSavings = false, openingBalancePaise = 5000000L)
        val savings = AccountEntity(id = 2, name = "Savings Account", type = AccountType.SAVINGS, openingBalancePaise = 2000000L)
        val accounts = listOf(regular, savings)

        // Transfer ₹5,000 (500000 paise) from savings (2) to regular (1)
        val transferTx = TransactionEntity(
            id = 1,
            type = TransactionType.TRANSFER,
            accountId = 2,
            toAccountId = 1,
            amountPaise = 500000L,
            merchantOrPayee = "Withdrawal to Checking",
            dateEpochMillis = 1716000000000L
        )

        val savingsBal = FinancialCalculator.calculateSavingsBalance(accounts, listOf(transferTx))
        val totalAssets = FinancialCalculator.calculateTotalAssetBalance(accounts, listOf(transferTx))

        assertEquals(1500000L, savingsBal) // ₹15,000 (20k - 5k)
        assertEquals(7000000L, totalAssets) // ₹70,000 combined remains unchanged
    }

    @Test
    fun `9 changing account savings classification changes savings balance without creating transactions`() = runBlocking {
        val acc = AccountEntity(name = "Flexi Account", type = AccountType.BANK, isSavings = false, openingBalancePaise = 5000000L)
        val id = repo.insertAccount(acc)

        val accountsBefore = repo.allAccounts.first()
        val txBefore = repo.allTransactions.first()
        val savingsBefore = FinancialCalculator.calculateSavingsBalance(accountsBefore, txBefore)
        assertEquals(0L, savingsBefore)
        assertTrue(txBefore.isEmpty())

        // Change classification to savings
        val saved = db.accountDao().getAccountById(id)!!
        repo.updateAccount(saved.copy(isSavings = true))

        val accountsAfter = repo.allAccounts.first()
        val txAfter = repo.allTransactions.first()
        val savingsAfter = FinancialCalculator.calculateSavingsBalance(accountsAfter, txAfter)

        assertEquals(5000000L, savingsAfter) // Now counts ₹50,000 towards savings
        assertTrue("Changing classification must never create transactions", txAfter.isEmpty())
        assertEquals(5000000L, FinancialCalculator.calculateTotalAssetBalance(accountsAfter, txAfter))
    }

    @Test
    fun `10 editing and deleting transactions updates savings balance correctly`() {
        val savings = AccountEntity(id = 1, name = "Emergency Fund", type = AccountType.SAVINGS, openingBalancePaise = 5000000L)
        val tx1 = TransactionEntity(
            id = 1,
            type = TransactionType.INCOME,
            accountId = 1,
            amountPaise = 2000000L,
            merchantOrPayee = "Bonus",
            dateEpochMillis = 1716000000000L
        )

        // With tx1
        assertEquals(7000000L, FinancialCalculator.calculateSavingsBalance(listOf(savings), listOf(tx1)))

        // Edit transaction amount from ₹20,000 to ₹10,000
        val editedTx1 = tx1.copy(amountPaise = 1000000L)
        assertEquals(6000000L, FinancialCalculator.calculateSavingsBalance(listOf(savings), listOf(editedTx1)))

        // Delete transaction
        assertEquals(5000000L, FinancialCalculator.calculateSavingsBalance(listOf(savings), emptyList()))
    }
}

