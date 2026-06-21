package com.carbonwise.connect.ingestion.enrichment

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class CategoryEnricherTest {

    private val enricher = CategoryEnricher()

    @Test
    fun `Restaurant maps to FOOD`() {
        val candidate = TestFixtures.createCandidate()
        val mtResult = MerchantTypeResult("RESTAURANT", 1.0, "EXACT_MATCH", "Swiggy")
        val result = enricher.enrich(candidate, mtResult)
        assertThat(result.category).isEqualTo("FOOD")
        assertThat(result.matchedRule).isEqualTo("MERCHANT_TYPE")
    }

    @Test
    fun `Cafe maps to FOOD`() {
        val candidate = TestFixtures.createCandidate()
        val mtResult = MerchantTypeResult("CAFE", 1.0, "EXACT_MATCH", "Starbucks")
        val result = enricher.enrich(candidate, mtResult)
        assertThat(result.category).isEqualTo("FOOD")
    }

    @Test
    fun `Uber maps to TRANSPORT`() {
        val candidate = TestFixtures.createCandidate(merchant = "Uber")
        val mtResult = MerchantTypeResult("TRANSPORT", 1.0, "EXACT_MATCH", "Uber")
        val result = enricher.enrich(candidate, mtResult)
        assertThat(result.category).isEqualTo("TRANSPORT")
    }

    @Test
    fun `Amazon maps to SHOPPING`() {
        val candidate = TestFixtures.createCandidate(merchant = "Amazon")
        val mtResult = MerchantTypeResult("ECOMMERCE", 1.0, "EXACT_MATCH", "Amazon")
        val result = enricher.enrich(candidate, mtResult)
        assertThat(result.category).isEqualTo("SHOPPING")
    }

    @Test
    fun `Amazon Fresh maps to GROCERY due to keyword priority`() {
        val candidate = TestFixtures.createCandidate(merchant = "Amazon Fresh")
        val mtResult = MerchantTypeResult("ECOMMERCE", 0.95, "PARTIAL_MATCH", "Amazon")
        val result = enricher.enrich(candidate, mtResult)
        assertThat(result.category).isEqualTo("GROCERY")
        assertThat(result.matchedRule).isEqualTo("EXPLICIT_KEYWORD")
    }

    @Test
    fun `IRCTC maps to TRAIN`() {
        val candidate = TestFixtures.createCandidate()
        val mtResult = MerchantTypeResult("RAILWAY", 1.0, "EXACT_MATCH", "IRCTC")
        val result = enricher.enrich(candidate, mtResult)
        assertThat(result.category).isEqualTo("TRAIN")
    }

    @Test
    fun `Air India maps to FLIGHT`() {
        val candidate = TestFixtures.createCandidate()
        val mtResult = MerchantTypeResult("AIRLINE", 1.0, "EXACT_MATCH", "Air India")
        val result = enricher.enrich(candidate, mtResult)
        assertThat(result.category).isEqualTo("FLIGHT")
    }

    @Test
    fun `Unknown maps to UNKNOWN`() {
        val candidate = TestFixtures.createCandidate()
        val mtResult = MerchantTypeResult("UNKNOWN", 0.0, null, null)
        val result = enricher.enrich(candidate, mtResult)
        assertThat(result.category).isEqualTo("UNKNOWN")
        assertThat(result.confidence).isEqualTo(0.0)
    }
}
