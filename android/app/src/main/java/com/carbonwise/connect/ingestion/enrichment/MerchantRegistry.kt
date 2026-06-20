package com.carbonwise.connect.ingestion.enrichment

/**
 * Configurable registry mapping merchant names to their respective types.
 * Supports exact match, case-insensitive matching, alias matching, and partial matching.
 */
class MerchantRegistry {
    
    // Internal map holding lowercase merchant names/aliases to their merchant types
    private val mappings = mutableMapOf(
        "swiggy" to "RESTAURANT",
        "zomato" to "RESTAURANT",
        "starbucks" to "CAFE",
        "uber" to "TRANSPORT",
        "ola" to "TRANSPORT",
        "rapido" to "TRANSPORT",
        "amazon" to "ECOMMERCE",
        "flipkart" to "ECOMMERCE",
        "blinkit" to "GROCERY",
        "bigbasket" to "GROCERY",
        "dmart" to "GROCERY",
        "reliance smart" to "GROCERY",
        "indian oil" to "FUEL",
        "hpcl" to "FUEL",
        "bpcl" to "FUEL",
        "irctc" to "RAILWAY",
        "air india" to "AIRLINE",
        "indigo" to "AIRLINE",
        "electricity board" to "ELECTRICITY",
        "cred" to "FINANCE",
        "phonepe" to "PAYMENT",
        "google pay" to "PAYMENT",
        "paytm" to "PAYMENT"
    )

    /**
     * Adds a new mapping to the registry, allowing extension without modifying enrichment logic.
     */
    fun register(merchantOrAlias: String, merchantType: String) {
        mappings[merchantOrAlias.lowercase()] = merchantType
    }

    /**
     * Determines the merchant type from the given text using deterministic rules.
     */
    fun findType(text: String?): MerchantTypeResult {
        if (text.isNullOrBlank()) {
            return MerchantTypeResult("UNKNOWN", 0.0, null, null)
        }

        val lowerText = text.lowercase()

        // 1. Exact Match Check (High Confidence)
        for ((merchant, type) in mappings) {
            if (lowerText == merchant) {
                return MerchantTypeResult(type, 1.0, "EXACT_MATCH", merchant)
            }
        }

        // 2. Partial / Alias Match Check (Slightly Lower Confidence)
        for ((merchant, type) in mappings) {
            if (lowerText.contains(merchant)) {
                return MerchantTypeResult(type, 0.95, "PARTIAL_MATCH", merchant)
            }
        }

        // Fallback
        return MerchantTypeResult("UNKNOWN", 0.0, null, null)
    }
}
