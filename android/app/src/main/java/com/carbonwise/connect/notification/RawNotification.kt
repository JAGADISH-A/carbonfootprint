package com.carbonwise.connect.notification

data class RawNotification(
    val appName: String?,
    val packageName: String,
    val title: String?,
    val text: String?,
    val subText: String?,
    val timestamp: Long,
    val notificationId: Int,
    val isOngoing: Boolean
)
