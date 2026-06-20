package com.carbonwise.connect.ingestion.enrichment

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis

class TransactionEnricherTest {

    private val mockMerchantTypeEnricher = mockk<MerchantTypeEnricher>()
    private val mockCategoryEnricher = mockk<CategoryEnricher>()
    private val mockCarbonHintEnricher = mockk<CarbonHintEnricher>()
    private val mockConfidenceCalculator = mockk<ConfidenceCalculator>()

    private val orchestrator = TransactionEnricher(
        mockMerchantTypeEnricher,
        mockCategoryEnricher,
        mockCarbonHintEnricher,
        mockConfidenceCalculator
    )

    @Test
    fun `pipeline executes in exact order and returns enriched transaction`() {
        val candidate = TestFixtures.createCandidate()

        val mtResult = MerchantTypeResult("RESTAURANT", 1.0, "EXACT_MATCH", "Swiggy")
        val catResult = CategoryResult("FOOD", 1.0, "MERCHANT_TYPE", "Mapped")
        val hintResult = CarbonHintResult("FOOD_DELIVERY", null, null, null, null, 1.0, "MERCHANT_ALIAS", "Mapped")
        val confResult = EnrichmentConfidenceResult(0.95, ConfidenceLevel.VERY_HIGH, emptyMap(), emptyList(), emptyList())

        every { mockMerchantTypeEnricher.enrich(any()) } returns mtResult
        every { mockCategoryEnricher.enrich(any(), any()) } returns catResult
        every { mockCarbonHintEnricher.enrich(any(), any(), any()) } returns hintResult
        every { mockConfidenceCalculator.enrich(any(), any(), any(), any()) } returns confResult

        val result = orchestrator.enrich(candidate)

        verifyOrder {
            mockMerchantTypeEnricher.enrich(candidate)
            mockCategoryEnricher.enrich(candidate, mtResult)
            mockCarbonHintEnricher.enrich(candidate, mtResult, catResult)
            mockConfidenceCalculator.enrich(candidate, mtResult, catResult, hintResult)
        }

        assertThat(result.merchantType).isEqualTo("RESTAURANT")
        assertThat(result.category).isEqualTo("FOOD")
        assertThat(result.carbonHint).isEqualTo("FOOD_DELIVERY")
        assertThat(result.overallConfidence).isEqualTo(0.95)
        assertThat(result.processingTimeMs).isAtLeast(0L)
        assertThat(result.stageMetricsMs).containsKey("MerchantType")
        assertThat(result.stageMetricsMs).containsKey("Category")
        assertThat(result.stageMetricsMs).containsKey("CarbonHint")
        assertThat(result.stageMetricsMs).containsKey("Confidence")
        assertThat(result.stageMetricsMs).containsKey("Total")
    }

    @Test
    fun `enricher exception does not crash pipeline and uses fallback`() {
        val candidate = TestFixtures.createCandidate()

        val mtResult = MerchantTypeResult("RESTAURANT", 1.0, "EXACT_MATCH", "Swiggy")
        val catResult = CategoryResult("FOOD", 1.0, "MERCHANT_TYPE", "Mapped")
        val confResult = EnrichmentConfidenceResult(0.50, ConfidenceLevel.MEDIUM, emptyMap(), emptyList(), listOf("Some warning"))

        every { mockMerchantTypeEnricher.enrich(any()) } returns mtResult
        every { mockCategoryEnricher.enrich(any(), any()) } returns catResult
        // Carbon hint throws exception!
        every { mockCarbonHintEnricher.enrich(any(), any(), any()) } throws RuntimeException("Mock failure")
        every { mockConfidenceCalculator.enrich(any(), any(), any(), any()) } returns confResult

        val result = orchestrator.enrich(candidate)

        assertThat(result.carbonHint).isEqualTo("UNKNOWN")
        assertThat(result.warnings.any { it.contains("CarbonHintEnricher failed") }).isTrue()
        assertThat(result.overallConfidence).isEqualTo(0.50)
    }

    @Test
    fun `performance test 100 sequential enrichments complete within expected bounds`() {
        val candidate = TestFixtures.createCandidate()

        val mtResult = MerchantTypeResult("RESTAURANT", 1.0, "EXACT_MATCH", "Swiggy")
        val catResult = CategoryResult("FOOD", 1.0, "MERCHANT_TYPE", "Mapped")
        val hintResult = CarbonHintResult("FOOD_DELIVERY", null, null, null, null, 1.0, "MERCHANT_ALIAS", "Mapped")
        val confResult = EnrichmentConfidenceResult(0.95, ConfidenceLevel.VERY_HIGH, emptyMap(), emptyList(), emptyList())

        every { mockMerchantTypeEnricher.enrich(any()) } returns mtResult
        every { mockCategoryEnricher.enrich(any(), any()) } returns catResult
        every { mockCarbonHintEnricher.enrich(any(), any(), any()) } returns hintResult
        every { mockConfidenceCalculator.enrich(any(), any(), any(), any()) } returns confResult

        val timeMs = measureTimeMillis {
            for (i in 1..100) {
                orchestrator.enrich(candidate)
            }
        }
        
        // 100 mocks usually take a few milliseconds.
        assertThat(timeMs).isLessThan(500L) // Allowing enough headroom for JVM warmup / mock overhead
    }

    @Test
    fun `null values do not crash pipeline`() {
        val candidate = TestFixtures.createCandidate(merchant = null, amount = null, currency = null, rawNotification = "")
        val realOrchestrator = TransactionEnricher(
            MerchantTypeEnricher(),
            CategoryEnricher(),
            CarbonHintEnricher(),
            ConfidenceCalculator(EnrichmentConfidenceCalculator())
        )
        val result = realOrchestrator.enrich(candidate)
        assertThat(result.merchantType).isEqualTo("UNKNOWN")
        assertThat(result.category).isEqualTo("UNKNOWN")
        assertThat(result.warnings).isNotEmpty()
    }

    @Test
    fun `very long merchant names and unicode chars handle gracefully`() {
        val realOrchestrator = TransactionEnricher(
            MerchantTypeEnricher(),
            CategoryEnricher(),
            CarbonHintEnricher(),
            ConfidenceCalculator(EnrichmentConfidenceCalculator())
        )
        val longMerchant = "a".repeat(1000) + "🔥"
        val candidate = TestFixtures.createCandidate(merchant = longMerchant)
        val result = realOrchestrator.enrich(candidate)
        assertThat(result.merchantType).isEqualTo("UNKNOWN")
    }

    @Test
    fun `tamil merchant names do not crash`() {
        val realOrchestrator = TransactionEnricher(
            MerchantTypeEnricher(),
            CategoryEnricher(),
            CarbonHintEnricher(),
            ConfidenceCalculator(EnrichmentConfidenceCalculator())
        )
        val candidate = TestFixtures.createCandidate(merchant = "ஸ்விக்கி") // Tamil for Swiggy
        val result = realOrchestrator.enrich(candidate)
        assertThat(result.merchantType).isEqualTo("UNKNOWN")
    }
}
