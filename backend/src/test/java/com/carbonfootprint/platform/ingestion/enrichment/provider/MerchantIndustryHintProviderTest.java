package com.carbonfootprint.platform.ingestion.enrichment.provider;

import com.carbonfootprint.platform.ingestion.enrichment.CarbonHintContext;
import com.carbonfootprint.platform.ingestion.enrichment.CarbonHintProvider;
import com.carbonfootprint.platform.ingestion.enrichment.model.MerchantIndustry;
import com.carbonfootprint.platform.ingestion.model.CarbonHints;
import com.carbonfootprint.platform.ingestion.model.ExtractionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class MerchantIndustryHintProviderTest {

    private MerchantIndustryHintProvider provider;

    @BeforeEach
    void setUp() {
        provider = new MerchantIndustryHintProvider();
    }

    @Test
    void order_is60() {
        assertThat(provider.getOrder()).isEqualTo(60);
    }

    // ── GROCERY ──────────────────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
            "BigBasket, GROCERY",
            "Blinkit, GROCERY",
            "DMart, GROCERY",
            "Zepto, GROCERY",
            "kirana store, GROCERY"
    })
    void provide_detectsGroceryBrands(String merchant, MerchantIndustry expected) {
        CarbonHints hints = provider.provide(contextWith(merchant, null));

        assertThat(hints.getMerchantIndustry()).isEqualTo(expected);
    }

    // ── RESTAURANT ───────────────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
            "Zomato, RESTAURANT",
            "Swiggy, RESTAURANT",
            "McDonalds, RESTAURANT",
            "KFC, RESTAURANT",
            "Starbucks, RESTAURANT",
            "Cafe Coffee Day, RESTAURANT"
    })
    void provide_detectsRestaurantBrands(String merchant, MerchantIndustry expected) {
        CarbonHints hints = provider.provide(contextWith(merchant, null));

        assertThat(hints.getMerchantIndustry()).isEqualTo(expected);
    }

    // ── CLOTHING ─────────────────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
            "Myntra, CLOTHING",
            "Zara, CLOTHING",
            "H&M, CLOTHING",
            "Uniqlo, CLOTHING",
            "Pantaloons, CLOTHING"
    })
    void provide_detectsClothingBrands(String merchant, MerchantIndustry expected) {
        CarbonHints hints = provider.provide(contextWith(merchant, null));

        assertThat(hints.getMerchantIndustry()).isEqualTo(expected);
    }

    // ── ELECTRONICS ──────────────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
            "Croma, ELECTRONICS",
            "Vijay Sales, ELECTRONICS",
            "Reliance Digital, ELECTRONICS",
            "Poorvika, ELECTRONICS"
    })
    void provide_detectsElectronicsBrands(String merchant, MerchantIndustry expected) {
        CarbonHints hints = provider.provide(contextWith(merchant, null));

        assertThat(hints.getMerchantIndustry()).isEqualTo(expected);
    }

    // ── PHARMACY ─────────────────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
            "Apollo Pharmacy, PHARMACY",
            "MedPlus, PHARMACY",
            "1mg, PHARMACY",
            "PharmEasy, PHARMACY"
    })
    void provide_detectsPharmacyBrands(String merchant, MerchantIndustry expected) {
        CarbonHints hints = provider.provide(contextWith(merchant, null));

        assertThat(hints.getMerchantIndustry()).isEqualTo(expected);
    }

    // ── FURNITURE ────────────────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
            "IKEA, FURNITURE",
            "Urban Ladder, FURNITURE",
            "Pepperfry, FURNITURE"
    })
    void provide_detectsFurnitureBrands(String merchant, MerchantIndustry expected) {
        CarbonHints hints = provider.provide(contextWith(merchant, null));

        assertThat(hints.getMerchantIndustry()).isEqualTo(expected);
    }

    // ── Merchant match confidence ────────────────────────────────────────

    @Test
    void provide_merchantMatch_setsHighConfidence() {
        CarbonHints hints = provider.provide(contextWith("BigBasket", null));

        assertThat(hints.getConfidence()).isEqualTo(CarbonHintProvider.CONFIDENCE_MERCHANT_MATCH);
    }

    @Test
    void provide_descriptionMatch_setsKeywordConfidence() {
        CarbonHints hints = provider.provide(contextWith(null, "supermarket grocery shopping"));

        assertThat(hints.getConfidence()).isEqualTo(CarbonHintProvider.CONFIDENCE_KEYWORD_MATCH);
    }

    // ── No signal ────────────────────────────────────────────────────────

    @Test
    void provide_noSignal_returnsEmpty() {
        CarbonHints hints = provider.provide(contextWith("Random Shop", "random item"));

        assertThat(hints).isEqualTo(CarbonHints.empty());
    }

    // ── Immutability ─────────────────────────────────────────────────────

    @Test
    void provide_doesNotModifyExtractionResult() {
        ExtractionResult original = ExtractionResult.builder()
                .merchant("BigBasket")
                .build();

        provider.provide(CarbonHintContext.from(original));

        assertThat(original.getMerchant()).isEqualTo("BigBasket");
    }

    // ── Helper ───────────────────────────────────────────────────────────

    private CarbonHintContext contextWith(String merchant, String description) {
        return CarbonHintContext.from(ExtractionResult.builder()
                .merchant(merchant)
                .description(description)
                .build());
    }
}
