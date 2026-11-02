# MoneyMap — Complete Project Architecture & Directory Structure

This document provides a comprehensive structural breakdown of the **MoneyMap** codebase. It maps out the Model-View-ViewModel (MVVM) architecture, Clean Architecture layers, database configurations, and Jetpack Compose component hierarchy.

---

## 📋 Table of Contents

1. [High-Level Architecture Overview](#1-high-level-architecture-overview)
2. [Data Flow & Unidirectional Data Flow (UDF)](#2-data-flow--unidirectional-data-flow-udf)
3. [Source Code Directory Tree](#3-source-code-directory-tree)
4. [Package by Package Tour](#4-package-by-package-tour)
   - [Application Root & Entry Points](#application-root--entry-points)
   - [`core/` Package (Models & Utilities)](#core-package)
   - [`data/` Package (Room, DAOs & DataStore)](#data-package)
   - [`domain/` Package (Business Logic)](#domain-package)
   - [`importer/` Package (ML Kit OCR & Smart Entry)](#importer-package)
   - [`receipt/` Package (Image Storage & Files)](#receipt-package)
   - [`security/` Package (PIN Hashing & Biometrics)](#security-package)
   - [`backup/` Package (JSON & ZIP Merging)](#backup-package)
   - [`widget/` Package (Android Home-Screen Widgets)](#widget-package)
   - [`ui/` Package (Jetpack Compose Screens & Theme)](#ui-package)
5. [Resource & Asset Files (`res/`)](#5-resource--asset-files-res)

---

## 1. High-Level Architecture Overview

MoneyMap strictly adheres to official Android architecture recommendations:
- **Presentation Layer:** Jetpack Compose (Declarative UI) with Material 3 styling.
- **State Management:** Reactive Kotlin `StateFlow` and Compose `collectAsStateWithLifecycle`.
- **Domain Layer:** Pure Kotlin accounting rules (`FinancialCalculator`), isolated from UI and storage.
- **Data Layer:** Single source of truth powered by `MoneyMapRepository`, `MoneyMapDatabase` (Room), and `AppPreferences` (Jetpack DataStore).

```
┌────────────────────────────────────────────────────────┐
│               Jetpack Compose UI Screens               │
│   (Dashboard, Accounts, Transactions, Budgets, etc.)   │
└───────────────────────────▲────────────────────────────┘
                            │ State / Flow
┌───────────────────────────┴────────────────────────────┐
│                    MoneyMapRepository                  │
│       Coordinates Room Database, Preferences, Widgets  │
└─────────────▲──────────────────────────▲───────────────┘
              │                          │
┌─────────────┴────────────┐  ┌──────────┴───────────────┐
│     MoneyMapDatabase     │  │      AppPreferences      │
│  (Room SQLite, Version 13)│  │   (Jetpack DataStore)   │
└──────────────────────────┘  └──────────────────────────┘
```

---

## 2. Data Flow & Unidirectional Data Flow (UDF)

1. **User Action:** The user taps a button or interacts with the UI (e.g., creating a transaction).
2. **Repository Call:** The Compose event calls a suspend function on `MoneyMapRepository` (e.g. `insertTransaction(tx)`).
3. **Validation & Calculation:** `FinancialCalculator` verifies account balances and ensures sufficient funds (if expense or transfer).
4. **Database Transaction:** Room executes the query atomically within `database.withTransaction`.
5. **Widget & Observer Notification:** A broadcast is dispatched to update Android home-screen widgets, and the active `Flow<List<TransactionEntity>>` automatically emits the new list to Compose screens.

---

## 3. Source Code Directory Tree

```
MoneyMap/
├── app/
│   ├── build.gradle.kts                   <-- Module-level build & dependency configuration
│   ├── proguard-rules.pro                 <-- R8 Proguard rules
│   └── src/
│       ├── main/
│           ├── AndroidManifest.xml        <-- Android Manifest (Permissions, Activities, Widgets)
│           ├── java/com/thecorporatedeveloper/moneymap/
│           │   ├── MainActivity.kt               <-- Main Activity & Navigation Graph
│           │   ├── MoneyMapApplication.kt        <-- Application class
│           │   │
│           │   ├── backup/
│           │   │   └── BackupManager.kt          <-- JSON & ZIP backup, export, and merge engine
│           │   │
│           │   ├── core/
│           │   │   ├── model/
│           │   │   │   ├── FinancialModels.kt    <-- Enums (AccountType, TransactionType), CurrencyFormatter
│           │   │   │   └── CurrencyRepository.kt <-- 39 World Currency definitions
│           │   │   └── util/
│           │   │       └── StorageSafety.kt      <-- Disk space and SQLite exception protection
│           │   │
│           │   ├── data/
│           │   │   ├── dao/
│           │   │   │   └── MoneyMapDaos.kt       <-- Room DAOs (Account, Category, Transaction, Budget, Receipt)
│           │   │   ├── database/
│           │   │   │   └── MoneyMapDatabase.kt   <-- Room database configuration & migrations 1 to 13
│           │   │   ├── entity/
│           │   │   │   └── Entities.kt           <-- Room SQLite Entities
│           │   │   ├── preferences/
│           │   │   │   └── AppPreferences.kt     <-- Jetpack DataStore preferences & UserPreferences state
│           │   │   └── repository/
│           │   │       └── MoneyMapRepository.kt <-- Single source of truth repository
│           │   │
│           │   ├── domain/
│           │   │   └── FinancialCalculator.kt    <-- Mathematical invariants, balances, savings, budgets
│           │   │
│           │   ├── importer/
│           │   │   ├── OcrProcessor.kt           <-- Google ML Kit on-device Text Recognition
│           │   │   ├── SmartEntryParser.kt       <-- UPI screenshot & regex parsing engine
│           │   │   └── PdfStatementParser.kt     <-- PDF bank statement table parser
│           │   │
│           │   ├── receipt/
│           │   │   └── ReceiptStorageManager.kt  <-- Image saving, cropping, thumbnail generation, rotation
│           │   │
│           │   ├── security/
│           │   │   ├── PinSecurityManager.kt     <-- PBKDF2WithHmacSHA256 salted PIN verifier
│           │   │   └── BiometricAuthHelper.kt    <-- AndroidX BiometricPrompt helper
│           │   │
│           │   ├── ui/
│           │   │   ├── components/
│           │   │   │   ├── MoneyMapComponents.kt     <-- Shared UI cards, chips, buttons, charts
│           │   │   │   └── CharactersAndAnimations.kt<-- Animated state transitions
│           │   │   ├── screens/
│           │   │   │   ├── AccountsScreens.kt        <-- Account management & details
│           │   │   │   ├── AppLockScreens.kt         <-- PIN setup, unlock, and change PIN screens
│           │   │   │   ├── CategoryScreen.kt         <-- Category management, icon & color picker
│           │   │   │   ├── CurrencySelectionScreen.kt<-- Currency picker with live search
│           │   │   │   ├── DashboardScreen.kt        <-- Executive home dashboard
│           │   │   │   ├── FavoritesScreen.kt        <-- Favorite transactions & templates
│           │   │   │   ├── ReceiptScreens.kt         <-- Receipt viewer, rotator, and gallery
│           │   │   │   ├── ReportsAndBudgetsScreens.kt<-- Reports, Donut charts & Budgets
│           │   │   │   ├── SettingsAndBackupScreens.kt<-- Settings, Backup & Restore, About
│           │   │   │   ├── SmartImportScreens.kt     <-- Screenshot OCR review & import review
│           │   │   │   ├── SplashScreen.kt           <-- Launch branding screen
│           │   │   │   └── TransactionScreens.kt     <-- Add, edit, and history of transactions
│           │   │   └── theme/
│           │   │       ├── Color.kt                  <-- Brand colors (Navy, Blue, Green, Red)
│           │   │       ├── Theme.kt                  <-- Light & Dark Material 3 theme definitions
│           │   │       └── Type.kt                   <-- Plus Jakarta Sans typography
│           │   │
│           │   └── widget/
│           │       ├── QuickActionsWidgetProvider.kt        <-- 4x1 Quick Actions Widget
│           │       ├── RecentTransactionsWidgetProvider.kt  <-- 4x2 Recent Transactions Widget
│           │       └── QuickAddActivity.kt                  <-- Fast 1-tap transaction dialog from widget
│           │
│           └── res/
│               ├── drawable/              <-- PNG logos, widget drawables, vector icons
│               ├── font/                  <-- `plus_jakarta_sans.ttf` font file
│               ├── layout/                <-- RemoteViews XML layouts for home-screen widgets
│               ├── mipmap-anydpi-v26/     <-- Adaptive launcher icons (vector XML)
│               ├── values/                <-- `strings.xml`, `colors.xml`, `themes.xml`
│               └── xml/                   <-- `file_paths.xml`, `backup_rules.xml`, widget info
│
├── gradle/
│   ├── libs.versions.toml                 <-- Central Version Catalog (All dependencies & versions)
│   └── wrapper/                           <-- Gradle wrapper files
├── build.gradle.kts                       <-- Root project Gradle build script
├── settings.gradle.kts                    <-- Root settings (Project name, repositories, plugins)
└── gradle.properties                      <-- JVM memory and build configuration
```

---

## 4. Package by Package Tour

### Application Root & Entry Points
- **`MainActivity.kt`:**
  - Configures `enableEdgeToEdge()`.
  - Initializes repository, preferences, and security managers.
  - Controls Android Navigation (`NavHost`) with 26 type-safe routes (`Screen.Home`, `Screen.Accounts`, `Screen.Add`, `Screen.Reports`, etc.).
  - Handles incoming Share Intents (`Intent.ACTION_SEND` and `ACTION_SEND_MULTIPLE`) for receiving shared UPI screenshots.
  - Enforces `FLAG_SECURE` whenever App Lock or Screenshot Blocking is turned on.
- **`MoneyMapApplication.kt`:**
  - Base `Application` class managing process lifecycle.

### `core/` Package
- **`core/model/FinancialModels.kt`:**
  - Defines `AccountType` (`BANK`, `CASH`, `WALLET`, `CREDIT_CARD`, `SAVINGS`).
  - Defines `TransactionType` (`EXPENSE`, `INCOME`, `TRANSFER`).
  - `CurrencyFormatter`: High-performance utility for converting integer paise (`Long`) to formatted currency strings with Indian grouping (`1,23,456.00`) or standard international grouping (`123,456.00`).
- **`core/model/CurrencyRepository.kt`:**
  - Catalog of 39 world currencies with flags, symbols, and ISO codes.

### `data/` Package
- **`data/entity/Entities.kt`:**
  - `AccountEntity`: Primary accounts table with balances and liability flags.
  - `CategoryEntity`: Spending and income categories with icons and colors.
  - `TransactionEntity`: Ledger records referencing accounts and categories with foreign key constraints.
  - `BudgetEntity`: Monthly spending limit definitions.
  - `ReceiptEntity`: Receipt file attachments linked to transactions with cascade delete.
  - `ImportBatchEntity`: Audit trail of batch imports.
- **`data/database/MoneyMapDatabase.kt`:**
  - The SQLite database instance (Version 13).
  - Includes automated Room migrations (`MIGRATION_1_2` through `MIGRATION_12_13`).
- **`data/dao/MoneyMapDaos.kt`:**
  - Compiled SQLite query interfaces with Kotlin Flow reactive queries.
- **`data/preferences/AppPreferences.kt`:**
  - Jetpack DataStore layer for user settings (Theme mode, currency, PIN lock state, quick amount buttons).
- **`data/repository/MoneyMapRepository.kt`:**
  - Coordinates all data mutations and transactional operations.

### `domain/` Package
- **`domain/FinancialCalculator.kt`:**
  - Pure Kotlin financial computation engine:
    - Net tracked balance (excluding liabilities).
    - Monthly period summaries (Income, Expense, Net cash flow).
    - Savings rate percentage.
    - Category breakdown percentages.
    - Budget utilization and approaching-limit thresholds.

### `importer/` Package
- **`importer/OcrProcessor.kt`:**
  - Integrates Google ML Kit Latin Text Recognition.
  - Safely copies shared content URIs into cache and extracts raw text locally without network calls.
- **`importer/SmartEntryParser.kt`:**
  - Advanced regular expression engine for UPI apps (Google Pay, PhonePe, Paytm, CRED).
  - Extracts amounts, dates, payee names, UTR reference IDs, and matches categories.
- **`importer/PdfStatementParser.kt`:**
  - Parses bank statement table data.

### `receipt/` Package
- **`receipt/ReceiptStorageManager.kt`:**
  - Handles image persistence to `context.filesDir/receipts/<txId>/`.
  - Generates compressed JPEG thumbnails to keep list scrolling silky smooth.
  - Supports 90° clockwise and counter-clockwise image rotation.

### `security/` Package
- **`security/PinSecurityManager.kt`:**
  - PBKDF2WithHmacSHA256 salted hash generator (10,000 iterations).
  - Constant-time equals comparison preventing timing attack vulnerabilities.
- **`security/BiometricAuthHelper.kt`:**
  - AndroidX BiometricPrompt wrapper supporting fingerprint and face unlock.

### `backup/` Package
- **`backup/BackupManager.kt`:**
  - Generates standard JSON backups and full ZIP archives containing both `data.json` and all attached receipt image files.
  - Dual restore engine: **Merge** (uses dynamic ID mapping so backup records are merged with current records without collision) or **Replace**.

### `widget/` Package
- **`widget/QuickActionsWidgetProvider.kt` & `RecentTransactionsWidgetProvider.kt`:**
  - AppWidgetProviders updating home-screen widgets via `RemoteViews`.
- **`widget/QuickAddActivity.kt`:**
  - Lightweight transparent activity launched from widgets to add transactions in 1 tap without opening the full application.

### `ui/` Package
- **`ui/screens/`:**
  - All 12 Compose screens implementing Material 3 cards, lazy lists, and dialogs.
- **`ui/theme/`:**
  - Central `Color.kt`, `Theme.kt`, and `Type.kt`.

---

*Next Step: To inspect third-party libraries, dependency versions, and licenses, proceed to **`THIRD_PARTY_DEPENDENCIES_AND_LICENSES.md`**.*
