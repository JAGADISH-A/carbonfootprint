package com.carbonwise.connect.ingestion.enrichment

enum class ConfidenceLevel {
    VERY_HIGH,
    HIGH,
    MEDIUM,
    LOW,
    VERY_LOW
}

/**
 * Encapsulates the overall confidence calculation for a fully enriched transaction.
 */
data class EnrichmentConfidenceResult(
    val overallConfidence: Double,
    val confidenceLevel: ConfidenceLevel,
    val confidenceBreakdown: Map<String, Double>,
    val reasoning: List<String>,
    val warnings: List<String>
)
