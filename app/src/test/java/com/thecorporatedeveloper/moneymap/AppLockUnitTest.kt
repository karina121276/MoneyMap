package com.thecorporatedeveloper.moneymap

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.thecorporatedeveloper.moneymap.data.preferences.AppPreferences
import com.thecorporatedeveloper.moneymap.data.preferences.UserPreferences
import com.thecorporatedeveloper.moneymap.security.PinSecurityManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppLockUnitTest {

    private lateinit var context: Context
    private lateinit var pinSecurityManager: PinSecurityManager
    private lateinit var appPreferences: AppPreferences

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext<Context>()
        pinSecurityManager = PinSecurityManager(context)
        appPreferences = AppPreferences(context)
        pinSecurityManager.clearPin()
    }

    @Test
    fun `pin is initially not set on fresh installation`() = runBlocking {
        assertFalse("PIN should not be set on fresh install", pinSecurityManager.isPinSet())
    }

    @Test
    fun `save pin stores salted PBKDF2 verifier and authenticates valid pin`() = runBlocking {
        val testPin = "123456"
        pinSecurityManager.savePin(testPin)

        assertTrue("PIN should be reported as set after saving", pinSecurityManager.isPinSet())
        assertTrue("Valid PIN should verify successfully", pinSecurityManager.verifyPin(testPin))
        assertFalse("Incorrect PIN should fail verification", pinSecurityManager.verifyPin("654321"))
        assertFalse("Partial PIN should fail verification", pinSecurityManager.verifyPin("12345"))
    }

    @Test
    fun `clearing pin removes stored verifier`() = runBlocking {
        pinSecurityManager.savePin("987654")
        assertTrue(pinSecurityManager.isPinSet())

        pinSecurityManager.clearPin()
        assertFalse("PIN should not be set after clearing", pinSecurityManager.isPinSet())
        assertFalse("Cleared PIN should fail verification", pinSecurityManager.verifyPin("987654"))
    }

    @Test
    fun `app lock preference defaults to disabled and onboarding defaults to completed`() = runBlocking {
        val prefs = appPreferences.userPreferencesFlow.first()
        assertFalse("App lock should default to disabled", prefs.appLockEnabled)
        assertTrue("Onboarding should default to completed (onboarding removed)", prefs.hasCompletedOnboarding)
        assertFalse("Biometrics should default to disabled", prefs.biometricsEnabled)
        assertFalse("Device credential should default to disabled", prefs.deviceCredentialEnabled)
        assertEquals("Auto-lock default timeout should be 1 minute", 1, prefs.autoLockMinutes)
    }

    @Test
    fun `toggling app lock settings persists correctly`() = runBlocking {
        appPreferences.setAppLockEnabled(true)
        appPreferences.setBiometricsEnabled(true)
        appPreferences.setDeviceCredentialEnabled(true)
        appPreferences.setAutoLockMinutes(5)

        val updated = appPreferences.userPreferencesFlow.first()
        assertTrue(updated.appLockEnabled)
        assertTrue(updated.biometricsEnabled)
        assertTrue(updated.deviceCredentialEnabled)
        assertEquals(5, updated.autoLockMinutes)
    }
}
