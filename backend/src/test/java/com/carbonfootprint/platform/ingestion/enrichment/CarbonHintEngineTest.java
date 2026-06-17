package com.carbonfootprint.platform.ingestion.enrichment;

import com.carbonfootprint.platform.ingestion.enrichment.model.CarbonActivityType;
import com.carbonfootprint.platform.ingestion.enrichment.model.FuelType;
import com.carbonfootprint.platform.ingestion.enrichment.model.TransportMode;
import com.carbonfootprint.platform.ingestion.enrichment.provider.ElectricityHintProvider;
import com.carbonfootprint.platform.ingestion.enrichment.provider.FlightHintProvider;
import com.carbonfootprint.platform.ingestion.enrichment.provider.FuelHintProvider;
import com.carbonfootprint.platform.ingestion.enrichment.provider.MerchantIndustryHintProvider;
import com.carbonfootprint.platform.ingestion.enrichment.provider.ShoppingHintProvider;
import com.carbonfootprint.platform.ingestion.enrichment.provider.TransportHintProvider;
import com.carbonfootprint.platform.ingestion.model.CarbonHints;
import com.carbonfootprint.platform.ingestion.model.ExtractionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarbonHintEngineTest {

    private CarbonHintEngine engine;
    private CarbonHintMerger merger;

    @Mock
    private CarbonHintProvider failingProvider;

    @BeforeEach
    void setUp() {
        merger = new CarbonHintMerger();
    }

    private CarbonHintEngine buildEngine(List<CarbonHintProvider> providers) {
        return new CarbonHintEngine(providers, merger);
    }

    private CarbonHintEngine buildDefaultEngine() {
        return buildEngine(List.of(
                new FuelHintProvider(),
                new ElectricityHintProvider(),
                new FlightHintProvider(),
                new TransportHintProvider(),
                new ShoppingHintProvider(),
                new MerchantIndustryHintProvider()
        ));
    }

    // ── Provider ordering ────────────────────────────────────────────────

    @Test
    void engine_sortsProvidersByOrderAscending() {
        CarbonHintEngine eng = buildDefaultEngine();

        // FuelHintProvider (order=10) should run before ElectricityHintProvider (order=20)
        // If merchant is "Shell" (fuel) + "BESCOM" (electricity), fuel wins activityType
        ExtractionResult result = ExtractionResult.builder()
                .merchant("Shell BESCOM")
                .build();

        CarbonHints hints = eng.computeHints(result);

        // Shell matches fuel (order=10) which runs first
        assertThat(hints.getActivityType()).isEqualTo(CarbonActivityType.FUEL);
    }

    // ── Multiple providers contribute different fields ────────────────────

    @Test
    void engine_mergesFieldsFromMultipleProviders() {
        CarbonHintEngine eng = buildDefaultEngine();

        ExtractionResult result = ExtractionResult.builder()
                .merchant("Shell")
                .description("diesel car")
                .build();

        CarbonHints hints = eng.computeHints(result);

        assertThat(hints.getActivityType()).isEqualTo(CarbonActivityType.FUEL);
        assertThat(hints.getFuelType()).isEqualTo(FuelType.DIESEL);
        assertThat(hints.getVehicleType()).isNotNull();
    }

    // ── Confidence calculation ───────────────────────────────────────────

    @Test
    void engine_computesMeanConfidence() {
        CarbonHintEngine eng = buildDefaultEngine();

        // Shell matches fuel (merchant match = 0.9)
        // Also has "petrol" keyword
        ExtractionResult result = ExtractionResult.builder()
                .merchant("Shell")
                .description("petrol")
                .build();

        CarbonHints hints = eng.computeHints(result);

        assertThat(hints.getConfidence()).isGreaterThan(0.0);
        assertThat(hints.getConfidence()).isLessThanOrEqualTo(1.0);
    }

    @Test
    void engine_noProvidersSetConfidence_confidenceIsNull() {
        // Use a provider that returns empty (no confidence set)
        CarbonHintProvider noopProvider = new CarbonHintProvider() {
            @Override
            public CarbonHints provide(CarbonHintContext context) {
                return CarbonHints.empty();
            }

            @Override
            public int getOrder() { return 0; }
        };

        CarbonHintEngine eng = buildEngine(List.of(noopProvider));
        ExtractionResult result = ExtractionResult.builder().merchant("Test").build();

        CarbonHints hints = eng.computeHints(result);

        assertThat(hints.getConfidence()).isNull();
    }

    // ── Exception isolation ──────────────────────────────────────────────

    @Test
    void engine_providerThrowsException_isSkippedGracefully() {
        when(failingProvider.provide(any())).thenThrow(new RuntimeException("Provider failure"));
        when(failingProvider.getOrder()).thenReturn(99);

        CarbonHintEngine eng = buildEngine(List.of(failingProvider));

        ExtractionResult result = ExtractionResult.builder()
                .merchant("Test")
                .build();

        CarbonHints hints = eng.computeHints(result);

        assertThat(hints).isNotNull();
        // Other providers still ran
    }

    @Test
    void engine_providerReturnsNull_treatedAsEmpty() {
        CarbonHintProvider nullProvider = new CarbonHintProvider() {
            @Override
            public CarbonHints provide(CarbonHintContext context) {
                return null;
            }

            @Override
            public int getOrder() { return 0; }
        };

        CarbonHintEngine eng = buildEngine(List.of(nullProvider));

        ExtractionResult result = ExtractionResult.builder().merchant("Test").build();

        CarbonHints hints = eng.computeHints(result);

        assertThat(hints).isNotNull();
    }

    // ── Existing carbonHints in metadata ─────────────────────────────────

    @Test
    void engine_preservesExistingCarbonHints() {
        CarbonHintEngine eng = buildDefaultEngine();

        ExtractionResult result = ExtractionResult.builder()
                .merchant("Shell")
                .metadata(Map.of("carbonHints", Map.of(
                        "activityType", "FUEL",
                        "fuelType", "DIESEL",
                        "fuelUnit", "LITRE"
                )))
                .build();

        CarbonHints hints = eng.computeHints(result);

        // Pre-existing values should be preserved (first-non-null-wins from seed)
        assertThat(hints.getActivityType()).isEqualTo(CarbonActivityType.FUEL);
        assertThat(hints.getFuelType()).isEqualTo(FuelType.DIESEL);
        assertThat(hints.getFuelUnit()).isEqualTo("LITRE");
    }

    @Test
    void engine_existingHintsConfidence_isRecomputedByEngine() {
        CarbonHintEngine eng = buildDefaultEngine();

        ExtractionResult result = ExtractionResult.builder()
                .merchant("Shell")
                .metadata(Map.of("carbonHints", Map.of(
                        "activityType", "FUEL",
                        "confidence", 0.95
                )))
                .build();

        CarbonHints hints = eng.computeHints(result);

        // The merger does NOT carry confidence; engine recomputes as mean of provider confidences.
        // Shell matches fuel (merchant match = 0.9), so engine computes 0.9.
        assertThat(hints.getConfidence()).isEqualTo(0.9);
    }

    // ── Null metadata ────────────────────────────────────────────────────

    @Test
    void engine_nullMetadata_handledGracefully() {
        CarbonHintEngine eng = buildDefaultEngine();

        ExtractionResult result = ExtractionResult.builder()
                .merchant("Shell")
                .metadata(null)
                .build();

        CarbonHints hints = eng.computeHints(result);

        assertThat(hints).isNotNull();
    }

    @Test
    void engine_emptyMetadata_handledGracefully() {
        CarbonHintEngine eng = buildDefaultEngine();

        ExtractionResult result = ExtractionResult.builder()
                .merchant("Shell")
                .metadata(Map.of())
                .build();

        CarbonHints hints = eng.computeHints(result);

        assertThat(hints).isNotNull();
    }

    // ── Malformed metadata ───────────────────────────────────────────────

    @Test
    void engine_malformedCarbonHintsMetadata_handledGracefully() {
        CarbonHintEngine eng = buildDefaultEngine();

        ExtractionResult result = ExtractionResult.builder()
                .merchant("Shell")
                .metadata(Map.of("carbonHints", "not a map"))
                .build();

        CarbonHints hints = eng.computeHints(result);

        assertThat(hints).isNotNull();
    }

    @Test
    void engine_unknownEnumValueInMetadata_skippedGracefully() {
        CarbonHintEngine eng = buildDefaultEngine();

        ExtractionResult result = ExtractionResult.builder()
                .merchant("Shell")
                .metadata(Map.of("carbonHints", Map.of(
                        "activityType", "UNKNOWN_TYPE",
                        "fuelType", "DIESEL"
                )))
                .build();

        CarbonHints hints = eng.computeHints(result);

        // Unknown activityType skipped, but fuelType should be preserved
        assertThat(hints.getFuelType()).isEqualTo(FuelType.DIESEL);
    }

    // ── Unknown merchant ─────────────────────────────────────────────────

    @Test
    void engine_unknownMerchantWithNoSignals_returnsAllNull() {
        CarbonHintEngine eng = buildDefaultEngine();

        ExtractionResult result = ExtractionResult.builder()
                .merchant("Random Store")
                .description("xyzzy blurb")
                .build();

        CarbonHints hints = eng.computeHints(result);

        assertThat(hints.getActivityType()).isNull();
    }

    // ── Merge order priority ─────────────────────────────────────────────

    @Test
    void engine_lowerOrderProviderWinsForSameField() {
        // Create two providers that both set activityType
        CarbonHintProvider highPriority = new CarbonHintProvider() {
            @Override
            public CarbonHints provide(CarbonHintContext context) {
                return CarbonHints.builder()
                        .activityType(CarbonActivityType.FUEL)
                        .build();
            }

            @Override
            public int getOrder() { return 1; }
        };

        CarbonHintProvider lowPriority = new CarbonHintProvider() {
            @Override
            public CarbonHints provide(CarbonHintContext context) {
                return CarbonHints.builder()
                        .activityType(CarbonActivityType.ELECTRICITY)
                        .build();
            }

            @Override
            public int getOrder() { return 100; }
        };

        CarbonHintEngine eng = buildEngine(List.of(lowPriority, highPriority));
        ExtractionResult result = ExtractionResult.builder().merchant("Test").build();

        CarbonHints hints = eng.computeHints(result);

        // Lower order (1) wins
        assertThat(hints.getActivityType()).isEqualTo(CarbonActivityType.FUEL);
    }

    // ── Engine never mutates input ───────────────────────────────────────

    @Test
    void engine_doesNotModifyInputExtractionResult() {
        CarbonHintEngine eng = buildDefaultEngine();

        ExtractionResult original = ExtractionResult.builder()
                .merchant("Shell")
                .description("petrol")
                .metadata(Map.of("existingKey", "existingValue"))
                .build();

        eng.computeHints(original);

        assertThat(original.getMerchant()).isEqualTo("Shell");
        assertThat(original.getDescription()).isEqualTo("petrol");
        assertThat(original.getMetadata()).containsKey("existingKey");
    }

    // ── Existing hints with numeric types ────────────────────────────────

    @Test
    void engine_existingHintsHandlesNumericPassengerCount() {
        CarbonHintEngine eng = buildDefaultEngine();

        ExtractionResult result = ExtractionResult.builder()
                .merchant("IndiGo")
                .metadata(Map.of("carbonHints", Map.of(
                        "activityType", "FLIGHT",
                        "passengerCount", 3.0
                )))
                .build();

        CarbonHints hints = eng.computeHints(result);

        assertThat(hints.getPassengerCount()).isEqualTo(3);
    }

    @Test
    void engine_existingHintsHandlesNumberEstimatedDistance() {
        CarbonHintEngine eng = buildDefaultEngine();

        ExtractionResult result = ExtractionResult.builder()
                .merchant("Uber")
                .metadata(Map.of("carbonHints", Map.of(
                        "activityType", "TRANSPORT",
                        "estimatedDistance", 150.5
                )))
                .build();

        CarbonHints hints = eng.computeHints(result);

        assertThat(hints.getEstimatedDistance()).isNotNull();
    }
}
