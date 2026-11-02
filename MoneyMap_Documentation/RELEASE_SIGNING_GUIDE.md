# MoneyMap — Production Release & Signing Guide

This guide covers everything you need to know about generating secure, production-grade **Signed Release APKs** and **Android App Bundles (AAB)** for the Google Play Store or alternative marketplaces.

---

## 📋 Table of Contents

1. [Debug vs. Release Builds: Key Differences](#1-debug-vs-release-builds-key-differences)
2. [Security Best Practices: Protecting Your Signing Key](#2-security-best-practices-protecting-your-signing-key)
3. [Creating a New Release Keystore (.jks)](#3-creating-a-new-release-keystore-jks)
4. [Configuring Signing in `build.gradle.kts`](#4-configuring-signing-in-buildgradlekts)
5. [Generating a Signed Android App Bundle (AAB) for Google Play](#5-generating-a-signed-android-app-bundle-aab-for-google-play)
6. [Generating a Signed Release APK for Direct Distribution](#6-generating-a-signed-release-apk-for-direct-distribution)
7. [Locating Generated Output Files](#7-locating-generated-output-files)
8. [Testing the Release Build on a Real Device](#8-testing-the-release-build-on-a-real-device)
9. [Updating `versionCode` and `versionName`](#9-updating-versioncode-and-versionname)
10. [Google Play Console Publishing Checklist](#10-google-play-console-publishing-checklist)

---

## 1. Debug vs. Release Builds: Key Differences

Android applications have two primary build configurations:

| Feature | Debug Build (`debug`) | Release Build (`release`) |
|---|---|---|
| **Purpose** | Development, testing, and debugging. | Final end-user distribution & Play Store. |
| **Signing Key** | Generic auto-generated `debug.keystore`. | Secure, private, developer-owned keystore. |
| **Debuggable** | `android:debuggable="true"` (Inspectable via ADB/Logcat). | `android:debuggable="false"` (Secure, cannot be inspected). |
| **Code Optimization** | Unminified, preserves all line numbers and symbols. | R8 compiler strips dead code and optimizes bytecode. |
| **Performance** | Slower due to debug assertions and profiling overhead. | Highly optimized AOT bytecode for fast startup and low RAM. |
| **Play Store Acceptance** | Rejected by Google Play Console. | Required for submission to Google Play Console. |

---

## 2. Security Best Practices: Protecting Your Signing Key

> ⚠️ **CRITICAL WARNING FOR BUYERS:**
> 1. **Never commit your `.jks` keystore file or key passwords to public GitHub or source code repositories.**
> 2. **Backup your keystore file securely in multiple offline locations (e.g., encrypted USB drive, password manager vault).**
> 3. If you lose your signing keystore (and are not enrolled in Google Play App Signing), **you will permanently lose the ability to publish updates for your app** on Google Play.

---

## 3. Creating a New Release Keystore (.jks)

Before building a release binary, you must generate your own unique cryptographic keystore.

### Method A: Via Android Studio GUI (Easiest)
1. Open the project in Android Studio.
2. In the top menu, go to:
   ```
   Build > Generate Signed Bundle / APK...
   ```
3. Select either **Android App Bundle** or **APK** and click **Next**.
4. Under **Key store path**, click **Create new...**.
5. Fill in the New Key Store dialog:
   - **Key store path:** Click the folder icon and choose a secure folder on your computer outside the project (e.g., `~/secure_keys/my_release_key.jks`).
   - **Password:** Choose a strong password (minimum 12 characters).
   - **Confirm:** Re-type the password.
   - **Alias:** Enter an identifier for this key (e.g. `moneymap_key` or `upload`).
   - **Key Password:** Enter a key password (or check "Same as key store password").
   - **Validity (years):** Enter **25** or higher (required by Google Play).
   - **Certificate Info:** Enter at least your name or organization (e.g., First and Last Name: `Your Name`, Organization: `Your Company`).
6. Click **OK** to generate the `.jks` file.

---

### Method B: Via Command Line (`keytool`)
You can also generate an RSA 2048-bit keystore using Java's `keytool` command:

```bash
keytool -genkeypair -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```
Follow the interactive prompts to set your password and certificate details.

---

## 4. Configuring Signing in `build.gradle.kts`

Open `app/build.gradle.kts` and inspect lines 27–46:

```kotlin
  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD") ?: "android"
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD") ?: "android"
    }
    create("debugConfig") { ... }
  }
```

### How to Configure for Your Own Keystore:

#### Approach 1: Using Environment Variables (Recommended for CI/CD & Safe Git Usage)
Set these environment variables in your terminal or build server:
- `KEYSTORE_PATH`: Absolute path to your `.jks` file.
- `STORE_PASSWORD`: Your keystore password.
- `KEY_PASSWORD`: Your key alias password.

#### Approach 2: Using Local `gradle.properties` (Recommended for Local Desktop Builds)
Add the following lines to your user-level `~/.gradle/gradle.properties` (or the project root `gradle.properties`, ensuring `gradle.properties` is not committed publicly):

```properties
RELEASE_STORE_FILE=C:/secure_keys/my_release_key.jks
RELEASE_STORE_PASSWORD=YourStorePassword123
RELEASE_KEY_ALIAS=upload
RELEASE_KEY_PASSWORD=YourKeyPassword123
```

Then reference them in `app/build.gradle.kts`:
```kotlin
  signingConfigs {
    create("release") {
      val propFile = project.findProperty("RELEASE_STORE_FILE") as String?
      if (propFile != null) {
        storeFile = file(propFile)
        storePassword = project.findProperty("RELEASE_STORE_PASSWORD") as String?
        keyAlias = project.findProperty("RELEASE_KEY_ALIAS") as String?
        keyPassword = project.findProperty("RELEASE_KEY_PASSWORD") as String?
      } else {
        // Fallback to environment variables or local key
        storeFile = file(System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks")
        storePassword = System.getenv("STORE_PASSWORD") ?: "android"
        keyAlias = "upload"
        keyPassword = System.getenv("KEY_PASSWORD") ?: "android"
      }
    }
  }
```

---

## 5. Generating a Signed Android App Bundle (AAB) for Google Play

Google Play requires the **Android App Bundle (.aab)** format for all new applications.

1. In Android Studio, go to:
   ```
   Build > Generate Signed Bundle / APK...
   ```
2. Select **Android App Bundle** and click **Next**.
3. Select your `.jks` keystore, enter the passwords, and select your alias.
4. Click **Next**.
5. Select the **release** destination folder and build variant:
   - **Build Variants:** select `release`.
6. Click **Finish**.
7. Android Studio will execute the R8 optimization and bundling tasks.

---

## 6. Generating a Signed Release APK for Direct Distribution

If you want to distribute your app directly to users via your website, email, or third-party stores (Amazon Appstore, Samsung Galaxy Store, F-Droid):

1. Go to: **Build > Generate Signed Bundle / APK...**
2. Select **APK** and click **Next**.
3. Select your keystore and enter your credentials.
4. Select the **release** build variant.
5. Ensure signature versions **V1 (Jar Signature)** and **V2 (Full APK Signature)** are enabled (or let Android Gradle Plugin sign automatically).
6. Click **Finish**.

Alternatively, run from the terminal:
```bash
# Windows
gradlew.bat assembleRelease

# macOS / Linux
./gradlew assembleRelease
```

---

## 7. Locating Generated Output Files

Once the build finishes successfully, your generated release artifacts are stored at:

### 1. Android App Bundle (for Google Play):
```
app/build/outputs/bundle/release/app-release.aab
```

### 2. Standalone Release APK (for Direct Sideloading):
```
app/build/outputs/apk/release/app-release.apk
```

---

## 8. Testing the Release Build on a Real Device

Before uploading to Google Play, always install and test the release binary on a real Android device to verify that R8 minification did not strip required reflection or Room database components.

1. Connect your Android device via USB.
2. If you already have a `debug` version of MoneyMap installed on your phone, **you must uninstall it first**. (Android will refuse to install a release APK over a debug APK due to signature mismatch `INSTALL_FAILED_UPDATE_INCOMPATIBLE`).
3. Install the release APK using ADB:
   ```bash
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```
4. Verify the following on the device:
   - [ ] App launches without crashes.
   - [ ] Creating an account, income, expense, and transfer works.
   - [ ] Attaching a receipt photo works properly.
   - [ ] Smart Entry screenshot parsing works.
   - [ ] Exporting and restoring a backup JSON/ZIP functions properly.
   - [ ] App Lock PIN and Biometrics function smoothly.

---

## 9. Updating `versionCode` and `versionName`

Every time you release an update on Google Play, you **must increment `versionCode`** by at least 1. Google Play Console will reject any upload with an equal or lower `versionCode`.

- **File:** `app/build.gradle.kts`
- **Lines 19–20:**

```kotlin
    defaultConfig {
        applicationId = "com.thecorporatedeveloper.moneymap"
        minSdk = 24
        targetSdk = 36
        versionCode = 4       // Increment for every new update (e.g. 3 -> 4 -> 5)
        versionName = "1.0.4" // User-visible semantic version (e.g. "1.0.4")
    }
```

---

## 10. Google Play Console Publishing Checklist

When submitting your AAB to the [Google Play Console](https://play.google.com/console):

1. **Create New Application:**
   - App Name: `YourAppName` (under 30 characters).
   - Default Language: English.
   - App or Game: App.
   - Free or Paid: Select Free (or Paid).
2. **Set up Store Listing:**
   - Short description (up to 80 characters).
   - Full description (up to 4000 characters).
   - App Icon: 512 x 512 px 32-bit PNG.
   - Feature Graphic: 1024 x 500 px JPEG or PNG.
   - Phone Screenshots: At least 4 screenshots (aspect ratio 16:9 or 9:16).
3. **App Content & Declarations:**
   - **Privacy Policy:** Provide your active public web URL (e.g. hosted on your website).
   - **Ads Declaration:** Select **"No, my app does not contain ads"**.
   - **App Access:** Select **"All functionality is available without special access"** (MoneyMap has no mandatory login or paywall).
   - **Financial Features:** MoneyMap is an offline personal expense tracker (does not offer banking services, loans, or money transfers).
   - **Data Safety Form:**
     - Data collection: **No user data is collected or shared with third parties** (all records remain 100% on the device).
4. **Release Track:**
   - Create a release in **Internal Testing** or **Production**.
   - Upload `app/build/outputs/bundle/release/app-release.aab`.
   - Enter release notes (e.g., *"Initial release of MyFinance - Track, Plan, and Grow."*).
   - Review and rollout!

---

*Next Step: If you encounter any compilation or signing issues, refer to **`TROUBLESHOOTING.md`**.*
