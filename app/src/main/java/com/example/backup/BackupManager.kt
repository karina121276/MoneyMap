package com.example.backup

import android.content.Context
import android.net.Uri
import com.example.core.model.AccountType
import com.example.core.model.CurrencyFormatter
import com.example.core.model.TransactionType
import com.example.data.entity.AccountEntity
import com.example.data.entity.BudgetEntity
import com.example.data.entity.CategoryEntity
import com.example.data.entity.TransactionEntity
import com.example.data.repository.MoneyMapRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    private const val BACKUP_VERSION = 1
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)

    /**
     * Creates a full JSON string of the user's financial database.
     */
    fun createJsonBackup(
        accounts: List<AccountEntity>,
        categories: List<CategoryEntity>,
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>
    ): String {
        val root = JSONObject()
        root.put("version", BACKUP_VERSION)
        root.put("appName", "MoneyMap")
        root.put("exportDate", dateFormat.format(Date()))

        // Accounts
        val accArray = JSONArray()
        for (a in accounts) {
            val obj = JSONObject()
            obj.put("id", a.id)
            obj.put("name", a.name)
            obj.put("type", a.type.name)
            obj.put("bankName", a.bankName)
            obj.put("accountNumberMasked", a.accountNumberMasked)
            obj.put("ifscCode", a.ifscCode)
            obj.put("accountHolder", a.accountHolder)
            obj.put("openingBalancePaise", a.openingBalancePaise)
            obj.put("colorHex", a.colorHex)
            obj.put("iconName", a.iconName)
            accArray.put(obj)
        }
        root.put("accounts", accArray)

        // Categories
        val catArray = JSONArray()
        for (c in categories) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("type", c.type.name)
            obj.put("iconName", c.iconName)
            obj.put("colorHex", c.colorHex)
            obj.put("isCustom", c.isCustom)
            catArray.put(obj)
        }
        root.put("categories", catArray)

        // Transactions
        val txArray = JSONArray()
        for (t in transactions) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("type", t.type.name)
            obj.put("amountPaise", t.amountPaise)
            obj.put("categoryId", t.categoryId ?: JSONObject.NULL)
            obj.put("accountId", t.accountId)
            obj.put("toAccountId", t.toAccountId ?: JSONObject.NULL)
            obj.put("dateEpochMillis", t.dateEpochMillis)
            obj.put("merchantOrPayee", t.merchantOrPayee)
            obj.put("description", t.description)
            obj.put("note", t.note)
            obj.put("paymentMethod", t.paymentMethod)
            obj.put("referenceId", t.referenceId ?: JSONObject.NULL)
            txArray.put(obj)
        }
        root.put("transactions", txArray)

        // Budgets
        val bgArray = JSONArray()
        for (b in budgets) {
            val obj = JSONObject()
            obj.put("id", b.id)
            obj.put("monthYear", b.monthYear)
            obj.put("totalLimitPaise", b.totalLimitPaise)
            obj.put("categoryLimitsJson", b.categoryLimitsJson)
            bgArray.put(obj)
        }
        root.put("budgets", bgArray)

        return root.toString(2)
    }

    /**
     * Parses and validates JSON backup content.
     */
    fun parseJsonBackup(jsonStr: String): Result<Triple<List<AccountEntity>, List<CategoryEntity>, List<TransactionEntity>>> {
        return try {
            val root = JSONObject(jsonStr)
            if (!root.has("accounts") || !root.has("transactions")) {
                return Result.failure(IllegalArgumentException("Invalid MoneyMap backup file: Missing accounts or transactions table."))
            }

            val accounts = mutableListOf<AccountEntity>()
            val accArray = root.getJSONArray("accounts")
            for (i in 0 until accArray.length()) {
                val obj = accArray.getJSONObject(i)
                accounts.add(
                    AccountEntity(
                        id = obj.getLong("id"),
                        name = obj.getString("name"),
                        type = AccountType.valueOf(obj.getString("type")),
                        bankName = obj.optString("bankName", ""),
                        accountNumberMasked = obj.optString("accountNumberMasked", ""),
                        ifscCode = obj.optString("ifscCode", ""),
                        accountHolder = obj.optString("accountHolder", ""),
                        openingBalancePaise = obj.optLong("openingBalancePaise", 0L),
                        colorHex = obj.optString("colorHex", "#1E56F0"),
                        iconName = obj.optString("iconName", "bank")
                    )
                )
            }

            val categories = mutableListOf<CategoryEntity>()
            val catArray = root.optJSONArray("categories")
            if (catArray != null) {
                for (i in 0 until catArray.length()) {
                    val obj = catArray.getJSONObject(i)
                    categories.add(
                        CategoryEntity(
                            id = obj.getLong("id"),
                            name = obj.getString("name"),
                            type = TransactionType.valueOf(obj.getString("type")),
                            iconName = obj.getString("iconName"),
                            colorHex = obj.getString("colorHex"),
                            isCustom = obj.optBoolean("isCustom", false)
                        )
                    )
                }
            }

            val transactions = mutableListOf<TransactionEntity>()
            val txArray = root.getJSONArray("transactions")
            for (i in 0 until txArray.length()) {
                val obj = txArray.getJSONObject(i)
                transactions.add(
                    TransactionEntity(
                        id = obj.getLong("id"),
                        type = TransactionType.valueOf(obj.getString("type")),
                        amountPaise = obj.getLong("amountPaise"),
                        categoryId = if (obj.isNull("categoryId")) null else obj.getLong("categoryId"),
                        accountId = obj.getLong("accountId"),
                        toAccountId = if (obj.isNull("toAccountId")) null else obj.getLong("toAccountId"),
                        dateEpochMillis = obj.getLong("dateEpochMillis"),
                        merchantOrPayee = obj.getString("merchantOrPayee"),
                        description = obj.optString("description", ""),
                        note = obj.optString("note", ""),
                        paymentMethod = obj.optString("paymentMethod", ""),
                        referenceId = if (obj.isNull("referenceId")) null else obj.getString("referenceId")
                    )
                )
            }

            Result.success(Triple(accounts, categories, transactions))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exports transactions to standard CSV format.
     */
    fun createCsvExport(
        transactions: List<TransactionEntity>,
        accountMap: Map<Long, AccountEntity>,
        categoryMap: Map<Long, CategoryEntity>
    ): String {
        val sb = StringBuilder()
        sb.append("Date,Type,Amount (INR),Category,Account,Merchant/Payee,Reference ID,Notes\n")

        val csvDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        for (t in transactions) {
            val dateStr = csvDateFormat.format(Date(t.dateEpochMillis))
            val typeStr = t.type.name
            val amountStr = String.format(Locale.ENGLISH, "%.2f", t.amountPaise / 100.0)
            val categoryStr = t.categoryId?.let { categoryMap[it]?.name } ?: ""
            val accountStr = accountMap[t.accountId]?.name ?: "Unknown Account"
            val merchantStr = sanitizeCsv(t.merchantOrPayee)
            val refStr = sanitizeCsv(t.referenceId ?: "")
            val notesStr = sanitizeCsv(t.note)

            sb.append("$dateStr,$typeStr,$amountStr,\"$categoryStr\",\"$accountStr\",\"$merchantStr\",\"$refStr\",\"$notesStr\"\n")
        }
        return sb.toString()
    }

    private fun sanitizeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }

    /**
     * Writes content to an Android Uri (SAF or Cache) with robust storage checking and error reporting.
     */
    fun writeToUri(context: Context, uri: Uri, content: String): Boolean {
        return try {
            val stat = android.os.StatFs(context.filesDir.absolutePath)
            val available = stat.availableBlocksLong * stat.blockSizeLong
            if (available < content.length.toLong() + 1024 * 1024) {
                return false
            }
            context.contentResolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os).use { writer ->
                    writer.write(content)
                    writer.flush()
                }
            } ?: return false
            true
        } catch (e: Exception) {
            false
        }
    }

    fun writeToUriSafely(context: Context, uri: Uri, content: String): Result<Unit> {
        return try {
            val stat = android.os.StatFs(context.filesDir.absolutePath)
            val available = stat.availableBlocksLong * stat.blockSizeLong
            if (available < content.length.toLong() + 1024 * 1024) {
                return Result.failure(java.io.IOException("Backup failed because the file could not be written. Your existing data has not been intentionally deleted."))
            }
            context.contentResolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os).use { writer ->
                    writer.write(content)
                    writer.flush()
                }
            } ?: return Result.failure(java.io.IOException("Backup failed because the file could not be written. Your existing data has not been intentionally deleted."))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(java.io.IOException("Backup failed because the file could not be written. Your existing data has not been intentionally deleted.", e))
        }
    }

    /**
     * Reads content from an Android Uri.
     */
    fun readFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
