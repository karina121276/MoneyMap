package com.thecorporatedeveloper.moneymap.core.model

import java.text.NumberFormat
import java.util.Locale

/**
 * Currency and financial models.
 * All monetary amounts are strictly maintained as Long integer paise (1 INR = 100 paise).
 */
object CurrencyFormatter {
    private val standardLocale = Locale.US
    @Volatile
    private var globalCurrencyCode: String = "INR"

    fun setActiveCurrency(code: String) {
        globalCurrencyCode = code
    }

    fun getActiveCurrency(): String = globalCurrencyCode

    /**
     * Formats integer paise into currency string (e.g. 12432000 paise -> "₹1,24,320" or "$124,320.00")
     */
    fun formatRupees(
        paise: Long,
        showDecimals: Boolean = false,
        showSign: Boolean = false,
        hideAmounts: Boolean = false,
        currencyCode: String = globalCurrencyCode
    ): String {
        if (hideAmounts) return "••••••"
        val currencyInfo = CurrencyRepository.getCurrency(currencyCode)
        val symbol = currencyInfo.symbol
        val isNegative = paise < 0
        val absPaise = kotlin.math.abs(paise)
        val units = absPaise / 100
        val remaining = absPaise % 100

        val formattedUnits = if (isIndianGrouping(currencyCode)) {
            formatIndianGrouping(units)
        } else {
            NumberFormat.getNumberInstance(standardLocale).format(units)
        }

        val prefix = when {
            showSign && isNegative -> "-$symbol"
            showSign && paise > 0 -> "+$symbol"
            isNegative -> "-$symbol"
            else -> symbol
        }

        return if (showDecimals && remaining > 0) {
            String.format(standardLocale, "%s%s.%02d", prefix, formattedUnits, remaining)
        } else {
            "$prefix$formattedUnits"
        }
    }

    /**
     * Formats transaction amount with explicit sign per transaction type.
     */
    fun formatTransactionAmount(
        paise: Long,
        type: TransactionType,
        hideAmounts: Boolean = false,
        currencyCode: String = globalCurrencyCode
    ): String {
        if (hideAmounts) return "••••••"
        val currencyInfo = CurrencyRepository.getCurrency(currencyCode)
        val symbol = currencyInfo.symbol
        val absPaise = kotlin.math.abs(paise)
        val units = absPaise / 100
        val formattedUnits = if (isIndianGrouping(currencyCode)) {
            formatIndianGrouping(units)
        } else {
            NumberFormat.getNumberInstance(standardLocale).format(units)
        }
        return when (type) {
            TransactionType.EXPENSE -> "-$symbol$formattedUnits"
            TransactionType.INCOME -> "+$symbol$formattedUnits"
            TransactionType.TRANSFER -> "$symbol$formattedUnits"
        }
    }

    /**
     * Compact formatting for charts/widgets (e.g., $85K, $1.2M)
     */
    fun formatCompactRupees(
        paise: Long,
        hideAmounts: Boolean = false,
        currencyCode: String = globalCurrencyCode
    ): String {
        if (hideAmounts) return "••••"
        val currencyInfo = CurrencyRepository.getCurrency(currencyCode)
        val symbol = currencyInfo.symbol
        val isNegative = paise < 0
        val units = kotlin.math.abs(paise) / 100.0
        val prefix = if (isNegative) "-$symbol" else symbol
        return when {
            units >= 10000000.0 -> String.format(standardLocale, "%s%.1fCr", prefix, units / 10000000.0)
            units >= 100000.0 -> String.format(standardLocale, "%s%.1fL", prefix, units / 100000.0)
            units >= 1000.0 -> String.format(standardLocale, "%s%.0fK", prefix, units / 1000.0)
            else -> "$prefix${units.toLong()}"
        }
    }

    private fun isIndianGrouping(code: String): Boolean {
        val upper = code.uppercase()
        return upper == "INR" || upper == "NPR" || upper == "PKR" || upper == "BDT" || upper == "LKR"
    }

    private fun formatIndianGrouping(number: Long): String {
        val s = number.toString()
        if (s.length <= 3) return s
        val lastThree = s.substring(s.length - 3)
        val remaining = s.substring(0, s.length - 3)
        val sb = StringBuilder()
        var count = 0
        for (i in remaining.length - 1 downTo 0) {
            sb.append(remaining[i])
            count++
            if (count % 2 == 0 && i != 0) {
                sb.append(',')
            }
        }
        return sb.reverse().toString() + "," + lastThree
    }

    fun parseInputToPaise(input: String, currencyCode: String = globalCurrencyCode): Long {
        val currencyInfo = CurrencyRepository.getCurrency(currencyCode)
        val cleaned = input.replace(currencyInfo.symbol, "")
            .replace("₹", "")
            .replace("$", "")
            .replace("€", "")
            .replace("£", "")
            .replace("¥", "")
            .replace(",", "")
            .trim()
        if (cleaned.isEmpty()) return 0L
        return try {
            val amountDouble = cleaned.toDouble()
            (amountDouble * 100).toLong()
        } catch (e: Exception) {
            0L
        }
    }
}

enum class AccountType(val displayName: String) {
    BANK("Bank Account"),
    SAVINGS("Savings Account"),
    CASH("Cash Account"),
    WALLET("Wallet Account"),
    CREDIT_CARD("Credit Card");

    val isLiability: Boolean
        get() = this == CREDIT_CARD
}

enum class TransactionType(val displayName: String) {
    EXPENSE("Expense"),
    INCOME("Income"),
    TRANSFER("Transfer")
}
