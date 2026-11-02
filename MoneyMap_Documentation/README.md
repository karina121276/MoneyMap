# MoneyMap — Official Buyer Documentation & Guide

Welcome to **MoneyMap**! MoneyMap is a premium, production-ready, offline-first personal finance and expense management application for Android built entirely with **Kotlin** and **Jetpack Compose (Material Design 3)**.

This package contains full Android Studio source code, assets, and documentation for running, customizing, reskinning, and publishing MoneyMap to the Google Play Store or other Android app marketplaces.

---

## 📌 Table of Contents

1. [Product Overview](#-product-overview)
2. [Key Features](#-key-features)
3. [Technology Stack](#-technology-stack)
4. [Software Prerequisites](#-software-prerequisites)
5. [Quick Start: Android Studio Setup](#-quick-start-android-studio-setup)
6. [Building and Running the App](#-building-and-running-the-app)
7. [Generating Release APK and Android App Bundle (AAB)](#-generating-release-apk-and-android-app-bundle-aab)
8. [Documentation Package Overview](#-documentation-package-overview)
9. [Accounting and Data Invariants](#-accounting-and-data-invariants)
10. [Support and Developer Information](#-support-and-developer-information)

---

## 📱 Product Overview

MoneyMap is designed for privacy-conscious users who want complete autonomy over their financial records without relying on third-party cloud servers, bank scraping, or mandatory user accounts. All financial data is stored locally in an encrypted Room SQLite database on the user's device.

- **Application Name:** MoneyMap
- **Package ID (Namespace & ApplicationId):** `com.thecorporatedeveloper.moneymap`
- **Current Version:** `1.0.3` (Version Code: `3`)
- **Target Platform:** Android 7.0 (API 24) to Android 16 (API 36)
- **Monetization / Ad Status:** Zero ads included (Clean source code, ready for AdMob/Subscription integration if desired)
- **Data Sovereignty:** 100% offline-first with robust JSON and ZIP backup/restore

---

## ✨ Key Features

1. **Executive Dashboard (Home)**
   - Net Tracked Balance with one-tap eye privacy visibility toggle (`••••••`).
   - Monthly Income, Expense, and Net Cash Flow metric cards.
   - Monthly Spending Goal progress gauge with real-time percentage indicators.
   - Recent transaction feed with category icons and quick navigation.

2. **Multi-Account Management**
   - Unlimited Bank accounts, Cash wallets, Digital wallets, and Credit Cards.
   - Real-world liability handling: Credit card balances are treated as liabilities rather than asset balances.
   - Dedicated Savings Account classification with balance calculation options.
   - Masked account numbers, IFSC codes, bank names, and custom account color accents.

3. **Transaction Tracking & Accounting Rules**
   - Income, Expense, and Account-to-Account Transfers.
   - Integer paise storage (`Long`) prevents any floating-point arithmetic rounding errors (1 INR/USD = 100 sub-units).
   - Fast numeric keypad with configurable Quick Amount buttons (default: 100, 500, 1000, 2000, 5000).
   - Atomic transfers that debit source and credit destination accounts simultaneously without distorting income/expense metrics.

4. **Smart Entry & Local OCR (Screenshot & Receipt Importer)**
   - Integrated with Google ML Kit Text Recognition running entirely on-device (zero network traffic).
   - One-tap parsing for UPI payment screenshots (Google Pay, PhonePe, Paytm, CRED, BHIM, Amazon Pay).
   - Automatic extraction of amount, date, reference ID (UTR), payee name, and smart category suggestions.
   - Intelligent duplicate detection engine (warning if reference ID or amount/time window already exists).

5. **Multi-Receipt Attachments & Camera Integration**
   - Attach multiple receipts per transaction using the Android Photo Picker or camera.
   - Secure local file storage in app-private directories (`filesDir/receipts/<txId>/`).
   - On-device image enhancements: 90-degree rotations, thumbnail generation, and full-screen zoomable viewer.

6. **Interactive Reports & Spending Analytics**
   - Time period filtering: Daily, Weekly, Monthly, and Yearly.
   - Visual Donut chart showing category percentage breakdowns.
   - Interactive category list showing spending trends, transaction counts, and average transaction size.
   - 7-day Calendar View with daily activity dots and daily cash flow summaries.

7. **Monthly Budgeting & Limits**
   - Total monthly spending limits and category-specific budget allocations.
   - Color-coded progress indicators (Safe green, Warning orange, Over-budget red).
   - Calculation of remaining spending budget and daily recommended spending velocity.

8. **Home-Screen Widgets (App Widgets)**
   - **Quick Actions Widget (4x1):** Instant shortcuts to Add Expense, Add Income, Transfer, and Camera Smart Entry.
   - **Recent Transactions Widget (4x2):** Displays live balance, monthly totals, and the latest 3 transactions with one-tap balance concealment.

9. **Security, App Lock & Privacy**
   - 6-digit App Lock PIN with cryptographic PBKDF2WithHmacSHA256 salted hashing.
   - Biometric authentication (Fingerprint, Face Unlock) via AndroidX BiometricPrompt.
   - In-app screenshot and task switcher preview blocking (`FLAG_SECURE`).
   - Configurable auto-lock timeouts (Immediately, 1 min, 5 min, 15 min).

10. **Full Backup & Restore (JSON & ZIP Packages)**
    - Export complete database to standard JSON or ZIP (including receipt images).
    - Dual Restore Strategy: **Merge** (safely merges backup records with existing data via dynamic foreign-key ID mapping) or **Replace** (clean overwrite with confirmation).
    - CSV transaction export compatible with Microsoft Excel and Google Sheets.

11. **Multi-Currency Support**
    - Built-in repository of 39 world currencies (USD, INR, EUR, GBP, JPY, AED, CAD, AUD, etc.) with custom symbols and grouping formats.

---

## 🛠 Technology Stack

| Layer | Component | Version / Library |
|---|---|---|
| **Language** | Kotlin | `2.2.10` |
| **Build Tool** | Android Gradle Plugin (AGP) | `9.1.1` (Gradle 8.11+) |
| **Compiler Plugin** | Kotlin Compose Compiler | `2.2.10` |
| **Annotation Processing** | KSP (Kotlin Symbol Processing) | `2.3.5` |
| **UI Framework** | Jetpack Compose (Material 3) | Compose BOM `2024.09.00` |
| **Architecture** | MVVM + Repository Pattern | Clean Modular Kotlin |
| **Local Database** | Room (SQLite) | `2.7.0` (Version 13 Schema) |
| **Key-Value Storage** | Jetpack DataStore Preferences | `1.1.7` |
| **Async & Streaming** | Kotlin Coroutines & Flow | `1.10.2` |
| **Image Loading** | Coil Compose | `2.7.0` |
| **OCR & Vision** | Google Play Services ML Kit | `19.0.1` (Text Recognition Latin) |
| **Security & Biometrics** | AndroidX Biometric | `1.1.0` |
| **JSON Serialization** | Moshi & Kotlin Reflection | `1.15.2` |
| **Compatibility** | Min SDK `24` / Target SDK `36` | Android 7.0 to Android 16 |

---

## 💻 Software Prerequisites

Before working with the project, ensure your computer has the following tools installed:

1. **Android Studio:**
   - Recommended: **Android Studio Ladybug (2024.2.1+)**, Koala, or Iguana.
   - Download from the [Official Android Developer Portal](https://developer.android.com/studio).
2. **Java Development Kit (JDK):**
   - JDK 17 or JDK 21 (bundled automatically inside modern Android Studio installations as JetBrains Runtime `jbr`).
3. **Android SDK Platform:**
   - Android SDK Platform 36 (Android 16 API level 36).
   - Android SDK Build-Tools 35.x or 36.x.
4. **Hardware Device or Emulator:**
   - Physical Android device running Android 7.0+ with USB Debugging enabled, OR
   - Android Virtual Device (AVD) running Pixel with API 34, 35, or 36.

---

## 🚀 Quick Start: Android Studio Setup

1. **Extract the Project:**
   Extract your purchased MoneyMap source ZIP to a local folder (e.g., `C:\Projects\MoneyMap` on Windows or `~/Projects/MoneyMap` on macOS/Linux).
   > **Note for Windows Users:** Keep the folder path short to avoid Windows 260-character maximum path limitations.

2. **Open in Android Studio:**
   - Launch Android Studio.
   - Choose **File > Open...**
   - Navigate to and select the root directory containing `settings.gradle.kts` and `build.gradle.kts`. Click **OK**.

3. **Wait for Gradle Sync:**
   - Android Studio will automatically resolve dependencies via Google Maven, MavenCentral, and the Foojay toolchain resolver.
   - If prompted by Android Studio to install missing SDK components (such as Android SDK 36), click **Install missing platform(s) and sync project**.

---

## 🏃 Building and Running the App

### Running on a Device or Emulator:
1. Connect your Android device via USB (ensure **USB Debugging** is turned on) or start an Android Emulator from the Device Manager.
2. Ensure the run configuration dropdown in the top toolbar shows **`app`**.
3. Click the green **Run (Play)** button or press `Shift + F10`.

### Building a Debug APK:
1. In Android Studio, go to the top menu: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
2. Once the build finishes, click the blue **locate** link in the bottom-right notification pop-up.
3. Your compiled debug APK is located at:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 📦 Generating Release APK and Android App Bundle (AAB)

When preparing to publish to the Google Play Store or distribute a production build:

1. In Android Studio, select: **Build > Generate Signed Bundle / APK...**
2. Choose **Android App Bundle** (required by Google Play) or **APK** (for direct distribution).
3. Select your existing release keystore or click **Create new...** to create a fresh `.jks` keystore.
4. Enter your keystore password, key alias, and key password.
5. Select the **release** build variant and click **Finish**.
6. The resulting output files will be created at:
   - **Signed AAB:** `app/build/outputs/bundle/release/app-release.aab`
   - **Signed APK:** `app/build/outputs/apk/release/app-release.apk`

*(For detailed, step-by-step release signing and keystore generation instructions, refer to `RELEASE_SIGNING_GUIDE.md`.)*

---

## 📚 Documentation Package Overview

This documentation folder contains dedicated guides for every stage of development, customization, and publishing:

| Document | Purpose |
|---|---|
| **`README.md`** | High-level product overview, features, specifications, and quick start. |
| **`INSTALLATION_GUIDE.md`** | Beginner-friendly setup instructions, JDK/SDK requirements, and running the app. |
| **`CONFIGURATION_GUIDE.md`** | Changing package ID, app name, launcher icons, splash screen, and preferences. |
| **`RESKINNING_AND_CUSTOMIZATION.md`** | Detailed rebranding tutorial: color palettes, typography, UI screens, and themes. |
| **`RELEASE_SIGNING_GUIDE.md`** | Generating custom production keystores, signed APKs/AABs, and Google Play release steps. |
| **`TROUBLESHOOTING.md`** | Solutions to common build errors, SDK mismatches, Room schemas, and Gradle issues. |
| **`PROJECT_STRUCTURE.md`** | Complete directory tour, architectural layers, data flows, and source explanations. |
| **`THIRD_PARTY_DEPENDENCIES_AND_LICENSES.md`** | Comprehensive list of dependencies, versions, licenses, and official URLs. |

---

## 🧮 Accounting and Data Invariants

MoneyMap is designed with strict mathematical and financial invariants:

1. **Integer Representation:** All currency amounts are stored as integer paise/cents (`Long`), preventing precision degradation from IEEE-754 floating-point operations.
2. **Transfer Isolation:** Account transfers debit one account and credit another in an atomic SQLite transaction. Transfers are strictly excluded from income and expense summaries.
3. **Credit Card Liabilities:** Credit card accounts are designated as liabilities; positive spending increases debt while payments reduce debt.
4. **Calculated Balances:** Account balances on the dashboard and accounts screen are derived directly from opening balances combined with verified transaction records.
5. **Transactional Integrity:** Restores and batch operations run inside `database.withTransaction` to guarantee zero half-written state if interrupted.

---

## 🤝 Support and Developer Information

- **Developer / Author:** The Corporate Developer
- **Support Email:** `thecorporatedeveloper@gmail.com`
- **YouTube Channel:** [The Corporate Developer on YouTube](https://youtube.com/@thecorporatedeveloper-yt?si=C8TMVT9e4umO6Af3)
- **Legal Terms & Privacy:** Full privacy policy template included in `MONEYMAP_PRIVACY_AND_DATA.md`.

*Thank you for purchasing MoneyMap! Follow the companion guides in this documentation folder to customize and launch your own personal finance brand.*
