# MoneyMap — Comprehensive Troubleshooting Guide

This guide provides concrete, proven solutions for common errors encountered during installation, Gradle synchronization, compilation, Room database migrations, installation, and release signing.

---

## 📋 Table of Contents

1. [Gradle Sync Failures](#1-gradle-sync-failures)
2. [Missing Android SDK Platform 36](#2-missing-android-sdk-platform-36)
3. [JDK Version Incompatibility (Java 11 vs 17 vs 21)](#3-jdk-version-incompatibility-java-11-vs-17-vs-21)
4. [Kotlin & KSP Compilation Errors](#4-kotlin--ksp-compilation-errors)
5. [Room Database Migration & Schema Issues](#5-room-database-migration--schema-issues)
6. [App Installation Failure: `INSTALL_FAILED_UPDATE_INCOMPATIBLE`](#6-app-installation-failure-install_failed_update_incompatible)
7. [App Crashing on Startup](#7-app-crashing-on-startup)
8. [FileProvider Authority Conflicts (`SecurityException`)](#8-fileprovider-authority-conflicts-securityexception)
9. [Release Signing & Keystore Password Errors](#9-release-signing--keystore-password-errors)
10. [Android Studio Heap Memory & Build Out of Memory](#10-android-studio-heap-memory--build-out-of-memory)

---

## 1. Gradle Sync Failures

### Symptom:
`"Gradle sync failed: Connection refused"`, or `"Could not resolve com.android.tools.build:gradle:9.1.1"`.

### Cause:
1. Android Studio was opened without an active internet connection on the initial sync.
2. A corporate firewall or proxy is blocking `mavenCentral()` or `google()`.
3. Gradle Daemon cache corruption.

### Solution:
1. Ensure your computer is connected to the internet.
2. Verify repository declarations in `settings.gradle.kts`:
   ```kotlin
   dependencyResolutionManagement {
       repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
       repositories {
           google()
           mavenCentral()
       }
   }
   ```
3. Stop any stuck Gradle daemons and restart Android Studio:
   - In Android Studio terminal, run:
     ```bash
     ./gradlew --stop
     ```
   - Go to **File > Invalidate Caches...**, check all boxes, and click **Invalidate and Restart**.

---

## 2. Missing Android SDK Platform 36

### Symptom:
`"Failed to find target with hash string 'android-36' in: /path/to/sdk"`.

### Cause:
The required Android 16 (API Level 36) platform SDK is not installed on your machine.

### Solution:
1. Click the blue link in the error log: **"Install missing platform(s) and sync project"**.
2. Or manually:
   - Go to **Tools > SDK Manager**.
   - Under **SDK Platforms**, select **Android 16.0 ("VanillaIceCream" / API Level 36)**.
   - Under **SDK Tools**, check **Android SDK Build-Tools 35.x/36.x**.
   - Click **Apply** and wait for the download to finish, then re-sync.

---

## 3. JDK Version Incompatibility (Java 11 vs 17 vs 21)

### Symptom:
`"Unsupported class file major version..."` or `"Android Gradle Plugin 9.x requires Java 17 to run. You are currently using Java 11."`

### Cause:
The Gradle JVM in Android Studio is configured to an old Java 8 or Java 11 runtime.

### Solution:
1. In Android Studio, open:
   - Windows/Linux: **File > Settings > Build, Execution, Deployment > Build Tools > Gradle**.
   - macOS: **Android Studio > Settings > Build, Execution, Deployment > Build Tools > Gradle**.
2. Under **Gradle Projects**, find the **Gradle JDK** dropdown.
3. Select **Embedded JDK (JetBrains Runtime `jbr-17` or `jbr-21`)**.
4. Click **Apply** and re-run the build.

---

## 4. Kotlin & KSP Compilation Errors

### Symptom:
`"e: [ksp] Cannot find symbol"` or `"Incompatible KSP version"`.

### Cause:
Google KSP (Kotlin Symbol Processing) versions are strictly tied to specific Kotlin versions.

### Verified Working Configuration in this Project:
- **File:** `gradle/libs.versions.toml`
  ```toml
  [versions]
  kotlin = "2.2.10"
  googleDevtoolsKsp = "2.3.5"
  ```
- **File:** `app/build.gradle.kts`:
  ```kotlin
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  ```
- If you ever modify the Kotlin compiler version, make sure to update KSP to the corresponding matching release.
- **Fix:** In Android Studio, run **Build > Clean Project**, then **Build > Rebuild Project**.

---

## 5. Room Database Migration & Schema Issues

### Symptom:
`"IllegalStateException: Room cannot verify the data integrity. A migration from X to Y was required but not found."`

### Explanation:
MoneyMap is currently at database **Version 13** (`MoneyMapDatabase.kt`).
The database schema has evolved through 12 migration steps:
- `MIGRATION_1_2` to `MIGRATION_12_13` (adding indexes, budgets, receipt tables, and cleaning deprecated tables).

### Solution:
- If you modify any Room Entity fields (e.g. adding a new column to `AccountEntity` or `TransactionEntity`):
  1. Increment the database version in `MoneyMapDatabase.kt` (e.g. `version = 14`).
  2. Write a migration object `val MIGRATION_13_14 = object : Migration(13, 14) { ... }`.
  3. Register the migration in `Room.databaseBuilder(...).addMigrations(...)`.
- **For fresh development testing:** You can uninstall the existing app from your test phone/emulator to wipe old SQLite files and start fresh with the new schema.

---

## 6. App Installation Failure: `INSTALL_FAILED_UPDATE_INCOMPATIBLE`

### Symptom:
`"Installation failed with message INSTALL_FAILED_UPDATE_INCOMPATIBLE: Package com.thecorporatedeveloper.moneymap signatures do not match previously installed version."`

### Cause:
You have a `debug`-signed version of MoneyMap installed on your phone and are trying to install a `release`-signed APK (or vice versa). Android security prevents replacing an app with a different signing certificate.

### Solution:
1. On your phone, locate the MoneyMap app icon.
2. Long-press the icon and tap **Uninstall**.
3. Now install your new APK via Android Studio or ADB:
   ```bash
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```

---

## 7. App Crashing on Startup

### Symptom:
App opens and immediately closes or crashes on the splash screen.

### How to Diagnose:
1. In Android Studio, open the **Logcat** tab at the bottom.
2. In the search filter, type:
   ```
   package:mine level:error
   ```
   or filter by tag: `FATAL EXCEPTION`.

### Common Causes & Fixes:
1. **BiometricPrompt Exception:**
   - On older devices without a fingerprint sensor, attempting to authenticate without hardware can throw an exception. MoneyMap safely wraps biometric checks with `BiometricManager.canAuthenticate()`.
2. **Missing `moneymap.db` or Corrupt File:**
   - Go to device **Settings > Apps > MoneyMap > Storage > Clear Storage**.
3. **Android 14+ / 16 Notification Permissions:**
   - On Android 13+, notification scheduling requires `POST_NOTIFICATIONS` runtime permission. Ensure you allow notification permissions when prompted.

---

## 8. FileProvider Authority Conflicts (`SecurityException`)

### Symptom:
`"java.lang.SecurityException: Provider authority com.thecorporatedeveloper.moneymap.fileprovider already exists"` during app install or multi-app testing.

### Cause:
Two installed apps cannot share the exact same `android:authorities` string.

### Solution:
In `app/src/main/AndroidManifest.xml`, the authority is defined dynamically using:
```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
```
When you change `applicationId` in `app/build.gradle.kts` (e.g. `com.mycompany.app`), Gradle automatically updates this string to `com.mycompany.app.fileprovider`. Ensure your `applicationId` is unique!

---

## 9. Release Signing & Keystore Password Errors

### Symptom:
`"Execution failed for task ':app:validateSigningRelease'. Keystore file not found"` or `"Password verification failed"`.

### Cause:
1. The `.jks` keystore path specified in `app/build.gradle.kts` does not exist.
2. The keystore password or key alias password is incorrect.

### Solution:
1. Verify the keystore file exists at the path specified in your environment or `build.gradle.kts`:
   ```bash
   ls -la my-upload-key.jks
   ```
2. Test your keystore credentials via Java's `keytool`:
   ```bash
   keytool -list -v -keystore my-upload-key.jks -alias upload
   ```
   Enter your password. If `keytool` displays the certificate fingerprints (SHA1, SHA256), your password and alias are 100% correct.

---

## 10. Android Studio Heap Memory & Build Out of Memory

### Symptom:
`"Expiring Daemon because JVM heap space is exhausted"` or Gradle compilation freezes during KSP processing.

### Solution:
Increase the memory allocated to the Gradle daemon.
1. Open `gradle.properties` in the project root.
2. Add or increase the heap limit:
   ```properties
   org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m -XX:+HeapDumpOnOutOfMemoryError
   org.gradle.parallel=true
   org.gradle.caching=true
   ```
3. Save the file and restart Android Studio. This gives Gradle 4 GB of RAM, accelerating builds significantly.

---

*Need additional assistance? Contact support at `thecorporatedeveloper@gmail.com`.*
