package com.example.crypto.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore System cryptographic manager.
 * Stores Binance API Credentials securely encrypted using hardware-backed AES-256-GCM.
 */
class KeyStoreSecurityManager(private val context: Context) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "NexusTrade_ApiKey_Alias"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val PREFS_NAME = "nexus_trade_secure_vault"
        private const val PREF_KEY_API_KEY = "encrypted_api_key"
        private const val PREF_KEY_API_SECRET = "encrypted_api_secret"
        private const val PREF_KEY_IV_KEY = "iv_api_key"
        private const val PREF_KEY_IV_SECRET = "iv_api_secret"
        private const val PREF_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val PREF_TESTNET_ENABLED = "use_testnet_by_default"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        ensureKeyExists()
    }

    private fun ensureKeyExists() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val parameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(parameterSpec)
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    fun encryptAndSaveCredentials(apiKey: String, apiSecret: String) {
        if (apiKey.isBlank() || apiSecret.isBlank()) {
            clearCredentials()
            return
        }

        try {
            val secretKey = getSecretKey()

            // Encrypt API Key
            val cipherKey = Cipher.getInstance(TRANSFORMATION)
            cipherKey.init(Cipher.ENCRYPT_MODE, secretKey)
            val ivKey = cipherKey.iv
            val encryptedKeyBytes = cipherKey.doFinal(apiKey.toByteArray(Charsets.UTF_8))

            // Encrypt API Secret
            val cipherSecret = Cipher.getInstance(TRANSFORMATION)
            cipherSecret.init(Cipher.ENCRYPT_MODE, secretKey)
            val ivSecret = cipherSecret.iv
            val encryptedSecretBytes = cipherSecret.doFinal(apiSecret.toByteArray(Charsets.UTF_8))

            prefs.edit()
                .putString(PREF_KEY_API_KEY, Base64.encodeToString(encryptedKeyBytes, Base64.NO_WRAP))
                .putString(PREF_KEY_IV_KEY, Base64.encodeToString(ivKey, Base64.NO_WRAP))
                .putString(PREF_KEY_API_SECRET, Base64.encodeToString(encryptedSecretBytes, Base64.NO_WRAP))
                .putString(PREF_KEY_IV_SECRET, Base64.encodeToString(ivSecret, Base64.NO_WRAP))
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getApiKey(): String {
        val encryptedBase64 = prefs.getString(PREF_KEY_API_KEY, null) ?: return ""
        val ivBase64 = prefs.getString(PREF_KEY_IV_KEY, null) ?: return ""
        return decryptString(encryptedBase64, ivBase64)
    }

    fun getApiSecret(): String {
        val encryptedBase64 = prefs.getString(PREF_KEY_API_SECRET, null) ?: return ""
        val ivBase64 = prefs.getString(PREF_KEY_IV_SECRET, null) ?: return ""
        return decryptString(encryptedBase64, ivBase64)
    }

    private fun decryptString(encryptedBase64: String, ivBase64: String): String {
        return try {
            val secretKey = getSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val decryptedBytes = cipher.doFinal(Base64.decode(encryptedBase64, Base64.NO_WRAP))
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun hasCredentials(): Boolean {
        return prefs.contains(PREF_KEY_API_KEY) && prefs.contains(PREF_KEY_API_SECRET)
    }

    fun getMaskedApiKey(): String {
        val key = getApiKey()
        if (key.length <= 8) return if (key.isNotEmpty()) "******" else ""
        return "${key.take(6)}...${key.takeLast(6)}"
    }

    fun isRealMainnet(): Boolean {
        // Defaults to true (Real Binance Mainnet API)
        return prefs.getBoolean("is_real_mainnet", true)
    }

    fun setRealMainnet(isReal: Boolean) {
        prefs.edit().putBoolean("is_real_mainnet", isReal).apply()
    }

    fun clearCredentials() {
        prefs.edit()
            .remove(PREF_KEY_API_KEY)
            .remove(PREF_KEY_IV_KEY)
            .remove(PREF_KEY_API_SECRET)
            .remove(PREF_KEY_IV_SECRET)
            .apply()
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(PREF_BIOMETRIC_ENABLED, false)
    }
}
