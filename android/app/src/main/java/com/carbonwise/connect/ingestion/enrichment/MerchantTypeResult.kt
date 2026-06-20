package com.carbonwise.connect.ingestion.enrichment

/**
 * Result of the MerchantTypeEnricher processing.
 */
data class MerchantTypeResult(
    val merchantType: String,
    val confidence: Double,
    val matchedRule: String?,
    val matchedMerchant: String?
)
