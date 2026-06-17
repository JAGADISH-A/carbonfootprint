package com.carbonfootprint.platform.ingestion.enrichment.provider;

import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.ingestion.enrichment.CarbonHintContext;
import com.carbonfootprint.platform.ingestion.enrichment.CarbonHintProvider;
import com.carbonfootprint.platform.ingestion.enrichment.model.CarbonActivityType;
import com.carbonfootprint.platform.ingestion.model.CarbonHints;
import com.carbonfootprint.platform.ingestion.model.ExtractionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ShoppingHintProviderTest {

    private ShoppingHintProvider provider;

    @BeforeEach
    void setUp() {
        provider = new ShoppingHintProvider();
    }

    @Test
    void order_is50() {
        assertThat(provider.getOrder()).isEqualTo(50);
    }

    // ── Brand detection ──────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"Flipkart", "Amazon", "Myntra", "Ajio", "Nykaa", "Meesho", "IKEA"})
    void provide_detectsShoppingBrands(String merchant) {
        CarbonHints hints = provider.provide(contextWith(merchant, null, null, null));

        assertThat(hints.getActivityType()).isEqualTo(CarbonActivityType.SHOPPING);
        assertThat(hints.getConfidence()).isEqualTo(CarbonHintProvider.CONFIDENCE_MERCHANT_MATCH);
    }

    // ── Keyword signal ───────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"online shopping", "e-commerce", "order #123", "purchase", "delivery charges"})
    void provide_detectsShoppingKeywords(String keyword) {
        CarbonHints hints = provider.provide(contextWith(null, keyword, null, null));

        assertThat(hints.getActivityType()).isEqualTo(CarbonActivityType.SHOPPING);
        assertThat(hints.getConfidence()).isEqualTo(CarbonHintProvider.CONFIDENCE_KEYWORD_MATCH);
    }

    // ── Category signal ──────────────────────────────────────────────────

    @Test
    void provide_categoryShopping_infersShopping() {
        CarbonHints hints = provider.provide(contextWith(null, null, null, ActivityCategory.SHOPPING));

        assertThat(hints.getActivityType()).isEqualTo(CarbonActivityType.SHOPPING);
        assertThat(hints.getConfidence()).isEqualTo(CarbonHintProvider.CONFIDENCE_CATEGORY_ONLY);
    }

    // ── Category filtering ───────────────────────────────────────────────

    @Test
    void provide_wrongCategory_returnsEmpty() {
        CarbonHints hints = provider.provide(contextWith("Amazon", null, null, ActivityCategory.FUEL));

        assertThat(hints).isEqualTo(CarbonHints.empty());
    }

    @Test
    void provide_nullCategoryWithKeyword_infersShopping() {
        CarbonHints hints = provider.provide(contextWith(null, "online shopping", null, null));

        assertThat(hints.getActivityType()).isEqualTo(CarbonActivityType.SHOPPING);
    }

    // ── No signal ────────────────────────────────────────────────────────

    @Test
    void provide_noSignal_returnsEmpty() {
        CarbonHints hints = provider.provide(contextWith("Random Shop", "random item", null, null));

        assertThat(hints).isEqualTo(CarbonHints.empty());
    }

    // ── Immutability ─────────────────────────────────────────────────────

    @Test
    void provide_doesNotModifyExtractionResult() {
        ExtractionResult original = ExtractionResult.builder()
                .merchant("Amazon")
                .build();

        provider.provide(CarbonHintContext.from(original));

        assertThat(original.getMerchant()).isEqualTo("Amazon");
    }

    // ── Helper ───────────────────────────────────────────────────────────

    private CarbonHintContext contextWith(String merchant, String description,
                                           String unit, ActivityCategory category) {
        return CarbonHintContext.from(ExtractionResult.builder()
                .merchant(merchant)
                .description(description)
                .unit(unit)
                .category(category)
                .build());
    }
}
