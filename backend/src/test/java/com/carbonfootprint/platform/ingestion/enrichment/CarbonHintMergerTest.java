package com.carbonfootprint.platform.ingestion.enrichment;

import com.carbonfootprint.platform.ingestion.enrichment.model.CabinClass;
import com.carbonfootprint.platform.ingestion.enrichment.model.CarbonActivityType;
import com.carbonfootprint.platform.ingestion.enrichment.model.EnergySource;
import com.carbonfootprint.platform.ingestion.enrichment.model.FuelType;
import com.carbonfootprint.platform.ingestion.enrichment.model.MerchantIndustry;
import com.carbonfootprint.platform.ingestion.enrichment.model.TransportMode;
import com.carbonfootprint.platform.ingestion.enrichment.model.VehicleType;
import com.carbonfootprint.platform.ingestion.model.CarbonHints;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CarbonHintMergerTest {

    private CarbonHintMerger merger;

    @BeforeEach
    void setUp() {
        merger = new CarbonHintMerger();
    }

    // ── Empty / null cases ────────────────────────────────────────────────

    @Test
    void merge_emptySeedAndEmptyPartials_returnsEmpty() {
        CarbonHints result = merger.merge(CarbonHints.empty(), List.of());

        assertThat(result).isEqualTo(CarbonHints.empty());
    }

    @Test
    void merge_nullSeedTreatedAsEmpty() {
        CarbonHints partial = CarbonHints.builder()
                .activityType(CarbonActivityType.FUEL)
                .build();

        CarbonHints result = merger.merge(null, List.of(partial));

        assertThat(result.getActivityType()).isEqualTo(CarbonActivityType.FUEL);
    }

    @Test
    void merge_nullPartialsInListAreSkipped() {
        List<CarbonHints> partials = new ArrayList<>();
        partials.add(null);
        partials.add(null);

        CarbonHints result = merger.merge(CarbonHints.empty(), partials);

        assertThat(result).isEqualTo(CarbonHints.empty());
    }

    // ── First-non-null-wins ──────────────────────────────────────────────

    @Test
    void merge_firstNonNullWinsForActivityType() {
        CarbonHints first = CarbonHints.builder()
                .activityType(CarbonActivityType.FUEL)
                .build();
        CarbonHints second = CarbonHints.builder()
                .activityType(CarbonActivityType.ELECTRICITY)
                .build();

        CarbonHints result = merger.merge(CarbonHints.empty(), List.of(first, second));

        assertThat(result.getActivityType()).isEqualTo(CarbonActivityType.FUEL);
    }

    @Test
    void merge_firstNonNullWinsForTransportMode() {
        CarbonHints first = CarbonHints.builder()
                .transportMode(TransportMode.BUS)
                .build();
        CarbonHints second = CarbonHints.builder()
                .transportMode(TransportMode.TAXI)
                .build();

        CarbonHints result = merger.merge(CarbonHints.empty(), List.of(first, second));

        assertThat(result.getTransportMode()).isEqualTo(TransportMode.BUS);
    }

    @Test
    void merge_firstNonNullWinsForFuelType() {
        CarbonHints first = CarbonHints.builder()
                .fuelType(FuelType.PETROL)
                .build();
        CarbonHints second = CarbonHints.builder()
                .fuelType(FuelType.DIESEL)
                .build();

        CarbonHints result = merger.merge(CarbonHints.empty(), List.of(first, second));

        assertThat(result.getFuelType()).isEqualTo(FuelType.PETROL);
    }

    @Test
    void merge_firstNonNullWinsForCabinClass() {
        CarbonHints first = CarbonHints.builder()
                .cabinClass(CabinClass.ECONOMY)
                .build();
        CarbonHints second = CarbonHints.builder()
                .cabinClass(CabinClass.BUSINESS)
                .build();

        CarbonHints result = merger.merge(CarbonHints.empty(), List.of(first, second));

        assertThat(result.getCabinClass()).isEqualTo(CabinClass.ECONOMY);
    }

    @Test
    void merge_firstNonNullWinsForMerchantIndustry() {
        CarbonHints first = CarbonHints.builder()
                .merchantIndustry(MerchantIndustry.GROCERY)
                .build();
        CarbonHints second = CarbonHints.builder()
                .merchantIndustry(MerchantIndustry.RESTAURANT)
                .build();

        CarbonHints result = merger.merge(CarbonHints.empty(), List.of(first, second));

        assertThat(result.getMerchantIndustry()).isEqualTo(MerchantIndustry.GROCERY);
    }

    // ── Seed takes priority over all partials ─────────────────────────────

    @Test
    void merge_seedValuesNeverOverwritten() {
        CarbonHints seed = CarbonHints.builder()
                .activityType(CarbonActivityType.FUEL)
                .fuelType(FuelType.DIESEL)
                .build();
        CarbonHints partial = CarbonHints.builder()
                .activityType(CarbonActivityType.ELECTRICITY)
                .fuelType(FuelType.PETROL)
                .build();

        CarbonHints result = merger.merge(seed, List.of(partial));

        assertThat(result.getActivityType()).isEqualTo(CarbonActivityType.FUEL);
        assertThat(result.getFuelType()).isEqualTo(FuelType.DIESEL);
    }

    @Test
    void merge_seedFillsNullFieldsFromPartials() {
        CarbonHints seed = CarbonHints.builder()
                .activityType(CarbonActivityType.FUEL)
                .build();
        CarbonHints partial = CarbonHints.builder()
                .fuelType(FuelType.PETROL)
                .vehicleType(VehicleType.CAR)
                .build();

        CarbonHints result = merger.merge(seed, List.of(partial));

        assertThat(result.getActivityType()).isEqualTo(CarbonActivityType.FUEL);
        assertThat(result.getFuelType()).isEqualTo(FuelType.PETROL);
        assertThat(result.getVehicleType()).isEqualTo(VehicleType.CAR);
    }

    // ── Confidence is excluded from merge ─────────────────────────────────

    @Test
    void merge_doesNotMergeConfidence() {
        CarbonHints first = CarbonHints.builder()
                .activityType(CarbonActivityType.FUEL)
                .confidence(0.9)
                .build();
        CarbonHints second = CarbonHints.builder()
                .activityType(CarbonActivityType.ELECTRICITY)
                .confidence(0.7)
                .build();

        CarbonHints result = merger.merge(CarbonHints.empty(), List.of(first, second));

        // Confidence is excluded from merge — engine computes final value
        assertThat(result.getConfidence()).isNull();
    }

    // ── All fields merge independently ────────────────────────────────────

    @Test
    void merge_allFieldsMergeIndependently() {
        CarbonHints first = CarbonHints.builder()
                .activityType(CarbonActivityType.FUEL)
                .fuelType(FuelType.PETROL)
                .fuelUnit("LITRE")
                .vehicleType(VehicleType.CAR)
                .build();
        CarbonHints second = CarbonHints.builder()
                .transportMode(TransportMode.BUS)
                .energySource(EnergySource.SOLAR)
                .electricityUnit("KWH")
                .cabinClass(CabinClass.BUSINESS)
                .passengerCount(2)
                .estimatedDistance(new BigDecimal("150.5"))
                .merchantIndustry(MerchantIndustry.GROCERY)
                .build();

        CarbonHints result = merger.merge(CarbonHints.empty(), List.of(first, second));

        assertThat(result.getActivityType()).isEqualTo(CarbonActivityType.FUEL);
        assertThat(result.getFuelType()).isEqualTo(FuelType.PETROL);
        assertThat(result.getFuelUnit()).isEqualTo("LITRE");
        assertThat(result.getVehicleType()).isEqualTo(VehicleType.CAR);
        assertThat(result.getTransportMode()).isEqualTo(TransportMode.BUS);
        assertThat(result.getEnergySource()).isEqualTo(EnergySource.SOLAR);
        assertThat(result.getElectricityUnit()).isEqualTo("KWH");
        assertThat(result.getCabinClass()).isEqualTo(CabinClass.BUSINESS);
        assertThat(result.getPassengerCount()).isEqualTo(2);
        assertThat(result.getEstimatedDistance()).isEqualByComparingTo(new BigDecimal("150.5"));
        assertThat(result.getMerchantIndustry()).isEqualTo(MerchantIndustry.GROCERY);
    }

    // ── Multiple partials fill different fields ───────────────────────────

    @Test
    void merge_multiplePartialsFillDifferentFields() {
        CarbonHints p1 = CarbonHints.builder()
                .activityType(CarbonActivityType.FUEL)
                .build();
        CarbonHints p2 = CarbonHints.builder()
                .fuelType(FuelType.DIESEL)
                .build();
        CarbonHints p3 = CarbonHints.builder()
                .vehicleType(VehicleType.TRUCK)
                .build();

        CarbonHints result = merger.merge(CarbonHints.empty(), List.of(p1, p2, p3));

        assertThat(result.getActivityType()).isEqualTo(CarbonActivityType.FUEL);
        assertThat(result.getFuelType()).isEqualTo(FuelType.DIESEL);
        assertThat(result.getVehicleType()).isEqualTo(VehicleType.TRUCK);
    }
}
