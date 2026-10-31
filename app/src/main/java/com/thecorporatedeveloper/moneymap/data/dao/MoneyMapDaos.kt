package com.thecorporatedeveloper.moneymap.data.dao

import androidx.room.*
import com.thecorporatedeveloper.moneymap.core.model.TransactionType
import com.thecorporatedeveloper.moneymap.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE isArchived = 0 ORDER BY createdAt ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isArchived = 0 ORDER BY createdAt ASC")
    suspend fun getAllAccountsList(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    fun getAccountByIdFlow(id: Long): Flow<AccountEntity?>

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getAccountCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<AccountEntity>)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAllCategoriesList(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE isArchived = 0 ORDER BY name ASC")
    fun getActiveCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE isArchived = 0 AND type = :type ORDER BY name ASC")
    fun getActiveCategoriesByType(type: TransactionType): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name ASC")
    fun getCategoriesByType(type: TransactionType): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()

    @Query("UPDATE categories SET isArchived = 1 WHERE id = :id")
    suspend fun archiveCategory(id: Long)

    @Query("UPDATE categories SET isArchived = 0 WHERE id = :id")
    suspend fun unarchiveCategory(id: Long)

    @Query("UPDATE categories SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)

    @Query("SELECT * FROM categories WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) AND type = :type LIMIT 1")
    suspend fun findCategoryByNameAndType(name: String, type: TransactionType): CategoryEntity?

    @Query("DELETE FROM categories WHERE isCustom = 0 AND id NOT IN (SELECT DISTINCT categoryId FROM transactions WHERE categoryId IS NOT NULL) AND id NOT IN (SELECT DISTINCT categoryId FROM budgets WHERE categoryId IS NOT NULL)")
    suspend fun deleteUnusedPredefinedCategories()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY dateEpochMillis DESC, createdAt DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY dateEpochMillis DESC, createdAt DESC")
    suspend fun getAllTransactionsList(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE dateEpochMillis BETWEEN :startMillis AND :endMillis ORDER BY dateEpochMillis DESC, createdAt DESC")
    fun getTransactionsByDateRange(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE dateEpochMillis BETWEEN :startMillis AND :endMillis ORDER BY dateEpochMillis DESC, createdAt DESC")
    suspend fun getTransactionsByDateRangeList(startMillis: Long, endMillis: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId OR toAccountId = :accountId ORDER BY dateEpochMillis DESC, createdAt DESC")
    fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId OR toAccountId = :accountId ORDER BY dateEpochMillis DESC, createdAt DESC")
    suspend fun getTransactionsByAccountList(accountId: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY dateEpochMillis DESC, createdAt DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE referenceId IS NOT NULL AND referenceId = :referenceId LIMIT 1")
    suspend fun getByReferenceId(referenceId: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE amountPaise = :amountPaise AND dateEpochMillis BETWEEN :startMillis AND :endMillis")
    suspend fun findPotentialDuplicates(amountPaise: Long, startMillis: Long, endMillis: Long): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("UPDATE transactions SET categoryId = :newCategoryId WHERE categoryId = :oldCategoryId")
    suspend fun reassignCategory(oldCategoryId: Long, newCategoryId: Long?)

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun getTransactionCountForCategory(categoryId: Long): Int

    @Query("SELECT * FROM transactions WHERE isFavorite = 1 ORDER BY dateEpochMillis DESC, createdAt DESC")
    fun getFavoriteTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isFavorite = 1 ORDER BY dateEpochMillis DESC, createdAt DESC")
    suspend fun getFavoriteTransactionsList(): List<TransactionEntity>

    @Query("UPDATE transactions SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE monthYear = :monthYear")
    fun getBudgetsForMonth(monthYear: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE monthYear = :monthYear LIMIT 1")
    fun getBudgetForMonth(monthYear: String): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE monthYear = :monthYear LIMIT 1")
    suspend fun getBudgetForMonthDirect(monthYear: String): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE id = :id LIMIT 1")
    suspend fun getBudgetById(id: Long): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setBudget(budget: BudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: Long)

    @Query("SELECT * FROM budgets ORDER BY monthYear DESC, id DESC")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets ORDER BY monthYear DESC, id DESC")
    suspend fun getAllBudgetsList(): List<BudgetEntity>
}

@Dao
interface ImportBatchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: ImportBatchEntity): Long

    @Query("SELECT * FROM import_batches ORDER BY timestamp DESC")
    fun getAllBatches(): Flow<List<ImportBatchEntity>>

    @Query("DELETE FROM import_batches")
    suspend fun deleteAllBatches()
}

@Dao
interface ReceiptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: ReceiptEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipts(receipts: List<ReceiptEntity>): List<Long>

    @Update
    suspend fun updateReceipt(receipt: ReceiptEntity)

    @Delete
    suspend fun deleteReceipt(receipt: ReceiptEntity)

    @Query("DELETE FROM receipts WHERE id = :id")
    suspend fun deleteReceiptById(id: Long)

    @Query("DELETE FROM receipts WHERE transactionId = :transactionId")
    suspend fun deleteReceiptsForTransaction(transactionId: Long)

    @Query("SELECT * FROM receipts WHERE transactionId = :transactionId ORDER BY isPrimary DESC, displayOrder ASC, id ASC")
    fun getReceiptsForTransaction(transactionId: Long): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE transactionId = :transactionId ORDER BY isPrimary DESC, displayOrder ASC, id ASC")
    suspend fun getReceiptsListForTransaction(transactionId: Long): List<ReceiptEntity>

    @Query("SELECT * FROM receipts WHERE id = :id LIMIT 1")
    suspend fun getReceiptById(id: Long): ReceiptEntity?

    @Query("SELECT * FROM receipts ORDER BY id DESC")
    fun getAllReceipts(): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts ORDER BY id DESC")
    suspend fun getAllReceiptsList(): List<ReceiptEntity>

    @Query("DELETE FROM receipts")
    suspend fun deleteAllReceipts()

    @Query("UPDATE receipts SET displayOrder = :order WHERE id = :id")
    suspend fun updateDisplayOrder(id: Long, order: Int)

    @Query("UPDATE receipts SET isPrimary = (id = :primaryId) WHERE transactionId = :transactionId")
    suspend fun setPrimaryReceipt(transactionId: Long, primaryId: Long)
}


