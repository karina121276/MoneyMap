package com.thecorporatedeveloper.moneymap.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "moneymap_preferences")

data class UserPreferences(
    val hasCompletedOnboarding: Boolean = true,
    val userName: String = "",
    val themeMode: String = "LIGHT", // "LIGHT", "DARK", "SYSTEM"
    val accentColorHex: String = "#1E56F0",
    val hideAmounts: Boolean = false,
    val appLockEnabled: Boolean = false,
    val biometricsEnabled: Boolean = false,
    val deviceCredentialEnabled: Boolean = false,
    val autoLockMinutes: Int = 1,
    val blockScreenshots: Boolean = false,
    val widgetTheme: String = "LIGHT",
    val widgetHideAmounts: Boolean = false,
    val firstDayOfWeek: String = "Monday",
    val dateFormat: String = "dd MMM yyyy",
    val defaultAccountId: Long = 0L,
    val quickAmounts: String = "100,500,1000,2000,5000",
    val notificationHour: Int = 9,
    val notificationMinute: Int = 0,
    val currency: String = "INR"
) {
    val quickAmountsList: List<Long>
        get() {
            return quickAmounts.split(",")
                .mapNotNull { it.trim().toLongOrNull() }
                .filter { it > 0 }
                .distinct()
                .ifEmpty { listOf(100L, 500L, 1000L, 2000L, 5000L) }
        }
}

class AppPreferences(private val context: Context) {

    private object Keys {
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val USER_NAME = stringPreferencesKey("user_name")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")

        val HIDE_AMOUNTS = booleanPreferencesKey("hide_amounts")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val BIOMETRICS_ENABLED = booleanPreferencesKey("biometrics_enabled")
        val DEVICE_CREDENTIAL_ENABLED = booleanPreferencesKey("device_credential_enabled")
        val AUTO_LOCK_MINUTES = intPreferencesKey("auto_lock_minutes")
        val BLOCK_SCREENSHOTS = booleanPreferencesKey("block_screenshots")
        val WIDGET_THEME = stringPreferencesKey("widget_theme")
        val WIDGET_HIDE_AMOUNTS = booleanPreferencesKey("widget_hide_amounts")
        val FIRST_DAY_OF_WEEK = stringPreferencesKey("first_day_of_week")
        val DATE_FORMAT = stringPreferencesKey("date_format")
        val DEFAULT_ACCOUNT_ID = longPreferencesKey("default_account_id")
        val QUICK_AMOUNTS = stringPreferencesKey("quick_amounts")
        val NOTIFICATION_HOUR = intPreferencesKey("notification_hour")
        val NOTIFICATION_MINUTE = intPreferencesKey("notification_minute")
        val CURRENCY = stringPreferencesKey("currency")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserPreferences(
                hasCompletedOnboarding = true, // Onboarding removed
                userName = preferences[Keys.USER_NAME] ?: "",
                themeMode = preferences[Keys.THEME_MODE] ?: "LIGHT",
                accentColorHex = preferences[Keys.ACCENT_COLOR] ?: "#1E56F0",
                hideAmounts = preferences[Keys.HIDE_AMOUNTS] ?: false,
                appLockEnabled = preferences[Keys.APP_LOCK_ENABLED] ?: false,
                biometricsEnabled = preferences[Keys.BIOMETRICS_ENABLED] ?: false,
                deviceCredentialEnabled = preferences[Keys.DEVICE_CREDENTIAL_ENABLED] ?: false,
                autoLockMinutes = preferences[Keys.AUTO_LOCK_MINUTES] ?: 1,
                blockScreenshots = preferences[Keys.BLOCK_SCREENSHOTS] ?: false,
                widgetTheme = preferences[Keys.WIDGET_THEME] ?: "LIGHT",
                widgetHideAmounts = preferences[Keys.WIDGET_HIDE_AMOUNTS] ?: false,
                firstDayOfWeek = preferences[Keys.FIRST_DAY_OF_WEEK] ?: "Monday",
                dateFormat = preferences[Keys.DATE_FORMAT] ?: "dd MMM yyyy",
                defaultAccountId = preferences[Keys.DEFAULT_ACCOUNT_ID] ?: 0L,
                quickAmounts = preferences[Keys.QUICK_AMOUNTS] ?: "100,500,1000,2000,5000",
                notificationHour = preferences[Keys.NOTIFICATION_HOUR] ?: 9,
                notificationMinute = preferences[Keys.NOTIFICATION_MINUTE] ?: 0,
                currency = preferences[Keys.CURRENCY] ?: "INR"
            )
        }

    // ... (rest of methods)



    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setAccentColor(hex: String) {
        context.dataStore.edit { it[Keys.ACCENT_COLOR] = hex }
    }

    suspend fun setHideAmounts(hide: Boolean) {
        context.dataStore.edit { it[Keys.HIDE_AMOUNTS] = hide }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.APP_LOCK_ENABLED] = enabled }
    }

    suspend fun setBiometricsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRICS_ENABLED] = enabled }
    }

    suspend fun setDeviceCredentialEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DEVICE_CREDENTIAL_ENABLED] = enabled }
    }

    suspend fun setAutoLockMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.AUTO_LOCK_MINUTES] = minutes }
    }

    suspend fun setBlockScreenshots(block: Boolean) {
        context.dataStore.edit { it[Keys.BLOCK_SCREENSHOTS] = block }
    }

    suspend fun setWidgetTheme(theme: String) {
        context.dataStore.edit { it[Keys.WIDGET_THEME] = theme }
    }

    suspend fun setWidgetHideAmounts(hide: Boolean) {
        context.dataStore.edit { it[Keys.WIDGET_HIDE_AMOUNTS] = hide }
    }

    suspend fun setDefaultAccountId(accountId: Long) {
        context.dataStore.edit { it[Keys.DEFAULT_ACCOUNT_ID] = accountId }
    }

    suspend fun setQuickAmounts(amounts: List<Long>) {
        val str = amounts.filter { it > 0 }.distinct().joinToString(",")
        context.dataStore.edit { it[Keys.QUICK_AMOUNTS] = str }
    }

    suspend fun setNotificationTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.NOTIFICATION_HOUR] = hour
            it[Keys.NOTIFICATION_MINUTE] = minute
        }
    }

    suspend fun setCurrency(code: String) {
        context.dataStore.edit { it[Keys.CURRENCY] = code }
    }
}
