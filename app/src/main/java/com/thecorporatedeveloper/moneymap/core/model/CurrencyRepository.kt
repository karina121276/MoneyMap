package com.thecorporatedeveloper.moneymap.core.model

data class CurrencyInfo(
    val code: String,       // e.g. "INR"
    val name: String,       // e.g. "Indian Rupee"
    val symbol: String,     // e.g. "₹"
    val flag: String,       // e.g. "🇮🇳"
    val country: String     // e.g. "India"
)

object CurrencyRepository {
    val supportedCurrencies = listOf(
        CurrencyInfo("INR", "Indian Rupee", "₹", "🇮🇳", "India"),
        CurrencyInfo("USD", "US Dollar", "$", "🇺🇸", "United States"),
        CurrencyInfo("EUR", "Euro", "€", "🇪🇺", "European Union"),
        CurrencyInfo("GBP", "British Pound", "£", "🇬🇧", "United Kingdom"),
        CurrencyInfo("JPY", "Japanese Yen", "¥", "🇯🇵", "Japan"),
        CurrencyInfo("AED", "UAE Dirham", "د.إ", "🇦🇪", "United Arab Emirates"),
        CurrencyInfo("CAD", "Canadian Dollar", "$", "🇨🇦", "Canada"),
        CurrencyInfo("AUD", "Australian Dollar", "$", "🇦🇺", "Australia"),
        CurrencyInfo("SGD", "Singapore Dollar", "$", "🇸🇬", "Singapore"),
        CurrencyInfo("CHF", "Swiss Franc", "CHF", "🇨🇭", "Switzerland"),
        CurrencyInfo("CNY", "Chinese Yuan", "¥", "🇨🇳", "China"),
        CurrencyInfo("SAR", "Saudi Riyal", "ر.س", "🇸🇦", "Saudi Arabia"),
        CurrencyInfo("BDT", "Bangladeshi Taka", "৳", "🇧🇩", "Bangladesh"),
        CurrencyInfo("PKR", "Pakistani Rupee", "₨", "🇵🇰", "Pakistan"),
        CurrencyInfo("LKR", "Sri Lankan Rupee", "Rs", "🇱🇰", "Sri Lanka"),
        CurrencyInfo("NPR", "Nepalese Rupee", "रू", "🇳🇵", "Nepal"),
        CurrencyInfo("ZAR", "South African Rand", "R", "🇿🇦", "South Africa"),
        CurrencyInfo("BRL", "Brazilian Real", "R$", "🇧🇷", "Brazil"),
        CurrencyInfo("RUB", "Russian Ruble", "₽", "🇷🇺", "Russia"),
        CurrencyInfo("KRW", "South Korean Won", "₩", "🇰🇷", "South Korea"),
        CurrencyInfo("NZD", "New Zealand Dollar", "$", "🇳🇿", "New Zealand"),
        CurrencyInfo("HKD", "Hong Kong Dollar", "$", "🇭🇰", "Hong Kong"),
        CurrencyInfo("SEK", "Swedish Krona", "kr", "🇸🇪", "Sweden"),
        CurrencyInfo("NOK", "Norwegian Krone", "kr", "🇳🇴", "Norway"),
        CurrencyInfo("DKK", "Danish Krone", "kr", "🇩🇰", "Denmark"),
        CurrencyInfo("PLN", "Polish Złoty", "zł", "🇵🇱", "Poland"),
        CurrencyInfo("THB", "Thai Baht", "฿", "🇹🇭", "Thailand"),
        CurrencyInfo("IDR", "Indonesian Rupiah", "Rp", "🇮🇩", "Indonesia"),
        CurrencyInfo("MYR", "Malaysian Ringgit", "RM", "🇲🇾", "Malaysia"),
        CurrencyInfo("PHP", "Philippine Peso", "₱", "🇵🇭", "Philippines"),
        CurrencyInfo("VND", "Vietnamese Dong", "₫", "🇻🇳", "Vietnam"),
        CurrencyInfo("MXN", "Mexican Peso", "$", "🇲🇽", "Mexico"),
        CurrencyInfo("CLP", "Chilean Peso", "$", "🇨🇱", "Chile"),
        CurrencyInfo("COP", "Colombian Peso", "$", "🇨🇴", "Colombia"),
        CurrencyInfo("ARS", "Argentine Peso", "$", "🇦🇷", "Argentina"),
        CurrencyInfo("EGP", "Egyptian Pound", "E£", "🇪🇬", "Egypt"),
        CurrencyInfo("NGN", "Nigerian Naira", "₦", "🇳🇬", "Nigeria"),
        CurrencyInfo("KES", "Kenyan Shilling", "KSh", "🇰🇪", "Kenya"),
        CurrencyInfo("GHS", "Ghanaian Cedi", "₵", "🇬🇭", "Ghana")
    )

    fun getCurrency(code: String): CurrencyInfo {
        return supportedCurrencies.find { it.code.equals(code, ignoreCase = true) }
            ?: CurrencyInfo("INR", "Indian Rupee", "₹", "🇮🇳", "India")
    }
}
