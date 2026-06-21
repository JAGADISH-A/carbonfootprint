package com.carbonwise.connect.data.repository

import android.util.Log
import com.carbonwise.connect.data.local.SettingsStore
import com.carbonwise.connect.data.model.BatchSyncItemRequest
import com.carbonwise.connect.data.model.BatchSyncRequest
import com.carbonwise.connect.data.queue.PendingActivityRepository
import com.carbonwise.connect.data.remote.ApiClient
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import com.carbonwise.connect.data.auth.TokenManager
import com.carbonwise.connect.data.model.ApiResponse
import com.carbonwise.connect.data.model.BatchSyncResponse

data class SyncBatchResult(
    val successCount: Int,
    val failedCount: Int
)

@Singleton
class UploadRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val settingsStore: SettingsStore,
    private val pendingActivityRepository: PendingActivityRepository,
    private val tokenManager: TokenManager
) {
    companion object {
        private const val TAG = "UploadRepo"
    }

    suspend fun syncBatch(syncSessionId: String): ApiResult<SyncBatchResult> {
        var processingIds = emptyList<String>()
        return try {
            val authToken = tokenManager.getDeviceToken() ?: ""
            if (authToken.isEmpty()) {
                return ApiResult.Error("Not authenticated")
            }

            val deviceId = tokenManager.getDeviceId() ?: ""
            if (deviceId.isEmpty()) {
                return ApiResult.Error("Device ID not found")
            }

            val pendingActivities = pendingActivityRepository.getEligibleForSync(maxRetries = 5)
            
            if (pendingActivities.isEmpty()) {
                val timestamp = System.currentTimeMillis()
                settingsStore.setLastSuccessfulUploadTimestamp(timestamp)
                return ApiResult.Success(SyncBatchResult(0, 0))
            }

            processingIds = pendingActivities.map { it.id }
            pendingActivityRepository.updateSyncStatus(processingIds, "SYNCING")

            val itemRequests = pendingActivities.map { activity ->
                BatchSyncItemRequest(
                    id = activity.id,
                    sender = activity.sender,
                    messageBody = activity.messageBody,
                    receivedTimestamp = activity.receivedTimestamp,
                    normalizedMerchant = activity.normalizedMerchant,
                    category = activity.category,
                    source = activity.source.name,
                    rawHash = activity.rawHash,
                    ingestionVersion = activity.ingestionVersion
                )
            }

            val request = BatchSyncRequest(
                deviceId = deviceId,
                syncSessionId = syncSessionId,
                items = itemRequests
            )

            val response = apiClient.apiService.syncBatch(
                authHeader = "Bearer $authToken",
                request = request
            )

            val envelope = response.body()
            if (response.isSuccessful) {
                if (envelope == null) {
                    return ApiResult.Error("Response envelope is null")
                }

                val success = envelope.success
                val data = envelope.data
                val results = data?.results

                if (!success || data == null || results == null) {
                    val errMsg = envelope.message ?: "Invalid backend response"
                    return ApiResult.Error(errMsg)
                }

                val successfulIds = mutableListOf<String>()
                val failedIds = mutableListOf<String>()

                for (result in results) {
                    if (result.status == "SUCCESS") {
                        successfulIds.add(result.id)
                    } else {
                        failedIds.add(result.id)
                    }
                }

                if (successfulIds.isNotEmpty()) {
                    pendingActivityRepository.updateSyncStatus(successfulIds, "SYNCED")
                }

                if (failedIds.isNotEmpty()) {
                    pendingActivityRepository.incrementRetryCountAndFail(failedIds)
                    
                    val permanentlyFailed = pendingActivities
                        .filter { failedIds.contains(it.id) && (it.retryCount + 1) >= 5 }
                        .map { it.id }
                        
                    if (permanentlyFailed.isNotEmpty()) {
                        pendingActivityRepository.markPermanentFailure(permanentlyFailed)
                    }
                }

                val timestamp = System.currentTimeMillis()
                settingsStore.setLastSuccessfulUploadTimestamp(timestamp)

                if (failedIds.isEmpty()) {
                    ApiResult.Success(SyncBatchResult(successfulIds.size, 0))
                } else {
                    ApiResult.Success(SyncBatchResult(successfulIds.size, failedIds.size))
                }
            } else {
                Log.w(TAG, "Batch sync failed (${response.code()})")
                pendingActivityRepository.incrementRetryCountAndFail(processingIds)
                ApiResult.Error("Sync failed (${response.code()})")
            }
        } catch (e: com.google.gson.JsonParseException) {
            Log.e(TAG, "JSON parsing error during sync", e)
            ApiResult.Error("Response parsing error: ${e.localizedMessage}")
        } catch (e: Exception) {
            Log.e(TAG, "Sync error", e)
            if (processingIds.isNotEmpty()) {
                try {
                    pendingActivityRepository.incrementRetryCountAndFail(processingIds)
                } catch (dbEx: Exception) {
                    Log.e(TAG, "Failed to mark items as failed", dbEx)
                }
            }
            ApiResult.Error("Network error: ${e.localizedMessage}")
        }
    }
}
