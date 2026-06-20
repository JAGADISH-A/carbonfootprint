package com.carbonwise.connect.ingestion.model

/**
 * Extracted transaction candidate from an ingestion event.
 */
data class TransactionCandidate(
    val merchant: String?,
    val amount: Double?,
    val currency: String?,
    val transactionType: String?,
    val sourceApp: String?,
    val timestamp: Long,
    val rawNotification: String,
    val confidence: Double
)
