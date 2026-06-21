package com.carbonwise.connect.ingestion.enrichment

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class EnrichmentConfidenceCalculatorTest {

    private val calculator = EnrichmentConfidenceCalculator()

    @Test
    fun `Very high confidence scenario`() {
        val candidate = TestFixtures.createCandidate()
        val mtResult = MerchantTypeResult("RESTAURANT", 0.95, "EXACT_MATCH", "Swiggy")
        val catResult = CategoryResult("FOOD", 0.90, "MERCHANT_TYPE", "Mapped")
        val hintResult = CarbonHintResult("FOOD_DELIVERY", null, null, null, null, 0.92, "MERCHANT_ALIAS", "Mapped")
        
        val result = calculator.calculate(candidate, mtResult, catResult, hintResult)
        assertThat(result.overallConfidence).isGreaterThan(0.90)
        assertThat(result.confidenceLevel).isEqualTo(ConfidenceLevel.VERY_HIGH)
    }

    @Test
    fun `Low confidence scenario`() {
        val candidate = TestFixtures.createCandidate(merchant = "UnknownStore")
        val mtResult = MerchantTypeResult("UNKNOWN", 0.0, null, null)
        val catResult = CategoryResult("UNKNOWN", 0.0, "None", "Mapped")
        val hintResult = CarbonHintResult("UNKNOWN", null, null, null, null, 0.0, "None", "Mapped")
        
        val result = calculator.calculate(candidate, mtResult, catResult, hintResult)
        assertThat(result.overallConfidence).isLessThan(0.30)
        assertThat(result.confidenceLevel).isEqualTo(ConfidenceLevel.VERY_LOW) // Or LOW based on exact match thresholds, but definitely poor
    }

    @Test
    fun `Missing merchant raises warning`() {
        val candidate = TestFixtures.createCandidate(merchant = null)
        val mtResult = MerchantTypeResult("UNKNOWN", 0.0, null, null)
        val catResult = CategoryResult("UNKNOWN", 0.0, "None", "Mapped")
        val hintResult = CarbonHintResult("UNKNOWN", null, null, null, null, 0.0, "None", "Mapped")
        
        val result = calculator.calculate(candidate, mtResult, catResult, hintResult)
        assertThat(result.warnings).contains("Unknown merchant")
    }

    @Test
    fun `Missing amount raises warning`() {
        val candidate = TestFixtures.createCandidate(amount = null)
        val mtResult = MerchantTypeResult("RESTAURANT", 0.95, "EXACT_MATCH", "Swiggy")
        val catResult = CategoryResult("FOOD", 0.90, "MERCHANT_TYPE", "Mapped")
        val hintResult = CarbonHintResult("FOOD_DELIVERY", null, null, null, null, 0.92, "MERCHANT_ALIAS", "Mapped")
        
        val result = calculator.calculate(candidate, mtResult, catResult, hintResult)
        assertThat(result.warnings).contains("Missing amount")
    }

    @Test
    fun `Missing currency raises warning`() {
        val candidate = TestFixtures.createCandidate(currency = null)
        val mtResult = MerchantTypeResult("RESTAURANT", 0.95, "EXACT_MATCH", "Swiggy")
        val catResult = CategoryResult("FOOD", 0.90, "MERCHANT_TYPE", "Mapped")
        val hintResult = CarbonHintResult("FOOD_DELIVERY", null, null, null, null, 0.92, "MERCHANT_ALIAS", "Mapped")
        
        val result = calculator.calculate(candidate, mtResult, catResult, hintResult)
        assertThat(result.warnings).contains("Unknown currency")
    }

    @Test
    fun `Missing transport mode raises warning`() {
        val candidate = TestFixtures.createCandidate(merchant = "Uber")
        val mtResult = MerchantTypeResult("TRANSPORT", 1.0, "EXACT_MATCH", "Uber")
        val catResult = CategoryResult("TRANSPORT", 1.0, "MERCHANT_TYPE", "Mapped")
        val hintResult = CarbonHintResult("RIDE_SHARE", null, null, null, null, 1.0, "MERCHANT_ALIAS", "Mapped")
        
        val result = calculator.calculate(candidate, mtResult, catResult, hintResult)
        assertThat(result.warnings).contains("No transport mode detected")
    }
}
