package com.thecorporatedeveloper.moneymap.backup

import android.content.Context
import android.net.Uri
import com.thecorporatedeveloper.moneymap.core.model.AccountType
import com.thecorporatedeveloper.moneymap.core.model.CurrencyFormatter
import com.thecorporatedeveloper.moneymap.core.model.TransactionType
import com.thecorporatedeveloper.moneymap.data.entity.AccountEntity
import com.thecorporatedeveloper.moneymap.data.entity.BudgetEntity
import com.thecorporatedeveloper.moneymap.data.entity.CategoryEntity
import com.thecorporatedeveloper.moneymap.data.entity.ReceiptEntity
import com.thecorporatedeveloper.moneymap.data.entity.TransactionEntity
import com.thecorporatedeveloper.moneymap.data.repository.MoneyMapRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupManager {

    private const val BACKUP_VERSION = 2
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)

    data class BackupPackageData(
        val accounts: List<AccountEntity>,
        val categories: List<CategoryEntity>,
        val transactions: List<TransactionEntity>,
        val budgets: List<BudgetEntity>,
        val receipts: List<ReceiptEntity> = emptyList()
    )

    /**
     * Creates a full JSON string of the user's financial database including receipts.
     */
    fun createJsonBackup(
        accounts: List<AccountEntity>,
        categories: List<CategoryEntity>,
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        receipts: List<ReceiptEntity> = emptyList()
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
            obj.put("name", b.name)
            obj.put("monthYear", b.monthYear)
            obj.put("period", b.period)
            obj.put("amountLimitPaise", b.amountLimitPaise)
            obj.put("scope", b.scope)
            obj.put("accountId", b.accountId ?: JSONObject.NULL)
            obj.put("categoryId", b.categoryId ?: JSONObject.NULL)
            obj.put("startDateEpochMillis", b.startDateEpochMillis)
            obj.put("endDateEpochMillis", b.endDateEpochMillis)
            obj.put("totalLimitPaise", b.totalLimitPaise)
            obj.put("categoryLimitsJson", b.categoryLimitsJson)
            obj.put("createdAt", b.createdAt)
            obj.put("updatedAt", b.updatedAt)
            bgArray.put(obj)
        }
        root.put("budgets", bgArray)

        // Receipts
        val rcArray = JSONArray()
        for (r in receipts) {
            val obj = JSONObject()
            obj.put("id", r.id)
            obj.put("transactionId", r.transactionId)
            obj.put("localFilePath", r.localFilePath)
            obj.put("thumbnailPath", r.thumbnailPath ?: JSONObject.NULL)
            obj.put("originalFileName", r.originalFileName)
            obj.put("mimeType", r.mimeType)
            obj.put("fileSize", r.fileSize)
            obj.put("displayOrder", r.displayOrder)
            obj.put("isPrimary", r.isPrimary)
            obj.put("createdAt", r.createdAt)
            rcArray.put(obj)
        }
        root.put("receipts", rcArray)

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
     * Parses budget entities from a JSON backup if present.
     */
    fun parseBudgetsFromJson(jsonStr: String): List<BudgetEntity> {
        val list = mutableListOf<BudgetEntity>()
        try {
            val root = JSONObject(jsonStr)
            val bgArray = root.optJSONArray("budgets") ?: return emptyList()
            for (i in 0 until bgArray.length()) {
                val obj = bgArray.getJSONObject(i)
                list.add(
                    BudgetEntity(
                        id = obj.optLong("id", 0L),
                        name = obj.optString("name", "Monthly Budget"),
                        monthYear = obj.optString("monthYear", ""),
                        period = obj.optString("period", "MONTHLY"),
                        amountLimitPaise = obj.optLong("amountLimitPaise", obj.optLong("totalLimitPaise", 0L)),
                        scope = obj.optString("scope", "ALL_ACCOUNTS"),
                        accountId = if (obj.isNull("accountId")) null else obj.optLong("accountId"),
                        categoryId = if (obj.isNull("categoryId")) null else obj.optLong("categoryId"),
                        startDateEpochMillis = obj.optLong("startDateEpochMillis", 0L),
                        endDateEpochMillis = obj.optLong("endDateEpochMillis", 0L),
                        totalLimitPaise = obj.optLong("totalLimitPaise", 0L),
                        categoryLimitsJson = obj.optString("categoryLimitsJson", "{}"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {}
        return list
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

    /**
     * Parses receipt entities from a JSON backup if present.
     */
    fun parseReceiptsFromJson(jsonStr: String): List<ReceiptEntity> {
        val list = mutableListOf<ReceiptEntity>()
        try {
            val root = JSONObject(jsonStr)
            val rcArray = root.optJSONArray("receipts") ?: return emptyList()
            for (i in 0 until rcArray.length()) {
                val obj = rcArray.getJSONObject(i)
                list.add(
                    ReceiptEntity(
                        id = obj.optLong("id", 0L),
                        transactionId = obj.getLong("transactionId"),
                        localFilePath = obj.getString("localFilePath"),
                        thumbnailPath = if (obj.isNull("thumbnailPath")) null else obj.optString("thumbnailPath", null),
                        originalFileName = obj.optString("originalFileName", ""),
                        mimeType = obj.optString("mimeType", "image/jpeg"),
                        fileSize = obj.optLong("fileSize", 0L),
                        displayOrder = obj.optInt("displayOrder", 0),
                        isPrimary = obj.optBoolean("isPrimary", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    /**
     * Creates a ZIP backup containing data.json and all receipt image files.
     */
    fun createZipBackup(
        context: Context,
        destinationUri: Uri,
        accounts: List<AccountEntity>,
        categories: List<CategoryEntity>,
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        receipts: List<ReceiptEntity>
    ): Result<Int> {
        return try {
            val jsonContent = createJsonBackup(accounts, categories, transactions, budgets, receipts)
            var filesCount = 0


            context.contentResolver.openOutputStream(destinationUri)?.use { os ->
                ZipOutputStream(os).use { zos ->
                    // 1. data.json entry
                    val dataEntry = ZipEntry("data.json")
                    zos.putNextEntry(dataEntry)
                    zos.write(jsonContent.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                    filesCount++

                    // 2. Receipt files
                    val filesDir = context.filesDir
                    for (receipt in receipts) {
                        val file = if (receipt.localFilePath.startsWith("/")) {
                            File(receipt.localFilePath)
                        } else {
                            File(filesDir, receipt.localFilePath)
                        }
                        if (file.exists() && file.isFile) {
                            val zipPath = receipt.localFilePath.trimStart('/')
                            val entry = ZipEntry(zipPath)
                            zos.putNextEntry(entry)
                            file.inputStream().use { input ->
                                input.copyTo(zos)
                            }
                            zos.closeEntry()
                            filesCount++
                        }

                        // Thumbnail file if exists
                        receipt.thumbnailPath?.let { thumbRelPath ->
                            val thumbFile = if (thumbRelPath.startsWith("/")) File(thumbRelPath) else File(filesDir, thumbRelPath)
                            if (thumbFile.exists() && thumbFile.isFile && thumbFile != file) {
                                val entry = ZipEntry(thumbRelPath.trimStart('/'))
                                zos.putNextEntry(entry)
                                thumbFile.inputStream().use { input ->
                                    input.copyTo(zos)
                                }
                                zos.closeEntry()
                                filesCount++
                            }
                        }
                    }
                }
            } ?: return Result.failure(java.io.IOException("Failed to open destination URI for backup."))

            Result.success(filesCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reads either a ZIP backup or a legacy JSON backup and extracts all models + receipt files.
     */
    fun readBackupPackage(
        context: Context,
        sourceUri: Uri
    ): Result<BackupPackageData> {
        return try {
            var isZip = false
            try {
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    val header = ByteArray(4)
                    val read = input.read(header)
                    if (read == 4 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() &&
                        header[2] == 0x03.toByte() && header[3] == 0x04.toByte()) {
                        isZip = true
                    }
                }
            } catch (_: Exception) {}

            var jsonContent: String? = null
            if (isZip) {
                val filesDir = context.filesDir
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    ZipInputStream(input).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            val entryName = entry.name
                            if (entryName == "data.json") {
                                jsonContent = zis.bufferedReader(Charsets.UTF_8).readText()
                            } else if (entryName.startsWith("receipts/")) {
                                val targetFile = File(filesDir, entryName)
                                targetFile.parentFile?.mkdirs()
                                FileOutputStream(targetFile).use { out ->
                                    zis.copyTo(out)
                                }
                            }
                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                    }
                }
            } else {
                jsonContent = readFromUri(context, sourceUri)
            }

            if (jsonContent.isNullOrBlank()) {
                return Result.failure(IllegalArgumentException("Backup file was empty or could not be read."))
            }

            val parseResult = parseJsonBackup(jsonContent!!)
            if (parseResult.isFailure) {
                return Result.failure(parseResult.exceptionOrNull() ?: Exception("Failed to parse backup JSON"))
            }

            val (accounts, categories, transactions) = parseResult.getOrThrow()
            val budgets = parseBudgetsFromJson(jsonContent!!)
            val receipts = parseReceiptsFromJson(jsonContent!!)

            Result.success(BackupPackageData(accounts, categories, transactions, budgets, receipts))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
