package com.carbonwise.connect.ingestion.enrichment

import com.carbonwise.connect.ingestion.model.EnrichedTransaction
import com.carbonwise.connect.ingestion.model.TransactionCandidate
import javax.inject.Inject
import java.util.logging.Logger
import kotlin.system.measureTimeMillis

/**
 * Defines a single step in the transaction enrichment pipeline.
 */
interface TransactionEnrichmentStep {
    fun enrich(transaction: EnrichedTransaction): EnrichedTransaction
}

/**
 * Enriches transactions with Merchant Type using a configurable Merchant Registry.
 */
class MerchantTypeEnricher(
    private val registry: MerchantRegistry = MerchantRegistry()
) : TransactionEnrichmentStep {

    /**
     * Core logic receiving TransactionCandidate and outputting MerchantTypeResult.
     */
    fun enrich(candidate: TransactionCandidate): MerchantTypeResult {
        val textToSearch = candidate.merchant ?: candidate.rawNotification
        return registry.findType(textToSearch)
    }

    /**
     * Pipeline integration step.
     */
    override fun enrich(transaction: EnrichedTransaction): EnrichedTransaction {
        val textToSearch = transaction.merchant ?: transaction.rawNotification
        val result = registry.findType(textToSearch)
        return transaction.copy(
            merchantType = if (result.merchantType != "UNKNOWN") result.merchantType else transaction.merchantType
        )
    }
}

/**
 * Enriches transactions with Category using rule-based mappings.
 */
class CategoryEnricher(
    private val registry: CategoryRulesRegistry = CategoryRulesRegistry()
) : TransactionEnrichmentStep {

    /**
     * Core logic receiving TransactionCandidate and MerchantTypeResult.
     */
    fun enrich(candidate: TransactionCandidate, merchantTypeResult: MerchantTypeResult): CategoryResult {
        return registry.resolve(candidate, merchantTypeResult)
    }

    /**
     * Pipeline integration step.
     */
    override fun enrich(transaction: EnrichedTransaction): EnrichedTransaction {
        // Reconstruct candidate and minimal merchantTypeResult to fulfill the core API
        val candidate = TransactionCandidate(
            merchant = transaction.merchant,
            amount = transaction.amount,
            currency = transaction.currency,
            transactionType = null,
            sourceApp = transaction.sourceApp,
            timestamp = transaction.timestamp,
            rawNotification = transaction.rawNotification,
            confidence = transaction.overallConfidence
        )
        
        val mtResult = MerchantTypeResult(
            merchantType = transaction.merchantType ?: "UNKNOWN",
            confidence = 1.0, 
            matchedRule = "PIPELINE_INHERITED",
            matchedMerchant = transaction.merchant
        )
        
        val result = enrich(candidate, mtResult)
        
        return transaction.copy(
            category = if (result.category != "UNKNOWN") result.category else transaction.category
        )
    }
}

/**
 * Enriches transactions with Carbon Hints using deterministic rule matching.
 */
class CarbonHintEnricher(
    private val registry: CarbonHintRulesRegistry = CarbonHintRulesRegistry()
) : TransactionEnrichmentStep {

    /**
     * Core logic receiving Candidate, MerchantType, and Category.
     */
    fun enrich(
        candidate: TransactionCandidate, 
        merchantTypeResult: MerchantTypeResult, 
        categoryResult: CategoryResult
    ): CarbonHintResult {
        return registry.resolve(candidate, merchantTypeResult, categoryResult)
    }

    /**
     * Pipeline integration step.
     */
    override fun enrich(transaction: EnrichedTransaction): EnrichedTransaction {
        val candidate = TransactionCandidate(
            merchant = transaction.merchant,
            amount = transaction.amount,
            currency = transaction.currency,
            transactionType = null,
            sourceApp = transaction.sourceApp,
            timestamp = transaction.timestamp,
            rawNotification = transaction.rawNotification,
            confidence = transaction.overallConfidence
        )
        
        val mtResult = MerchantTypeResult(
            merchantType = transaction.merchantType ?: "UNKNOWN",
            confidence = 1.0,
            matchedRule = "PIPELINE_INHERITED",
            matchedMerchant = transaction.merchant
        )
        
        val catResult = CategoryResult(
            category = transaction.category ?: "UNKNOWN",
            confidence = 1.0,
            matchedRule = "PIPELINE_INHERITED",
            reasoning = "Pipeline inherited"
        )
        
        val result = enrich(candidate, mtResult, catResult)
        
        return transaction.copy(
            carbonHint = if (result.carbonHint != "UNKNOWN") result.carbonHint else transaction.carbonHint
        )
    }
}

/**
 * Calculates final Confidence for the enriched transaction.
 */
class ConfidenceCalculator(
    private val calculator: EnrichmentConfidenceCalculator = EnrichmentConfidenceCalculator()
) : TransactionEnrichmentStep {

    fun enrich(
        candidate: TransactionCandidate,
        merchantResult: MerchantTypeResult,
        categoryResult: CategoryResult,
        carbonHintResult: CarbonHintResult
    ): EnrichmentConfidenceResult {
        return calculator.calculate(candidate, merchantResult, categoryResult, carbonHintResult)
    }

    override fun enrich(transaction: EnrichedTransaction): EnrichedTransaction {
        val candidate = TransactionCandidate(
            merchant = transaction.merchant,
            amount = transaction.amount,
            currency = transaction.currency,
            transactionType = null,
            sourceApp = transaction.sourceApp,
            timestamp = transaction.timestamp,
            rawNotification = transaction.rawNotification,
            confidence = transaction.overallConfidence
        )
        
        val mtResult = MerchantTypeResult(
            merchantType = transaction.merchantType ?: "UNKNOWN",
            confidence = if (transaction.merchantType != null) 0.9 else 0.0,
            matchedRule = "PIPELINE",
            matchedMerchant = transaction.merchant
        )
        
        val catResult = CategoryResult(
            category = transaction.category ?: "UNKNOWN",
            confidence = if (transaction.category != null) 0.9 else 0.0,
            matchedRule = "PIPELINE",
            reasoning = ""
        )
        
        val hintResult = CarbonHintResult(
            carbonHint = transaction.carbonHint ?: "UNKNOWN",
            transportMode = null,
            fuelType = null,
            energyType = null,
            purchaseType = null,
            confidence = if (transaction.carbonHint != null) 0.9 else 0.0,
            matchedRule = "PIPELINE",
            reasoning = ""
        )
        
        val result = enrich(candidate, mtResult, catResult, hintResult)
        
        return transaction.copy(
            overallConfidence = result.overallConfidence
        )
    }
}

/**
 * Component responsible for orchestrating the raw transaction enrichment pipeline.
 * Defines the single entry point for all enrichment using sequential component calls.
 */
class TransactionEnricher @Inject constructor(
    private val merchantTypeEnricher: MerchantTypeEnricher,
    private val categoryEnricher: CategoryEnricher,
    private val carbonHintEnricher: CarbonHintEnricher,
    private val confidenceCalculator: ConfidenceCalculator
) {
    private val logger = Logger.getLogger("TransactionEnricher")

    fun enrich(candidate: TransactionCandidate): EnrichedTransaction {
        logger.info("Starting enrichment...")
        val stageMetrics = mutableMapOf<String, Long>()
        val warnings = mutableListOf<String>()
        val totalStart = System.currentTimeMillis()

        // 1. MerchantType
        var merchantTypeResult: MerchantTypeResult? = null
        val merchantTypeTime = measureTimeMillis {
            try {
                merchantTypeResult = merchantTypeEnricher.enrich(candidate)
            } catch (e: Exception) {
                warnings.add("MerchantTypeEnricher failed: ${e.message}")
            }
        }
        stageMetrics["MerchantType"] = merchantTypeTime
        
        val finalMerchantTypeResult = merchantTypeResult ?: MerchantTypeResult("UNKNOWN", 0.0, "FALLBACK", null)
        logger.info("MerchantType resolved: ${finalMerchantTypeResult.merchantType}")

        // 2. Category
        var categoryResult: CategoryResult? = null
        val categoryTime = measureTimeMillis {
            try {
                categoryResult = categoryEnricher.enrich(candidate, finalMerchantTypeResult)
            } catch (e: Exception) {
                warnings.add("CategoryEnricher failed: ${e.message}")
            }
        }
        stageMetrics["Category"] = categoryTime
        
        val finalCategoryResult = categoryResult ?: CategoryResult("UNKNOWN", 0.0, "FALLBACK", "Category enrichment failed")
        logger.info("Category resolved: ${finalCategoryResult.category}")

        // 3. CarbonHint
        var carbonHintResult: CarbonHintResult? = null
        val carbonHintTime = measureTimeMillis {
            try {
                carbonHintResult = carbonHintEnricher.enrich(candidate, finalMerchantTypeResult, finalCategoryResult)
            } catch (e: Exception) {
                warnings.add("CarbonHintEnricher failed: ${e.message}")
            }
        }
        stageMetrics["CarbonHint"] = carbonHintTime
        
        val finalCarbonHintResult = carbonHintResult ?: CarbonHintResult("UNKNOWN", null, null, null, null, 0.0, "FALLBACK", "CarbonHint enrichment failed")
        logger.info("Carbon Hint resolved: ${finalCarbonHintResult.carbonHint}")

        // 4. Confidence
        var confidenceResult: EnrichmentConfidenceResult? = null
        val confidenceTime = measureTimeMillis {
            try {
                confidenceResult = confidenceCalculator.enrich(candidate, finalMerchantTypeResult, finalCategoryResult, finalCarbonHintResult)
            } catch (e: Exception) {
                warnings.add("ConfidenceCalculator failed: ${e.message}")
            }
        }
        stageMetrics["Confidence"] = confidenceTime
        
        val finalConfidenceResult = confidenceResult ?: EnrichmentConfidenceResult(
            overallConfidence = 0.0, 
            confidenceLevel = ConfidenceLevel.VERY_LOW, 
            confidenceBreakdown = emptyMap(), 
            reasoning = emptyList(), 
            warnings = listOf("Confidence calculation failed")
        )
        logger.info("Confidence: ${finalConfidenceResult.overallConfidence}")
        
        warnings.addAll(finalConfidenceResult.warnings)

        val totalDuration = System.currentTimeMillis() - totalStart
        stageMetrics["Total"] = totalDuration
        logger.info("Completed in $totalDuration ms")

        return EnrichedTransaction(
            merchant = candidate.merchant,
            amount = candidate.amount,
            currency = candidate.currency,
            sourceApp = candidate.sourceApp,
            timestamp = candidate.timestamp,
            rawNotification = candidate.rawNotification,
            
            merchantType = finalMerchantTypeResult.merchantType,
            
            category = finalCategoryResult.category,
            
            carbonHint = finalCarbonHintResult.carbonHint,
            transportMode = finalCarbonHintResult.transportMode,
            fuelType = finalCarbonHintResult.fuelType,
            energyType = finalCarbonHintResult.energyType,
            purchaseType = finalCarbonHintResult.purchaseType,
            
            overallConfidence = finalConfidenceResult.overallConfidence,
            confidenceLevel = finalConfidenceResult.confidenceLevel,
            confidenceBreakdown = finalConfidenceResult.confidenceBreakdown,
            
            enrichmentVersion = "1.0",
            processedAt = System.currentTimeMillis(),
            processingTimeMs = totalDuration,
            stageMetricsMs = stageMetrics,
            warnings = warnings,
            reasoning = finalConfidenceResult.reasoning
        )
    }
}
