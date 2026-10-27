package com.thecorporatedeveloper.moneymap

import com.thecorporatedeveloper.moneymap.core.model.AccountType
import com.thecorporatedeveloper.moneymap.core.model.TransactionType
import com.thecorporatedeveloper.moneymap.data.entity.AccountEntity
import com.thecorporatedeveloper.moneymap.data.entity.CategoryEntity
import com.thecorporatedeveloper.moneymap.importer.SmartEntryParser
import org.junit.Assert.*
import org.junit.Test

class SmartEntryParserTest {

    private val sampleAccounts = listOf(
        AccountEntity(id = 1L, name = "HDFC Bank", type = AccountType.BANK, openingBalancePaise = 1000000L),
        AccountEntity(id = 2L, name = "Paytm Wallet", type = AccountType.WALLET, openingBalancePaise = 500000L),
        AccountEntity(id = 3L, name = "SBI Savings", type = AccountType.SAVINGS, openingBalancePaise = 2500000L, isSavings = true)
    )

    private val sampleCategories = listOf(
        CategoryEntity(id = 10L, name = "Food & Dining", type = TransactionType.EXPENSE, iconName = "restaurant", colorHex = "#FF5722"),
        CategoryEntity(id = 11L, name = "Shopping", type = TransactionType.EXPENSE, iconName = "shopping_bag", colorHex = "#E91E63"),
        CategoryEntity(id = 12L, name = "Salary", type = TransactionType.INCOME, iconName = "payments", colorHex = "#4CAF50"),
        CategoryEntity(id = 13L, name = "Transfer", type = TransactionType.TRANSFER, iconName = "swap_horiz", colorHex = "#2196F3")
    )

    @Test
    fun testGooglePayReceiptParsing() {
        val gpayText = """
            Paid to Zomato
            ₹485.50
            Payment successful
            24 Oct 2026, 8:30 PM
            UPI Ref No: 429810573912
            Debited from HDFC Bank ****1234
        """.trimIndent()

        val result = SmartEntryParser.parseText(gpayText, sampleAccounts, sampleCategories)

        assertEquals(48550L, result.amountPaise.value)
        assertEquals("Zomato", result.merchantOrPayee.value)
        assertEquals(TransactionType.EXPENSE, result.transactionType.value)
        assertEquals("429810573912", result.referenceId.value)
        assertEquals(1L, result.suggestedAccountId)
        assertEquals(10L, result.suggestedCategoryId)
    }

    @Test
    fun testPhonePeReceiptParsing() {
        val phonePeText = """
            Transaction Successful
            Paid to Swiggy
            ₹620
            Date: 15 Oct 2026, 01:15 PM
            UTR: 610293847561
            Paid using Paytm Wallet
        """.trimIndent()

        val result = SmartEntryParser.parseText(phonePeText, sampleAccounts, sampleCategories)

        assertEquals(62000L, result.amountPaise.value)
        assertEquals("Swiggy", result.merchantOrPayee.value)
        assertEquals(TransactionType.EXPENSE, result.transactionType.value)
        assertEquals("610293847561", result.referenceId.value)
        assertEquals(2L, result.suggestedAccountId)
        assertEquals(10L, result.suggestedCategoryId)
    }

    @Test
    fun testPaytmReceivedMoneyParsing() {
        val paytmText = """
            Money Received
            +₹5,000.00
            From: Ramesh Kumar
            12 Oct 2026, 11:45 AM
            Ref ID: 509876543210
            Credited to SBI Savings
        """.trimIndent()

        val result = SmartEntryParser.parseText(paytmText, sampleAccounts, sampleCategories)

        assertEquals(500000L, result.amountPaise.value)
        assertEquals("Ramesh Kumar", result.merchantOrPayee.value)
        assertEquals(TransactionType.INCOME, result.transactionType.value)
        assertEquals("509876543210", result.referenceId.value)
        assertEquals(3L, result.suggestedAccountId)
    }

    @Test
    fun testGenericUpiReceiptParsing() {
        val genericText = """
            Transfer to Friend
            Rs. 1,250
            Txn ID: 987654321098
            Date: 01/11/2026
        """.trimIndent()

        val result = SmartEntryParser.parseText(genericText, sampleAccounts, sampleCategories)

        assertEquals(125000L, result.amountPaise.value)
        assertEquals("987654321098", result.referenceId.value)
        assertTrue(result.amountPaise.confidence > 50)
    }

    @Test
    fun testNoteExtractionAndStatusWarning() {
        val noteText = """
            Paid to Zomato
            ₹485.50
            Payment successful
            24 Oct 2026, 8:30 PM
            Note: Food order with friends
            UPI Ref No: 429810573912
        """.trimIndent()

        val result = SmartEntryParser.parseText(noteText, sampleAccounts, sampleCategories)
        assertEquals("Food order with friends", result.note.value)
        assertNull("Successful payment should have no status warning", result.paymentStatusWarning)

        val failedText = """
            Payment Failed
            Paid to Swiggy
            ₹620
            Declined by bank
        """.trimIndent()

        val failedResult = SmartEntryParser.parseText(failedText, sampleAccounts, sampleCategories)
        assertNotNull("Failed payment should generate warning", failedResult.paymentStatusWarning)
        assertTrue(failedResult.paymentStatusWarning!!.contains("FAILED"))
    }

    @Test
    fun testReferenceScreenshotsFixtureRegression() {
        // 1. Phonepay income.jpeg
        val phonePayIncomeText = """
            Transaction Successful
            12:34 PM on 11 Oct 2026
            Received from
            Usman Chi Mummy
            +91 ••••• •6099
            ₹500
            Banking Name : Hajiya Riyaj Khan
            Transfer Details
            PhonePe Transaction ID
            T2610111234475096516998
            Credited to
            G Pay • raj121276thakre@oksbi
            ₹500
            UTR: 298937849448
        """.trimIndent()
        val r1 = SmartEntryParser.parseText(phonePayIncomeText, sampleAccounts, sampleCategories)
        assertEquals(50000L, r1.amountPaise.value)
        assertEquals(TransactionType.INCOME, r1.transactionType.value)
        assertEquals("Usman Chi Mummy", r1.merchantOrPayee.value)
        assertEquals("298937849448", r1.referenceId.value)

        // 2. Phonepay expense.jpeg
        val phonePayExpenseText = """
            Transaction Successful
            08:12 PM on 24 Oct 2026
            Paid to
            Usman Chi Mummy
            +91 ••••• •6099
            ₹500
            Banking Name : Hajiya Riyaj Khan
            Transfer Details
            PhonePe Transaction ID
            T2610242012370320019279
            Debited from
            XXXXXX1116
            ₹500
            UTR: 969995835092
        """.trimIndent()
        val r2 = SmartEntryParser.parseText(phonePayExpenseText, sampleAccounts, sampleCategories)
        assertEquals(50000L, r2.amountPaise.value)
        assertEquals(TransactionType.EXPENSE, r2.transactionType.value)
        assertEquals("Usman Chi Mummy", r2.merchantOrPayee.value)
        assertEquals("969995835092", r2.referenceId.value)

        // 3. gpay income.jpeg
        val gpayIncomeText = """
            From KARINA NARESH THAKARE
            ₹300
            Payment from PhonePe
            Completed
            12 Oct 2026, 12:12 pm
            State Bank of India 1116
            UPI transaction ID
            005214945367
            To: RAJ NARESH THAKARE
            .....akre@oksbi on Google Pay
            From: KARINA NARESH THAKARE
            ......1856@axl on PhonePe
            Google transaction ID
            CICAgPjm1qeYJA
        """.trimIndent()
        val r3 = SmartEntryParser.parseText(gpayIncomeText, sampleAccounts, sampleCategories)
        assertEquals(30000L, r3.amountPaise.value)
        assertEquals(TransactionType.INCOME, r3.transactionType.value)
        assertEquals("KARINA NARESH THAKARE", r3.merchantOrPayee.value)
        assertEquals("005214945367", r3.referenceId.value)

        // 4. gpay expense.jpeg
        val gpayExpenseText = """
            To ROHINI ANIL SAWANT
            ₹80
            maase
            Pay again
            Completed
            14 Aug 2026, 6:39 pm
            State Bank of India 3758
            UPI transaction ID
            622668689286
            To: ROHINI ANIL SAWANT
            ......2260@postbank
            From: NARESH CHATIRAM THAKARE (State Bank of India)
            ....akre@okicici on Google Pay
            Google transaction ID
            CICAgJin_9ScFA
        """.trimIndent()
        val r4 = SmartEntryParser.parseText(gpayExpenseText, sampleAccounts, sampleCategories)
        println("NOTE r4: '${r4.note.value}'")
        assertEquals(8000L, r4.amountPaise.value)
        assertEquals(TransactionType.EXPENSE, r4.transactionType.value)
        assertEquals("ROHINI ANIL SAWANT", r4.merchantOrPayee.value)
        assertEquals("maase", r4.note.value)
        assertEquals("622668689286", r4.referenceId.value)
    }

    @Test
    fun testEmptyOrInvalidTextHandling() {
        val invalidText = "Hello world random image with no payment details"

        val result = SmartEntryParser.parseText(invalidText, sampleAccounts, sampleCategories)

        assertEquals(0L, result.amountPaise.value)
        assertNull(result.referenceId.value)
        assertTrue(result.amountPaise.confidence < 50)
    }
}
