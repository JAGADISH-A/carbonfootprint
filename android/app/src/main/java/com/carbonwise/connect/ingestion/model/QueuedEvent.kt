package com.carbonwise.connect.ingestion.model

/**
 * Queued event with classification info, ready for upload.
 * Used by QueueRepository implementations across all data sources.
 */
data class QueuedEvent(
    val id: String,
    val packageName: String,
    val title: String,
    val body: String,
    val timestamp: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val attemptCount: Int = 0,
    val lastError: String? = null,
    val classification: ClassificationResult
)
