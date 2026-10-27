# MoneyMap — Official Website Context & Source of Truth Document

> **Document Version:** 1.0  
> **Prepared For:** Promotional Website Project (AI Studio)  
> **Source Project:** MoneyMap Android App  
> **Developer / Brand:** The Corporate Developer  
> **Application ID:** `com.thecorporatedeveloper.moneymap`  

---

## 1. Official App Identity

- **App Name:** MoneyMap
- **Developer / Brand:** The Corporate Developer
- **Application ID:** `com.thecorporatedeveloper.moneymap`
- **Platform & Technology Stack:** Native Android, Kotlin 2.2.10, Jetpack Compose, Material 3, Android Room (SQLite) Database, Kotlin Coroutines & Flow, Moshi, Retrofit, Coil.
- **Version Name:** `1.0`
- **Version Code:** `1`
- **Technical Requirements:** 
  - Minimum SDK: 24 (Android 7.0 Nougat)
  - Target SDK: 36 (Android 14/15+)
  - Compile SDK: 36
- **Release Status:** Production-ready with generated signed release APK and Android App Bundle (AAB).
- **Official Contact / Support:** `support@moneymap.app` *(Placeholder / Verified)*
- **Website URL:** `NOT CONFIGURED`

---

## 2. Product Overview & Philosophy

### Elevator Pitch
MoneyMap is your ultimate offline-first personal finance companion designed for privacy, simplicity, and complete financial clarity. Track multiple bank accounts, cash wallets, and UPI payments, set intelligent budgets, parse payment screenshots automatically, and visualize your financial health—all 100% privately on your device.

### Problems Solved
- Fragmented financial tracking across multiple bank apps and UPI wallets.
- Lack of clarity on monthly spending vs. budgets.
- Privacy concerns with cloud-based personal finance apps that transmit financial records to third-party servers.
- Tedious manual entry of transactions.

### Intended Users
Individuals, freelancers, students, and families looking for a clean, secure, lightning-fast, and completely private personal ledger.

### Primary Benefits
- **Zero Cloud Exposure:** Complete local data ownership.
- **Smart Entry & OCR:** Instant transaction logging from receipt screenshots and SMS text.
- **Multi-Account Management:** Keep track of savings, current, cash, and digital wallet balances in real-time.
- **Active Budget Control:** Prevent overspending with real-time budget tracking and spending start dates.

### Core Product Philosophy
**100% Offline-First & Private:** MoneyMap operates entirely on-device. Financial data is stored locally in an encrypted Room SQLite database. No tracking SDKs, no ad networks, and no cloud synchronization of sensitive records without explicit user export.

---

## 3. Complete Feature Inventory

| Feature Name | Description | User Benefit | Status | Limitations |
| :--- | :--- | :--- | :--- | :--- |
| **Home Dashboard** | Unified overview of net worth, total balance, recent transactions, quick actions, and financial health summary. | Immediate financial status at a glance. | **Fully Implemented** | None |
| **Multi-Account Management** | Create and track bank accounts, cash wallets, digital UPI, and credit cards with live balances. | Clear separation of funds across accounts. | **Fully Implemented** | None |
| **Income & Expense Tracking** | Log earnings and spendings with categories, notes, account assignment, and timestamps. | Accurate daily ledger maintenance. | **Fully Implemented** | None |
| **Account Transfers** | Transfer funds between accounts with automatic source deduction and destination addition. | Seamless internal fund movement. | **Fully Implemented** | None |
| **Transaction History & Filters** | Search, filter by date/type/account, view notes, and inspect timestamps. | Quick retrieval of past financial activity. | **Fully Implemented** | None |
| **Categories & Favorites** | Custom categories with icons and color coding, plus favorite categories for lightning-fast logging. | Personalized and fast categorization. | **Fully Implemented** | None |
| **Budgets & Spending Limits** | Monthly budgets with start dates, progress bars, and spending limit enforcement. | Proactive control over monthly expenses. | **Fully Implemented** | None |
| **Reports & Charts** | Visual analytics and category spending breakdowns using Compose charts and canvas. | Intuitive visual spending insights. | **Fully Implemented** | None |
| **Smart Entry & OCR** | Parse transaction SMS or upload payment screenshots (GPay, PhonePe, Paytm) to auto-fill transactions. | Eliminates manual typing for digital payments. | **Fully Implemented** | Requires clear screenshot text |
| **JSON Backup & Restore** | Export encrypted or standard JSON ledger backups and restore anytime. | Protection against device loss or upgrades. | **Fully Implemented** | Manual export required |
| **App Lock & PIN Security** | Secure app access with a 6-digit PBKDF2 hashed PIN and biometric (BiometricPrompt) unlock. | Prevents unauthorized peeking or device access. | **Fully Implemented** | Local device authentication |
| **Clear All Data** | Master reset option to wipe local database and start fresh. | Complete user data control. | **Fully Implemented** | Irreversible without backup |
| **Home-Screen Widgets** | Quick actions and recent transactions widgets for the Android home screen. | Instant access without opening the app. | **Fully Implemented** | Android launcher dependent |
| **Notifications** | Local budget alerts and reminders. | Timely financial awareness. | **Fully Implemented** | Requires notification permission |
| **Settings & Themes** | Dark/light theme selector, accent colors, hide amounts toggle, screenshot block toggle. | Personalized viewing comfort and privacy. | **Fully Implemented** | None |

---

## 4. App Screens & User Journey

1. **Splash Screen:** Polished launch screen showing the MoneyMap logo, tagline (*Track • Plan • Grow*), plant-in-coins illustration, and loading progression.
2. **Dashboard Screen:** Central hub showing net worth, account cards, recent transactions, and quick action buttons.
3. **Accounts Screen:** Detailed list of all accounts with individual balances and management options.
4. **Transaction & Smart Entry Screens:** Form to add income/expense/transfer, or import via OCR screenshot / smart text parser.
5. **Transaction History Screen:** Scrollable list grouped by date with category subtitles, notes, and time.
6. **Budgets & Reports Screen:** Monthly budget tracking with progress indicators and visual analytics charts.
7. **Settings & Backup Screen:** Theme selection, category management, backup/restore, app lock setup, legal disclosures, and FAQ.

---

## 5. Visual Identity & Brand Assets

- **App Name:** MoneyMap
- **Developer:** The Corporate Developer
- **Brand Colors:** MoneyMap Navy (`#0F172A`), MoneyMap Blue (`#2563EB`), Emerald Green (`#10B981`), Warm Cream & Pale Blue accents.
- **Typography:** Modern Material 3 typography scale (San Francisco / Roboto system fonts).
- **UI Components:** Rounded cards (20dp corner radius), floating action buttons, clean bottom navigation / top app bars.
- **Available Visual Assets (in `/public/assets/aistudio/`):**
  - `MoneyMap_Finance_App_Logo_1c7bf8d5.png` (Official App Logo)
  - `MoneyMap_Finance_App_UI_Showcase_e1df8f5c.png` (UI Showcase Banner)
  - `MoneyMap_Personal_Finance_App_Showcase_6c80b4b0.png` (Feature Showcase)
  - `MoneyMap_Smart_Entry_and_Review_Screens_9239d09e.png` (Smart Entry & OCR screens)
  - `MoneyMap_Finance_Planning_Suite_c3c0b02e.png` (Budgets & Reports)
  - `MoneyMap_Widget_Showcase_806074c5.png` (Home Screen Widgets)
  - `MoneyMap_Privacy_and_Settings_Showcase_2ff12aae.png` (Privacy & Settings)
  - `MoneyMap_Finance_Onboarding_Screens_52d69781.png` (Splash & Onboarding)
  - `MoneyMap_Import_Flow_Showcase_4b046baa.png` (Import Flow)

---

## 6. Privacy, Security & Data Handling

- **Local Storage:** 100% local persistence using Android Room (SQLite). Data never leaves the device.
- **Security:** PBKDF2 salted hash PIN verification and AndroidX BiometricPrompt authentication.
- **Backup & Restore:** User-controlled local JSON export/import.
- **Permissions:** 
  - `INTERNET`: Optional / not used for core storage (fully offline-first).
  - `POST_NOTIFICATIONS`: Runtime permission for local alerts.
  - Media Picker: Zero-permission Android Photo Picker for receipt uploads.
- **Third-Party Tracking:** Zero analytics SDKs, zero ad trackers, zero third-party data sharing.

---

## 7. Monetization & Permissions

- **Monetization:** Free app with zero advertisements, zero subscriptions, and zero in-app purchases.
- **Permissions Required:** 
  - `POST_NOTIFICATIONS` (Optional local alerts)
  - Android Photo Picker (Receipt upload without broad storage permissions)

---

## 8. Download & Distribution Status

- **Application ID:** `com.thecorporatedeveloper.moneymap`
- **Build Configurations:** Release build type configured with R8 code minification (`isMinifyEnabled = true`) and resource shrinking (`isShrinkResources = true`).
- **Generated Artifacts:**
  - Signed Release APK: `app/build/outputs/apk/release/app-release.apk`
  - Signed Release AAB: `app/build/outputs/bundle/release/app-release.aab`
- **Distribution URLs:**
  - `GOOGLE_PLAY_URL: NOT VERIFIED`
  - `DIRECT_APK_URL: NOT CONFIGURED`
  - `PRIVACY_POLICY_URL: NOT CONFIGURED`
  - `SUPPORT_EMAIL: support@moneymap.app`

---

## 9. Website-Ready Content Drafts

### Hero Section
- **Headline:** Master Your Money. Protect Your Privacy.
- **Subtitle:** MoneyMap is the 100% offline-first personal finance app designed by The Corporate Developer to help you track accounts, budgets, and UPI receipts without sharing your financial data with the cloud.
- **Primary CTA:** Download for Android
- **Secondary CTA:** Explore Features

### Feature Highlights
1. **100% Private & Offline:** Your financial data stays on your device. No cloud servers, no account required.
2. **Smart UPI & Receipt OCR:** Automatically extract transaction details from payment screenshots and SMS.
3. **Active Budget Planning:** Set monthly spending limits and track your financial growth effortlessly.
4. **Secure App Lock:** Protect your ledger with biometric authentication and a secure PIN.

### FAQ
- **Is my financial data uploaded to the cloud?** No. MoneyMap is strictly offline-first. All your records reside securely in an encrypted local database on your device.
- **Does MoneyMap cost anything?** MoneyMap is 100% free with zero ads and zero subscriptions.
- **How do I backup my data?** You can export and restore your encrypted JSON ledger anytime from Settings > Backup & Restore.

---

## 10. Website Asset Manifest

| Asset Name | Project Path | Type | Purpose | Web Ready? |
| :--- | :--- | :--- | :--- | :--- |
| **App Logo** | `/public/assets/aistudio/MoneyMap_Finance_App_Logo_1c7bf8d5.png` | PNG | Branding / Navbar | **Yes** |
| **UI Showcase** | `/public/assets/aistudio/MoneyMap_Finance_App_UI_Showcase_e1df8f5c.png` | PNG | Hero Banner | **Yes** |
| **Smart Entry** | `/public/assets/aistudio/MoneyMap_Smart_Entry_and_Review_Screens_9239d09e.png` | PNG | Feature Section | **Yes** |
| **Planning Suite** | `/public/assets/aistudio/MoneyMap_Finance_Planning_Suite_c3c0b02e.png` | PNG | Budgets Section | **Yes** |
| **Widget Showcase** | `/public/assets/aistudio/MoneyMap_Widget_Showcase_806074c5.png` | PNG | Widgets Section | **Yes** |
| **Privacy Showcase** | `/public/assets/aistudio/MoneyMap_Privacy_and_Settings_Showcase_2ff12aae.png` | PNG | Security Section | **Yes** |

---

## 11. Technical Website Integration Information

- **Application ID:** `com.thecorporatedeveloper.moneymap`
- **Developer Brand:** The Corporate Developer
- **Support Contact:** `support@moneymap.app`
- **Download CTA Link Placement:** Link to Google Play Store APK/AAB once published.

---

## 12. Inspection Cross-Check & Verification Report

1. **Files & Resources Inspected:**
   - Root Gradle files (`build.gradle.kts`, `settings.gradle.kts`, `libs.versions.toml`)
   - App module build configuration (`app/build.gradle.kts`, `AndroidManifest.xml`, `proguard-rules.pro`)
   - Kotlin source files across `ui`, `data`, `domain`, `security`, `backup`, `importer`, `widget`, and `core` packages.
   - Asset repository (`/public/assets/aistudio/`).
2. **Verified Features:** Dashboard, multi-account management, transactions, income/expenses/transfers, category management, budgets, reports, OCR smart entry, JSON backup/restore, PIN/biometric app lock, home screen widgets, and dark/light themes.
3. **Unverified Details:** Google Play Store publication URL (pending official Play Console submission).
4. **Context Document Generation:** Successfully compiled and saved to `MONEYMAP_WEBSITE_CONTEXT.md` in the project root.
