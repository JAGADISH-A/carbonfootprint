package com.carbonwise.connect.data.network

import com.carbonwise.connect.data.auth.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
    // Use Provider to avoid circular dependency since AuthInterceptor is used in Retrofit which provides ApiService
    private val apiServiceProvider: Provider<MobileApiService>
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        // Don't intercept token refresh or pair endpoints
        val path = request.url.encodedPath
        if (path.contains("/mobile/pair") || path.contains("/mobile/refresh") || path.contains("/mobile/pairing/generate")) {
            return chain.proceed(request)
        }

        val deviceToken = tokenManager.getDeviceToken()
        val builder = request.newBuilder()
        
        if (deviceToken != null) {
            builder.addHeader("Authorization", "Bearer $deviceToken")
        }
        
        var response = chain.proceed(builder.build())
        
        if (response.code == 401 && deviceToken != null) {
            response.close()
            
            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken != null) {
                // Try to refresh token synchronously
                val refreshSuccessful = refreshTokenSync(refreshToken)
                if (refreshSuccessful) {
                    val newDeviceToken = tokenManager.getDeviceToken()
                    if (newDeviceToken != null) {
                        val newRequest = request.newBuilder()
                            .addHeader("Authorization", "Bearer $newDeviceToken")
                            .build()
                        response = chain.proceed(newRequest)
                    }
                } else {
                    // Refresh failed (e.g., expired), force logout
                    tokenManager.clearTokens()
                    // Optional: Broadcast event to UI to navigate to Welcome/Pairing screen
                }
            }
        }
        
        return response
    }
    
    private fun refreshTokenSync(refreshToken: String): Boolean {
        return try {
            runBlocking {
                val apiService = apiServiceProvider.get()
                val response = apiService.refreshToken(RefreshTokenRequest(refreshToken))
                if (response.isSuccessful && response.body()?.data != null) {
                    val tokens = response.body()!!.data!!
                    val deviceId = tokenManager.getDeviceId() ?: ""
                    tokenManager.saveTokens(tokens.deviceToken, tokens.refreshToken, deviceId, tokens.expiresInSeconds)
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }
}
