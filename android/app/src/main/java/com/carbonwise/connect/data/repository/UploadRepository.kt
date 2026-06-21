package com.carbonwise.connect.data.repository

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

    suspend fun syncBatch(syncSessionId: String): ApiResult<SyncBatchResult> {
        var processingIds = emptyList<String>()
        return try {
            val authToken = tokenManager.getDeviceToken() ?: ""
            if (authToken.isEmpty()) {
                android.util.Log.w("UploadPipeline", "Early return in syncBatch(): Auth token is empty / not authenticated.")
                return ApiResult.Error("Not authenticated")
            }

            val deviceId = tokenManager.getDeviceId() ?: ""
            if (deviceId.isEmpty()) {
                android.util.Log.w("UploadPipeline", "Early return in syncBatch(): Device ID is empty.")
                return ApiResult.Error("Device ID not found")
            }

            // 1. Fetch eligible items from PendingActivityRepository
            val pendingActivities = pendingActivityRepository.getEligibleForSync(maxRetries = 5)
            
            if (pendingActivities.isEmpty()) {
                val timestamp = System.currentTimeMillis()
                android.util.Log.d("MOBILE_SYNC", "Sync completed successfully. Updating lastSyncTime=$timestamp")
                settingsStore.setLastSuccessfulUploadTimestamp(timestamp)
                return ApiResult.Success(SyncBatchResult(0, 0))
            }

            // 2. Mark them as SYNCING
            processingIds = pendingActivities.map { it.id }
            pendingActivityRepository.updateSyncStatus(processingIds, "SYNCING")

            // 3. Map Domain to DTO
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

            // 4. Send request to Backend API
            val response = apiClient.apiService.syncBatch(
                authHeader = "Bearer $authToken",
                request = request
            )

            // 5. Process Response
            val envelope = response.body()
            if (response.isSuccessful) {
                if (envelope == null) {
                    android.util.Log.e("UploadPipeline", "Response envelope is null")
                    return ApiResult.Error("Response envelope is null")
                }

                val success = envelope.success
                val data = envelope.data
                val results = data?.results

                if (!success || data == null || results == null) {
                    val errMsg = envelope.message ?: "Invalid backend response format (success=false, data=null, or results=null)"
                    android.util.Log.e("UploadPipeline", "Response validation failed: $errMsg")
                    return ApiResult.Error(errMsg)
                }

                // Only mark individual activities SUCCESS or FAILED using BatchSyncResponse.results
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
                    
                    // Mark permanent failures (re-fetch items to check retry count)
                    val permanentlyFailed = pendingActivities
                        .filter { failedIds.contains(it.id) && (it.retryCount + 1) >= 5 }
                        .map { it.id }
                        
                    if (permanentlyFailed.isNotEmpty()) {
                        pendingActivityRepository.markPermanentFailure(permanentlyFailed)
                    }
                }

                // 6. Only call setLastSuccessfulUploadTimestamp() after Room updates complete
                val timestamp = System.currentTimeMillis()
                android.util.Log.d("MOBILE_SYNC", "Sync completed successfully. Updating lastSyncTime=$timestamp")
                settingsStore.setLastSuccessfulUploadTimestamp(timestamp)

                if (failedIds.isEmpty()) {
                    ApiResult.Success(SyncBatchResult(successfulIds.size, 0))
                } else {
                    ApiResult.Success(SyncBatchResult(successfulIds.size, failedIds.size))
                }
            } else {
                android.util.Log.w("UploadPipeline", "Batch call failed (response not successful or null body). Marking all processing IDs as failed.")
                pendingActivityRepository.incrementRetryCountAndFail(processingIds)
                ApiResult.Error("Sync failed (${response.code()})")
            }
        } catch (e: com.google.gson.JsonParseException) {
            android.util.Log.e("UploadPipeline", "JSON parsing exception during syncBatch()", e)
            ApiResult.Error("Response parsing error: ${e.localizedMessage}")
        } catch (e: Exception) {
            android.util.Log.e("UploadPipeline", "Exception occurred during syncBatch()", e)
            if (processingIds.isNotEmpty()) {
                try {
                    pendingActivityRepository.incrementRetryCountAndFail(processingIds)
                } catch (dbEx: Exception) {
                    android.util.Log.e("UploadPipeline", "Failed to mark items as failed after main exception", dbEx)
                }
            }
            ApiResult.Error("Network error: ${e.localizedMessage}")
        }
    }
}
