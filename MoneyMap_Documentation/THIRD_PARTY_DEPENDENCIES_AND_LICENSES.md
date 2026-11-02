# MoneyMap — Third-Party Dependencies & Licenses

This document catalogs all external libraries, build plugins, and dependencies used by the **MoneyMap** Android application. All versions match the project's central Version Catalog (`gradle/libs.versions.toml`) and `app/build.gradle.kts`.

---

## 📋 Dependency Summary Table

| Library / Component | Group & Artifact | Version | Purpose | License | Official Project URL |
|---|---|---|---|---|---|
| **AndroidX Core KTX** | `androidx.core:core-ktx` | `1.18.0` | Android Kotlin extensions & compatibility helpers | Apache 2.0 | [developer.android.com](https://developer.android.com/jetpack/androidx/releases/core) |
| **AndroidX Activity Compose** | `androidx.activity:activity-compose` | `1.10.1` | Jetpack Compose integration for Android Activities | Apache 2.0 | [developer.android.com](https://developer.android.com/jetpack/androidx/releases/activity) |
| **Jetpack Compose BOM** | `androidx.compose:compose-bom` | `2024.09.00` | Aligned version bill-of-materials for Jetpack Compose | Apache 2.0 | [developer.android.com](https://developer.android.com/jetpack/compose/bom) |
| **Compose UI** | `androidx.compose.ui:ui` | `BOM (2024.09.00)` | Core Compose layout, drawing, and input components | Apache 2.0 | [developer.android.com](https://developer.android.com/jetpack/compose) |
| **Compose Material 3** | `androidx.compose.material3:material3` | `BOM (2024.09.00)` | Material Design 3 UI components (Cards, Surfaces, Scaffolds) | Apache 2.0 | [developer.android.com](https://developer.android.com/jetpack/compose/material3) |
| **Compose Extended Icons** | `androidx.compose.material:material-icons-extended` | `BOM (2024.09.00)` | Material Symbols icon library for navigation & categories | Apache 2.0 | [developer.android.com](https://developer.android.com/jetpack/compose/material) |
| **Navigation Compose** | `androidx.navigation:navigation-compose` | `2.8.9` | Single-activity screen navigation and backstack handling | Apache 2.0 | [developer.android.com](https://developer.android.com/jetpack/androidx/releases/navigation) |
| **Lifecycle & ViewModel Compose** | `androidx.lifecycle:lifecycle-viewmodel-compose` | `2.8.7` | ViewModel lifecycle awareness in Compose | Apache 2.0 | [developer.android.com](https://developer.android.com/jetpack/androidx/releases/lifecycle) |
| **Lifecycle Runtime Compose** | `androidx.lifecycle:lifecycle-runtime-compose` | `2.8.7` | `collectAsStateWithLifecycle` for safe UI state collection | Apache 2.0 | [developer.android.com](https://developer.android.com/jetpack/androidx/releases/lifecycle) |
| **Room Runtime & KTX** | `androidx.room:room-runtime`, `room-ktx` | `2.7.0` | SQLite ORM database, migrations, and reactive Flow queries | Apache 2.0 | [developer.android.com](https://developer.android.com/jetpack/androidx/releases/room) |
| **Room Compiler (KSP)** | `androidx.room:room-compiler` | `2.7.0` | Annotation processor generating SQLite queries at compile time | Apache 2.0 | [developer.android.com](https://developer.android.com/jetpack/androidx/releases/room) |
| **DataStore Preferences** | `androidx.datastore:datastore-preferences` | `1.1.7` | Async key-value preferences storage replacing SharedPreferences | Apache 2.0 | [developer.android.com](https://developer.android.com/jetpack/androidx/releases/datastore) |
| **AndroidX Biometric** | `androidx.biometric:biometric` | `1.1.0` | BiometricPrompt (fingerprint and face unlock) | Apache 2.0 | [developer.android.com](https://developer.android.com/jetpack/androidx/releases/biometric) |
| **Google ML Kit Text Recognition** | `com.google.android.gms:play-services-mlkit-text-recognition` | `19.0.1` | On-device optical character recognition for payment screenshots | Android SDK Terms / Google APIs | [developers.google.com/ml-kit](https://developers.google.com/ml-kit/vision/text-recognition/android) |
| **Coil Compose** | `io.coil-kt:coil-compose` | `2.7.0` | Kotlin-first asynchronous image loader for receipt thumbnails | Apache 2.0 | [coil-kt.github.io/coil](https://coil-kt.github.io/coil/) |
| **Kotlinx Coroutines** | `org.jetbrains.kotlinx:kotlinx-coroutines-android` | `1.10.2` | Concurrency, background workers, and asynchronous streams | Apache 2.0 | [github.com/Kotlin/kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) |
| **Moshi & Codegen** | `com.squareup.moshi:moshi-kotlin` | `1.15.2` | JSON serialization and deserialization library | Apache 2.0 | [github.com/square/moshi](https://github.com/square/moshi) |
| **Retrofit & OkHttp** | `com.squareup.retrofit2:retrofit`, `com.squareup.okhttp3:okhttp` | `2.12.0` / `4.10.0` | Networking client & HTTP protocol engine | Apache 2.0 | [square.github.io/retrofit](https://square.github.io/retrofit/) |
| **Plus Jakarta Sans Font** | Local TrueType Font asset (`plus_jakarta_sans.ttf`) | Version 2.6 | Clean, modern sans-serif typography | SIL Open Font License (OFL 1.1) | [fonts.google.com/specimen/Plus+Jakarta+Sans](https://fonts.google.com/specimen/Plus+Jakarta+Sans) |

---

## 🔍 Note on Cloud & Ad Services in Source Code

To guarantee buyers a clean, production-ready codebase:

1. **Advertising (AdMob):**
   - The project is **100% ad-free**. There are no banner, interstitial, or rewarded ads loaded in the application.
   - The dependency `libs.play.services.ads` is commented out in `app/build.gradle.kts`.
2. **Firebase Firestore & Authentication:**
   - MoneyMap operates **100% offline-first**. All transactions, accounts, and budgets are stored in the local SQLite database.
   - The dependencies `libs.firebase.firestore` and `libs.firebase.auth` are commented out in `app/build.gradle.kts`.
3. **Google Play Services ML Kit:**
   - Uses the Latin script recognition model, which processes images entirely on the CPU/GPU of the local Android device without transmitting images to external cloud endpoints.

---

## 📄 License Texts & Attributions

Most open-source dependencies in MoneyMap are distributed under the **Apache License, Version 2.0**:

```
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

The **Plus Jakarta Sans** font is licensed under the **SIL Open Font License, Version 1.1**:
```
This Font Software is licensed under the SIL Open Font License, Version 1.1.
This license allows the font to be used, studied, modified and redistributed freely.
```
