package com.carbonwise.connect.data.repository

import com.carbonwise.connect.data.auth.TokenManager
import com.carbonwise.connect.data.network.DeviceRegistrationRequest
import com.carbonwise.connect.data.network.MobileApiService
import com.carbonwise.connect.data.network.RefreshTokenRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthenticationRepository @Inject constructor(
    private val mobileApiService: MobileApiService,
    private val tokenManager: TokenManager
) {

    suspend fun pairDevice(
        pairingCode: String,
        deviceName: String,
        manufacturer: String,
        model: String,
        androidVersion: String,
        appVersion: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val deviceId = tokenManager.getDeviceId() 
                ?: java.util.UUID.randomUUID().toString().also {
                    tokenManager.saveDeviceId(it)
                }

            val request = DeviceRegistrationRequest(
                pairingCode = pairingCode,
                deviceId = deviceId,
                deviceName = deviceName,
                manufacturer = manufacturer,
                model = model,
                androidVersion = androidVersion,
                appVersion = appVersion,
                notificationPermission = true,
                smsPermission = true,
                syncEnabled = true
            )
            
            val response = mobileApiService.pairDevice(request)
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) {
                    tokenManager.saveTokens(
                        deviceToken = data.deviceToken,
                        refreshToken = data.refreshToken,
                        deviceId = deviceId,
                        expiresInSeconds = data.expiresInSeconds
                    )
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Missing token data in response"))
                }
            } else {
                Result.failure(Exception("Pairing failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshToken(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val refreshToken = tokenManager.getRefreshToken()
                ?: return@withContext Result.failure(Exception("No refresh token available"))

            val request = RefreshTokenRequest(refreshToken = refreshToken)
            val response = mobileApiService.refreshToken(request)
            
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) {
                    val currentDeviceId = tokenManager.getDeviceId() ?: ""
                    tokenManager.saveTokens(
                        deviceToken = data.deviceToken,
                        refreshToken = data.refreshToken,
                        deviceId = currentDeviceId,
                        expiresInSeconds = data.expiresInSeconds
                    )
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Missing token data in response"))
                }
            } else {
                Result.failure(Exception("Refresh token failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Best effort call to API
            mobileApiService.logout()
            
            // Clear local tokens regardless of API success
            tokenManager.clearTokens()
            Result.success(Unit)
        } catch (e: Exception) {
            // Still clear tokens on network error
            tokenManager.clearTokens()
            Result.success(Unit)
        }
    }

    suspend fun clearTokens() {
        withContext(Dispatchers.IO) {
            tokenManager.clearTokens()
        }
    }
}
