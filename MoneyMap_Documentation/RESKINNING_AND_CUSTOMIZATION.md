# MoneyMap — Reskinning & Customization Tutorial

This step-by-step tutorial guides buyers who want to thoroughly rebrand and redesign **MoneyMap** for their own business or client. Every instruction references exact file paths and source code lines verified in the project.

---

## 📋 Table of Contents

1. [Replacing the App Logo](#1-replacing-the-app-logo)
2. [Replacing Launcher & Adaptive Icons](#2-replacing-launcher--adaptive-icons)
3. [Changing Application Name & App Strings](#3-changing-application-name--app-strings)
4. [Customizing the Color Palette](#4-customizing-the-color-palette)
5. [Customizing Typography & Font Weights](#5-customizing-typography--font-weights)
6. [Customizing the Executive Dashboard](#6-customizing-the-executive-dashboard)
7. [Customizing Transaction Screens & Keypad](#7-customizing-transaction-screens--keypad)
8. [Customizing Account Management Screens](#8-customizing-account-management-screens)
9. [Customizing Budget & Spending Limit Screens](#9-customizing-budget--spending-limit-screens)
10. [Customizing Reports & Donut Charts](#10-customizing-reports--donut-charts)
11. [Customizing Predefined Categories](#11-customizing-predefined-categories)
12. [Modifying Bottom Navigation Bar](#12-modifying-bottom-navigation-bar)
13. [Customizing Splash Screen Animations](#13-customizing-splash-screen-animations)
14. [Customizing Android Home-Screen Widgets](#14-customizing-android-home-screen-widgets)
15. [Building, Testing, and Verifying the Reskin](#15-building-testing-and-verifying-the-reskin)

---

## 1. Replacing the App Logo

MoneyMap uses a transparent logo graphic displayed on the splash screen and about dialogs.

- **Exact File Path:** `app/src/main/res/drawable/moneymap_logo.png`
- **What it controls:** The primary visual logo shown during startup in `SplashScreen.kt`.
- **How to change:**
  1. Prepare your logo in PNG or WebP format with a transparent background.
  2. Recommended resolution: **512 x 512 pixels**.
  3. Overwrite `app/src/main/res/drawable/moneymap_logo.png` with your new file (keep the same filename to avoid missing resource errors).
- **Warning:** Do not use capital letters or spaces in Android drawable filenames.

---

## 2. Replacing Launcher & Adaptive Icons

Android 8.0+ devices use adaptive icons composed of two layers (foreground and background), while older devices use density-specific PNGs.

### File Paths:
- `app/src/main/res/drawable/ic_launcher_background.xml` (Vector background layer)
- `app/src/main/res/drawable/ic_launcher_foreground.xml` (Vector foreground logo layer)
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` (Square adaptive icon definition)
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` (Circular adaptive icon definition)
- Legacy density folders: `app/src/main/res/mipmap-mdpi/`, `mipmap-hdpi/`, `mipmap-xhdpi/`, `mipmap-xxhdpi/`, `mipmap-xxxhdpi/`

### Changing via Android Asset Studio:
1. In Android Studio's Project view, right-click `app/src/main/res`.
2. Select **New > Image Asset**.
3. Set **Icon Type:** `Launcher Icons (Adaptive and Legacy)`.
4. In the **Foreground Layer** tab:
   - Select **Source Asset:** `Image`.
   - Browse to your transparent logo PNG or vector SVG.
   - Adjust the **Resize** slider until the logo is safely inside the inner circular guideline.
5. In the **Background Layer** tab:
   - Select **Source Asset:** `Color`.
   - Enter your hex color (e.g. `#0C2B8A` or `#1E56F0`).
6. Click **Next** and then **Finish**.

---

## 3. Changing Application Name & App Strings

All user-visible labels, titles, and button texts are stored in Android string resources.

- **Exact File Path:** `app/src/main/res/values/strings.xml`
- **Key Strings to Update:**

```xml
<!-- Line 2: App Name under launcher icon and in top bar -->
<string name="app_name">MyFinance</string>

<!-- Line 3: Tagline on Splash screen -->
<string name="tagline">Save Smart • Live Well</string>

<!-- Line 97: About dialog description -->
<string name="about_moneymap_desc">Offline Personal Finance • Track • Plan • Grow</string>

<!-- Line 67: Smart Entry privacy badge -->
<string name="local_processing_badge">100% Private &amp; Offline</string>
```

- **Warning:** In XML, special characters like `&` must be escaped as `&amp;`, and single quotes `'` must be escaped as `\'`.

---

## 4. Customizing the Color Palette

MoneyMap features a refined, modern financial color scheme inspired by premium iOS and Material 3 interfaces.

- **Exact File Path:** `app/src/main/java/com/thecorporatedeveloper/moneymap/ui/theme/Color.kt`
- **What it controls:** Central color variables referenced throughout all Compose components.

### Example: Switching from Blue to Modern Emerald & Dark Forest Theme
Replace the color constants in `Color.kt`:

```kotlin
// Before (Blue Theme):
val MoneyMapNavy = Color(0xFF0C2B8A)
val MoneyMapBlue = Color(0xFF1E56F0)
val MoneyMapBlueLight = Color(0xFFEFF6FF)

// After (Emerald Green Brand):
val MoneyMapNavy = Color(0xFF064E3B)       // Deep Forest Green
val MoneyMapBlue = Color(0xFF059669)       // Vibrant Emerald Accent
val MoneyMapBlueLight = Color(0xFFECFDF5)  // Pale Mint Background Tint
```

- **Theme Configuration:**
  - **File:** `app/src/main/java/com/thecorporatedeveloper/moneymap/ui/theme/Theme.kt`
  - Inspect `LightColorScheme` (lines 26–42) and `DarkColorScheme` (lines 8–24) to adjust container colors, surfaces, and text contrast.

---

## 5. Customizing Typography & Font Weights

MoneyMap uses the sleek, geometric sans-serif font **Plus Jakarta Sans**.

- **Font File:** `app/src/main/res/font/plus_jakarta_sans.ttf`
- **Typography Definitions:** `app/src/main/java/com/thecorporatedeveloper/moneymap/ui/theme/Type.kt`

### Steps to Change Font:
1. Download any open-source TTF font (e.g. *Inter*, *Outfit*, *Urbanist*, or *Roboto*).
2. Save the font file as `app/src/main/res/font/custom_font.ttf`.
3. In `Type.kt`, update lines 11–13:
   ```kotlin
   val CustomFontFamily = FontFamily(
       Font(R.font.custom_font, FontWeight.Normal)
   )
   ```
4. Replace `PlusJakartaSans` with `CustomFontFamily` across the `MoneyMapTypography` styles (displayLarge, headlineMedium, titleLarge, bodyLarge, etc.).

---

## 6. Customizing the Executive Dashboard

- **Exact File Path:** `app/src/main/java/com/thecorporatedeveloper/moneymap/ui/screens/DashboardScreen.kt`
- **What it controls:** The primary screen users see after launching MoneyMap.

### Key Sections:
1. **Total Balance Card (`ExecutiveBalanceCard`):**
   - Displays the net tracked wealth with eye visibility toggle.
   - Background gradient is configured with `Brush.linearGradient(listOf(currentAccent, MoneyMapNavy))`.
2. **Monthly Summary Strip (Income, Expense, Net Flow):**
   - Green positive indicator for income, red badge for expenses, and net cash flow gauge.
3. **Monthly Spending Goal Card:**
   - Displays budget status with linear progress indicators.
4. **Action Shortcuts Grid:**
   - Quick action buttons (Add Expense, Add Income, Transfer, Smart Entry).
5. **Recent Transactions Feed:**
   - Displays the latest transactions with account badges and relative time labels.

---

## 7. Customizing Transaction Screens & Keypad

- **Exact File Path:** `app/src/main/java/com/thecorporatedeveloper/moneymap/ui/screens/TransactionScreens.kt`
- **What it controls:** `AddTransactionScreen`, `EditTransactionScreen`, `TransactionDetailsScreen`, and `TransactionHistoryScreen`.

### Key Customization Points:
- **Numeric Keypad:** The high-speed custom keypad layout is defined in `CompactNumericKeypad`.
- **Quick Amount Suggestions:** Default amounts (100, 500, 1000, 2000, 5000) are rendered in a horizontal chip row. Users can configure these in Settings, or you can change the hardcoded defaults in `AppPreferences.kt` (line 30).
- **Payment Method Options:** Cash, UPI, Debit Card, Credit Card, Net Banking. (Look for `paymentMethod` selector).
- **Receipt Attachment Trigger:** The camera and gallery image picker button that invokes `ReceiptStorageManager`.

---

## 8. Customizing Account Management Screens

- **Exact File Path:** `app/src/main/java/com/thecorporatedeveloper/moneymap/ui/screens/AccountsScreens.kt`
- **What it controls:** `AccountsScreen`, `AddAccountScreen`, `EditAccountScreen`, and `AccountDetailsScreen`.

### Account Types Supported:
- Defined in `app/src/main/java/com/thecorporatedeveloper/moneymap/core/model/FinancialModels.kt`:
  ```kotlin
  enum class AccountType(val displayName: String, val isLiability: Boolean) {
      BANK("Bank", false),
      CASH("Cash", false),
      WALLET("Wallet", false),
      CREDIT_CARD("Credit Card", true),
      SAVINGS("Savings", false)
  }
  ```
- **Liability Logic:** Credit cards automatically carry the `isLiability = true` flag. Spending on credit cards increases liability without subtracting from liquid cash balances.

---

## 9. Customizing Budget & Spending Limit Screens

- **Exact File Path:** `app/src/main/java/com/thecorporatedeveloper/moneymap/ui/screens/ReportsAndBudgetsScreens.kt`
- **What it controls:** `BudgetsScreen`, budget creation dialogs, and budget progress calculations.

### Budget Logic & Warnings:
- Budgets can be configured per month (e.g. `2026-11`).
- Progress thresholds:
  - **< 80%:** Normal progress (Green `MoneyMapGreen`).
  - **80% - 100%:** Approaching limit warning (Orange `MoneyMapOrange`).
  - **> 100%:** Over-budget alert (Red `MoneyMapRed`).

---

## 10. Customizing Reports & Donut Charts

- **Exact File Path:** `app/src/main/java/com/thecorporatedeveloper/moneymap/ui/screens/ReportsAndBudgetsScreens.kt`
- **What it controls:** `ReportsScreen`, period selector chips (Day, Week, Month, Year), category breakdown bars, and the animated Canvas Donut chart.

### Donut Chart Customization:
Search for the `Canvas` block rendering the arc segments:
- You can adjust the stroke width (`strokeWidth = 32.dp.toPx()`) or inner donut radius.
- Category slice colors correspond to the category's `colorHex` value stored in the database.

---

## 11. Customizing Predefined Categories

When MoneyMap is installed, default Indian and international categories are prepared for the user.

- **Entity & DAOs:**
  - `app/src/main/java/com/thecorporatedeveloper/moneymap/data/dao/MoneyMapDaos.kt`
  - `app/src/main/java/com/thecorporatedeveloper/moneymap/ui/screens/CategoryScreen.kt`
- **Default Category Names:**
  - *Expense:* Food & Dining, Groceries, Shopping, Transportation, Bills & Utilities, Entertainment, Health & Medical, Education, Travel, Personal Care, Investments, Gifts & Donations.
  - *Income:* Salary, Freelance, Business, Investments, Rental, Gifts.
- **To add new default categories:**
  Inspect the category seeding queries in `MoneyMapDaos.kt` or the category creation UI in `CategoryScreen.kt`.

---

## 12. Modifying Bottom Navigation Bar

- **Exact File Path:** `app/src/main/java/com/thecorporatedeveloper/moneymap/MainActivity.kt`
- **Composable:** `MoneyMapBottomBar` (lines 1251–1350)
- **What it controls:** The bottom navigation bar with 5 navigation anchors:
  1. **Home** (`Icons.Default.Home`)
  2. **Accounts** (`Icons.Default.AccountBalance`)
  3. **Central Floating Add (+) Button:** Floating circular action button opening the Add Transaction flow.
  4. **History** (`Icons.Default.History`)
  5. **Reports** (`Icons.Default.BarChart`)

### Customizing Navigation Icons or Routes:
Modify `BottomNavItem` definitions in `MainActivity.kt` to add or reorder destinations (e.g. swapping Reports with Budgets or Calendar).

---

## 13. Customizing Splash Screen Animations

- **Exact File Path:** `app/src/main/java/com/thecorporatedeveloper/moneymap/ui/screens/SplashScreen.kt`
- **What it controls:** The opening brand screen displayed for ~1.2 seconds while Room initializes.

### Customization Options:
- Change the background gradient from blue to your theme colors.
- Adjust `scale` and `alpha` animatable parameters.
- Change the duration: `delay(1200L)` inside `LaunchedEffect(Unit)`.

---

## 14. Customizing Android Home-Screen Widgets

MoneyMap includes two widgets that update when transactions or balances change:

1. **Quick Actions Widget (4x1):**
   - **Provider:** `app/src/main/java/com/thecorporatedeveloper/moneymap/widget/QuickActionsWidgetProvider.kt`
   - **Layout:** `app/src/main/res/layout/widget_quick_actions.xml`
   - **XML Info:** `app/src/main/res/xml/quick_actions_widget_info.xml`
2. **Recent Transactions Widget (4x2):**
   - **Provider:** `app/src/main/java/com/thecorporatedeveloper/moneymap/widget/RecentTransactionsWidgetProvider.kt`
   - **Layout:** `app/src/main/res/layout/widget_recent_transactions.xml`
   - **XML Info:** `app/src/main/res/xml/recent_transactions_widget_info.xml`

### Customizing Widget Layout & Colors:
- Update background drawables in `app/src/main/res/drawable/`:
  - `bg_widget_card.xml` (rounded corner container)
  - `bg_widget_btn_expense.xml`, `bg_widget_btn_income.xml`, `bg_widget_btn_transfer.xml`
- Modify text sizes or margins in `widget_quick_actions.xml` or `widget_recent_transactions.xml`.

---

## 15. Building, Testing, and Verifying the Reskin

After completing your visual modifications:

1. **Clean and Sync:**
   - In Android Studio: **Build > Clean Project**.
   - Then: **File > Sync Project with Gradle Files**.
2. **Run on Device or Emulator:**
   - Click the green **Run (Play)** button (`Shift + F10`).
3. **Verify Checklist:**
   - [ ] Launcher icon displays properly on Android launcher home screen.
   - [ ] App name under the icon matches your new brand.
   - [ ] Splash screen displays your new logo and tagline.
   - [ ] Theme colors reflect your chosen primary and secondary palette.
   - [ ] Both Light and Dark modes render clearly with readable contrast.
   - [ ] Creating an account, transaction, and budget works smoothly.
   - [ ] Home-screen widgets can be placed and reflect current balances.

---

*Next Step: To configure production signing keys and build signed APKs/AABs for publishing, read **`RELEASE_SIGNING_GUIDE.md`**.*
