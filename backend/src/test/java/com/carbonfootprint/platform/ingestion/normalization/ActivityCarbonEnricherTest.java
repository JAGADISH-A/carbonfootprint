package com.carbonfootprint.platform.ingestion.normalization;

import com.carbonfootprint.platform.ingestion.enrichment.CarbonHintEngine;
import com.carbonfootprint.platform.ingestion.enrichment.model.CarbonActivityType;
import com.carbonfootprint.platform.ingestion.enrichment.model.CabinClass;
import com.carbonfootprint.platform.ingestion.enrichment.model.EnergySource;
import com.carbonfootprint.platform.ingestion.enrichment.model.FuelType;
import com.carbonfootprint.platform.ingestion.enrichment.model.MerchantIndustry;
import com.carbonfootprint.platform.ingestion.enrichment.model.TransportMode;
import com.carbonfootprint.platform.ingestion.enrichment.model.VehicleType;
import com.carbonfootprint.platform.ingestion.model.CarbonHints;
import com.carbonfootprint.platform.ingestion.model.ExtractionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityCarbonEnricherTest {

    @Mock
    private CarbonHintEngine carbonHintEngine;

    private ActivityCarbonEnricher enricher;

    @BeforeEach
    void setUp() {
        enricher = new ActivityCarbonEnricher(carbonHintEngine);
    }

    // ── Empty hints → return original ────────────────────────────────────

    @Test
    void enrich_whenEngineReturnsEmpty_returnsOriginalUnchanged() {
        when(carbonHintEngine.computeHints(any())).thenReturn(CarbonHints.empty());

        ExtractionResult original = ExtractionResult.builder()
                .merchant("Shell")
                .build();

        ExtractionResult result = enricher.enrich(original);

        assertThat(result).isSameAs(original);
    }

    // ── Enum serialization ───────────────────────────────────────────────

    @Test
    void enrich_serializesEnumFieldsAsStrings() {
        CarbonHints hints = CarbonHints.builder()
                .activityType(CarbonActivityType.FUEL)
                .fuelType(FuelType.PETROL)
                .vehicleType(VehicleType.CAR)
                .build();

        when(carbonHintEngine.computeHints(any())).thenReturn(hints);

        ExtractionResult result = enricher.enrich(ExtractionResult.builder().merchant("Shell").build());

        Map<String, Object> carbonHints = getCarbonHintsMap(result);

        assertThat(carbonHints.get("activityType")).isEqualTo("FUEL");
        assertThat(carbonHints.get("fuelType")).isEqualTo("PETROL");
        assertThat(carbonHints.get("vehicleType")).isEqualTo("CAR");
    }

    @Test
    void enrich_serializesAllEnumFieldsAsStrings() {
        CarbonHints hints = CarbonHints.builder()
                .activityType(CarbonActivityType.TRANSPORT)
                .transportMode(TransportMode.BUS)
                .cabinClass(CabinClass.BUSINESS)
                .energySource(EnergySource.SOLAR)
                .merchantIndustry(MerchantIndustry.GROCERY)
                .build();

        when(carbonHintEngine.computeHints(any())).thenReturn(hints);

        ExtractionResult result = enricher.enrich(ExtractionResult.builder().merchant("Test").build());

        Map<String, Object> carbonHints = getCarbonHintsMap(result);

        assertThat(carbonHints.get("activityType")).isEqualTo("TRANSPORT");
        assertThat(carbonHints.get("transportMode")).isEqualTo("BUS");
        assertThat(carbonHints.get("cabinClass")).isEqualTo("BUSINESS");
        assertThat(carbonHints.get("energySource")).isEqualTo("SOLAR");
        assertThat(carbonHints.get("merchantIndustry")).isEqualTo("GROCERY");
    }

    // ── Non-null fields only ─────────────────────────────────────────────

    @Test
    void enrich_onlyIncludesNonNullFields() {
        CarbonHints hints = CarbonHints.builder()
                .activityType(CarbonActivityType.FUEL)
                .fuelType(FuelType.DIESEL)
                .build();

        when(carbonHintEngine.computeHints(any())).thenReturn(hints);

        ExtractionResult result = enricher.enrich(ExtractionResult.builder().merchant("Shell").build());

        Map<String, Object> carbonHints = getCarbonHintsMap(result);

        assertThat(carbonHints).containsKey("activityType");
        assertThat(carbonHints).containsKey("fuelType");
        assertThat(carbonHints).doesNotContainKey("transportMode");
        assertThat(carbonHints).doesNotContainKey("cabinClass");
        assertThat(carbonHints).doesNotContainKey("merchantIndustry");
    }

    // ── Metadata preservation ────────────────────────────────────────────

    @Test
    void enrich_preservesExistingMetadataKeys() {
        when(carbonHintEngine.computeHints(any())).thenReturn(
                CarbonHints.builder().activityType(CarbonActivityType.FUEL).build());

        ExtractionResult original = ExtractionResult.builder()
                .merchant("Shell")
                .metadata(Map.of("existingKey", "existingValue", "anotherKey", 42))
                .build();

        ExtractionResult result = enricher.enrich(original);

        assertThat(result.getMetadata()).containsKey("existingKey");
        assertThat(result.getMetadata()).containsKey("anotherKey");
        assertThat(result.getMetadata()).containsKey("carbonHints");
    }

    @Test
    void enrich_replacesExistingCarbonHintsEntry() {
        when(carbonHintEngine.computeHints(any())).thenReturn(
                CarbonHints.builder().activityType(CarbonActivityType.ELECTRICITY).build());

        ExtractionResult original = ExtractionResult.builder()
                .merchant("BESCOM")
                .metadata(Map.of("carbonHints", Map.of("activityType", "FUEL")))
                .build();

        ExtractionResult result = enricher.enrich(original);

        Map<String, Object> carbonHints = getCarbonHintsMap(result);
        assertThat(carbonHints.get("activityType")).isEqualTo("ELECTRICITY");
    }

    // ── Null metadata ────────────────────────────────────────────────────

    @Test
    void enrich_nullMetadata_createsNewMetadataMap() {
        when(carbonHintEngine.computeHints(any())).thenReturn(
                CarbonHints.builder().activityType(CarbonActivityType.FUEL).build());

        ExtractionResult original = ExtractionResult.builder()
                .merchant("Shell")
                .metadata(null)
                .build();

        ExtractionResult result = enricher.enrich(original);

        assertThat(result.getMetadata()).isNotNull();
        assertThat(result.getMetadata()).containsKey("carbonHints");
    }

    // ── Input immutability ───────────────────────────────────────────────

    @Test
    void enrich_doesNotModifyInputResult() {
        when(carbonHintEngine.computeHints(any())).thenReturn(
                CarbonHints.builder().activityType(CarbonActivityType.FUEL).build());

        ExtractionResult original = ExtractionResult.builder()
                .merchant("Shell")
                .amount(new BigDecimal("1000"))
                .currency("INR")
                .metadata(Map.of("key", "value"))
                .build();

        enricher.enrich(original);

        assertThat(original.getMerchant()).isEqualTo("Shell");
        assertThat(original.getAmount()).isEqualByComparingTo(new BigDecimal("1000"));
        assertThat(original.getCurrency()).isEqualTo("INR");
        assertThat(original.getMetadata()).containsEntry("key", "value");
    }

    // ── Confidence serialization ─────────────────────────────────────────

    @Test
    void enrich_serializesConfidenceAsDouble() {
        CarbonHints hints = CarbonHints.builder()
                .activityType(CarbonActivityType.FUEL)
                .confidence(0.9)
                .build();

        when(carbonHintEngine.computeHints(any())).thenReturn(hints);

        ExtractionResult result = enricher.enrich(ExtractionResult.builder().merchant("Shell").build());

        Map<String, Object> carbonHints = getCarbonHintsMap(result);

        assertThat(carbonHints.get("confidence")).isEqualTo(0.9);
    }

    // ── Numeric fields ───────────────────────────────────────────────────

    @Test
    void enrich_serializesPassengerCountAndDistance() {
        CarbonHints hints = CarbonHints.builder()
                .activityType(CarbonActivityType.FLIGHT)
                .passengerCount(2)
                .estimatedDistance(new BigDecimal("1500.5"))
                .build();

        when(carbonHintEngine.computeHints(any())).thenReturn(hints);

        ExtractionResult result = enricher.enrich(ExtractionResult.builder().merchant("IndiGo").build());

        Map<String, Object> carbonHints = getCarbonHintsMap(result);

        assertThat(carbonHints.get("passengerCount")).isEqualTo(2);
        assertThat(new BigDecimal("1500.5").compareTo((BigDecimal) carbonHints.get("estimatedDistance"))).isEqualTo(0);
    }

    // ── String fields ────────────────────────────────────────────────────

    @Test
    void enrich_serializesStringFields() {
        CarbonHints hints = CarbonHints.builder()
                .activityType(CarbonActivityType.ELECTRICITY)
                .electricityUnit("KWH")
                .fuelUnit("LITRE")
                .build();

        when(carbonHintEngine.computeHints(any())).thenReturn(hints);

        ExtractionResult result = enricher.enrich(ExtractionResult.builder().merchant("BESCOM").build());

        Map<String, Object> carbonHints = getCarbonHintsMap(result);

        assertThat(carbonHints.get("electricityUnit")).isEqualTo("KWH");
        assertThat(carbonHints.get("fuelUnit")).isEqualTo("LITRE");
    }

    // ── Full hints with all fields ───────────────────────────────────────

    @Test
    void enrich_serializesAllFieldsCorrectly() {
        CarbonHints hints = CarbonHints.builder()
                .activityType(CarbonActivityType.FUEL)
                .transportMode(TransportMode.TAXI)
                .fuelType(FuelType.DIESEL)
                .energySource(EnergySource.GRID)
                .electricityUnit("KWH")
                .fuelUnit("LITRE")
                .estimatedDistance(new BigDecimal("100"))
                .passengerCount(2)
                .cabinClass(CabinClass.ECONOMY)
                .vehicleType(VehicleType.CAR)
                .merchantIndustry(MerchantIndustry.GROCERY)
                .confidence(0.85)
                .build();

        when(carbonHintEngine.computeHints(any())).thenReturn(hints);

        ExtractionResult result = enricher.enrich(ExtractionResult.builder().merchant("Test").build());

        Map<String, Object> carbonHints = getCarbonHintsMap(result);

        assertThat(carbonHints).hasSize(12);
        assertThat(carbonHints.get("activityType")).isEqualTo("FUEL");
        assertThat(carbonHints.get("transportMode")).isEqualTo("TAXI");
        assertThat(carbonHints.get("fuelType")).isEqualTo("DIESEL");
        assertThat(carbonHints.get("energySource")).isEqualTo("GRID");
        assertThat(carbonHints.get("electricityUnit")).isEqualTo("KWH");
        assertThat(carbonHints.get("fuelUnit")).isEqualTo("LITRE");
        assertThat(new BigDecimal("100").compareTo((BigDecimal) carbonHints.get("estimatedDistance"))).isEqualTo(0);
        assertThat(carbonHints.get("passengerCount")).isEqualTo(2);
        assertThat(carbonHints.get("cabinClass")).isEqualTo("ECONOMY");
        assertThat(carbonHints.get("vehicleType")).isEqualTo("CAR");
        assertThat(carbonHints.get("merchantIndustry")).isEqualTo("GROCERY");
        assertThat(carbonHints.get("confidence")).isEqualTo(0.85);
    }

    // ── Helper ───────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> getCarbonHintsMap(ExtractionResult result) {
        return (Map<String, Object>) result.getMetadata().get("carbonHints");
    }
}
