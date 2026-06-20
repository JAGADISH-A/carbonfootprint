package com.carbonwise.connect.ingestion.model

import com.carbonwise.connect.ingestion.enrichment.ConfidenceLevel

/**
 * Immutable model representing an enriched transaction.
 */
data class EnrichedTransaction(
    // Original transaction
    val merchant: String?,
    val amount: Double?,
    val currency: String?,
    val sourceApp: String?,
    val timestamp: Long,
    val rawNotification: String,
    
    // Merchant enrichment
    val merchantType: String?,
    
    // Category enrichment
    val category: String?,
    
    // Carbon enrichment
    val carbonHint: String?,
    val transportMode: String?,
    val fuelType: String?,
    val energyType: String?,
    val purchaseType: String?,
    
    // Confidence
    val overallConfidence: Double,
    val confidenceLevel: ConfidenceLevel,
    val confidenceBreakdown: Map<String, Double>,
    
    // Metadata
    val enrichmentVersion: String,
    val processedAt: Long,
    val processingTimeMs: Long,
    val stageMetricsMs: Map<String, Long>,
    val warnings: List<String>,
    val reasoning: List<String>
)
