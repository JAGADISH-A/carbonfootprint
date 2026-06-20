package com.carbonwise.connect.ingestion.enrichment

/**
 * Result of the CarbonHintEnricher processing.
 * Models carbon-domain metadata to be consumed by the backend.
 */
data class CarbonHintResult(
    val carbonHint: String,
    val transportMode: String?,
    val fuelType: String?,
    val energyType: String?,
    val purchaseType: String?,
    val confidence: Double,
    val matchedRule: String,
    val reasoning: String
)
