package com.carbonwise.connect.ingestion.enrichment

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class CarbonHintEnricherTest {

    private val enricher = CarbonHintEnricher()

    @Test
    fun `Swiggy maps to FOOD_DELIVERY`() {
        val candidate = TestFixtures.createCandidate(merchant = "Swiggy")
        val mtResult = MerchantTypeResult("RESTAURANT", 1.0, "EXACT_MATCH", "Swiggy")
        val catResult = CategoryResult("FOOD", 1.0, "MERCHANT_TYPE", "Mapped from type")
        
        val result = enricher.enrich(candidate, mtResult, catResult)
        assertThat(result.carbonHint).isEqualTo("FOOD_DELIVERY")
    }

    @Test
    fun `Zomato maps to FOOD_DELIVERY`() {
        val candidate = TestFixtures.createCandidate(merchant = "Zomato")
        val mtResult = MerchantTypeResult("RESTAURANT", 1.0, "EXACT_MATCH", "Zomato")
        val catResult = CategoryResult("FOOD", 1.0, "MERCHANT_TYPE", "Mapped from type")
        
        val result = enricher.enrich(candidate, mtResult, catResult)
        assertThat(result.carbonHint).isEqualTo("FOOD_DELIVERY")
    }

    @Test
    fun `Starbucks maps to CAFE_VISIT`() {
        val candidate = TestFixtures.createCandidate(merchant = "Starbucks")
        val mtResult = MerchantTypeResult("CAFE", 1.0, "EXACT_MATCH", "Starbucks")
        val catResult = CategoryResult("FOOD", 1.0, "MERCHANT_TYPE", "Mapped from type")
        
        val result = enricher.enrich(candidate, mtResult, catResult)
        assertThat(result.carbonHint).isEqualTo("CAFE_VISIT")
    }

    @Test
    fun `Uber maps to RIDE_SHARE`() {
        val candidate = TestFixtures.createCandidate(merchant = "Uber")
        val mtResult = MerchantTypeResult("TRANSPORT", 1.0, "EXACT_MATCH", "Uber")
        val catResult = CategoryResult("TRANSPORT", 1.0, "MERCHANT_TYPE", "Mapped from type")
        
        val result = enricher.enrich(candidate, mtResult, catResult)
        assertThat(result.carbonHint).isEqualTo("RIDE_SHARE")
        assertThat(result.transportMode).isEqualTo("CAR")
    }

    @Test
    fun `Rapido maps to RIDE_SHARE with bike mode`() {
        val candidate = TestFixtures.createCandidate(merchant = "Rapido")
        val mtResult = MerchantTypeResult("TRANSPORT", 1.0, "EXACT_MATCH", "Rapido")
        val catResult = CategoryResult("TRANSPORT", 1.0, "MERCHANT_TYPE", "Mapped")
        
        val result = enricher.enrich(candidate, mtResult, catResult)
        assertThat(result.carbonHint).isEqualTo("RIDE_SHARE")
        assertThat(result.transportMode).isEqualTo("BIKE")
    }

    @Test
    fun `IRCTC maps to TRAIN_TRAVEL`() {
        val candidate = TestFixtures.createCandidate(merchant = "irctc")
        val mtResult = MerchantTypeResult("RAILWAY", 1.0, "EXACT_MATCH", "irctc")
        val catResult = CategoryResult("TRAIN", 1.0, "MERCHANT_TYPE", "Mapped")
        
        val result = enricher.enrich(candidate, mtResult, catResult)
        assertThat(result.carbonHint).isEqualTo("TRAIN_TRAVEL")
        assertThat(result.transportMode).isEqualTo("TRAIN")
    }

    @Test
    fun `Indian Oil maps to FUEL_PURCHASE`() {
        val candidate = TestFixtures.createCandidate(merchant = "Indian Oil")
        val mtResult = MerchantTypeResult("FUEL", 1.0, "EXACT_MATCH", "indian oil")
        val catResult = CategoryResult("FUEL", 1.0, "MERCHANT_TYPE", "Mapped")
        
        val result = enricher.enrich(candidate, mtResult, catResult)
        assertThat(result.carbonHint).isEqualTo("FUEL_PURCHASE")
    }

    @Test
    fun `Electricity Board maps to ELECTRICITY_PAYMENT`() {
        val candidate = TestFixtures.createCandidate(merchant = "Electricity Board")
        val mtResult = MerchantTypeResult("ELECTRICITY", 1.0, "EXACT_MATCH", "electricity board")
        val catResult = CategoryResult("ELECTRICITY", 1.0, "MERCHANT_TYPE", "Mapped")
        
        val result = enricher.enrich(candidate, mtResult, catResult)
        assertThat(result.carbonHint).isEqualTo("ELECTRICITY_PAYMENT")
    }

    @Test
    fun `Amazon maps to ONLINE_SHOPPING`() {
        val candidate = TestFixtures.createCandidate(merchant = "Amazon")
        val mtResult = MerchantTypeResult("ECOMMERCE", 1.0, "EXACT_MATCH", "amazon")
        val catResult = CategoryResult("SHOPPING", 1.0, "MERCHANT_TYPE", "Mapped")
        
        val result = enricher.enrich(candidate, mtResult, catResult)
        assertThat(result.carbonHint).isEqualTo("ONLINE_SHOPPING")
    }

    @Test
    fun `Blinkit maps to GROCERY_DELIVERY`() {
        val candidate = TestFixtures.createCandidate(merchant = "Blinkit")
        val mtResult = MerchantTypeResult("GROCERY", 1.0, "EXACT_MATCH", "blinkit")
        val catResult = CategoryResult("GROCERY", 1.0, "MERCHANT_TYPE", "Mapped")
        
        val result = enricher.enrich(candidate, mtResult, catResult)
        assertThat(result.carbonHint).isEqualTo("GROCERY_DELIVERY")
    }

    @Test
    fun `Unknown maps to UNKNOWN`() {
        val candidate = TestFixtures.createCandidate()
        val mtResult = MerchantTypeResult("UNKNOWN", 0.0, null, null)
        val catResult = CategoryResult("UNKNOWN", 0.0, "None", "None")
        
        val result = enricher.enrich(candidate, mtResult, catResult)
        assertThat(result.carbonHint).isEqualTo("UNKNOWN")
    }
}
