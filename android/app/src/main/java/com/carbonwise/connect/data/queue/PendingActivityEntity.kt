package com.carbonwise.connect.data.queue

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_activities")
data class PendingActivityEntity(
    @PrimaryKey
    val id: String, // Maps to rawHash
    val sender: String,
    val messageBody: String,
    val receivedTimestamp: Long,
    val normalizedMerchant: String,
    val category: String? = null,
    val source: String = "SMS",
    val syncStatus: String = "PENDING",
    val rawHash: String,
    val ingestionVersion: Int = 1,
    val retryCount: Int = 0
)
