package com.carbonwise.connect.data.model

data class PermissionState(
    val smsGranted: Boolean = false,
    val notificationGranted: Boolean = false,
    val batteryOptimizationIgnored: Boolean = false,
    val backgroundSyncReady: Boolean = false
)
