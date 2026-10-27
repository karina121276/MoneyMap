# MoneyMap — Offline-First Personal Finance App for Android

**Tagline:** Track • Plan • Grow  
**Platform:** Android (API 26+)  
**Language:** Kotlin  
**UI Framework:** Jetpack Compose (Material 3)  
**Database:** Room (SQLite)  
**Architecture:** MVVM + Clean Architecture + Coroutines/Flow  
**Currency:** Indian Rupee (INR, ₹) in integer paise  

---

## 📱 Features

1. **Home Dashboard**
   - Live Total Tracked Balance with eye visibility toggle
   - Monthly Income, Expense, and Net Cash Flow summaries
   - Monthly Spending Goal progress indicator
   - Recent transaction history and quick navigation
2. **Account Management**
   - Bank accounts, Cash wallets, UPI wallets, and Credit Cards
   - Distinguishes assets from credit card liabilities
   - Masked account numbers, IFSC codes, and opening balances
3. **Transactions**
   - Income, Expense, and Transfer transactions
   - Keypad and quick amount suggestions (₹500, ₹1,000, ₹2,000, ₹5,000)
   - Category and account selectors
   - Search, filter by type/category/account, and date grouping
4. **Categories**
   - 17 default Indian spending & income categories
   - Custom category creator with color & icon picker
   - Safe deletion with transaction reassignment protection
5. **Reports & Spending Analysis**
   - Daily, weekly, monthly, and yearly breakdowns
   - Interactive Donut chart and Category percentage breakdown
   - Top spending categories with progress bars
6. **Calendar View**
   - 7-day calendar grid with daily income and expense activity dots
   - Selected date transaction list and daily net cash flow
7. **Monthly Budgets**
   - Total spending budget and category limit tracking
   - Real-time remaining budget and warning indications
8. **Smart Entry & Importer**
   - Text and UPI screenshot parser (Google Pay, PhonePe, Paytm, CRED)
   - PDF bank statement table parser
   - Duplicate detection engine with similarity scoring (Keep Existing, Keep Both, Update Existing, Skip New)
9. **Android Home-Screen Widget**
   - Large (4x2) resizable home-screen widget
   - Live Total Balance, Monthly Income, Expense, and Savings
   - One-tap eye toggle to hide/show balances
   - Quick Add activity for instant 1-tap transaction entry
10. **Offline Privacy & Security**
    - 100% offline-first: no mandatory accounts, cloud sync, or bank credentials
    - App Lock toggle (device credential/biometric)
    - Sensitive amount hiding
    - In-app screenshot blocking (`FLAG_SECURE`)
11. **Backup & Restore**
    - JSON database export and restore with schema validation
    - CSV transaction export for Excel/Google Sheets
12. **Localization**
    - English, Hindi (हिंदी), and Marathi (मराठी)

---

## 🧮 Accounting Invariants

- Monetary values are stored as positive integer paise (`Long`, 1 INR = 100 paise).
- Transfers affect both accounts atomically and do not alter total tracked assets.
- Transfers are strictly excluded from income and expense summaries.
- Credit cards are treated as liabilities.
- Balances are derived directly from opening balances and verified ledger records.

---

## 🧪 Testing

Run unit and Robolectric tests:
```bash
gradle :app:testDebugUnitTest
```
