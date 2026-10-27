package com.example.security

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.data.preferences.dataStore
import kotlinx.coroutines.flow.first
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PinSecurityManager(private val context: Context) {

    private object Keys {
        val PIN_SALT = stringPreferencesKey("pin_salt_hex")
        val PIN_HASH = stringPreferencesKey("pin_hash_hex")
    }

    suspend fun hasPinSetup(): Boolean {
        val prefs = context.dataStore.data.first()
        val salt = prefs[Keys.PIN_SALT]
        val hash = prefs[Keys.PIN_HASH]
        return !salt.isNullOrBlank() && !hash.isNullOrBlank()
    }

    suspend fun isPinSet(): Boolean = hasPinSetup()

    suspend fun savePin(pin: String): Boolean {
        if (pin.length != 6) return false
        val salt = generateSalt()
        val hash = hashPin(pin, salt)
        context.dataStore.edit {
            it[Keys.PIN_SALT] = bytesToHex(salt)
            it[Keys.PIN_HASH] = bytesToHex(hash)
        }
        return true
    }

    suspend fun verifyPin(enteredPin: String): Boolean {
        if (enteredPin.length != 6) return false
        val prefs = context.dataStore.data.first()
        val saltHex = prefs[Keys.PIN_SALT] ?: return false
        val storedHashHex = prefs[Keys.PIN_HASH] ?: return false

        val salt = hexToBytes(saltHex)
        val computedHash = hashPin(enteredPin, salt)
        val computedHashHex = bytesToHex(computedHash)

        return constantTimeEquals(storedHashHex, computedHashHex)
    }

    suspend fun clearPin() {
        context.dataStore.edit {
            it.remove(Keys.PIN_SALT)
            it.remove(Keys.PIN_HASH)
        }
    }

    private fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val iterations = 10000
        val keyLength = 256
        val spec: KeySpec = PBEKeySpec(pin.toCharArray(), salt, iterations, keyLength)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        val hexArray = "0123456789ABCDEF".toCharArray()
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = hexArray[v ushr 4]
            hexChars[i * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }
}
