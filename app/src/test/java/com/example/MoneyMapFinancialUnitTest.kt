package com.example

import com.example.backup.BackupManager
import com.example.core.model.AccountType
import com.example.core.model.CurrencyFormatter
import com.example.core.model.TransactionType
import com.example.data.entity.AccountEntity
import com.example.data.entity.BudgetEntity
import com.example.data.entity.CategoryEntity
import com.example.data.entity.TransactionEntity
import com.example.domain.FinancialCalculator
import com.example.importer.SmartEntryParser
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MoneyMapFinancialUnitTest {

    @Test
    fun testCurrencyFormatting() {
        assertEquals("₹1,24,320", CurrencyFormatter.formatRupees(12432000L))
        assertEquals("₹85,750", CurrencyFormatter.formatRupees(8575000L))
        assertEquals("-₹637", CurrencyFormatter.formatRupees(-63700L))
        assertEquals("••••••", CurrencyFormatter.formatRupees(12432000L, hideAmounts = true))
    }

    @Test
    fun testTransactionAmountFormatting() {
        assertEquals("-₹2,000", CurrencyFormatter.formatTransactionAmount(200000L, TransactionType.EXPENSE))
        assertEquals("+₹350", CurrencyFormatter.formatTransactionAmount(35000L, TransactionType.INCOME))
        assertEquals("₹3,000", CurrencyFormatter.formatTransactionAmount(300000L, TransactionType.TRANSFER))
    }

    @Test
    fun testIncomeIncreasesAccountBalance() {
        val account = AccountEntity(id = 1, name = "HDFC Bank", type = AccountType.BANK, openingBalancePaise = 1000000L) // ₹10,000
        val incomeTx = TransactionEntity(
            id = 10,
            type = TransactionType.INCOME,
            amountPaise = 500000L, // +₹5,000
            accountId = 1,
            dateEpochMillis = System.currentTimeMillis(),
            merchantOrPayee = "Salary"
        )
        val balance = FinancialCalculator.calculateAccountBalance(account, listOf(incomeTx))
        assertEquals(1500000L, balance) // ₹15,000
    }

    @Test
    fun testExpenseDecreasesAccountBalance() {
        val account = AccountEntity(id = 1, name = "HDFC Bank", type = AccountType.BANK, openingBalancePaise = 1000000L) // ₹10,000
        val expenseTx = TransactionEntity(
            id = 11,
            type = TransactionType.EXPENSE,
            amountPaise = 250000L, // -₹2,500
            accountId = 1,
            dateEpochMillis = System.currentTimeMillis(),
            merchantOrPayee = "Groceries"
        )
        val balance = FinancialCalculator.calculateAccountBalance(account, listOf(expenseTx))
        assertEquals(750000L, balance) // ₹7,500
    }

    @Test
    fun testTransferAffectsBothAccountsAndPreservesTotalAssets() {
        val hdfc = AccountEntity(id = 1, name = "HDFC Bank", type = AccountType.BANK, openingBalancePaise = 2000000L) // ₹20,000
        val cash = AccountEntity(id = 2, name = "Cash Wallet", type = AccountType.CASH, openingBalancePaise = 500000L) // ₹5,000

        val transferTx = TransactionEntity(
            id = 12,
            type = TransactionType.TRANSFER,
            amountPaise = 500000L, // ₹5,000
            accountId = 1, // source
            toAccountId = 2, // dest
            dateEpochMillis = System.currentTimeMillis(),
            merchantOrPayee = "ATM Withdrawal"
        )

        val txList = listOf(transferTx)
        val hdfcBalance = FinancialCalculator.calculateAccountBalance(hdfc, txList)
        val cashBalance = FinancialCalculator.calculateAccountBalance(cash, txList)

        assertEquals(1500000L, hdfcBalance) // ₹15,000
        assertEquals(1000000L, cashBalance) // ₹10,000

        val totalAssets = FinancialCalculator.calculateTotalAssetBalance(listOf(hdfc, cash), txList)
        assertEquals(2500000L, totalAssets) // ₹25,000 total assets preserved!
    }

    @Test
    fun testTransfersExcludedFromPeriodIncomeAndExpense() {
        val transferTx = TransactionEntity(
            id = 1,
            type = TransactionType.TRANSFER,
            amountPaise = 1000000L,
            accountId = 1,
            toAccountId = 2,
            dateEpochMillis = System.currentTimeMillis(),
            merchantOrPayee = "Transfer"
        )
        val summary = FinancialCalculator.calculatePeriodSummary(listOf(transferTx))
        assertEquals(0L, summary.totalIncomePaise)
        assertEquals(0L, summary.totalExpensePaise)
        assertEquals(0L, summary.netCashFlowPaise)
    }

    @Test
    fun testSavingsRateCalculation() {
        val income = TransactionEntity(
            id = 1,
            type = TransactionType.INCOME,
            amountPaise = 10000000L, // ₹1,00,000
            accountId = 1,
            dateEpochMillis = System.currentTimeMillis(),
            merchantOrPayee = "Salary"
        )
        val expense = TransactionEntity(
            id = 2,
            type = TransactionType.EXPENSE,
            amountPaise = 6000000L, // ₹60,000
            accountId = 1,
            dateEpochMillis = System.currentTimeMillis(),
            merchantOrPayee = "Rent"
        )
        val summary = FinancialCalculator.calculatePeriodSummary(listOf(income, expense))
        assertEquals(10000000L, summary.totalIncomePaise)
        assertEquals(6000000L, summary.totalExpensePaise)
        assertEquals(4000000L, summary.netCashFlowPaise)
        assertEquals(40.0, summary.savingsRatePercentage, 0.001)
    }

    @Test
    fun testCreditCardLiabilityAccounting() {
        val card = AccountEntity(id = 3, name = "HDFC Credit Card", type = AccountType.CREDIT_CARD, openingBalancePaise = 0L)
        val purchase = TransactionEntity(
            id = 1,
            type = TransactionType.EXPENSE,
            amountPaise = 500000L, // ₹5,000 spent on card
            accountId = 3,
            dateEpochMillis = System.currentTimeMillis(),
            merchantOrPayee = "Amazon"
        )
        val payment = TransactionEntity(
            id = 2,
            type = TransactionType.INCOME,
            amountPaise = 200000L, // ₹2,000 paid to card
            accountId = 3,
            dateEpochMillis = System.currentTimeMillis(),
            merchantOrPayee = "Card Bill Payment"
        )
        val debtOwed = FinancialCalculator.calculateAccountBalance(card, listOf(purchase, payment))
        assertEquals(300000L, debtOwed) // ₹3,000 still owed
    }

    @Test
    fun testSmartEntryUpiParsing() {
        val sampleText = """
            Paid to Zomato ₹637.00
            Payment successful
            20 May 2026, 2:14 PM
            UPI Ref No. 614057380921
        """.trimIndent()

        val parsed = SmartEntryParser.parseText(sampleText)
        assertEquals(63700L, parsed.amountPaise.value)
        assertEquals("Zomato", parsed.merchantOrPayee.value)
        assertEquals("Food & Dining", parsed.suggestedCategoryName.value)
        assertEquals(TransactionType.EXPENSE, parsed.transactionType.value)
        assertEquals("614057380921", parsed.referenceId.value)
    }

    @Test
    fun testBackupAndRestoreValidation() {
        val account = AccountEntity(id = 1, name = "SBI", type = AccountType.BANK, openingBalancePaise = 500000L)
        val category = CategoryEntity(id = 2, name = "Fuel", type = TransactionType.EXPENSE, iconName = "local_gas_station", colorHex = "#F97316")
        val tx = TransactionEntity(
            id = 3,
            type = TransactionType.EXPENSE,
            amountPaise = 150000L,
            categoryId = 2,
            accountId = 1,
            dateEpochMillis = System.currentTimeMillis(),
            merchantOrPayee = "Petrol"
        )

        val jsonBackup = BackupManager.createJsonBackup(listOf(account), listOf(category), listOf(tx), emptyList())
        val restoreResult = BackupManager.parseJsonBackup(jsonBackup)

        assertTrue(restoreResult.isSuccess)
        val (restoredAccs, restoredCats, restoredTxs) = restoreResult.getOrThrow()
        assertEquals(1, restoredAccs.size)
        assertEquals("SBI", restoredAccs[0].name)
        assertEquals(1, restoredCats.size)
        assertEquals("Fuel", restoredCats[0].name)
        assertEquals(1, restoredTxs.size)
        assertEquals(150000L, restoredTxs[0].amountPaise)
    }
}
