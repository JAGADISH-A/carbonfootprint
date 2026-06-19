package com.carbonwise.connect.ingestion.queue

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for the upload queue.
 * Stores classified events from any data source (Notification, SMS, Email, Camera).
 */
@Entity(tableName = "ingestion_queue")
data class QueuedEventEntity(
    @PrimaryKey val id: String,
    val source: String,
    val category: String,
    val confidence: Float,
    val packageName: String,
    val title: String,
    val body: String,
    val extras: String,
    val timestamp: Long,
    val createdAt: Long,
    val attemptCount: Int = 0,
    val lastError: String? = null,
    val status: String = "pending",
    val retryAfter: Long = 0
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_UPLOADING = "uploading"
        const val STATUS_FAILED = "failed"
        const val STATUS_SYNCED = "synced"
    }
}
