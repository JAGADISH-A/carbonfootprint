package com.carbonwise.connect.ingestion.enrichment

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class MerchantTypeEnricherTest {

    private val enricher = MerchantTypeEnricher()

    @Test
    fun `exact merchant match returns correct type`() {
        val candidate = TestFixtures.createCandidate(merchant = "Swiggy")
        val result = enricher.enrich(candidate)
        assertThat(result.merchantType).isEqualTo("RESTAURANT")
        assertThat(result.matchedRule).isEqualTo("EXACT_MATCH")
        assertThat(result.confidence).isEqualTo(1.0)
    }

    @Test
    fun `case-insensitive match works`() {
        val candidate = TestFixtures.createCandidate(merchant = "SWIGGY")
        val result = enricher.enrich(candidate)
        assertThat(result.merchantType).isEqualTo("RESTAURANT")
        assertThat(result.matchedRule).isEqualTo("EXACT_MATCH")
    }

    @Test
    fun `partial match returns correct type`() {
        val candidate = TestFixtures.createCandidate(merchant = "Amazon India")
        val result = enricher.enrich(candidate)
        assertThat(result.merchantType).isEqualTo("ECOMMERCE")
        assertThat(result.matchedRule).isEqualTo("PARTIAL_MATCH")
        assertThat(result.confidence).isEqualTo(0.95)
    }

    @Test
    fun `alias match works`() {
        val candidate = TestFixtures.createCandidate(merchant = "Swiggy UPI")
        val result = enricher.enrich(candidate)
        assertThat(result.merchantType).isEqualTo("RESTAURANT")
        assertThat(result.matchedRule).isEqualTo("PARTIAL_MATCH")
    }

    @Test
    fun `unknown merchant returns UNKNOWN`() {
        val candidate = TestFixtures.createCandidate(merchant = "Some Random Shop")
        val result = enricher.enrich(candidate)
        assertThat(result.merchantType).isEqualTo("UNKNOWN")
        assertThat(result.confidence).isEqualTo(0.0)
    }

    @Test
    fun `empty merchant returns UNKNOWN`() {
        val candidate = TestFixtures.createCandidate(merchant = "")
        val result = enricher.enrich(candidate)
        assertThat(result.merchantType).isEqualTo("UNKNOWN")
    }

    @Test
    fun `null merchant falls back to rawNotification text`() {
        val candidate = TestFixtures.createCandidate(merchant = null, rawNotification = "Paid swiggy")
        val result = enricher.enrich(candidate)
        assertThat(result.merchantType).isEqualTo("RESTAURANT")
    }
}
