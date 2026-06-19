package com.carbonwise.connect.data.model

import com.google.gson.annotations.SerializedName

data class AccountInfo(
    @SerializedName("user_id") val userId: String,
    @SerializedName("email") val email: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("connected_at") val connectedAt: String
)

data class SyncRequest(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("notifications") val notifications: List<PendingNotification>,
    @SerializedName("sms") val sms: List<PendingSms>,
    @SerializedName("timestamp") val timestamp: String
)

data class SyncResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("synced_count") val syncedCount: Int,
    @SerializedName("server_time") val serverTime: String
)

data class HealthResponse(
    @SerializedName("status") val status: String,
    @SerializedName("version") val version: String
)

data class PendingNotification(
    @SerializedName("id") val id: Long,
    @SerializedName("package") val packageName: String,
    @SerializedName("title") val title: String,
    @SerializedName("text") val text: String,
    @SerializedName("timestamp") val timestamp: Long
)

data class PendingSms(
    @SerializedName("id") val id: Long,
    @SerializedName("address") val address: String,
    @SerializedName("body") val body: String,
    @SerializedName("timestamp") val timestamp: Long
)

data class ConnectRequest(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("auth_token") val authToken: String
)

data class ConnectResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("account") val account: AccountInfo?,
    @SerializedName("error") val error: String?
)
