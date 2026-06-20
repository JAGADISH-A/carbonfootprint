package com.carbonwise.connect.ingestion.notification

import com.carbonwise.connect.data.local.SettingsStore
import com.carbonwise.connect.data.remote.ApiClient
import com.carbonwise.connect.data.model.SyncRequest
import com.carbonwise.connect.data.model.PendingNotification
import com.carbonwise.connect.ingestion.model.QueuedEvent
import com.carbonwise.connect.ingestion.model.UploadResult
import com.carbonwise.connect.ingestion.pipeline.UploadManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import com.carbonwise.connect.data.auth.TokenManager

/**
 * Handles upload of classified notification events to the backend.
 * Single responsibility: translate queued events to API calls and handle responses.
 */
@Singleton
class NotificationUploadManager @Inject constructor(
    private val apiClient: ApiClient,
    private val tokenManager: TokenManager
) : UploadManager {

    override suspend fun upload(events: List<QueuedEvent>): UploadResult {
        if (events.isEmpty()) return UploadResult.Success(0)

        return try {
            val authToken = tokenManager.getDeviceToken() ?: ""
            if (authToken.isEmpty()) {
                return UploadResult.Failure("Not authenticated", retryable = false)
            }

            val deviceId = tokenManager.getDeviceId() ?: ""
            val notifications = events.map { event ->
                PendingNotification(
                    id = event.id.hashCode().toLong(),
                    packageName = event.packageName,
                    title = event.title,
                    text = event.body,
                    timestamp = event.timestamp
                )
            }

            val request = SyncRequest(
                deviceId = deviceId,
                notifications = notifications,
                sms = emptyList(),
                timestamp = System.currentTimeMillis().toString()
            )

            val response = apiClient.apiService.sync(
                authHeader = "Bearer $authToken",
                request = request
            )

            if (response.isSuccessful && response.body()?.success == true) {
                UploadResult.Success(events.size)
            } else {
                val errorMsg = response.body()?.toString() ?: "Upload failed"
                UploadResult.Failure(errorMsg, retryable = true)
            }
        } catch (e: Exception) {
            UploadResult.Failure(
                error = e.localizedMessage ?: "Network error",
                retryable = true
            )
        }
    }

    override suspend fun uploadSingle(event: QueuedEvent): UploadResult {
        return upload(listOf(event))
    }
}
