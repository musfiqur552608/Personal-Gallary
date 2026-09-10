package com.freedu.personalgallary.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.freedu.personalgallary.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Password-based encrypted backup (AES-256-GCM + PBKDF2) of app metadata.
 * File format: salt(16) + iv(12) + ciphertext. Extension .pgbak
 */
object CryptoBackup {

    private const val ITERATIONS = 100_000
    private const val EXT = ".pgbak"

    /** Encrypts [plain] with password, returns salt+iv+ciphertext bytes. */
    fun encrypt(password: String, plain: ByteArray): ByteArray {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val key = derive(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        val ct = cipher.doFinal(plain)
        return salt + iv + ct
    }

    /** Returns plaintext or null on wrong password / corruption. */
    fun decrypt(password: String, blob: ByteArray): ByteArray? {
        return try {
            if (blob.size < 29) return null
            val salt = blob.copyOfRange(0, 16)
            val iv = blob.copyOfRange(16, 28)
            val ct = blob.copyOfRange(28, blob.size)
            val key = derive(password, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
            cipher.doFinal(ct)
        } catch (_: Exception) {
            null
        }
    }

    private fun derive(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded.also { spec.clearPassword() }
    }

    suspend fun writeBackupFile(context: Context, name: String, blob: ByteArray): Uri? =
        withContext(Dispatchers.IO) {
            try {
                val fileName = if (name.endsWith(EXT)) name else name + EXT
                val values = ContentValues().apply {
                    put(MediaStore.Files.FileColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.Files.FileColumns.MIME_TYPE, "application/octet-stream")
                    if (Build.VERSION.SDK_INT >= 29) {
                        put(
                            MediaStore.Files.FileColumns.RELATIVE_PATH,
                            Environment.DIRECTORY_DOCUMENTS + "/PersonalGallary"
                        )
                        put(MediaStore.Files.FileColumns.IS_PENDING, 1)
                    }
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(
                    MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL), values
                ) ?: return@withContext null
                resolver.openOutputStream(uri)?.use { it.write(blob) }
                    ?: run { resolver.delete(uri, null, null); return@withContext null }
                if (Build.VERSION.SDK_INT >= 29) {
                    resolver.update(
                        uri,
                        ContentValues().apply {
                            put(MediaStore.Files.FileColumns.IS_PENDING, 0)
                        },
                        null, null
                    )
                }
                uri
            } catch (_: Exception) {
                null
            }
        }

    fun defaultName(): String =
        "PG_BACKUP_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
}
