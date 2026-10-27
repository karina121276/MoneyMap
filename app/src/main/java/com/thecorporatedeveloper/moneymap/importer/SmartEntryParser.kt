package com.thecorporatedeveloper.moneymap.importer

import com.thecorporatedeveloper.moneymap.core.model.CurrencyFormatter
import com.thecorporatedeveloper.moneymap.core.model.TransactionType
import com.thecorporatedeveloper.moneymap.data.entity.AccountEntity
import com.thecorporatedeveloper.moneymap.data.entity.CategoryEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale
import java.util.regex.Pattern

data class ParsedField<T>(
    val value: T,
    val confidence: Int // 0 to 100 percentage
)

data class ExtractedTransaction(
    val amountPaise: ParsedField<Long>,
    val isAmountDetected: Boolean,
    val merchantOrPayee: ParsedField<String>,
    val dateEpochMillis: ParsedField<Long>,
    val isDateDetected: Boolean,
    val timeFormatted: String?,
    val transactionType: ParsedField<TransactionType>,
    val isTypeUncertain: Boolean,
    val suggestedCategoryName: ParsedField<String>,
    val suggestedCategoryId: Long?,
    val suggestedAccountId: Long?,
    val referenceId: ParsedField<String?>,
    val note: ParsedField<String> = ParsedField("", 0),
    val paymentStatusWarning: String? = null,
    val rawText: String = ""
)

object SmartEntryParser {

    /**
     * Parses raw text from a UPI screenshot, receipt OCR, or SMS notification.
     */
    fun parseText(
        rawText: String,
        accounts: List<AccountEntity> = emptyList(),
        categories: List<CategoryEntity> = emptyList()
    ): ExtractedTransaction {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val fullText = rawText.lowercase(Locale.ENGLISH)

        // ---------------------------------------------------------------------
        // 1. Transaction Type Detection
        // ---------------------------------------------------------------------
        val isTransferToSelf = fullText.contains("transfer to self") ||
                fullText.contains("transferred to your other account") ||
                fullText.contains("self transfer") ||
                fullText.contains("to own account")

        val startsWithFrom = lines.firstOrNull()?.startsWith("from", ignoreCase = true) == true ||
                fullText.startsWith("from ") || fullText.contains("\nfrom ")
        val startsWithTo = lines.firstOrNull()?.startsWith("to", ignoreCase = true) == true ||
                fullText.startsWith("to ") || fullText.contains("\nto ")

        val isIncome = fullText.contains("received from") ||
                fullText.contains("money received") ||
                fullText.contains("credited to") ||
                fullText.contains("credited with") ||
                fullText.contains("added to your account") ||
                fullText.contains("cashback received") ||
                fullText.contains("refund from") ||
                fullText.contains("refund received") ||
                fullText.contains("salary") ||
                (startsWithFrom && !fullText.contains("paid to") && !fullText.contains("payment to"))

        val isExpense = fullText.contains("paid to") ||
                fullText.contains("payment to") ||
                fullText.contains("sent to") ||
                fullText.contains("money sent") ||
                fullText.contains("debited from") ||
                fullText.contains("bill paid") ||
                fullText.contains("payment successful") ||
                fullText.contains("paid successfully") ||
                (startsWithTo && !fullText.contains("received from"))

        val txType: TransactionType
        val typeConfidence: Int
        var isTypeUncertain = false

        when {
            isTransferToSelf -> {
                txType = TransactionType.TRANSFER
                typeConfidence = 95
            }
            isIncome -> {
                txType = TransactionType.INCOME
                typeConfidence = if (fullText.contains("credited") || fullText.contains("received from")) 95 else 85
            }
            isExpense -> {
                txType = TransactionType.EXPENSE
                typeConfidence = if (fullText.contains("paid to") || fullText.contains("sent to") || fullText.contains("payment successful")) 95 else 80
            }
            else -> {
                // Default to Expense as UPI apps are predominantly used for spending,
                // but mark as uncertain so the user is asked to review and confirm.
                txType = TransactionType.EXPENSE
                typeConfidence = 45
                isTypeUncertain = true
            }
        }

        // ---------------------------------------------------------------------
        // 2. Amount Extraction
        // ---------------------------------------------------------------------
        var amountPaise = 0L
        var amountConfidence = 0
        var isAmountDetected = false

        // Matches ₹450, ₹ 450, Rs. 1,250.00, INR 500, etc.
        val currencySymbolsPattern = Pattern.compile(
            "(?:₹|rs\\.?|inr)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)",
            Pattern.CASE_INSENSITIVE
        )
        val symbolMatcher = currencySymbolsPattern.matcher(rawText)
        if (symbolMatcher.find()) {
            val amtStr = symbolMatcher.group(1) ?: "0"
            amountPaise = CurrencyFormatter.parseInputToPaise(amtStr)
            if (amountPaise > 0) {
                amountConfidence = 100
                isAmountDetected = true
            }
        }

        if (!isAmountDetected) {
            // Look for standalone numbers with decimal (e.g. 450.00 or 1,250.50)
            val decimalPattern = Pattern.compile("\\b([0-9]{1,3}(?:,[0-9]{3})*\\.[0-9]{2})\\b")
            val decMatcher = decimalPattern.matcher(rawText)
            while (decMatcher.find()) {
                val candidateStr = decMatcher.group(1) ?: ""
                val p = CurrencyFormatter.parseInputToPaise(candidateStr)
                if (p in 100..1000000000L) { // ₹1 to ₹1,00,00,000
                    amountPaise = p
                    amountConfidence = 80
                    isAmountDetected = true
                    break
                }
            }
        }

        if (!isAmountDetected) {
            // Fallback: look for clean integer line surrounded by payment text
            for (line in lines) {
                val clean = line.replace(",", "").trim()
                val num = clean.toLongOrNull()
                if (num != null && num in 10..5000000) {
                    amountPaise = num * 100
                    amountConfidence = 60
                    isAmountDetected = true
                    break
                }
            }
        }

        // ---------------------------------------------------------------------
        // 3. Merchant / Payee / Sender Extraction
        // ---------------------------------------------------------------------
        var merchant = ""
        var merchantConfidence = 40

        // Check explicit patterns like "Paid to ABC", "Money Sent to XYZ", "Received from ABC", "To: ABC"
        val explicitPatterns = if (txType == TransactionType.EXPENSE) {
            listOf(
                Pattern.compile("(?:paid to|payment to)\\s+([A-Za-z0-9 &._'-]{2,40})", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(?:money sent(?: successfully)? to|sent to)\\s+([A-Za-z0-9 &._'-]{2,40})", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(?:to:?)\\s+([A-Za-z0-9 &._'-]{2,40})", Pattern.CASE_INSENSITIVE)
            )
        } else {
            listOf(
                Pattern.compile("(?:received from|money received from|from:?)\\s+([A-Za-z0-9 &._'-]{2,40})", Pattern.CASE_INSENSITIVE)
            )
        }

        for (pattern in explicitPatterns) {
            val matcher = pattern.matcher(rawText)
            if (matcher.find()) {
                val candidate = cleanExtractedName(matcher.group(1) ?: "")
                if (candidate.isNotBlank() && !isDisallowedKeyword(candidate)) {
                    merchant = candidate
                    merchantConfidence = 95
                    break
                }
            }
        }

        // Fallback to general patterns if nothing found
        if (merchant.isBlank()) {
            val fallbackPatterns = listOf(
                Pattern.compile("(?:received from|money received from|from:?)\\s+([A-Za-z0-9 &._'-]{2,40})", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(?:to:?)\\s+([A-Za-z0-9 &._'-]{2,40})", Pattern.CASE_INSENSITIVE)
            )
            for (pattern in fallbackPatterns) {
                val matcher = pattern.matcher(rawText)
                if (matcher.find()) {
                    val candidate = cleanExtractedName(matcher.group(1) ?: "")
                    if (candidate.isNotBlank() && !isDisallowedKeyword(candidate)) {
                        merchant = candidate
                        merchantConfidence = 85
                        break
                    }
                }
            }
        }

        // If not found, check recognized Indian merchants
        if (merchant.isBlank()) {
            val knownMerchants = listOf(
                "Zomato", "Swiggy", "Blinkit", "Zepto", "Instamart", "Amazon", "Flipkart",
                "Myntra", "Uber", "Ola", "Rapido", "Petrol", "HPCL", "BPCL", "Indian Oil",
                "MSEDCL", "Jio Postpaid", "Jio", "Airtel", "Netflix", "Spotify", "YouTube",
                "Starbucks", "McDonalds", "KFC", "Burger King", "Domino's", "Apollo Pharmacy",
                "DMart", "BigBasket", "Nature's Basket", "Reliance Fresh", "Decathlon", "BookMyShow"
            )
            for (km in knownMerchants) {
                if (rawText.contains(km, ignoreCase = true)) {
                    merchant = km
                    merchantConfidence = 90
                    break
                }
            }
        }

        if (merchant.isBlank()) {
            merchant = if (txType == TransactionType.INCOME) "Sender" else "Merchant"
            merchantConfidence = 30
        }

        // ---------------------------------------------------------------------
        // 4. Date and Time Extraction
        // ---------------------------------------------------------------------
        var extractedDateMillis: Long? = null
        var extractedTimeStr: String? = null
        var isDateDetected = false
        var dateConfidence = 30

        // Parse date
        val dateResult = parseDateFromText(rawText)
        if (dateResult != null) {
            extractedDateMillis = dateResult.first
            extractedTimeStr = dateResult.second
            isDateDetected = true
            dateConfidence = 95
        }

        val finalDateMillis = extractedDateMillis ?: System.currentTimeMillis()

        // ---------------------------------------------------------------------
        // 5. UPI Reference ID / Transaction Number
        // ---------------------------------------------------------------------
        var refId: String? = null
        var refConfidence = 0

        val refPatterns = listOf(
            Pattern.compile("(?:upi ref(?: no\\.?)?|upi reference no\\.?|utr(?: no\\.?)?)\\s*:?\\s*([0-9]{12})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:txn id|transaction id|order id|google transaction id)\\s*:?\\s*([0-9A-Za-z_-]{8,35})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b([0-9]{12})\\b") // 12-digit standalone number usually UTR
        )

        for (p in refPatterns) {
            val m = p.matcher(rawText)
            if (m.find()) {
                val candidateRef = m.group(1)?.trim()
                if (!candidateRef.isNullOrBlank() && candidateRef.length in 8..35) {
                    refId = candidateRef
                    refConfidence = 100
                    break
                }
            }
        }

        // ---------------------------------------------------------------------
        // 6. Category Suggestion & Matching
        // ---------------------------------------------------------------------
        val suggestedCatName = suggestCategory(merchant, fullText)
        val matchingCat = categories.find { it.name.equals(suggestedCatName, ignoreCase = true) }
            ?: categories.firstOrNull { it.type == txType }

        // ---------------------------------------------------------------------
        // 7. Account Suggestion & Matching
        // ---------------------------------------------------------------------
        var matchedAccountId: Long? = null
        val normalizedText = rawText.lowercase(Locale.ENGLISH).replace("\\s+".toRegex(), " ")

        for (acc in accounts) {
            val accNameNorm = acc.name.lowercase(Locale.ENGLISH).replace("\\s+".toRegex(), " ").trim()
            val bankNameNorm = acc.bankName.lowercase(Locale.ENGLISH).replace("\\s+".toRegex(), " ").trim()
            if (accNameNorm.isNotBlank() && normalizedText.contains(accNameNorm)) {
                matchedAccountId = acc.id
                break
            }
            if (bankNameNorm.isNotBlank() && normalizedText.contains(bankNameNorm)) {
                matchedAccountId = acc.id
                break
            }
        }

        if (matchedAccountId == null && accounts.isNotEmpty()) {
            for (acc in accounts) {
                val keywords = (acc.name + " " + acc.bankName).lowercase(Locale.ENGLISH)
                    .split(" ", "_", "-", "/")
                    .map { it.trim() }
                    .filter { it.length >= 3 && it !in listOf("bank", "account", "wallet", "savings", "card") }
                if (keywords.any { normalizedText.contains(it) }) {
                    matchedAccountId = acc.id
                    break
                }
            }
        }

        // ---------------------------------------------------------------------
        // 8. Note / Purpose / Remarks Extraction
        // ---------------------------------------------------------------------
        var extractedNote = ""
        var noteConfidence = 0

        // Explicit note pattern
        val noteExplicitPatterns = listOf(
            Pattern.compile("(?:note|remarks?|for|message|purpose|description)\\s*:?\\s*([A-Za-z0-9 &._'-]{2,60})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:add a note|payment for)\\s*:?\\s*([A-Za-z0-9 &._'-]{2,60})", Pattern.CASE_INSENSITIVE)
        )
        for (np in noteExplicitPatterns) {
            val nm = np.matcher(rawText)
            if (nm.find()) {
                val candidateNote = nm.group(1)?.trim() ?: ""
                if (candidateNote.isNotBlank() && !isDisallowedKeyword(candidateNote) && !candidateNote.equals(merchant, ignoreCase = true)) {
                    extractedNote = candidateNote
                    noteConfidence = 90
                    break
                }
            }
        }

        // Line-by-line fallback for descriptive purpose text or custom remark line
        if (extractedNote.isBlank()) {
            val systemKeywords = listOf(
                "completed", "successful", "failed", "pending", "processing", "pay again", "view details",
                "transfer details", "upi transaction id", "google transaction id", "utr", "powered by",
                "state bank", "hdfc", "icici", "axis", "paytm", "g pay", "phonepe", "from:", "to:", "transaction"
            )
            for (line in lines) {
                val lower = line.lowercase(Locale.ENGLISH)
                val isSystem = systemKeywords.any { lower.contains(it) } ||
                        line.startsWith("₹") || line.startsWith("Rs", ignoreCase = true) || line.contains("@") ||
                        line.length < 2 || line.length > 40 ||
                        line.equals(merchant, ignoreCase = true) ||
                        lower.contains(merchant.lowercase(Locale.ENGLISH)) ||
                        line.matches(Regex("^[0-9\\W]+$"))

                if (!isSystem) {
                    extractedNote = line
                    noteConfidence = 80
                    break
                }
            }
        }

        // Fallback: Append UTR / UPI Ref if present to keep useful context
        if (extractedNote.isBlank() && !refId.isNullOrBlank()) {
            extractedNote = "UPI Ref: $refId"
            noteConfidence = 60
        }

        // ---------------------------------------------------------------------
        // 9. Payment Status Check (Failed / Pending)
        // ---------------------------------------------------------------------
        var statusWarning: String? = null
        val lowerFullText = fullText.lowercase(Locale.ENGLISH)
        when {
            lowerFullText.contains("payment failed") || lowerFullText.contains("transaction failed") || lowerFullText.contains("declined") -> {
                statusWarning = "Warning: This screenshot shows a FAILED or DECLINED payment."
            }
            lowerFullText.contains("payment pending") || lowerFullText.contains("processing") -> {
                statusWarning = "Warning: This screenshot shows a PENDING or PROCESSING payment."
            }
        }

        return ExtractedTransaction(
            amountPaise = ParsedField(amountPaise, amountConfidence),
            isAmountDetected = isAmountDetected,
            merchantOrPayee = ParsedField(merchant, merchantConfidence),
            dateEpochMillis = ParsedField(finalDateMillis, dateConfidence),
            isDateDetected = isDateDetected,
            timeFormatted = extractedTimeStr,
            transactionType = ParsedField(txType, typeConfidence),
            isTypeUncertain = isTypeUncertain,
            suggestedCategoryName = ParsedField(suggestedCatName, if (suggestedCatName != "Others") 90 else 50),
            suggestedCategoryId = matchingCat?.id,
            suggestedAccountId = matchedAccountId ?: accounts.firstOrNull()?.id,
            referenceId = ParsedField(refId, refConfidence),
            note = ParsedField(extractedNote, noteConfidence),
            paymentStatusWarning = statusWarning,
            rawText = rawText
        )
    }

    private fun cleanExtractedName(raw: String): String {
        return raw.split("\n")
            .firstOrNull()
            ?.replace(Regex("(?i)\\b(completed|successful|payment|paid|rs\\.?|₹|upi|ref|txn|id)\\b.*"), "")
            ?.trim()
            ?.trimEnd('.', ',', '-', ':')
            ?: ""
    }

    private fun isDisallowedKeyword(text: String): Boolean {
        val lower = text.lowercase(Locale.ENGLISH).trim()
        val disallowed = listOf("successful", "completed", "failed", "pending", "processing", "upi", "bank", "account")
        return disallowed.contains(lower) || lower.length < 2
    }

    /**
     * Extracts date and optional time from receipt text.
     * Returns Pair(epochMillis, timeString) or null if no valid date found.
     */
    fun parseDateFromText(text: String): Pair<Long, String?>? {
        val zone = ZoneId.systemDefault()

        // 1. "20 October 2026" / "20 Oct 2026" / "20th Oct, 2026"
        val dayMonthYearRegex = Pattern.compile(
            "\\b([0-3]?[0-9])(?:st|nd|rd|th)?\\s+(Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)[,\\s]+([12][0-9]{3})\\b",
            Pattern.CASE_INSENSITIVE
        )
        val dmyMatcher = dayMonthYearRegex.matcher(text)
        if (dmyMatcher.find()) {
            val day = dmyMatcher.group(1)?.toIntOrNull() ?: 1
            val monthStr = dmyMatcher.group(2) ?: "Jan"
            val year = dmyMatcher.group(3)?.toIntOrNull() ?: 2026
            val month = parseMonthNameToNumber(monthStr)

            val timePair = extractTimeFromText(text)
            val ldt = LocalDateTime.of(year, month, day.coerceIn(1, 31), timePair.first.hour, timePair.first.minute)
            return Pair(ldt.atZone(zone).toInstant().toEpochMilli(), timePair.second)
        }

        // 2. "Oct 20, 2026" / "October 20, 2026"
        val monthDayYearRegex = Pattern.compile(
            "\\b(Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\\s+([0-3]?[0-9])(?:st|nd|rd|th)?[,\\s]+([12][0-9]{3})\\b",
            Pattern.CASE_INSENSITIVE
        )
        val mdyMatcher = monthDayYearRegex.matcher(text)
        if (mdyMatcher.find()) {
            val monthStr = mdyMatcher.group(1) ?: "Jan"
            val day = mdyMatcher.group(2)?.toIntOrNull() ?: 1
            val year = mdyMatcher.group(3)?.toIntOrNull() ?: 2026
            val month = parseMonthNameToNumber(monthStr)

            val timePair = extractTimeFromText(text)
            val ldt = LocalDateTime.of(year, month, day.coerceIn(1, 31), timePair.first.hour, timePair.first.minute)
            return Pair(ldt.atZone(zone).toInstant().toEpochMilli(), timePair.second)
        }

        // 3. "20/10/2026" or "20-10-2026" or "20.10.2026"
        val numericDateRegex = Pattern.compile(
            "\\b([0-3]?[0-9])[\\/\\-\\.]([01]?[0-9])[\\/\\-\\.]([12][0-9]{3})\\b"
        )
        val numMatcher = numericDateRegex.matcher(text)
        if (numMatcher.find()) {
            val day = numMatcher.group(1)?.toIntOrNull() ?: 1
            val month = numMatcher.group(2)?.toIntOrNull() ?: 1
            val year = numMatcher.group(3)?.toIntOrNull() ?: 2026

            val timePair = extractTimeFromText(text)
            val ldt = LocalDateTime.of(year, month.coerceIn(1, 12), day.coerceIn(1, 31), timePair.first.hour, timePair.first.minute)
            return Pair(ldt.atZone(zone).toInstant().toEpochMilli(), timePair.second)
        }

        return null
    }

    private fun parseMonthNameToNumber(name: String): Int {
        val lower = name.lowercase(Locale.ENGLISH)
        return when {
            lower.startsWith("jan") -> 1
            lower.startsWith("feb") -> 2
            lower.startsWith("mar") -> 3
            lower.startsWith("apr") -> 4
            lower.startsWith("may") -> 5
            lower.startsWith("jun") -> 6
            lower.startsWith("jul") -> 7
            lower.startsWith("aug") -> 8
            lower.startsWith("sep") -> 9
            lower.startsWith("oct") -> 10
            lower.startsWith("nov") -> 11
            lower.startsWith("dec") -> 12
            else -> 1
        }
    }

    private fun extractTimeFromText(text: String): Pair<LocalTime, String?> {
        val timeRegex = Pattern.compile(
            "\\b([01]?[0-9]|2[0-3]):([0-5][0-9])(?::([0-5][0-9]))?\\s*(am|pm|AM|PM)?\\b"
        )
        val m = timeRegex.matcher(text)
        if (m.find()) {
            var hour = m.group(1)?.toIntOrNull() ?: 12
            val minute = m.group(2)?.toIntOrNull() ?: 0
            val amPm = m.group(4)?.uppercase(Locale.ENGLISH)

            if (amPm == "PM" && hour < 12) hour += 12
            if (amPm == "AM" && hour == 12) hour = 0

            val formatted = String.format(Locale.ENGLISH, "%02d:%02d %s", if (hour > 12) hour - 12 else if (hour == 0) 12 else hour, minute, if (hour >= 12) "PM" else "AM")
            return Pair(LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)), formatted)
        }
        return Pair(LocalTime.now(), null)
    }

    /**
     * Maps merchant/payee and text clues to standard Indian financial categories.
     */
    fun suggestCategory(merchant: String, fullText: String): String {
        val lower = (merchant + " " + fullText).lowercase(Locale.ENGLISH)
        return when {
            lower.contains("zomato") || lower.contains("swiggy") || lower.contains("mcdonald") ||
                    lower.contains("kfc") || lower.contains("burger king") || lower.contains("domino") ||
                    lower.contains("pizza") || lower.contains("restaurant") || lower.contains("cafe") ||
                    lower.contains("starbucks") || lower.contains("chai") || lower.contains("coffee") ||
                    lower.contains("food") || lower.contains("dining") || lower.contains("hotel") -> "Food & Dining"

            lower.contains("blinkit") || lower.contains("zepto") || lower.contains("instamart") ||
                    lower.contains("dmart") || lower.contains("grocery") || lower.contains("kirana") ||
                    lower.contains("vegetables") || lower.contains("fruits") || lower.contains("supermarket") ||
                    lower.contains("milk") || lower.contains("dairy") -> "Groceries"

            lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") ||
                    lower.contains("meesho") || lower.contains("shopping") || lower.contains("zara") ||
                    lower.contains("h&m") || lower.contains("clothes") || lower.contains("mall") -> "Shopping"

            lower.contains("petrol") || lower.contains("fuel") || lower.contains("hpcl") ||
                    lower.contains("bpcl") || lower.contains("indian oil") || lower.contains("cng") ||
                    lower.contains("diesel") -> "Fuel"

            lower.contains("uber") || lower.contains("ola") || lower.contains("rapido") ||
                    lower.contains("metro") || lower.contains("auto") || lower.contains("cab") ||
                    lower.contains("bus") || lower.contains("irctc") || lower.contains("railway") ||
                    lower.contains("toll") || lower.contains("fastag") -> "Transport"

            lower.contains("electricity") || lower.contains("msedcl") || lower.contains("bill") ||
                    lower.contains("jio") || lower.contains("airtel") || lower.contains("vi ") ||
                    lower.contains("water") || lower.contains("cylinder") || lower.contains("gas") ||
                    lower.contains("wifi") || lower.contains("broadband") -> "Bills & Utilities"

            lower.contains("netflix") || lower.contains("spotify") || lower.contains("youtube") ||
                    lower.contains("bookmyshow") || lower.contains("movie") || lower.contains("cinema") ||
                    lower.contains("gaming") || lower.contains("steam") || lower.contains("hotstar") ||
                    lower.contains("prime video") -> "Entertainment"

            lower.contains("apollo") || lower.contains("pharmacy") || lower.contains("hospital") ||
                    lower.contains("dr.") || lower.contains("clinic") || lower.contains("med") ||
                    lower.contains("chemist") || lower.contains("diagnostic") || lower.contains("1mg") -> "Health"

            lower.contains("salary") || lower.contains("payroll") -> "Salary"
            lower.contains("freelance") || lower.contains("client") || lower.contains("consulting") -> "Freelance"
            lower.contains("invest") || lower.contains("mutual fund") || lower.contains("zerodha") ||
                    lower.contains("groww") || lower.contains("sip") -> "Investments"
            else -> "Others"
        }
    }
}
