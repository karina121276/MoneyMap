package com.thecorporatedeveloper.moneymap.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.thecorporatedeveloper.moneymap.core.model.AccountType
import com.thecorporatedeveloper.moneymap.core.model.TransactionType
import com.thecorporatedeveloper.moneymap.data.database.MoneyMapDatabase
import com.thecorporatedeveloper.moneymap.data.entity.*
import com.thecorporatedeveloper.moneymap.domain.FinancialCalculator
import com.thecorporatedeveloper.moneymap.domain.PeriodSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class MoneyMapRepository(
    private val database: MoneyMapDatabase,
    private val context: Context? = null
) {
    private val accountDao = database.accountDao()
    private val categoryDao = database.categoryDao()
    private val transactionDao = database.transactionDao()
    private val budgetDao = database.budgetDao()
    private val importBatchDao = database.importBatchDao()
    private val receiptDao = database.receiptDao()

    // Observables
    val allAccounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val favoriteTransactions: Flow<List<TransactionEntity>> = transactionDao.getFavoriteTransactions()
    val allBudgets: Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()
    val allReceipts: Flow<List<ReceiptEntity>> = receiptDao.getAllReceipts()

    suspend fun getAllCategoriesList(): List<CategoryEntity> = categoryDao.getAllCategoriesList()


    suspend fun cleanUnusedPredefinedCategories() {
        categoryDao.deleteUnusedPredefinedCategories()
    }

    suspend fun toggleFavorite(transaction: TransactionEntity) {
        transactionDao.updateFavoriteStatus(transaction.id, !transaction.isFavorite)
        notifyWidgetChanged()
    }

    suspend fun setFavorite(transactionId: Long, isFavorite: Boolean) {
        transactionDao.updateFavoriteStatus(transactionId, isFavorite)
        notifyWidgetChanged()
    }

    fun getRecentTransactions(limit: Int = 10): Flow<List<TransactionEntity>> =
        transactionDao.getRecentTransactions(limit)

    fun getTransactionsForMonth(yearMonth: YearMonth): Flow<List<TransactionEntity>> {
        val start = yearMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = yearMonth.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return transactionDao.getTransactionsByDateRange(start, end)
    }

    fun getTransactionsForDateRange(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByDateRange(startMillis, endMillis)

    fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByAccount(accountId)

    suspend fun getTransactionById(id: Long): TransactionEntity? =
        transactionDao.getTransactionById(id)

    suspend fun getAccountById(id: Long): AccountEntity? =
        accountDao.getAccountById(id)

    fun getAccountByIdFlow(id: Long): Flow<AccountEntity?> =
        accountDao.getAccountByIdFlow(id)

    private suspend fun validateTransactionBalance(transaction: TransactionEntity, excludeTransactionId: Long? = null) {
        if (transaction.type == TransactionType.EXPENSE || transaction.type == TransactionType.TRANSFER) {
            val account = accountDao.getAccountById(transaction.accountId) ?: return
            if (!account.type.isLiability) {
                val allTx = transactionDao.getAllTransactionsList()
                val filteredTx = if (excludeTransactionId != null) {
                    allTx.filter { it.id != excludeTransactionId }
                } else {
                    allTx
                }
                val currentBalance = FinancialCalculator.calculateAccountBalance(account, filteredTx)
                if (transaction.amountPaise > currentBalance) {
                    throw IllegalStateException("Insufficient balance in ${account.name}. Available balance is ${com.thecorporatedeveloper.moneymap.core.model.CurrencyFormatter.formatRupees(currentBalance)}.")
                }
            }
        }
    }

    // Mutations with Widget sync
    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return database.withTransaction {
            try {
                if (!transaction.referenceId.isNullOrBlank()) {
                    val existing = transactionDao.getByReferenceId(transaction.referenceId!!)
                    if (existing != null) {
                        return@withTransaction existing.id
                    }
                }
                validateTransactionBalance(transaction)
                val id = transactionDao.insertTransaction(transaction)
                notifyWidgetChanged()
                id
            } catch (e: Exception) {
                if (com.thecorporatedeveloper.moneymap.core.util.StorageSafety.isStorageOrDatabaseError(e)) {
                    throw java.io.IOException("Not enough storage to save this transaction. Free some device space and try again.", e)
                } else {
                    throw e
                }
            }
        }
    }

    suspend fun insertTransactions(transactions: List<TransactionEntity>): List<Long> {
        return database.withTransaction {
            try {
                val ids = mutableListOf<Long>()
                for (tx in transactions) {
                    validateTransactionBalance(tx)
                    val id = transactionDao.insertTransaction(tx)
                    ids.add(id)
                }
                notifyWidgetChanged()
                ids
            } catch (e: Exception) {
                if (com.thecorporatedeveloper.moneymap.core.util.StorageSafety.isStorageOrDatabaseError(e)) {
                    throw java.io.IOException("Not enough storage to save these transactions. Free some device space and try again.", e)
                } else {
                    throw e
                }
            }
        }
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        database.withTransaction {
            try {
                validateTransactionBalance(transaction, excludeTransactionId = transaction.id)
                transactionDao.updateTransaction(transaction)
                notifyWidgetChanged()
            } catch (e: Exception) {
                if (com.thecorporatedeveloper.moneymap.core.util.StorageSafety.isStorageOrDatabaseError(e)) {
                    throw java.io.IOException("Not enough storage to update this transaction. Free some device space and try again.", e)
                } else {
                    throw e
                }
            }
        }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        database.withTransaction {
            transactionDao.deleteTransaction(transaction)
            notifyWidgetChanged()
        }
        context?.let { ctx ->
            com.thecorporatedeveloper.moneymap.receipt.ReceiptStorageManager.deleteTransactionReceiptsFolder(ctx, transaction.id)
        }
    }

    suspend fun deleteTransactionById(id: Long) {
        database.withTransaction {
            transactionDao.deleteTransactionById(id)
            notifyWidgetChanged()
        }
        context?.let { ctx ->
            com.thecorporatedeveloper.moneymap.receipt.ReceiptStorageManager.deleteTransactionReceiptsFolder(ctx, id)
        }
    }

    // Accounts
    suspend fun insertAccount(account: AccountEntity): Long {
        val id = accountDao.insertAccount(account)
        notifyWidgetChanged()
        return id
    }

    suspend fun updateAccount(account: AccountEntity) {
        accountDao.updateAccount(account)
        notifyWidgetChanged()
    }

    suspend fun deleteAccount(account: AccountEntity) {
        accountDao.deleteAccount(account)
        notifyWidgetChanged()
    }

    // Categories
    val allActiveCategories: Flow<List<CategoryEntity>> = categoryDao.getActiveCategories()

    fun getActiveCategoriesByType(type: TransactionType): Flow<List<CategoryEntity>> =
        categoryDao.getActiveCategoriesByType(type)

    suspend fun insertCategory(category: CategoryEntity): Long =
        categoryDao.insertCategory(category)

    suspend fun updateCategory(category: CategoryEntity) =
        categoryDao.updateCategory(category)

    suspend fun archiveCategory(categoryId: Long) =
        categoryDao.archiveCategory(categoryId)

    suspend fun unarchiveCategory(categoryId: Long) =
        categoryDao.unarchiveCategory(categoryId)

    suspend fun toggleCategoryFavorite(category: CategoryEntity) {
        categoryDao.updateFavoriteStatus(category.id, !category.isFavorite)
    }

    suspend fun getTransactionCountForCategory(categoryId: Long): Int =
        transactionDao.getTransactionCountForCategory(categoryId)

    suspend fun isDuplicateCategoryName(name: String, type: TransactionType, excludeId: Long? = null): Boolean {
        val trimmed = name.trim().lowercase()
        if (trimmed.isEmpty()) return false
        val allCats = categoryDao.getAllCategoriesList()
        return allCats.any {
            it.type == type &&
            it.name.trim().lowercase() == trimmed &&
            (excludeId == null || it.id != excludeId)
        }
    }

    suspend fun deleteCategory(category: CategoryEntity, reassignToId: Long? = null) {
        database.withTransaction {
            if (reassignToId != null) {
                transactionDao.reassignCategory(category.id, reassignToId)
            }
            categoryDao.deleteCategory(category)
        }
    }

    // Budgets
    fun getBudgetForMonth(monthYear: String): Flow<BudgetEntity?> =
        budgetDao.getBudgetForMonth(monthYear)

    fun getBudgetsForMonthList(monthYear: String): Flow<List<BudgetEntity>> =
        budgetDao.getBudgetsForMonth(monthYear)

    suspend fun setBudget(budget: BudgetEntity): Long =
        budgetDao.setBudget(budget)

    suspend fun insertBudget(budget: BudgetEntity): Long =
        budgetDao.insertBudget(budget)

    suspend fun updateBudget(budget: BudgetEntity) =
        budgetDao.updateBudget(budget)

    suspend fun deleteBudget(budget: BudgetEntity) =
        budgetDao.deleteBudget(budget)

    suspend fun deleteBudgetById(id: Long) =
        budgetDao.deleteBudgetById(id)

    suspend fun getBudgetById(id: Long): BudgetEntity? =
        budgetDao.getBudgetById(id)

    // Import Batches
    suspend fun recordImportBatch(batch: ImportBatchEntity): Long =
        importBatchDao.insertBatch(batch)

    // Duplicate Check
    suspend fun checkDuplicate(referenceId: String?, amountPaise: Long, dateMillis: Long): TransactionEntity? {
        if (!referenceId.isNullOrBlank()) {
            val byRef = transactionDao.getByReferenceId(referenceId)
            if (byRef != null) return byRef
        }
        val dayWindow = 86400000L * 2
        val candidates = transactionDao.findPotentialDuplicates(amountPaise, dateMillis - dayWindow, dateMillis + dayWindow)
        return candidates.firstOrNull()
    }

    // Receipts
    fun getReceiptsForTransaction(transactionId: Long): Flow<List<ReceiptEntity>> =
        receiptDao.getReceiptsForTransaction(transactionId)

    suspend fun getReceiptsListForTransaction(transactionId: Long): List<ReceiptEntity> =
        receiptDao.getReceiptsListForTransaction(transactionId)

    suspend fun getReceiptById(id: Long): ReceiptEntity? =
        receiptDao.getReceiptById(id)

    suspend fun insertReceipt(receipt: ReceiptEntity): Long =
        receiptDao.insertReceipt(receipt)

    suspend fun updateReceipt(receipt: ReceiptEntity) =
        receiptDao.updateReceipt(receipt)

    suspend fun deleteReceipt(receipt: ReceiptEntity) {
        receiptDao.deleteReceipt(receipt)
        context?.let { ctx ->
            com.thecorporatedeveloper.moneymap.receipt.ReceiptStorageManager.deleteReceiptFiles(ctx, receipt)
        }
    }

    suspend fun deleteReceiptById(id: Long) {
        val receipt = receiptDao.getReceiptById(id)
        if (receipt != null) {
            deleteReceipt(receipt)
        }
    }

    suspend fun setPrimaryReceipt(transactionId: Long, receiptId: Long) =
        receiptDao.setPrimaryReceipt(transactionId, receiptId)

    suspend fun attachReceiptToTransaction(
        transactionId: Long,
        tempFile: java.io.File,
        originalName: String? = null,
        isPrimary: Boolean = false
    ): ReceiptEntity? {
        val ctx = context ?: return null
        val currentReceipts = receiptDao.getReceiptsListForTransaction(transactionId)
        val shouldBePrimary = isPrimary || currentReceipts.isEmpty()
        val displayOrder = currentReceipts.size

        val entity = com.thecorporatedeveloper.moneymap.receipt.ReceiptStorageManager.saveConfirmedReceipt(
            context = ctx,
            transactionId = transactionId,
            tempFile = tempFile,
            originalFileName = originalName ?: "",
            displayOrder = displayOrder,
            isPrimary = shouldBePrimary
        )
        val id = receiptDao.insertReceipt(entity)
        return entity.copy(id = id)
    }

    suspend fun insertTransactionWithReceipts(
        transaction: TransactionEntity,
        tempReceiptFiles: List<java.io.File>
    ): Long {
        return database.withTransaction {
            val txId = insertTransaction(transaction)
            if (context != null && tempReceiptFiles.isNotEmpty()) {
                tempReceiptFiles.forEachIndexed { index, tempFile ->
                    val entity = com.thecorporatedeveloper.moneymap.receipt.ReceiptStorageManager.saveConfirmedReceipt(
                        context = context,
                        transactionId = txId,
                        tempFile = tempFile,
                        originalFileName = tempFile.name,
                        displayOrder = index,
                        isPrimary = (index == 0)
                    )
                    receiptDao.insertReceipt(entity)
                    com.thecorporatedeveloper.moneymap.receipt.ReceiptStorageManager.deleteTempFile(tempFile)
                }
            }
            txId
        }
    }

    suspend fun replaceReceiptImage(
        receipt: ReceiptEntity,
        newTempFile: java.io.File
    ): ReceiptEntity? {
        val ctx = context ?: return null
        // Delete old files
        com.thecorporatedeveloper.moneymap.receipt.ReceiptStorageManager.deleteReceiptFiles(ctx, receipt)
        // Save new file
        val updatedEntity = com.thecorporatedeveloper.moneymap.receipt.ReceiptStorageManager.saveConfirmedReceipt(
            context = ctx,
            transactionId = receipt.transactionId,
            tempFile = newTempFile,
            originalFileName = receipt.originalFileName,
            displayOrder = receipt.displayOrder,
            isPrimary = receipt.isPrimary
        ).copy(id = receipt.id)

        receiptDao.updateReceipt(updatedEntity)
        return updatedEntity
    }

    // Full Database Backup & Restore in Atomic Transaction
    suspend fun getAllDataForBackup(): Map<String, Any> {
        val accounts = accountDao.getAllAccountsList()
        val categories = categoryDao.getAllCategoriesList()
        val transactions = transactionDao.getAllTransactionsList()
        val budgets = budgetDao.getAllBudgetsList()
        val receipts = receiptDao.getAllReceiptsList()
        return mapOf(
            "accounts" to accounts,
            "categories" to categories,
            "transactions" to transactions,
            "budgets" to budgets,
            "receipts" to receipts
        )
    }

    suspend fun restoreAllData(
        isMerge: Boolean,
        accounts: List<AccountEntity>,
        categories: List<CategoryEntity>,
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        receipts: List<ReceiptEntity> = emptyList()
    ) {
        database.withTransaction {
            if (!isMerge) {
                clearAllData()
            }

            // ID Mapping
            val accIdMap = mutableMapOf<Long, Long>()
            val catIdMap = mutableMapOf<Long, Long>()
            val txIdMap = mutableMapOf<Long, Long>()

            // Merge Categories
            for (cat in categories) {
                val newCat = cat.copy(id = 0)
                val newId = categoryDao.insertCategory(newCat)
                catIdMap[cat.id] = newId
            }

            // Merge Accounts
            for (acc in accounts) {
                val newAcc = acc.copy(id = 0)
                val newId = accountDao.insertAccount(newAcc)
                accIdMap[acc.id] = newId
            }

            // Merge Transactions
            for (tx in transactions) {
                val newTx = tx.copy(
                    id = 0,
                    accountId = accIdMap[tx.accountId] ?: tx.accountId,
                    categoryId = tx.categoryId?.let { catIdMap[it] } ?: tx.categoryId
                )
                val newId = transactionDao.insertTransaction(newTx)
                txIdMap[tx.id] = newId
            }

            // Merge Budgets
            for (b in budgets) {
                val newB = b.copy(
                    id = 0,
                    accountId = b.accountId?.let { accIdMap[it] },
                    categoryId = b.categoryId?.let { catIdMap[it] }
                )
                budgetDao.insertBudget(newB)
            }

            // Merge Receipts
            for (r in receipts) {
                val newR = r.copy(
                    id = 0,
                    transactionId = txIdMap[r.transactionId] ?: r.transactionId
                )
                receiptDao.insertReceipt(newR)
            }
        }
        notifyWidgetChanged()
    }

    suspend fun clearAllData() {
        database.withTransaction {
            receiptDao.deleteAllReceipts()
            transactionDao.deleteAllTransactions()
            accountDao.deleteAllAccounts()
            categoryDao.deleteAllCategories()
            budgetDao.deleteAllBudgets()
            importBatchDao.deleteAllBatches()
        }
        context?.let { ctx ->
            java.io.File(ctx.filesDir, "receipts").deleteRecursively()
            java.io.File(ctx.cacheDir, "receipt_temp").deleteRecursively()
        }
        notifyWidgetChanged()
    }


    private fun notifyWidgetChanged() {
        context?.let { ctx ->
            try {
                val intent = android.content.Intent("com.thecorporatedeveloper.moneymap.ACTION_MONEYMAP_DATA_CHANGED")
                intent.setPackage(ctx.packageName)
                ctx.sendBroadcast(intent)
            } catch (e: Exception) {
                // Ignore widget broadcast error in test environment
            }
        }
    }
}
