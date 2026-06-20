package com.carbonwise.connect.ingestion.enrichment

/**
 * Result of the CategoryEnricher processing.
 */
data class CategoryResult(
    val category: String,
    val confidence: Double,
    val matchedRule: String,
    val reasoning: String
)
