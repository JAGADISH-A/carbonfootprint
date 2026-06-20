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

@Singleton
class UploadRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val settingsStore: SettingsStore,
    private val pendingActivityRepository: PendingActivityRepository
) {
    suspend fun syncBatch(syncSessionId: String): ApiResult<Int> {
        var processingIds = emptyList<String>()
        return try {
            val authToken = settingsStore.authToken.first()
            if (authToken.isEmpty()) return ApiResult.Error("Not authenticated")

            val deviceId = settingsStore.deviceId.first()
            if (deviceId.isEmpty()) return ApiResult.Error("Device ID not found")

            // 1. Fetch eligible items from PendingActivityRepository
            val pendingActivities = pendingActivityRepository.getEligibleForSync(maxRetries = 5)
            if (pendingActivities.isEmpty()) return ApiResult.Success(0)

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
                    source = activity.source,
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
            if (response.isSuccessful && response.body() != null) {
                val batchResponse = response.body()!!
                val successfulIds = mutableListOf<String>()
                val failedIds = mutableListOf<String>()

                for (result in batchResponse.results) {
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
                    // We only need to check the items we fetched initially
                    val permanentlyFailed = pendingActivities
                        .filter { failedIds.contains(it.id) && (it.retryCount + 1) >= 5 }
                        .map { it.id }
                        
                    if (permanentlyFailed.isNotEmpty()) {
                        pendingActivityRepository.markPermanentFailure(permanentlyFailed)
                    }
                }

                settingsStore.setLastSyncTime(System.currentTimeMillis())

                if (failedIds.isEmpty()) {
                     ApiResult.Success(successfulIds.size)
                } else {
                     ApiResult.Error("Partial success. Failed ${failedIds.size} items.")
                }
            } else {
                // If the entire batch call failed (network or 500), mark all as failed
                pendingActivityRepository.incrementRetryCountAndFail(processingIds)
                ApiResult.Error("Sync failed (${response.code()})")
            }
        } catch (e: Exception) {
            if (processingIds.isNotEmpty()) {
                pendingActivityRepository.incrementRetryCountAndFail(processingIds)
            }
            ApiResult.Error("Network error: ${e.localizedMessage}")
        }
    }
}
