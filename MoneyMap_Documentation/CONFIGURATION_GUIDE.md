# MoneyMap — Complete Configuration Guide

This guide explains how to configure and personalize **MoneyMap** for your own business or branding requirements. It provides concrete, code-verified instructions for updating the application name, package ID, namespace, launcher icons, splash screen, theme colors, default currencies, and application preferences.

---

## 📋 Table of Contents

1. [Understanding Application Identity: ID vs Namespace](#1-understanding-application-identity-id-vs-namespace)
2. [Changing the Application Name](#2-changing-the-application-name)
3. [Safely Changing the Package Name / Application ID](#3-safely-changing-the-package-name--application-id)
4. [Customizing Launcher & Adaptive Icons](#4-customizing-launcher--adaptive-icons)
5. [Customizing Splash Screen & Logo](#5-customizing-splash-screen--logo)
6. [Configuring Developer Links & Legal URLs](#6-configuring-developer-links--legal-urls)
7. [Customizing Theme Colors (Light & Dark Mode)](#7-customizing-theme-colors-light--dark-mode)
8. [Customizing Typography & Font Families](#8-customizing-typography--font-families)
9. [Configuring Supported Currencies](#9-configuring-supported-currencies)
10. [Configuring Default User Preferences](#10-configuring-default-user-preferences)

---

## 1. Understanding Application Identity: ID vs Namespace

In modern Android development with AGP (Android Gradle Plugin 8.x / 9.x), there are three distinct concepts related to package naming:

1. **`applicationId` (`app/build.gradle.kts`):**
   - The unique identifier for your app on the Google Play Store and on Android devices (e.g., `com.mycompany.myfinanceapp`).
   - Android OS uses this to differentiate apps. Two apps with different `applicationId`s can coexist on the same phone.
2. **`namespace` (`app/build.gradle.kts`):**
   - The Kotlin/Java package where the generated `R` class (resources) and `BuildConfig` reside.
3. **Source Directory Structure (`app/src/main/java/...`):**
   - The physical filesystem path of your Kotlin classes. Currently: `com/thecorporatedeveloper/moneymap/`.

> 💡 **Best Practice Tip:** If your goal is simply to publish your own unique branded app on Google Play, **you only need to change the `applicationId`** in `app/build.gradle.kts`. You do not have to move your Kotlin source files unless you specifically want the internal source folder names to match your company domain.

---

## 2. Changing the Application Name

To rename the app as it appears under the launcher icon, in the top bar, and in system dialogs:

### Step 1: Update `strings.xml`
- **File:** `app/src/main/res/values/strings.xml`
- **Target Line (Line 2):**
  ```xml
  <string name="app_name">MoneyMap</string>
  ```
- **Change To:**
  ```xml
  <string name="app_name">YourAppName</string>
  ```

### Step 2: Update `settings.gradle.kts`
- **File:** `settings.gradle.kts`
- **Target Line (Line 25):**
  ```kotlin
  rootProject.name = "MoneyMap"
  ```
- **Change To:**
  ```kotlin
  rootProject.name = "YourAppName"
  ```

### Step 3: Update About Text
- **File:** `app/src/main/res/values/strings.xml`
- **Target Line (Line 97):**
  ```xml
  <string name="about_moneymap_desc">Offline Personal Finance • Track • Plan • Grow</string>
  ```

---

## 3. Safely Changing the Package Name / Application ID

If you want to customize your Google Play package identifier from `com.thecorporatedeveloper.moneymap` to your own domain (e.g. `com.yourcompany.wallet`):

### Option A: Change `applicationId` Only (Recommended & 100% Safe)
This changes the identity on Google Play and Android devices without touching any Kotlin code.

1. Open `app/build.gradle.kts`.
2. Locate `defaultConfig`:
   ```kotlin
   defaultConfig {
       applicationId = "com.thecorporatedeveloper.moneymap" // <-- Change this
       minSdk = 24
       targetSdk = 36
       versionCode = 3
       versionName = "1.0.3"
   }
   ```
3. Change `applicationId` to your unique identifier:
   ```kotlin
   applicationId = "com.yourcompany.wallet"
   ```
4. Note that `AndroidManifest.xml` uses the dynamic variable `${applicationId}.fileprovider`:
   ```xml
   <provider
       android:name="androidx.core.content.FileProvider"
       android:authorities="${applicationId}.fileprovider"
       android:exported="false"
       android:grantUriPermissions="true">
   ```
   Because it uses `${applicationId}`, the FileProvider authority will automatically update to `com.yourcompany.wallet.fileprovider` without any manual XML editing!
5. Sync Gradle. Done!

---

### Option B: Complete Source Refactoring (Namespace + Package Directories)
If you wish to refactor the internal Kotlin packages as well:

#### 1. Update `app/build.gradle.kts`:
```kotlin
android {
    namespace = "com.yourcompany.wallet"
    defaultConfig {
        applicationId = "com.yourcompany.wallet"
        ...
    }
}
```

#### 2. Update `AndroidManifest.xml`:
Replace the widget update broadcast action in `app/src/main/AndroidManifest.xml`:
```xml
<!-- Before -->
<action android:name="com.thecorporatedeveloper.moneymap.ACTION_MONEYMAP_DATA_CHANGED" />

<!-- After -->
<action android:name="com.yourcompany.wallet.ACTION_MONEYMAP_DATA_CHANGED" />
```

#### 3. Update Broadcast References in Kotlin Code:
Search and replace `com.thecorporatedeveloper.moneymap.ACTION_MONEYMAP_DATA_CHANGED` in:
- `app/src/main/java/com/thecorporatedeveloper/moneymap/data/repository/MoneyMapRepository.kt` (line 448)
- `app/src/main/java/com/thecorporatedeveloper/moneymap/widget/QuickActionsWidgetProvider.kt`
- `app/src/main/java/com/thecorporatedeveloper/moneymap/widget/RecentTransactionsWidgetProvider.kt`

#### 4. Rename Package in Android Studio:
1. In the Android Studio **Project** tool window (top left), click the gear icon (Settings) and **uncheck** `Compact Middle Packages`.
2. Navigate down through:
   `app > src > main > java > com > thecorporatedeveloper > moneymap`
3. Right-click `thecorporatedeveloper` > **Refactor > Rename...** > Select **Rename package** > Enter `yourcompany` > Click **Refactor** and **Do Refactor**.
4. Right-click `moneymap` > **Refactor > Rename...** > Select **Rename package** > Enter `wallet` > Click **Refactor** and **Do Refactor**.
5. Select **Build > Clean Project**, then **Build > Rebuild Project**.

*(Note: A helper Python script `refactor_package.py` is also included in `/app` which demonstrates automated text replacement across all sources.)*

---

## 4. Customizing Launcher & Adaptive Icons

MoneyMap supports modern adaptive icons on Android 8.0+ (API 26+) with legacy fallbacks.

### File Locations:
- **Adaptive Icon XML:**
  `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
  `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- **Vector Background:**
  `app/src/main/res/drawable/ic_launcher_background.xml`
- **Vector Foreground:**
  `app/src/main/res/drawable/ic_launcher_foreground.xml`
- **Raster Fallbacks:**
  - `app/src/main/res/mipmap-mdpi/ic_launcher.png` (48x48 px)
  - `app/src/main/res/mipmap-hdpi/ic_launcher.png` (72x72 px)
  - `app/src/main/res/mipmap-xhdpi/ic_launcher.png` (96x96 px)
  - `app/src/main/res/mipmap-xxhdpi/ic_launcher.png` (144x144 px)
  - `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png` (192x192 px)

### Replacing Icons Using Android Asset Studio (Recommended):
1. In Android Studio, right-click the `app/src/main/res` folder.
2. Select **New > Image Asset**.
3. In the **Asset Type** dropdown, select **Launcher Icons (Adaptive and Legacy)**.
4. **Foreground Layer:**
   - Choose **Source Asset: Image** and select your logo file (PNG or SVG).
   - Adjust the **Resize** slider so your logo fits cleanly inside the circular safety zone.
5. **Background Layer:**
   - Select **Source Asset: Color** (e.g. `#1E56F0` or your primary brand color).
6. Click **Next**, review the output file paths, and click **Finish**. Android Studio will replace all density mipmap files automatically.

---

## 5. Customizing Splash Screen & Logo

MoneyMap displays an elegant branded launch screen while initializing database schemas and preferences.

### File Locations:
- **Splash Screen Composable:**
  `app/src/main/java/com/thecorporatedeveloper/moneymap/ui/screens/SplashScreen.kt`
- **Logo Graphic:**
  `app/src/main/res/drawable/moneymap_logo.png`
- **Tagline String:**
  `app/src/main/res/values/strings.xml` (line 3: `<string name="tagline">Track • Plan • Grow</string>`)

### How to Customize:
1. Replace `moneymap_logo.png` in `app/src/main/res/drawable/` with your own PNG or WebP logo file (transparent background, recommended size: 512x512 px).
2. To modify the animation duration or layout, inspect `SplashScreen.kt`:
   - Change logo scale, pulse animation, or background gradient.
   - The splash delay is controlled by `delay(1200L)` inside `LaunchedEffect`.

---

## 6. Configuring Developer Links & Legal URLs

The **Settings** screen contains direct links to developer support and compliance documents:

- **File:** `app/src/main/java/com/thecorporatedeveloper/moneymap/ui/screens/SettingsAndBackupScreens.kt`

### YouTube Channel Link (Line 153):
```kotlin
// Change this to your company YouTube channel or social link:
val url = "https://youtube.com/@thecorporatedeveloper-yt?si=C8TMVT9e4umO6Af3"
```

### Privacy Policy Web URL (Line 211):
```kotlin
// Change this to your live public Privacy Policy URL:
val url = "https://doc-hosting.flycricket.io/moneymap-privacy-policy/01d50b25-5834-403b-be59-1c0967a0c3d4/privacy"
```

### Support Email Address (Line 326 & 333):
```kotlin
Text(
    text = "Support Email: thecorporatedeveloper@gmail.com",
    ...
)
val intent = Intent(Intent.ACTION_SENDTO).apply {
    data = Uri.parse("mailto:your_support_email@yourcompany.com")
    putExtra(Intent.EXTRA_SUBJECT, "App Support Request")
}
```

---

## 7. Customizing Theme Colors (Light & Dark Mode)

All colors used in Jetpack Compose UI elements are centrally defined in:
`app/src/main/java/com/thecorporatedeveloper/moneymap/ui/theme/Color.kt` and `Theme.kt`.

### 1. Color Palette (`Color.kt`):
```kotlin
package com.thecorporatedeveloper.moneymap.ui.theme

import androidx.compose.ui.graphics.Color

// Brand Primaries
val MoneyMapNavy = Color(0xFF0C2B8A)      // Deep Navy Container
val MoneyMapBlue = Color(0xFF1E56F0)      // Primary Accent Blue
val MoneyMapBlueLight = Color(0xFFEFF6FF) // Tinted Background Surface
val MoneyMapCyan = Color(0xFF06B6D4)      // Cyan Accent

// Financial Status Colors
val MoneyMapGreen = Color(0xFF10B981)     // Positive / Income
val MoneyMapGreenLight = Color(0xFFE6F9F0)
val MoneyMapRed = Color(0xFFEF4444)       // Negative / Expense / Warning
val MoneyMapRedLight = Color(0xFFFEE2E2)

// Surface & Text (Light Mode)
val SurfaceLight = Color(0xFFF8FAFC)
val SurfaceCard = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF0F172A)
val TextSecondary = Color(0xFF64748B)
val BorderSubtle = Color(0xFFE2E8F0)

// Surface & Text (Dark Mode)
val SurfaceDark = Color(0xFF0F172A)
val SurfaceCardDark = Color(0xFF1E293B)
val TextPrimaryDark = Color(0xFFF8FAFC)
val TextSecondaryDark = Color(0xFF94A3B8)
val BorderDark = Color(0xFF334155)
```

### 2. Changing the Color Scheme (`Theme.kt`):
- `LightColorScheme` maps `primary = MoneyMapBlue`, `secondary = MoneyMapGreen`, and `background = SurfaceLight`.
- To introduce a new theme (such as Emerald Green or Royal Purple), modify the hex codes in `Color.kt` or adjust the `colorScheme` mappings in `Theme.kt`.

---

## 8. Customizing Typography & Font Families

MoneyMap uses the modern **Plus Jakarta Sans** font family.

### File Locations:
- **Font File:** `app/src/main/res/font/plus_jakarta_sans.ttf`
- **Type Definitions:** `app/src/main/java/com/thecorporatedeveloper/moneymap/ui/theme/Type.kt`

### To Replace with a Custom Font (e.g., Poppins, Inter, Roboto):
1. Download your preferred font file in `.ttf` format.
2. Rename the file to lowercase with underscores only (e.g., `poppins_regular.ttf`).
3. Paste the file into `app/src/main/res/font/`.
4. In `Type.kt`, update lines 11–13:
   ```kotlin
   val MyCustomFont = FontFamily(
       Font(R.font.poppins_regular, FontWeight.Normal)
   )
   ```
5. Replace `PlusJakartaSans` with `MyCustomFont` in `MoneyMapTypography`.

---

## 9. Configuring Supported Currencies

MoneyMap comes pre-configured with **39 international currencies** in:
`app/src/main/java/com/thecorporatedeveloper/moneymap/core/model/CurrencyRepository.kt`.

### Adding a New Currency or Changing the Default:
In `CurrencyRepository.kt`:
1. **Add new entry** to `supportedCurrencies`:
   ```kotlin
   CurrencyInfo(
       code = "BHD",
       name = "Bahraini Dinar",
       symbol = "BD",
       flag = "🇧🇭",
       country = "Bahrain"
   )
   ```
2. **Change Fallback Currency** (Line 56):
   ```kotlin
   // Default fallback if no preference is stored:
   fun getCurrency(code: String): CurrencyInfo {
       return supportedCurrencies.find { it.code.equals(code, ignoreCase = true) }
           ?: CurrencyInfo("USD", "US Dollar", "$", "🇺🇸", "United States") // <-- Change here
   }
   ```

---

## 10. Configuring Default User Preferences

When a user installs MoneyMap for the first time, initial settings are controlled by `UserPreferences`:

- **File:** `app/src/main/java/com/thecorporatedeveloper/moneymap/data/preferences/AppPreferences.kt`
- **Lines 14–34:**
  ```kotlin
  data class UserPreferences(
      val hasCompletedOnboarding: Boolean = true,
      val userName: String = "",
      val themeMode: String = "LIGHT",         // Options: "LIGHT", "DARK", "SYSTEM"
      val accentColorHex: String = "#1E56F0",  // Default primary accent
      val hideAmounts: Boolean = false,        // Default privacy state
      val appLockEnabled: Boolean = false,     // Default PIN lock state
      val autoLockMinutes: Int = 1,            // 0 (Immediately), 1, 5, or 15 minutes
      val blockScreenshots: Boolean = false,   // Default screenshot policy
      val widgetTheme: String = "LIGHT",       // Default widget style
      val firstDayOfWeek: String = "Monday",   // "Monday" or "Sunday"
      val dateFormat: String = "dd MMM yyyy",  // Standard date formatting
      val quickAmounts: String = "100,500,1000,2000,5000", // Keypad quick buttons
      val currency: String = "INR"             // Default currency: change to "USD", "EUR", etc.
  )
  ```

Changing any of these default field values will alter the out-of-the-box configuration for all fresh installs!

---

*Next Step: For comprehensive visual reskinning examples, screen layout customization, and widget tweaks, refer to **`RESKINNING_AND_CUSTOMIZATION.md`**.*
