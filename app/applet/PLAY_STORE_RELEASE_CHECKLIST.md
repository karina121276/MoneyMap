# Google Play Store Release Checklist — MoneyMap

## 1. App Metadata & Store Listing

- **App Name:** MoneyMap (30 characters max, clean & compliant)
- **Short Description (80 chars max):** Track expenses, accounts, budgets & smart UPI receipts 100% offline & securely.
- **Full Description:**
  MoneyMap is your ultimate offline-first personal finance companion designed for privacy, simplicity, and financial clarity.

  **Key Features:**
  - **Multi-Account Tracking:** Manage bank accounts, cash wallets, digital UPI, and credit cards in one place.
  - **Smart Expense Entry:** Quick logging with automated categorization and transaction notes.
  - **Budget Management:** Set monthly spending limits with alerts and start-date tracking.
  - **Smart UPI Import:** Automatically parse transaction SMS/receipt screenshots securely on device.
  - **Robust Reports & Analytics:** Visualize your spending patterns with interactive charts and category breakdowns.
  - **100% Offline-First & Private:** Your financial data never leaves your device. Secured with AES-backed local Room database and optional PIN / App Lock.
  - **Backup & Restore:** Easily export and restore your encrypted financial ledger JSON files.

- **Category:** Finance
- **Tags / Keywords:** Expense tracker, budget planner, finance manager, UPI tracker, offline money manager, expense log.

---

## 2. Privacy Policy & Data Safety

- **Data Collection:** **None**. MoneyMap does not collect, transmit, or share any personal or financial data with external servers or third parties.
- **Data Storage:** 100% local device storage using Android Room (SQLite) database and encrypted preferences.
- **Third-Party SDKs:** None. Zero ad SDKs, zero analytics tracking, zero crash reporters transmitting identifiable data.
- **Account Deletion / Data Control:** Users retain full control. Data can be exported or completely wiped at any time directly from the in-app Settings.

---

## 3. Permissions & Manifest Audit

- `INTERNET`: Not required for core functionality (fully offline-first).
- `POST_NOTIFICATIONS`: Optional runtime permission (Android 13+) used exclusively for local budget alerts and reminders.
- **Media Access:** Zero-permission Android Photo Picker (`ActivityResultContracts.PickVisualMedia`) utilized for receipt attachments. Broad storage permissions (`READ_EXTERNAL_STORAGE`) are NOT requested.

---

## 4. Content Rating & Compliance

- **Content Rating:** PEGI 3 / Everyone. Contains no violence, sexual content, gambling, or age-restricted material.
- **Financial Functions Disclaimer:** MoneyMap is a personal record-keeping tool and does not provide financial advisory, banking services, loans, or money lending.

---

## 5. Build & Release Configuration

- **Target SDK:** 34+
- **Min SDK:** 26 (Android 8.0 Oreo)
- **ProGuard / R8:** Enabled with optimized rules (`proguard-rules.pro`).
- **Signing Config:** Production release signing configuration ready.
