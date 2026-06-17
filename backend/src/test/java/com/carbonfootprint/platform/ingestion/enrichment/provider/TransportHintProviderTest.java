package com.carbonfootprint.platform.ingestion.enrichment.provider;

import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.ingestion.enrichment.CarbonHintContext;
import com.carbonfootprint.platform.ingestion.enrichment.CarbonHintProvider;
import com.carbonfootprint.platform.ingestion.enrichment.model.CarbonActivityType;
import com.carbonfootprint.platform.ingestion.enrichment.model.TransportMode;
import com.carbonfootprint.platform.ingestion.model.CarbonHints;
import com.carbonfootprint.platform.ingestion.model.ExtractionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class TransportHintProviderTest {

    private TransportHintProvider provider;

    @BeforeEach
    void setUp() {
        provider = new TransportHintProvider();
    }

    @Test
    void order_is40() {
        assertThat(provider.getOrder()).isEqualTo(40);
    }

    // ── Transport mode detection ─────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
            "KSRTC, BUS",
            "BMTC, BUS",
            "RedBus, BUS",
            "volvo bus, BUS",
            "DMRC, METRO",
            "metro rail, METRO",
            "IRCTC, TRAIN",
            "indian railways, TRAIN",
            "uber, TAXI",
            "ola cabs, TAXI",
            "rapido, TAXI",
            "cab, TAXI",
            "autorickshaw, AUTO",
            "auto rickshaw, AUTO",
            "tuk tuk, AUTO",
            "yulu, BIKE",
            "bicycle, BIKE",
            "cycle, BIKE"
    })
    void provide_detectsTransportMode(String keyword, TransportMode expected) {
        CarbonHints hints = provider.provide(contextWith(null, keyword, null, null));

        assertThat(hints.getActivityType()).isEqualTo(CarbonActivityType.TRANSPORT);
        assertThat(hints.getTransportMode()).isEqualTo(expected);
    }

    // ── Generic keywords (lower specificity) ──────────────────────────────

    @ParameterizedTest
    @CsvSource({
            "metro, METRO",
            "train, TRAIN",
            "taxi, TAXI",
            "auto, AUTO",
            "bike, BIKE",
            "bus, BUS"
    })
    void provide_detectsGenericKeywords(String keyword, TransportMode expected) {
        CarbonHints hints = provider.provide(contextWith(null, keyword, null, null));

        assertThat(hints.getTransportMode()).isEqualTo(expected);
    }

    // ── Category signal ──────────────────────────────────────────────────

    @Test
    void provide_categoryTransportWithoutKeyword_infersTransportWithNullMode() {
        CarbonHints hints = provider.provide(contextWith(null, null, null, ActivityCategory.TRANSPORT));

        assertThat(hints.getActivityType()).isEqualTo(CarbonActivityType.TRANSPORT);
        assertThat(hints.getTransportMode()).isNull();
        assertThat(hints.getConfidence()).isEqualTo(CarbonHintProvider.CONFIDENCE_CATEGORY_ONLY);
    }

    // ── Category filtering ───────────────────────────────────────────────

    @Test
    void provide_wrongCategory_returnsEmpty() {
        CarbonHints hints = provider.provide(contextWith(null, "uber ride", null, ActivityCategory.FUEL));

        assertThat(hints).isEqualTo(CarbonHints.empty());
    }

    @Test
    void provide_nullCategoryWithKeyword_infersTransport() {
        CarbonHints hints = provider.provide(contextWith(null, "uber ride", null, null));

        assertThat(hints.getActivityType()).isEqualTo(CarbonActivityType.TRANSPORT);
    }

    // ── Merchant match confidence ────────────────────────────────────────

    @Test
    void provide_merchantMatch_setsHighConfidence() {
        CarbonHints hints = provider.provide(contextWith("Uber", null, null, null));

        assertThat(hints.getConfidence()).isEqualTo(CarbonHintProvider.CONFIDENCE_MERCHANT_MATCH);
    }

    @Test
    void provide_descriptionMatch_setsKeywordConfidence() {
        CarbonHints hints = provider.provide(contextWith(null, "uber ride", null, null));

        assertThat(hints.getConfidence()).isEqualTo(CarbonHintProvider.CONFIDENCE_KEYWORD_MATCH);
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
                .merchant("Uber")
                .description("ride to airport")
                .build();

        provider.provide(CarbonHintContext.from(original));

        assertThat(original.getMerchant()).isEqualTo("Uber");
        assertThat(original.getDescription()).isEqualTo("ride to airport");
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
