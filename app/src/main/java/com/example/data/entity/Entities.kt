package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.model.AccountType
import com.example.core.model.TransactionType

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType,
    val bankName: String = "",
    val accountNumberMasked: String = "",
    val ifscCode: String = "",
    val accountHolder: String = "",
    val openingBalancePaise: Long = 0L,
    val colorHex: String = "#1E56F0",
    val iconName: String = "bank",
    val isSavings: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isSavingsAccount: Boolean
        get() = isSavings || type == AccountType.SAVINGS
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: TransactionType,
    val iconName: String,
    val colorHex: String,
    val isCustom: Boolean = false,
    val isArchived: Boolean = false,
    val isFavorite: Boolean = false
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("accountId"),
        Index("toAccountId"),
        Index("categoryId"),
        Index("dateEpochMillis"),
        Index("referenceId"),
        Index("isFavorite")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    val amountPaise: Long,
    val categoryId: Long? = null,
    val accountId: Long,
    val toAccountId: Long? = null,
    val dateEpochMillis: Long,
    val merchantOrPayee: String = "",
    val description: String = "",
    val note: String = "",
    val paymentMethod: String = "",
    val referenceId: String? = null,
    val attachmentUri: String? = null,
    val isImported: Boolean = false,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "budgets",
    indices = [
        Index("monthYear"),
        Index("accountId"),
        Index("categoryId")
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "Monthly Budget",
    val monthYear: String, // format "YYYY-MM", e.g., "2026-10"
    val period: String = "MONTHLY",
    val amountLimitPaise: Long = 0L,
    val scope: String = "ALL_ACCOUNTS", // "ALL_ACCOUNTS", "ACCOUNT", "CATEGORY", "ACCOUNT_CATEGORY"
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val startDateEpochMillis: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val totalLimitPaise: Long = 0L,
    val categoryLimitsJson: String = "{}"
) {
    val effectiveLimitPaise: Long
        get() = if (amountLimitPaise > 0L) amountLimitPaise else totalLimitPaise
}

@Entity(tableName = "import_batches")
data class ImportBatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceType: String, // "SCREENSHOT", "RECEIPT", "PDF"
    val sourceName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val totalItems: Int,
    val importedItems: Int,
    val skippedDuplicates: Int,
    val failedItems: Int
)
