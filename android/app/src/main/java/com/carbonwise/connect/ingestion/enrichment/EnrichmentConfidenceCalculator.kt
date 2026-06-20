package com.carbonwise.connect.ingestion.enrichment

import com.carbonwise.connect.ingestion.model.TransactionCandidate
import kotlin.math.round

/**
 * Configuration for confidence weights.
 * Default sum is 1.0 (100%).
 */
data class ConfidenceWeights(
    val merchantWeight: Double = 0.30,
    val categoryWeight: Double = 0.30,
    val carbonHintWeight: Double = 0.30,
    val completenessWeight: Double = 0.10
)

/**
 * Calculates a single overall confidence score for an enriched transaction.
 * Evaluates completeness and aggregates individual enricher confidences.
 */
class EnrichmentConfidenceCalculator(
    private val weights: ConfidenceWeights = ConfidenceWeights()
) {

    fun calculate(
        candidate: TransactionCandidate,
        merchantResult: MerchantTypeResult,
        categoryResult: CategoryResult,
        carbonHintResult: CarbonHintResult
    ): EnrichmentConfidenceResult {
        
        var fieldsPresent = 0
        val totalFields = 5
        
        val warnings = mutableListOf<String>()
        val reasoningPoints = mutableListOf<String>()
        
        // 1. Transaction Completeness
        if (candidate.merchant != null) fieldsPresent++
        else warnings.add("Unknown merchant")
        
        if (candidate.amount != null) fieldsPresent++
        else warnings.add("Missing amount")
        
        if (candidate.currency != null) fieldsPresent++
        else warnings.add("Unknown currency")
        
        if (candidate.timestamp > 0) fieldsPresent++ // Basic check
        
        if (candidate.rawNotification.isNotBlank()) fieldsPresent++
        
        val completenessScore = fieldsPresent.toDouble() / totalFields
        
        // 2. Reasonings & Warnings
        if (merchantResult.merchantType == "UNKNOWN") {
            if (!warnings.contains("Unknown merchant")) warnings.add("Unknown merchant")
        } else {
            reasoningPoints.add("Merchant was recognized.")
        }
        
        if (candidate.amount != null) {
            reasoningPoints.add("Amount detected.")
        }
        
        if (categoryResult.category == "UNKNOWN") {
            warnings.add("Weak category match")
        } else {
            reasoningPoints.add("Category matched.")
            if (categoryResult.confidence < 0.80) {
                warnings.add("Weak category match")
            }
        }
        
        if (carbonHintResult.carbonHint != "UNKNOWN") {
            reasoningPoints.add("Carbon hint inferred.")
            val isTransport = carbonHintResult.carbonHint in listOf("RIDE_SHARE", "PUBLIC_TRANSPORT", "TRAIN_TRAVEL", "AIR_TRAVEL")
            if (isTransport && carbonHintResult.transportMode == null) {
                warnings.add("No transport mode detected")
            }
        }
        
        if (merchantResult.matchedRule != "EXACT_MATCH" && merchantResult.matchedRule != "MERCHANT_ALIAS" && merchantResult.matchedRule != "PARTIAL_MATCH") {
            warnings.add("No merchant alias matched")
        }
        
        if (fieldsPresent == totalFields) {
            reasoningPoints.add("Transaction contained complete metadata.")
        }
        
        // 3. Weighted Score Calculation
        val weightedMerchant = merchantResult.confidence * weights.merchantWeight
        val weightedCategory = categoryResult.confidence * weights.categoryWeight
        val weightedCarbonHint = carbonHintResult.confidence * weights.carbonHintWeight
        val weightedCompleteness = completenessScore * weights.completenessWeight
        
        val overallConfidenceRaw = weightedMerchant + weightedCategory + weightedCarbonHint + weightedCompleteness
        val overallConfidence = round(overallConfidenceRaw * 100) / 100.0
        
        val breakdown = mapOf(
            "Merchant" to round(merchantResult.confidence * 100) / 100.0,
            "Category" to round(categoryResult.confidence * 100) / 100.0,
            "Carbon Hint" to round(carbonHintResult.confidence * 100) / 100.0,
            "Completeness" to round(completenessScore * 100) / 100.0,
            "Weighted Score" to overallConfidence
        )
        
        // 4. Level Mapping
        val level = when {
            overallConfidence >= 0.90 -> ConfidenceLevel.VERY_HIGH
            overallConfidence >= 0.70 -> ConfidenceLevel.HIGH
            overallConfidence >= 0.50 -> ConfidenceLevel.MEDIUM
            overallConfidence >= 0.30 -> ConfidenceLevel.LOW
            else -> ConfidenceLevel.VERY_LOW
        }
        
        val finalReasoning = mutableListOf<String>()
        if (overallConfidence >= 0.70) {
            finalReasoning.add("High confidence because:")
        } else {
            finalReasoning.add("Confidence is reduced because:")
        }
        finalReasoning.addAll(reasoningPoints.map { "• $it" })
        
        return EnrichmentConfidenceResult(
            overallConfidence = overallConfidence,
            confidenceLevel = level,
            confidenceBreakdown = breakdown,
            reasoning = finalReasoning,
            warnings = warnings
        )
    }
}
