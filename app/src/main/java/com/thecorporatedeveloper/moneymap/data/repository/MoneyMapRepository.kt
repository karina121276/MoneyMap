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

    // Observables
    val allAccounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val favoriteTransactions: Flow<List<TransactionEntity>> = transactionDao.getFavoriteTransactions()
    val allBudgets: Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()

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
    }

    suspend fun deleteTransactionById(id: Long) {
        database.withTransaction {
            transactionDao.deleteTransactionById(id)
            notifyWidgetChanged()
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

    // Full Database Backup & Restore in Atomic Transaction
    suspend fun getAllDataForBackup(): Map<String, Any> {
        val accounts = accountDao.getAllAccountsList()
        val categories = categoryDao.getAllCategoriesList()
        val transactions = transactionDao.getAllTransactionsList()
        return mapOf(
            "accounts" to accounts,
            "categories" to categories,
            "transactions" to transactions
        )
    }

    suspend fun restoreAllData(
        accounts: List<AccountEntity>,
        categories: List<CategoryEntity>,
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>
    ) {
        database.withTransaction {
            // Restore categories first
            for (cat in categories) {
                categoryDao.insertCategory(cat)
            }
            // Restore accounts
            for (acc in accounts) {
                accountDao.insertAccount(acc)
            }
            // Restore transactions
            for (tx in transactions) {
                transactionDao.insertTransaction(tx)
            }
            // Restore budgets
            for (b in budgets) {
                budgetDao.setBudget(b)
            }
        }
        notifyWidgetChanged()
    }

    suspend fun clearAllData() {
        database.withTransaction {
            transactionDao.deleteAllTransactions()
            accountDao.deleteAllAccounts()
            categoryDao.deleteAllCategories()
            budgetDao.deleteAllBudgets()
            importBatchDao.deleteAllBatches()
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
