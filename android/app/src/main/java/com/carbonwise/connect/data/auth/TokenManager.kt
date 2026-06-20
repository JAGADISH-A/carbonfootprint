package com.carbonwise.connect.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveTokens(deviceToken: String, refreshToken: String, deviceId: String, expiresInSeconds: Long) {
        val expiryTimeMillis = System.currentTimeMillis() + (expiresInSeconds * 1000)
        sharedPreferences.edit()
            .putString("device_token", deviceToken)
            .putString("refresh_token", refreshToken)
            .putString("device_id", deviceId)
            .putBoolean("is_paired", true)
            .putLong("token_expiry", expiryTimeMillis)
            .apply()
    }

    fun getDeviceToken(): String? = sharedPreferences.getString("device_token", null)

    fun getRefreshToken(): String? = sharedPreferences.getString("refresh_token", null)

    fun getDeviceId(): String? = sharedPreferences.getString("device_id", null)

    fun saveDeviceId(deviceId: String) {
        sharedPreferences.edit()
            .putString("device_id", deviceId)
            .apply()
    }

    fun isPaired(): Boolean = sharedPreferences.getBoolean("is_paired", false)

    fun getTokenExpiry(): Long = sharedPreferences.getLong("token_expiry", 0)

    fun clearTokens() {
        sharedPreferences.edit().clear().apply()
    }

    fun hasValidTokens(): Boolean {
        return getDeviceToken() != null && getRefreshToken() != null
    }

    fun isTokenExpired(): Boolean {
        val expiry = getTokenExpiry()
        if (expiry == 0L) return true
        // Consider token expired if it expires within the next 60 seconds
        return System.currentTimeMillis() > (expiry - 60000)
    }
}
