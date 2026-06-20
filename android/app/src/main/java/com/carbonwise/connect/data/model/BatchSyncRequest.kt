package com.carbonwise.connect.data.model

import com.google.gson.annotations.SerializedName

data class BatchSyncRequest(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("syncSessionId") val syncSessionId: String,
    @SerializedName("items") val items: List<BatchSyncItemRequest>
)

data class BatchSyncItemRequest(
    @SerializedName("id") val id: String,
    @SerializedName("sender") val sender: String,
    @SerializedName("messageBody") val messageBody: String,
    @SerializedName("receivedTimestamp") val receivedTimestamp: Long,
    @SerializedName("normalizedMerchant") val normalizedMerchant: String,
    @SerializedName("category") val category: String?,
    @SerializedName("source") val source: String,
    @SerializedName("rawHash") val rawHash: String,
    @SerializedName("ingestionVersion") val ingestionVersion: Int
)

data class BatchSyncResponse(
    @SerializedName("syncSessionId") val syncSessionId: String,
    @SerializedName("results") val results: List<BatchSyncItemResponse>
)

data class BatchSyncItemResponse(
    @SerializedName("id") val id: String,
    @SerializedName("status") val status: String,
    @SerializedName("reason") val reason: String?
)
