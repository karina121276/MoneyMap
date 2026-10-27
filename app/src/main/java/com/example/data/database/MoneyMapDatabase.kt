package com.example.data.database

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.core.model.AccountType
import com.example.core.model.TransactionType
import com.example.data.dao.*
import com.example.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

import androidx.room.migration.Migration

class Converters {
    @TypeConverter
    fun fromAccountType(value: AccountType?): String? = value?.name

    @TypeConverter
    fun toAccountType(value: String?): AccountType? = value?.let {
        try {
            AccountType.valueOf(it)
        } catch (e: Exception) {
            AccountType.BANK
        }
    }

    @TypeConverter
    fun fromTransactionType(value: TransactionType?): String? = value?.name

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? = value?.let { TransactionType.valueOf(it) }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {}
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE categories ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE accounts ADD COLUMN isSavings INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_isFavorite ON transactions(isFavorite)")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE categories ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE budgets ADD COLUMN name TEXT NOT NULL DEFAULT 'Monthly Budget'")
        db.execSQL("ALTER TABLE budgets ADD COLUMN period TEXT NOT NULL DEFAULT 'MONTHLY'")
        db.execSQL("ALTER TABLE budgets ADD COLUMN amountLimitPaise INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE budgets ADD COLUMN scope TEXT NOT NULL DEFAULT 'ALL_ACCOUNTS'")
        db.execSQL("ALTER TABLE budgets ADD COLUMN accountId INTEGER")
        db.execSQL("ALTER TABLE budgets ADD COLUMN categoryId INTEGER")
        db.execSQL("ALTER TABLE budgets ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE budgets ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")

        db.execSQL("UPDATE budgets SET amountLimitPaise = totalLimitPaise WHERE amountLimitPaise = 0")

        db.execSQL("DROP INDEX IF EXISTS index_budgets_monthYear")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_monthYear ON budgets(monthYear)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_accountId ON budgets(accountId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_categoryId ON budgets(categoryId)")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE budgets ADD COLUMN startDateEpochMillis INTEGER NOT NULL DEFAULT 0")
    }
}

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        ImportBatchEntity::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MoneyMapDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun importBatchDao(): ImportBatchDao

    companion object {
        @Volatile
        private var INSTANCE: MoneyMapDatabase? = null

        fun getDatabase(context: Context): MoneyMapDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MoneyMapDatabase::class.java,
                    "moneymap.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8
                    )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(context.applicationContext))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                val database = getDatabase(context)
                populateInitialData(database)
            }
        }

        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
            super.onDestructiveMigration(db)
            CoroutineScope(Dispatchers.IO).launch {
                val database = getDatabase(context)
                populateInitialData(database)
            }
        }
    }
}

/**
 * Pre-populates the database with Indian financial categories only.
 * Clean installs start with zero accounts, zero transactions, and zero budgets.
 */
suspend fun populateInitialData(db: MoneyMapDatabase) {
    val categoryDao = db.categoryDao()

    if (categoryDao.getCategoryCount() > 0) return

    // Predefined Expense Categories (18) & Income Categories (7)
    val defaultCategories = listOf(
        // Expense Categories
        CategoryEntity(name = "Food & Dining", type = TransactionType.EXPENSE, iconName = "restaurant", colorHex = "#EF4444", isFavorite = true),
        CategoryEntity(name = "Groceries", type = TransactionType.EXPENSE, iconName = "shopping_cart", colorHex = "#10B981", isFavorite = true),
        CategoryEntity(name = "Shopping", type = TransactionType.EXPENSE, iconName = "shopping_bag", colorHex = "#F59E0B", isFavorite = true),
        CategoryEntity(name = "Transport", type = TransactionType.EXPENSE, iconName = "directions_car", colorHex = "#3B82F6", isFavorite = true),
        CategoryEntity(name = "Fuel", type = TransactionType.EXPENSE, iconName = "local_gas_station", colorHex = "#F97316", isFavorite = true),
        CategoryEntity(name = "Vehicle Servicing", type = TransactionType.EXPENSE, iconName = "build", colorHex = "#64748B"),
        CategoryEntity(name = "Bills & Utilities", type = TransactionType.EXPENSE, iconName = "receipt_long", colorHex = "#EAB308", isFavorite = true),
        CategoryEntity(name = "Electricity", type = TransactionType.EXPENSE, iconName = "bolt", colorHex = "#FBBF24"),
        CategoryEntity(name = "Mobile & Internet", type = TransactionType.EXPENSE, iconName = "phone_android", colorHex = "#06B6D4"),
        CategoryEntity(name = "Rent", type = TransactionType.EXPENSE, iconName = "home", colorHex = "#14B8A6"),
        CategoryEntity(name = "Education", type = TransactionType.EXPENSE, iconName = "school", colorHex = "#6366F1"),
        CategoryEntity(name = "Health", type = TransactionType.EXPENSE, iconName = "local_hospital", colorHex = "#EC4899"),
        CategoryEntity(name = "Entertainment", type = TransactionType.EXPENSE, iconName = "sports_esports", colorHex = "#8B5CF6"),
        CategoryEntity(name = "Personal Care", type = TransactionType.EXPENSE, iconName = "spa", colorHex = "#F43F5E"),
        CategoryEntity(name = "Travel", type = TransactionType.EXPENSE, iconName = "flight", colorHex = "#0EA5E9"),
        CategoryEntity(name = "Home", type = TransactionType.EXPENSE, iconName = "roofing", colorHex = "#10B981"),
        CategoryEntity(name = "Gifts", type = TransactionType.EXPENSE, iconName = "card_giftcard", colorHex = "#D946EF"),
        CategoryEntity(name = "Other", type = TransactionType.EXPENSE, iconName = "more_horiz", colorHex = "#94A3B8"),

        // Income Categories
        CategoryEntity(name = "Salary", type = TransactionType.INCOME, iconName = "payments", colorHex = "#10B981", isFavorite = true),
        CategoryEntity(name = "Freelance", type = TransactionType.INCOME, iconName = "work", colorHex = "#3B82F6", isFavorite = true),
        CategoryEntity(name = "Business", type = TransactionType.INCOME, iconName = "business", colorHex = "#6366F1", isFavorite = true),
        CategoryEntity(name = "Investments", type = TransactionType.INCOME, iconName = "trending_up", colorHex = "#8B5CF6"),
        CategoryEntity(name = "Interest", type = TransactionType.INCOME, iconName = "account_balance", colorHex = "#06B6D4"),
        CategoryEntity(name = "Gifts Received", type = TransactionType.INCOME, iconName = "card_giftcard", colorHex = "#EC4899"),
        CategoryEntity(name = "Other Income", type = TransactionType.INCOME, iconName = "savings", colorHex = "#14B8A6")
    )
    categoryDao.insertAll(defaultCategories)
}
