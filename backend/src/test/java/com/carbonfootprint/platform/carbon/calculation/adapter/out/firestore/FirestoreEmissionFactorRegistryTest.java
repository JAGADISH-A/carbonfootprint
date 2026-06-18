package com.carbonfootprint.platform.carbon.calculation.adapter.out.firestore;

import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionFactor;
import com.carbonfootprint.platform.carbon.calculation.port.out.EmissionFactorRepository;
import com.carbonfootprint.platform.ingestion.enrichment.model.FuelType;
import com.carbonfootprint.platform.ingestion.enrichment.model.TransportMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirestoreEmissionFactorRegistryTest {

    @Mock
    private EmissionFactorRepository repository;

    private FirestoreEmissionFactorRegistry registry;

    private static final Instant VALID_AT = Instant.parse("2026-01-15T10:00:00Z");

    private EmissionFactor petrolFactor() {
        return EmissionFactor.builder()
                .id("ef-petrol").category(ActivityCategory.FUEL).fuelType(FuelType.PETROL)
                .value(BigDecimal.valueOf(2.31)).unit("litre")
                .source("DEFRA 2024").version("2024-v1").methodName("Tier 1")
                .validFrom(Instant.parse("2024-01-01T00:00:00Z")).build();
    }

    private EmissionFactor dieselFactor() {
        return EmissionFactor.builder()
                .id("ef-diesel").category(ActivityCategory.FUEL).fuelType(FuelType.DIESEL)
                .value(BigDecimal.valueOf(2.68)).unit("litre")
                .source("DEFRA 2024").version("2024-v1").methodName("Tier 1")
                .validFrom(Instant.parse("2024-01-01T00:00:00Z")).build();
    }

    private EmissionFactor electricityFactor() {
        return EmissionFactor.builder()
                .id("ef-elec").category(ActivityCategory.ELECTRICITY)
                .value(BigDecimal.valueOf(0.82)).unit("kWh")
                .source("DEFRA 2024").version("2024-v1").methodName("Tier 1")
                .validFrom(Instant.parse("2024-01-01T00:00:00Z")).build();
    }

    private EmissionFactor taxiFactor() {
        return EmissionFactor.builder()
                .id("ef-taxi").category(ActivityCategory.TRANSPORT).transportMode(TransportMode.TAXI)
                .value(BigDecimal.valueOf(0.21)).unit("km")
                .source("DEFRA 2024").version("2024-v1").methodName("Tier 1")
                .validFrom(Instant.parse("2024-01-01T00:00:00Z")).build();
    }

    private EmissionFactor shoppingFactor() {
        return EmissionFactor.builder()
                .id("ef-shopping").category(ActivityCategory.SHOPPING)
                .value(BigDecimal.valueOf(0.00085)).unit("INR")
                .source("DEFRA 2024").version("2024-v1").methodName("Spend-based")
                .validFrom(Instant.parse("2024-01-01T00:00:00Z")).build();
    }

    @BeforeEach
    void setUp() {
        when(repository.findAll()).thenReturn(List.of(
                petrolFactor(), dieselFactor(), electricityFactor(), taxiFactor(), shoppingFactor()
        ));
        registry = new FirestoreEmissionFactorRegistry(repository);
    }

    // ── Category-only lookup ───────────────────────────────────────────────

    @Test
    void find_categoryOnly_returnsCorrectFactor() {
        Optional<EmissionFactor> result = registry.find(ActivityCategory.ELECTRICITY, VALID_AT);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("ef-elec");
    }

    @Test
    void find_categoryOnly_noCategory_returnsEmpty() {
        Optional<EmissionFactor> result = registry.find(ActivityCategory.ACCOMMODATION, VALID_AT);

        assertThat(result).isEmpty();
    }

    @Test
    void find_categoryOnly_returnsCategoryOnlyFactors() {
        // FUEL has fuelType-specific factors, no category-only factor
        Optional<EmissionFactor> result = registry.find(ActivityCategory.FUEL, VALID_AT);

        assertThat(result).isEmpty();
    }

    @Test
    void find_categoryOnly_returnsShoppingFactor() {
        Optional<EmissionFactor> result = registry.find(ActivityCategory.SHOPPING, VALID_AT);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("ef-shopping");
    }

    // ── Fuel-specific lookup ───────────────────────────────────────────────

    @Test
    void find_fuelType_returnsCorrectFactor() {
        Optional<EmissionFactor> result = registry.find(ActivityCategory.FUEL, FuelType.PETROL, VALID_AT);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("ef-petrol");
        assertThat(result.get().getValue()).isEqualByComparingTo(BigDecimal.valueOf(2.31));
    }

    @Test
    void find_fuelType_noMatchingFuel_returnsEmpty() {
        Optional<EmissionFactor> result = registry.find(ActivityCategory.FUEL, FuelType.CNG, VALID_AT);

        assertThat(result).isEmpty();
    }

    // ── Transport-specific lookup ──────────────────────────────────────────

    @Test
    void find_transportMode_returnsCorrectFactor() {
        Optional<EmissionFactor> result = registry.find(ActivityCategory.TRANSPORT, TransportMode.TAXI, VALID_AT);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("ef-taxi");
        assertThat(result.get().getValue()).isEqualByComparingTo(BigDecimal.valueOf(0.21));
    }

    @Test
    void find_transportMode_noMatchingMode_returnsEmpty() {
        Optional<EmissionFactor> result = registry.find(ActivityCategory.TRANSPORT, TransportMode.BIKE, VALID_AT);

        assertThat(result).isEmpty();
    }

    // ── Validity period ────────────────────────────────────────────────────

    @Test
    void find_validAt_beforeValidFrom_returnsEmpty() {
        Instant beforeValid = Instant.parse("2023-01-01T00:00:00Z");

        Optional<EmissionFactor> result = registry.find(ActivityCategory.ELECTRICITY, beforeValid);

        assertThat(result).isEmpty();
    }

    @Test
    void find_validAt_afterValidFrom_returnsFactor() {
        Optional<EmissionFactor> result = registry.find(ActivityCategory.ELECTRICITY, VALID_AT);

        assertThat(result).isPresent();
    }

    @Test
    void find_validAt_withExpiredFactor_returnsEmpty() {
        EmissionFactor expired = EmissionFactor.builder()
                .id("ef-expired").category(ActivityCategory.SHOPPING)
                .value(BigDecimal.valueOf(0.001)).unit("INR")
                .source("OLD").version("v1").methodName("Tier 1")
                .validFrom(Instant.parse("2020-01-01T00:00:00Z"))
                .validTo(Instant.parse("2023-01-01T00:00:00Z"))
                .build();

        when(repository.findAll()).thenReturn(List.of(expired));
        registry = new FirestoreEmissionFactorRegistry(repository);

        Optional<EmissionFactor> result = registry.find(ActivityCategory.SHOPPING, VALID_AT);

        assertThat(result).isEmpty();
    }

    // ── findAll / findByCategory / count ───────────────────────────────────

    @Test
    void findAll_returnsAllFactors() {
        assertThat(registry.findAll()).hasSize(5);
    }

    @Test
    void findByCategory_filtersCorrectly() {
        List<EmissionFactor> fuelFactors = registry.findByCategory(ActivityCategory.FUEL);

        assertThat(fuelFactors).hasSize(2);
        assertThat(fuelFactors).allMatch(f -> f.getCategory() == ActivityCategory.FUEL);
    }

    @Test
    void count_returnsCorrectCount() {
        assertThat(registry.count()).isEqualTo(5);
    }

    // ── Full specificity lookup ────────────────────────────────────────────

    @Test
    void find_fullSpecificity_matchesAllDimensions() {
        Optional<EmissionFactor> result = registry.find(
                ActivityCategory.FUEL, FuelType.PETROL, null, null, VALID_AT);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("ef-petrol");
    }
}
