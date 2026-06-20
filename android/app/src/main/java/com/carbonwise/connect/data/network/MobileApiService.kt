package com.carbonwise.connect.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class DeviceRegistrationRequest(
    val pairingCode: String,
    val deviceId: String,
    val deviceName: String,
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val appVersion: String,
    val notificationPermission: Boolean,
    val smsPermission: Boolean,
    val syncEnabled: Boolean
)

data class PairingCodeResponse(
    val pairingCode: String,
    val expiresAt: String
)

data class TokenResponse(
    val deviceToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long
)

data class RefreshTokenRequest(
    val refreshToken: String
)

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T?,
    val error: Any?
)

interface MobileApiService {
    @POST("api/v1/mobile/pair")
    suspend fun pairDevice(@Body request: DeviceRegistrationRequest): Response<ApiResponse<TokenResponse>>

    @POST("api/v1/mobile/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<ApiResponse<TokenResponse>>
    
    @POST("api/v1/mobile/logout")
    suspend fun logout(): Response<ApiResponse<Void>>
}
