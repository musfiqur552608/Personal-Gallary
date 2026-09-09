package com.freedu.personalgallary.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest

/**
 * PIN storage using EncryptedSharedPreferences. Only a SHA-256 hash is kept.
 */
class LockRepository(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "lock_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun hasPin(): Boolean = prefs.contains(KEY_PIN)

    fun setPin(pin: String) {
        prefs.edit().putString(KEY_PIN, sha256(pin)).apply()
    }

    fun verifyPin(pin: String): Boolean {
        val stored = prefs.getString(KEY_PIN, null) ?: return false
        return stored == sha256(pin)
    }

    fun clearPin() {
        prefs.edit().remove(KEY_PIN).apply()
    }

    /** Decoy PIN opens a limited innocuous library (favorites/vault/trash hidden). */
    fun hasDecoy(): Boolean = prefs.contains(KEY_DECOY)

    fun setDecoyPin(pin: String) {
        prefs.edit().putString(KEY_DECOY, sha256(pin)).apply()
    }

    fun verifyDecoy(pin: String): Boolean {
        val stored = prefs.getString(KEY_DECOY, null) ?: return false
        return stored == sha256(pin)
    }

    fun clearDecoy() {
        prefs.edit().remove(KEY_DECOY).apply()
    }

    private fun sha256(s: String): String {
        val d = MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
        return d.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_PIN = "pin_hash"
        private const val KEY_DECOY = "decoy_hash"
    }
}
