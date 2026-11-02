# MoneyMap — Complete Installation Guide

This guide provides beginner-friendly, step-by-step instructions to set up, sync, build, and run the **MoneyMap** Android Studio project on your local workstation.

---

## 📋 Table of Contents

1. [System & Software Requirements](#1-system--software-requirements)
2. [Downloading and Installing Android Studio](#2-downloading-and-installing-android-studio)
3. [Android SDK & Build Tools Setup](#3-android-sdk--build-tools-setup)
4. [Extracting the Source Project](#4-extracting-the-source-project)
5. [Opening the Project in Android Studio](#5-opening-the-project-in-android-studio)
6. [Performing Gradle Synchronization](#6-performing-gradle-synchronization)
7. [Resolving Missing SDK & Dependency Prompts](#7-resolving-missing-sdk--dependency-prompts)
8. [Connecting an Android Device (USB Debugging)](#8-connecting-an-android-device-usb-debugging)
9. [Setting up an Android Virtual Device (Emulator)](#9-setting-up-an-android-virtual-device-emulator)
10. [Running MoneyMap on Your Device](#10-running-moneymap-on-your-device)
11. [Generating a Debug APK](#11-generating-a-debug-apk)
12. [Locating Your Generated APK](#12-locating-your-generated-apk)

---

## 1. System & Software Requirements

MoneyMap utilizes modern Android toolchains, including Android Gradle Plugin 9.x, Kotlin 2.2.x, and Jetpack Compose. Verify your computer meets these specifications:

- **Operating System:**
  - Windows 10/11 (64-bit)
  - macOS 12 Monterey or higher (Apple Silicon M1/M2/M3/M4 or Intel)
  - Linux (Ubuntu 20.04+, Fedora 34+, or 64-bit equivalent)
- **RAM:** Minimum 8 GB (16 GB or more strongly recommended for smooth compilation and emulator performance).
- **Disk Space:** At least 15 GB of free SSD storage (for Android Studio, Android SDK platforms, and Gradle build caches).
- **Java Development Kit (JDK):** **JDK 17** or **JDK 21**. Modern versions of Android Studio bundle their own verified JetBrains Runtime (JBR), so you do not need to install an external JDK manually.

---

## 2. Downloading and Installing Android Studio

1. Visit the official developer portal: [developer.android.com/studio](https://developer.android.com/studio).
2. Download the latest stable release:
   - **Recommended:** **Android Studio Ladybug (2024.2.1+)**, **Koala**, or **Iguana**.
3. Run the installer and ensure all default components are checked:
   - *Android Studio*
   - *Android SDK*
   - *Android Virtual Device (AVD)*
4. Complete the installation wizard and launch Android Studio.

---

## 3. Android SDK & Build Tools Setup

MoneyMap targets Android 16 (API Level 36) with backward compatibility to Android 7.0 (API Level 24).

1. In Android Studio, navigate to the SDK Manager:
   - On the Welcome Screen: Click **More Actions > SDK Manager**.
   - Inside an open project: Go to **Tools > SDK Manager** or **Settings / Preferences > Appearance & Behavior > System Settings > Android SDK**.
2. Under the **SDK Platforms** tab:
   - Check the box for **Android 16.0 ("VanillaIceCream" / API Level 36)**.
   - Click **Apply** and wait for the download to finish.
3. Under the **SDK Tools** tab:
   - Ensure the following tools are installed:
     - **Android SDK Build-Tools 35.x / 36.x**
     - **Android SDK Platform-Tools**
     - **Android SDK Command-line Tools (latest)**
     - **Android Emulator**
4. Click **OK** to apply changes.

---

## 4. Extracting the Source Project

1. Locate the downloaded MoneyMap archive (`MoneyMap_Source.zip`).
2. Right-click and extract the archive to a standard directory.
   - **Windows:** Extract to a short path such as `C:\Projects\MoneyMap`.
     > ⚠️ **Warning for Windows:** Do NOT extract deeply into nested folders like `C:\Users\username\Downloads\Compressed\Folder\Subfolder\MoneyMap`. Windows has a 260-character maximum path limit (`MAX_PATH`) that can cause Gradle file lock and build failures.
   - **macOS / Linux:** Extract to `~/Projects/MoneyMap`.
3. Verify that the extracted folder directly contains:
   ```
   MoneyMap/
   ├── app/
   ├── gradle/
   ├── build.gradle.kts
   ├── settings.gradle.kts
   ├── gradle.properties
   └── MoneyMap_Documentation/
   ```

---

## 5. Opening the Project in Android Studio

> ⚠️ **CRITICAL STEP:** You must open the **root** folder containing `settings.gradle.kts`, NOT the inner `app` subfolder!

1. Open Android Studio.
2. In the Welcome window, click **Open** (or in the top menu: **File > Open...**).
3. Browse to the extracted `MoneyMap` root folder.
4. Select the `MoneyMap` folder and click **OK** (or **Open**).
5. Android Studio will open the workspace and begin indexing the source files.

---

## 6. Performing Gradle Synchronization

When you open the project for the first time, Android Studio will automatically initiate a **Gradle Sync**.

1. You will see a progress bar at the bottom: `"Gradle: Sync in progress..."` or `"Configuring project :app"`.
2. Gradle will download:
   - Android Gradle Plugin (AGP `9.1.1`)
   - Kotlin compiler plugin (`2.2.10`)
   - Google KSP compiler (`2.3.5`)
   - Google Play Services ML Kit libraries
   - Jetpack Compose dependencies
3. If the sync does not start automatically, click the **Elephant icon** in the top-right toolbar:
   - Menu: **File > Sync Project with Gradle Files**.
4. Once completed, the Build panel at the bottom will display:
   ```
   BUILD SUCCESSFUL in Xm Ys
   ```

---

## 7. Resolving Missing SDK & Dependency Prompts

If you encounter any blue hyperlinks in the **Build / Sync** panel:

- **"Install missing platform(s) and sync project"**:
  Click the link. Android Studio will automatically install Android SDK Platform 36 and re-sync.
- **"Install Build Tools"**:
  Click the link to download the matching build tools.
- **"Upgrade Gradle wrapper"**:
  Do **NOT** upgrade unless prompted by an explicit incompatibility. The project is pre-configured with the exact required Gradle distribution.

---

## 8. Connecting an Android Device (USB Debugging)

To test MoneyMap on a real Android smartphone or tablet:

1. **Enable Developer Options on your phone:**
   - Go to your phone's **Settings > About Phone**.
   - Tap on **Build Number** 7 times continuously until you see the prompt: *"You are now a developer!"*
2. **Enable USB Debugging:**
   - Go to **Settings > System > Developer Options** (or **Settings > Additional Settings > Developer Options**).
   - Toggle **USB Debugging** to **ON**.
3. **Connect to Computer:**
   - Connect your phone to your computer using a high-quality USB data cable.
   - On your phone, you will see a prompt: *"Allow USB debugging from this computer?"*
   - Check **Always allow from this computer** and tap **Allow**.
4. In Android Studio, check the device dropdown in the top toolbar. Your phone's model name will appear in the device list.

---

## 9. Setting up an Android Virtual Device (Emulator)

If you do not have a physical device available:

1. In Android Studio, open the **Device Manager**:
   - Menu: **Tools > Device Manager** (or click the phone/tablet icon in the right sidebar).
2. Click **Create Virtual Device** (or the **+** button).
3. Select a hardware profile: **Phone > Pixel 8** or **Pixel 7**. Click **Next**.
4. Under the **System Image** list:
   - Select **VanillaIceCream (API 36)** or **UpsideDownCake (API 34)**.
   - If not downloaded, click the download icon next to the release name.
5. Click **Next**, keep default settings, and click **Finish**.
6. Click the green **Play** triangle next to your new virtual device to start the emulator.

---

## 10. Running MoneyMap on Your Device

1. Look at the top toolbar in Android Studio:
   - **Configuration:** Ensure the dropdown displays **`app`**.
   - **Target Device:** Select your physical device or running emulator.
2. Click the green **Run (Play)** button (or press `Shift + F10`).
3. The Gradle build will execute:
   - Compiling Kotlin and Compose code.
   - Processing Room entity schemas with KSP.
   - Packaging resources into a debug APK.
   - Installing the APK onto your device.
4. MoneyMap will automatically open on your screen with the splash screen and executive dashboard.

---

## 11. Generating a Debug APK

If you want an APK file to test or share with testers on their own phones without running Android Studio:

### Method A: Via Android Studio Menu
1. In the top menu, go to:
   ```
   Build > Build Bundle(s) / APK(s) > Build APK(s)
   ```
2. Android Studio will assemble the debug package.
3. Once finished, a notification popup will appear in the bottom-right corner:
   ```
   APK(s) generated successfully for 1 module:
   locate
   ```
4. Click the blue **locate** link to open the folder in your file explorer.

### Method B: Via Terminal / Command Line
1. Open the **Terminal** tab at the bottom of Android Studio.
2. Run the Gradle assemble task:
   - **Windows:**
     ```cmd
     gradlew.bat assembleDebug
     ```
   - **macOS / Linux:**
     ```bash
     ./gradlew assembleDebug
     ```

---

## 12. Locating Your Generated APK

Your compiled debug APK is always saved at the following relative path inside the project root:

```
app/build/outputs/apk/debug/app-debug.apk
```

You can transfer this `app-debug.apk` file to any Android device running Android 7.0 (API 24) or higher, tap the file in the device's file manager, and install it directly!

---

*Next Step: To rebrand the app, change the package name, or customize colors and currencies, proceed to **`CONFIGURATION_GUIDE.md`**.*
