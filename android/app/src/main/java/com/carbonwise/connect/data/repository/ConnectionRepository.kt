package com.carbonwise.connect.data.repository

import com.carbonwise.connect.data.local.PendingDataDao
import com.carbonwise.connect.data.local.PendingDataEntity
import com.carbonwise.connect.data.local.SyncLogDao
import com.carbonwise.connect.data.local.SyncLogEntity
import com.carbonwise.connect.data.local.SettingsStore
import com.carbonwise.connect.data.model.ConnectRequest
import com.carbonwise.connect.data.model.ConnectResponse
import com.carbonwise.connect.data.model.HealthResponse
import com.carbonwise.connect.data.model.PendingNotification
import com.carbonwise.connect.data.model.PendingSms
import com.carbonwise.connect.data.model.SyncRequest
import com.carbonwise.connect.data.remote.ApiClient
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
}

@Singleton
class ConnectionRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val settingsStore: SettingsStore,
    private val pendingDataDao: PendingDataDao,
    private val syncLogDao: SyncLogDao
) {
    suspend fun healthCheck(): ApiResult<HealthResponse> {
        return try {
            val response = apiClient.apiService.healthCheck()
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error("Backend unreachable (${response.code()})")
            }
        } catch (e: Exception) {
            ApiResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    suspend fun connect(authToken: String): ApiResult<ConnectResponse> {
        return try {
            val deviceId = settingsStore.deviceId.first().ifEmpty {
                UUID.randomUUID().toString().also { settingsStore.setDeviceId(it) }
            }
            val response = apiClient.apiService.connect(
                ConnectRequest(deviceId = deviceId, authToken = authToken)
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success && body.account != null) {
                    settingsStore.setConnected(true)
                    settingsStore.setAuthToken(authToken)
                    settingsStore.setAccountInfo(
                        userId = body.account.userId,
                        email = body.account.email,
                        name = body.account.displayName
                    )
                }
                ApiResult.Success(body)
            } else {
                ApiResult.Error("Connection failed (${response.code()})")
            }
        } catch (e: Exception) {
            ApiResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    suspend fun syncPendingData(): ApiResult<Int> {
        return try {
            val authToken = settingsStore.authToken.first()
            if (authToken.isEmpty()) return ApiResult.Error("Not authenticated")

            val pending = pendingDataDao.getPending()
            if (pending.isEmpty()) return ApiResult.Success(0)

            val notifications = pending
                .filter { it.type == "notification" }
                .map {
                    PendingNotification(
                        id = it.id,
                        packageName = it.sourcePackage,
                        title = it.title,
                        text = it.body,
                        timestamp = it.timestamp
                    )
                }

            val sms = pending
                .filter { it.type == "sms" }
                .map {
                    PendingSms(
                        id = it.id,
                        address = it.sourcePackage,
                        title = it.title,
                        body = it.body,
                        timestamp = it.timestamp
                    )
                }

            val deviceId = settingsStore.deviceId.first()
            val request = SyncRequest(
                deviceId = deviceId,
                notifications = notifications,
                sms = sms,
                timestamp = System.currentTimeMillis().toString()
            )

            val response = apiClient.apiService.sync(
                authHeader = "Bearer $authToken",
                request = request
            )

            if (response.isSuccessful && response.body()?.success == true) {
                val syncedIds = pending.map { it.id }
                pendingDataDao.markSynced(syncedIds)
                pendingDataDao.cleanupSynced()
                settingsStore.setLastSyncTime(System.currentTimeMillis())

                syncLogDao.insert(
                    SyncLogEntity(
                        timestamp = System.currentTimeMillis(),
                        success = true,
                        itemsSynced = pending.size
                    )
                )
                ApiResult.Success(pending.size)
            } else {
                val errorMsg = response.body()?.toString() ?: "Sync failed"
                syncLogDao.insert(
                    SyncLogEntity(
                        timestamp = System.currentTimeMillis(),
                        success = false,
                        itemsSynced = 0,
                        error = errorMsg
                    )
                )
                ApiResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            syncLogDao.insert(
                SyncLogEntity(
                    timestamp = System.currentTimeMillis(),
                    success = false,
                    itemsSynced = 0,
                    error = e.localizedMessage
                )
            )
            ApiResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    suspend fun disconnect(): ApiResult<Boolean> {
        return try {
            val authToken = settingsStore.authToken.first()
            if (authToken.isEmpty()) return ApiResult.Error("Not authenticated")

            val deviceId = settingsStore.deviceId.first()
            apiClient.apiService.disconnect(
                authHeader = "Bearer $authToken",
                request = mapOf("device_id" to deviceId)
            )
            pendingDataDao.clearAll()
            syncLogDao.clearAll()
            settingsStore.clearAll()
            ApiResult.Success(true)
        } catch (e: Exception) {
            settingsStore.clearAll()
            ApiResult.Success(true)
        }
    }

    suspend fun getPendingCount(): Int = pendingDataDao.getPendingCount()

    suspend fun storeNotification(notification: PendingDataEntity) {
        pendingDataDao.insert(notification)
    }

    suspend fun storeSms(sms: PendingDataEntity) {
        pendingDataDao.insert(sms)
    }
}
