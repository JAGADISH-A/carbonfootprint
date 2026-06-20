package com.carbonwise.connect.data.remote

import com.carbonwise.connect.data.model.ConnectRequest
import com.carbonwise.connect.data.model.ConnectResponse
import com.carbonwise.connect.data.model.HealthResponse
import com.carbonwise.connect.data.model.SyncRequest
import com.carbonwise.connect.data.model.SyncResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {

    @GET("health")
    suspend fun healthCheck(): Response<HealthResponse>

    @POST("api/v1/device/connect")
    suspend fun connect(@Body request: ConnectRequest): Response<ConnectResponse>

    @POST("api/v1/device/sync")
    suspend fun sync(
        @Header("Authorization") authHeader: String,
        @Body request: SyncRequest
    ): Response<SyncResponse>

    @POST("api/v1/mobile/sync/batch")
    suspend fun syncBatch(
        @Header("Authorization") authHeader: String,
        @Body request: com.carbonwise.connect.data.model.BatchSyncRequest
    ): Response<com.carbonwise.connect.data.model.BatchSyncResponse>

    @POST("api/v1/device/disconnect")
    suspend fun disconnect(
        @Header("Authorization") authHeader: String,
        @Body request: Map<String, String>
    ): Response<Map<String, Boolean>>
}
