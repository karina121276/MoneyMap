package com.thecorporatedeveloper.moneymap.data.database

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.thecorporatedeveloper.moneymap.core.model.AccountType
import com.thecorporatedeveloper.moneymap.core.model.TransactionType
import com.thecorporatedeveloper.moneymap.data.dao.*
import com.thecorporatedeveloper.moneymap.data.entity.*
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

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE budgets ADD COLUMN endDateEpochMillis INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS receipts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                transactionId INTEGER NOT NULL,
                localFilePath TEXT NOT NULL,
                thumbnailPath TEXT,
                originalFileName TEXT NOT NULL DEFAULT '',
                mimeType TEXT NOT NULL DEFAULT 'image/jpeg',
                fileSize INTEGER NOT NULL DEFAULT 0,
                displayOrder INTEGER NOT NULL DEFAULT 0,
                isPrimary INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(transactionId) REFERENCES transactions(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_receipts_transactionId ON receipts(transactionId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_receipts_displayOrder ON receipts(displayOrder)")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS bill_reminders (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                billName TEXT NOT NULL,
                expectedAmountPaise INTEGER,
                categoryId INTEGER NOT NULL,
                dueDateEpochMillis INTEGER NOT NULL,
                recurrenceType TEXT NOT NULL DEFAULT 'NONE',
                customRecurrenceIntervalDays INTEGER NOT NULL DEFAULT 0,
                reminderOffsetsJson TEXT NOT NULL DEFAULT '[0]',
                accountId INTEGER,
                notes TEXT NOT NULL DEFAULT '',
                isActive INTEGER NOT NULL DEFAULT 1,
                createdAt INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE SET_NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS bill_payments (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                billId INTEGER NOT NULL,
                dueDateForOccurrenceEpochMillis INTEGER NOT NULL,
                actualAmountPaidPaise INTEGER NOT NULL,
                paymentDateEpochMillis INTEGER NOT NULL,
                accountId INTEGER NOT NULL,
                categoryId INTEGER NOT NULL,
                linkedTransactionId INTEGER,
                note TEXT NOT NULL DEFAULT '',
                createdAt INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(billId) REFERENCES bill_reminders(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(linkedTransactionId) REFERENCES transactions(id) ON UPDATE NO ACTION ON DELETE SET_NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_bill_reminders_categoryId ON bill_reminders(categoryId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_bill_reminders_accountId ON bill_reminders(accountId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_bill_reminders_dueDateEpochMillis ON bill_reminders(dueDateEpochMillis)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_bill_reminders_isActive ON bill_reminders(isActive)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_bill_payments_billId ON bill_payments(billId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_bill_payments_accountId ON bill_payments(accountId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_bill_payments_categoryId ON bill_payments(categoryId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_bill_payments_linkedTransactionId ON bill_payments(linkedTransactionId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_bill_payments_dueDateForOccurrenceEpochMillis ON bill_payments(dueDateForOccurrenceEpochMillis)")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE bill_reminders ADD COLUMN notificationHour INTEGER NOT NULL DEFAULT -1")
        db.execSQL("ALTER TABLE bill_reminders ADD COLUMN notificationMinute INTEGER NOT NULL DEFAULT -1")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS bill_payments")
        db.execSQL("DROP TABLE IF EXISTS bill_reminders")
    }
}

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        ImportBatchEntity::class,
        ReceiptEntity::class
    ],
    version = 13,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MoneyMapDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun importBatchDao(): ImportBatchDao
    abstract fun receiptDao(): ReceiptDao

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
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13
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
    // Clean installs start with zero accounts, zero categories, zero transactions, and zero budgets.
}
