package com.carbonfootprint.platform.ingestion.enrichment.provider;

import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.ingestion.enrichment.CarbonHintContext;
import com.carbonfootprint.platform.ingestion.enrichment.CarbonHintProvider;
import com.carbonfootprint.platform.ingestion.enrichment.model.CabinClass;
import com.carbonfootprint.platform.ingestion.enrichment.model.CarbonActivityType;
import com.carbonfootprint.platform.ingestion.model.CarbonHints;
import com.carbonfootprint.platform.ingestion.model.ExtractionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class FlightHintProviderTest {

    private FlightHintProvider provider;

    @BeforeEach
    void setUp() {
        provider = new FlightHintProvider();
    }

    @Test
    void order_is30() {
        assertThat(provider.getOrder()).isEqualTo(30);
    }

    // ── Brand detection ──────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"IndiGo", "Air India", "Vistara", "Emirates", "Lufthansa", "SpiceJet"})
    void provide_detectsAirlineBrands(String merchant) {
        CarbonHints hints = provider.provide(contextWith(merchant, null, null, null));

        assertThat(hints.getActivityType()).isEqualTo(CarbonActivityType.FLIGHT);
        assertThat(hints.getConfidence()).isEqualTo(CarbonHintProvider.CONFIDENCE_MERCHANT_MATCH);
    }

    @Test
    void provide_genericAirlinesKeyword_infersFlight() {
        CarbonHints hints = provider.provide(contextWith(null, "airlines ticket", null, null));

        assertThat(hints.getActivityType()).isEqualTo(CarbonActivityType.FLIGHT);
    }

    // ── Category signal ──────────────────────────────────────────────────

    @Test
    void provide_categoryFlight_infersFlight() {
        CarbonHints hints = provider.provide(contextWith(null, null, null, ActivityCategory.FLIGHT));

        assertThat(hints.getActivityType()).isEqualTo(CarbonActivityType.FLIGHT);
        assertThat(hints.getConfidence()).isEqualTo(CarbonHintProvider.CONFIDENCE_CATEGORY_ONLY);
    }

    @Test
    void provide_noSignal_returnsEmpty() {
        CarbonHints hints = provider.provide(contextWith("Random Shop", null, null, null));

        assertThat(hints).isEqualTo(CarbonHints.empty());
    }

    // ── Cabin class detection ────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
            "economy, ECONOMY",
            "economy class, ECONOMY",
            "eco class, ECONOMY",
            "premium economy, PREMIUM_ECONOMY",
            "premium eco, PREMIUM_ECONOMY",
            "business class, BUSINESS",
            "biz class, BUSINESS",
            "club class, BUSINESS",
            "first class, FIRST"
    })
    void provide_detectsCabinClass(String keyword, CabinClass expected) {
        CarbonHints hints = provider.provide(contextWith("IndiGo", keyword, null, null));

        assertThat(hints.getCabinClass()).isEqualTo(expected);
    }

    @Test
    void provide_standaloneFirstKeyword_matchesFirstClass() {
        CarbonHints hints = provider.provide(contextWith("Air India", "first", null, null));

        assertThat(hints.getCabinClass()).isEqualTo(CabinClass.FIRST);
    }

    @Test
    void provide_firstNameDoesNotMatch() {
        CarbonHints hints = provider.provide(contextWith("Air India", "first name", null, null));

        assertThat(hints.getCabinClass()).isEqualTo(CabinClass.ECONOMY);
    }

    @Test
    void provide_noCabinKeyword_defaultsToEconomy() {
        CarbonHints hints = provider.provide(contextWith("IndiGo", null, null, null));

        assertThat(hints.getCabinClass()).isEqualTo(CabinClass.ECONOMY);
    }

    // ── Passenger count detection ────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"2 passengers", "3 pax", "for 1 adult", "adults: 2"})
    void provide_detectsPassengerCount(String keyword) {
        CarbonHints hints = provider.provide(contextWith("IndiGo", keyword, null, null));

        assertThat(hints.getPassengerCount()).isBetween(1, 9);
    }

    @Test
    void provide_passengerCountExceeding9_returnsNull() {
        CarbonHints hints = provider.provide(contextWith("IndiGo", "15 passengers", null, null));

        assertThat(hints.getPassengerCount()).isNull();
    }

    @Test
    void provide_noPassengerKeyword_returnsNull() {
        CarbonHints hints = provider.provide(contextWith("IndiGo", "economy", null, null));

        assertThat(hints.getPassengerCount()).isNull();
    }

    // ── Immutability ─────────────────────────────────────────────────────

    @Test
    void provide_doesNotModifyExtractionResult() {
        ExtractionResult original = ExtractionResult.builder()
                .merchant("IndiGo")
                .description("2 passengers economy")
                .build();

        provider.provide(CarbonHintContext.from(original));

        assertThat(original.getMerchant()).isEqualTo("IndiGo");
        assertThat(original.getDescription()).isEqualTo("2 passengers economy");
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
