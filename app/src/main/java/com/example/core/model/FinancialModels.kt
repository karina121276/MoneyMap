package com.example.core.model

import java.text.NumberFormat
import java.util.Locale

/**
 * Currency and financial models.
 * All monetary amounts are strictly maintained as Long integer paise (1 INR = 100 paise).
 */
object CurrencyFormatter {
    private val indianLocale = Locale("en", "IN")

    /**
     * Formats integer paise into standard Indian Rupee string (e.g. 12432000 paise -> "₹1,24,320")
     */
    fun formatRupees(
        paise: Long,
        showDecimals: Boolean = false,
        showSign: Boolean = false,
        hideAmounts: Boolean = false
    ): String {
        if (hideAmounts) return "••••••"
        val isNegative = paise < 0
        val absPaise = kotlin.math.abs(paise)
        val rupees = absPaise / 100
        val remainingPaise = absPaise % 100

        val formattedRupees = formatIndianGrouping(rupees)
        val prefix = when {
            showSign && isNegative -> "-₹"
            showSign && paise > 0 -> "+₹"
            isNegative -> "-₹"
            else -> "₹"
        }

        return if (showDecimals && remainingPaise > 0) {
            String.format(indianLocale, "%s%s.%02d", prefix, formattedRupees, remainingPaise)
        } else {
            "$prefix$formattedRupees"
        }
    }

    /**
     * Formats transaction amount with explicit sign per transaction type using Indian grouping.
     * Expense: -₹2,000, Income: +₹350, Transfer: ₹3,000
     */
    fun formatTransactionAmount(
        paise: Long,
        type: TransactionType,
        hideAmounts: Boolean = false
    ): String {
        if (hideAmounts) return "••••••"
        val absPaise = kotlin.math.abs(paise)
        val rupees = absPaise / 100
        val formattedRupees = formatIndianGrouping(rupees)
        return when (type) {
            TransactionType.EXPENSE -> "-₹$formattedRupees"
            TransactionType.INCOME -> "+₹$formattedRupees"
            TransactionType.TRANSFER -> "₹$formattedRupees"
        }
    }

    /**
     * Compact formatting for charts/widgets (e.g., ₹85K, ₹1.2L)
     */
    fun formatCompactRupees(paise: Long, hideAmounts: Boolean = false): String {
        if (hideAmounts) return "••••"
        val isNegative = paise < 0
        val rupees = kotlin.math.abs(paise) / 100.0
        val prefix = if (isNegative) "-₹" else "₹"
        return when {
            rupees >= 10000000.0 -> String.format(indianLocale, "%s%.1fCr", prefix, rupees / 10000000.0)
            rupees >= 100000.0 -> String.format(indianLocale, "%s%.1fL", prefix, rupees / 100000.0)
            rupees >= 1000.0 -> String.format(indianLocale, "%s%.0fK", prefix, rupees / 1000.0)
            else -> "$prefix${rupees.toLong()}"
        }
    }

    /**
     * Formats numbers according to Indian grouping (e.g., 1,23,456)
     */
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

    /**
     * Parses user input rupee string into paise Long.
     */
    fun parseInputToPaise(input: String): Long {
        val cleaned = input.replace("₹", "").replace(",", "").trim()
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
