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
    init {
        android.util.Log.e("UPLOAD_TEST", "UploadRepository created")
    }

    suspend fun syncBatch(syncSessionId: String): ApiResult<SyncBatchResult> {
        android.util.Log.e("UPLOAD_TEST", "===== syncBatch ENTERED =====")
        var processingIds = emptyList<String>()
        android.util.Log.d("UploadPipeline", "UploadRepository.syncBatch() started with session ID: $syncSessionId")
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
            
            android.util.Log.d("UploadPipeline", "Number of PendingActivities fetched from Room: ${pendingActivities.size}")
            
            if (pendingActivities.isEmpty()) {
                android.util.Log.d("UploadPipeline", "Early return in syncBatch(): No activities to sync. Setting last sync time.")
                val timestamp = System.currentTimeMillis()
                android.util.Log.d("MOBILE_SYNC", "Sync completed successfully. Updating lastSyncTime=$timestamp")
                settingsStore.setLastSuccessfulUploadTimestamp(timestamp)
                android.util.Log.d("UploadPipeline", "Last sync time set.")
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

            val uploadBatchSize = request.items.size
            android.util.Log.d("UploadPipeline", "Size of the upload batch: $uploadBatchSize")

            // Serialize using Gson to log size
            val gson = com.google.gson.Gson()
            val serializedRequest = gson.toJson(request)
            val requestSizeInBytes = serializedRequest.toByteArray(Charsets.UTF_8).size
            android.util.Log.d("UploadPipeline", "Serialized request size: $requestSizeInBytes bytes")

            val apiEndpoint = "POST api/v1/mobile/sync/batch"
            android.util.Log.d("UploadPipeline", "Invoking API endpoint: $apiEndpoint")

            // 4. Send request to Backend API
            val response = apiClient.apiService.syncBatch(
                authHeader = "Bearer $authToken",
                request = request
            )

            android.util.Log.d("UploadPipeline", "HTTP response code: ${response.code()}")

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

                // 7. Add verification log
                android.util.Log.e(
                    "UPLOAD_CONTRACT",
                    "success=$success\nmessage=${envelope.message}\nresults=${results?.size}"
                )

                if (!success || data == null || results == null) {
                    val errMsg = envelope.message ?: "Invalid backend response format (success=false, data=null, or results=null)"
                    android.util.Log.e("UploadPipeline", "Response validation failed: $errMsg")
                    return ApiResult.Error(errMsg)
                }

                // Only mark individual activities SUCCESS or FAILED using BatchSyncResponse.results
                android.util.Log.d("UploadPipeline", "Response body: ${gson.toJson(data)}")
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
                    android.util.Log.d("UploadPipeline", "Marking ${successfulIds.size} rows as SYNCED in Room")
                    pendingActivityRepository.updateSyncStatus(successfulIds, "SYNCED")
                    android.util.Log.d("UploadPipeline", "Marked ${successfulIds.size} rows as SYNCED")
                }

                if (failedIds.isNotEmpty()) {
                    android.util.Log.d("UploadPipeline", "Marking/Incrementing retry count for ${failedIds.size} failed rows in Room")
                    pendingActivityRepository.incrementRetryCountAndFail(failedIds)
                    
                    // Mark permanent failures (re-fetch items to check retry count)
                    val permanentlyFailed = pendingActivities
                        .filter { failedIds.contains(it.id) && (it.retryCount + 1) >= 5 }
                        .map { it.id }
                        
                    if (permanentlyFailed.isNotEmpty()) {
                        android.util.Log.d("UploadPipeline", "Marking ${permanentlyFailed.size} rows as FAILED_PERMANENT in Room")
                        pendingActivityRepository.markPermanentFailure(permanentlyFailed)
                        android.util.Log.d("UploadPipeline", "Marked ${permanentlyFailed.size} rows as FAILED_PERMANENT")
                    }
                }

                // 6. Only call setLastSuccessfulUploadTimestamp() after Room updates complete
                val timestamp = System.currentTimeMillis()
                android.util.Log.d("MOBILE_SYNC", "Sync completed successfully. Updating lastSyncTime=$timestamp")
                settingsStore.setLastSuccessfulUploadTimestamp(timestamp)

                if (failedIds.isEmpty()) {
                    android.util.Log.d("UploadPipeline", "Batch sync completed successfully. All $uploadBatchSize items uploaded.")
                    ApiResult.Success(SyncBatchResult(successfulIds.size, 0))
                } else {
                    android.util.Log.d("UploadPipeline", "Batch sync partially completed. Success: ${successfulIds.size}, Failed: ${failedIds.size}")
                    ApiResult.Success(SyncBatchResult(successfulIds.size, failedIds.size))
                }
            } else {
                val errorBodyString = response.errorBody()?.string() ?: ""
                android.util.Log.e("UploadPipeline", "HTTP error response body: $errorBodyString")
                android.util.Log.w("UploadPipeline", "Batch call failed (response not successful or null body). Marking all $uploadBatchSize rows as failed.")
                pendingActivityRepository.incrementRetryCountAndFail(processingIds)
                ApiResult.Error("Sync failed (${response.code()})")
            }
        } catch (e: com.google.gson.JsonParseException) {
            android.util.Log.e("UploadPipeline", "JSON parsing exception during syncBatch()", e)
            ApiResult.Error("Response parsing error: ${e.localizedMessage}")
        } catch (e: Exception) {
            android.util.Log.e("UploadPipeline", "Exception occurred during syncBatch()", e)
            if (processingIds.isNotEmpty()) {
                android.util.Log.d("UploadPipeline", "Marking all processing IDs as failed due to exception.")
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
