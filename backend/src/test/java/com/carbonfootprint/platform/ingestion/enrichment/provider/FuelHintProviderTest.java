package com.carbonfootprint.platform.ingestion.enrichment.provider;

import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.ingestion.enrichment.CarbonHintContext;
import com.carbonfootprint.platform.ingestion.enrichment.CarbonHintProvider;
import com.carbonfootprint.platform.ingestion.enrichment.model.CarbonActivityType;
import com.carbonfootprint.platform.ingestion.enrichment.model.FuelType;
import com.carbonfootprint.platform.ingestion.enrichment.model.VehicleType;
import com.carbonfootprint.platform.ingestion.model.CarbonHints;
import com.carbonfootprint.platform.ingestion.model.ExtractionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class FuelHintProviderTest {

    private FuelHintProvider provider;

    @BeforeEach
    void setUp() {
        provider = new FuelHintProvider();
    }

    @Test
    void order_is10() {
        assertThat(provider.getOrder()).isEqualTo(10);
    }

    // ── Brand detection ──────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"Indian Oil", "HPCL", "Shell", "BPCL", "Reliance Petroleum", "Essar"})
    void provide_detectsFuelStationBrands(String merchant) {
        CarbonHints hints = provider.provide(contextWith(merchant, null, null, null));

        assertThat(hints.getActivityType()).isEqualTo(CarbonActivityType.FUEL);
        assertThat(hints.getConfidence()).isEqualTo(CarbonHintProvider.CONFIDENCE_MERCHANT_MATCH);
    }

    @Test
    void provide_unknownMerchantWithNoFuelKeyword_returnsEmpty() {
        CarbonHints hints = provider.provide(contextWith("Random Shop", null, null, null));

        assertThat(hints).isEqualTo(CarbonHints.empty());
    }

    // ── Fuel type detection ──────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
            "petrol, PETROL",
            "gasoline, PETROL",
            "diesel, DIESEL",
            "hsd, DIESEL",
            "lpg, LPG",
            "autogas, LPG",
            "cng, CNG",
            "compressed natural gas, CNG"
    })
    void provide_detectsFuelType(String keyword, FuelType expected) {
        CarbonHints hints = provider.provide(contextWith("Shell", keyword, null, null));

        assertThat(hints.getFuelType()).isEqualTo(expected);
    }

    @Test
    void provide_brandMatchedWithoutFuelKeyword_fuelTypeIsUnknown() {
        CarbonHints hints = provider.provide(contextWith("Shell", null, null, null));

        assertThat(hints.getFuelType()).isEqualTo(FuelType.UNKNOWN);
    }

    // ── Vehicle type detection ───────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
            "car, CAR",
            "bike, MOTORBIKE",
            "motorcycle, MOTORBIKE",
            "truck, TRUCK",
            "auto, AUTO_RICKSHAW"
    })
    void provide_detectsVehicleType(String keyword, VehicleType expected) {
        CarbonHints hints = provider.provide(contextWith("Shell", "petrol", keyword, null));

        assertThat(hints.getVehicleType()).isEqualTo(expected);
    }

    @Test
    void provide_noVehicleKeyword_vehicleTypeIsNull() {
        CarbonHints hints = provider.provide(contextWith("Shell", "petrol", null, null));

        assertThat(hints.getVehicleType()).isNull();
    }

    // ── Category signal ──────────────────────────────────────────────────

    @Test
    void provide_categoryFuelWithoutBrand_infersFuel() {
        CarbonHints hints = provider.provide(contextWith(null, null, null, ActivityCategory.FUEL));

        assertThat(hints.getActivityType()).isEqualTo(CarbonActivityType.FUEL);
        assertThat(hints.getConfidence()).isEqualTo(CarbonHintProvider.CONFIDENCE_CATEGORY_ONLY);
    }

    @Test
    void provide_fuelKeywordWithoutBrand_infersFuel() {
        // Use a description that doesn't contain any brand keyword ("petro" is a brand)
        CarbonHints hints = provider.provide(contextWith(null, "gasoline fill up", null, null));

        assertThat(hints.getActivityType()).isEqualTo(CarbonActivityType.FUEL);
        assertThat(hints.getConfidence()).isEqualTo(CarbonHintProvider.CONFIDENCE_KEYWORD_MATCH);
    }

    @Test
    void provide_noSignal_returnsEmpty() {
        CarbonHints hints = provider.provide(contextWith("Random Shop", "random item", null, null));

        assertThat(hints).isEqualTo(CarbonHints.empty());
    }

    // ── Unit ─────────────────────────────────────────────────────────────

    @Test
    void provide_alwaysSetsFuelUnitToLitre() {
        CarbonHints hints = provider.provide(contextWith("Shell", null, null, null));

        assertThat(hints.getFuelUnit()).isEqualTo("LITRE");
    }

    // ── Immutability ─────────────────────────────────────────────────────

    @Test
    void provide_doesNotModifyExtractionResult() {
        ExtractionResult original = ExtractionResult.builder()
                .merchant("Shell")
                .description("diesel")
                .build();

        provider.provide(CarbonHintContext.from(original));

        assertThat(original.getMerchant()).isEqualTo("Shell");
        assertThat(original.getDescription()).isEqualTo("diesel");
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
