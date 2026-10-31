package com.thecorporatedeveloper.moneymap

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.thecorporatedeveloper.moneymap.backup.BackupManager
import com.thecorporatedeveloper.moneymap.core.model.AccountType
import com.thecorporatedeveloper.moneymap.core.model.TransactionType
import com.thecorporatedeveloper.moneymap.data.database.MoneyMapDatabase
import com.thecorporatedeveloper.moneymap.data.entity.AccountEntity
import com.thecorporatedeveloper.moneymap.data.entity.ReceiptEntity
import com.thecorporatedeveloper.moneymap.data.entity.TransactionEntity
import com.thecorporatedeveloper.moneymap.data.repository.MoneyMapRepository
import com.thecorporatedeveloper.moneymap.receipt.ReceiptStorageManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReceiptAttachmentUnitTest {

    private lateinit var database: MoneyMapDatabase
    private lateinit var repository: MoneyMapRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, MoneyMapDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = MoneyMapRepository(database, context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testReceiptInsertionAndQuery() = runBlocking {
        val account = AccountEntity(id = 1, name = "Cash", type = AccountType.CASH, openingBalancePaise = 100000)
        repository.insertAccount(account)

        val tx = TransactionEntity(
            id = 1,
            accountId = 1,
            amountPaise = 50000,
            type = TransactionType.EXPENSE,
            dateEpochMillis = System.currentTimeMillis(),
            merchantOrPayee = "Grocery Mart"
        )
        val txId = repository.insertTransaction(tx)

        val receipt1 = ReceiptEntity(
            transactionId = txId,
            localFilePath = "receipts/$txId/receipt_1.jpg",
            thumbnailPath = "receipts/$txId/thumb_1.jpg",
            originalFileName = "grocery_receipt.jpg",
            isPrimary = true,
            displayOrder = 0
        )
        val rId1 = repository.insertReceipt(receipt1)
        assertTrue(rId1 > 0)

        val receipt2 = ReceiptEntity(
            transactionId = txId,
            localFilePath = "receipts/$txId/receipt_2.jpg",
            originalFileName = "grocery_bill2.jpg",
            isPrimary = false,
            displayOrder = 1
        )
        repository.insertReceipt(receipt2)

        val list = repository.getReceiptsListForTransaction(txId)
        assertEquals(2, list.size)
        assertTrue(list[0].isPrimary)
        assertEquals("grocery_receipt.jpg", list[0].originalFileName)
        assertEquals("grocery_bill2.jpg", list[1].originalFileName)
    }

    @Test
    fun testCascadeDeleteTransactionRemovesReceipts() = runBlocking {
        val account = AccountEntity(id = 1, name = "Bank", type = AccountType.BANK, openingBalancePaise = 200000)
        repository.insertAccount(account)

        val tx = TransactionEntity(
            id = 10,
            accountId = 1,
            amountPaise = 15000,
            type = TransactionType.EXPENSE,
            dateEpochMillis = System.currentTimeMillis(),
            merchantOrPayee = "Dinner"
        )
        val txId = repository.insertTransaction(tx)

        val receipt = ReceiptEntity(
            transactionId = txId,
            localFilePath = "receipts/$txId/bill.jpg",
            originalFileName = "bill.jpg"
        )
        repository.insertReceipt(receipt)

        val receiptsBefore = repository.getReceiptsListForTransaction(txId)
        assertEquals(1, receiptsBefore.size)

        repository.deleteTransactionById(txId)

        val receiptsAfter = repository.getReceiptsListForTransaction(txId)
        assertTrue(receiptsAfter.isEmpty())
    }

    @Test
    fun testSetPrimaryReceipt() = runBlocking {
        val account = AccountEntity(id = 1, name = "Wallet", type = AccountType.CASH, openingBalancePaise = 50000)
        repository.insertAccount(account)

        val tx = TransactionEntity(
            id = 5,
            accountId = 1,
            amountPaise = 1000,
            type = TransactionType.EXPENSE,
            dateEpochMillis = System.currentTimeMillis(),
            merchantOrPayee = "Tea"
        )
        val txId = repository.insertTransaction(tx)

        val r1 = ReceiptEntity(id = 101, transactionId = txId, localFilePath = "r1.jpg", isPrimary = true)
        val r2 = ReceiptEntity(id = 102, transactionId = txId, localFilePath = "r2.jpg", isPrimary = false)
        repository.insertReceipt(r1)
        repository.insertReceipt(r2)

        // Switch primary to r2
        repository.setPrimaryReceipt(txId, 102)

        val updatedR1 = repository.getReceiptById(101)
        val updatedR2 = repository.getReceiptById(102)

        assertFalse(updatedR1!!.isPrimary)
        assertTrue(updatedR2!!.isPrimary)
    }

    @Test
    fun testBackupAndRestoreIncludesReceipts() = runBlocking {
        val account = AccountEntity(id = 1, name = "Test Acc", type = AccountType.CASH, openingBalancePaise = 50000)
        val tx = TransactionEntity(id = 1, accountId = 1, amountPaise = 2000, type = TransactionType.EXPENSE, dateEpochMillis = 1000L, merchantOrPayee = "Cafe")
        val receipt = ReceiptEntity(id = 55, transactionId = 1, localFilePath = "receipts/1/cafe.jpg", originalFileName = "cafe.jpg")

        val jsonStr = BackupManager.createJsonBackup(
            accounts = listOf(account),
            categories = emptyList(),
            transactions = listOf(tx),
            budgets = emptyList(),
            receipts = listOf(receipt)
        )

        assertTrue(jsonStr.contains("cafe.jpg"))

        val parsedReceipts = BackupManager.parseReceiptsFromJson(jsonStr)
        assertEquals(1, parsedReceipts.size)
        assertEquals(55L, parsedReceipts[0].id)
        assertEquals(1L, parsedReceipts[0].transactionId)
        assertEquals("cafe.jpg", parsedReceipts[0].originalFileName)
        assertEquals("receipts/1/cafe.jpg", parsedReceipts[0].localFilePath)
    }

    @Test
    fun testTempFileAndFolderCreation() {
        val tempDir = ReceiptStorageManager.getTempDirectory(context)
        assertTrue(tempDir.exists())

        val tempFile = ReceiptStorageManager.createTempReceiptFile(context)
        assertTrue(tempFile.name.startsWith("temp_receipt_"))
        assertEquals(tempDir.absolutePath, tempFile.parentFile?.absolutePath)
    }
}
