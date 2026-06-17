package com.carbonfootprint.platform.carbon.calculation.calculator;

import com.carbonfootprint.platform.activity.model.Activity;
import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionFactor;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionFactorRegistry;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionResult;
import com.carbonfootprint.platform.ingestion.enrichment.model.FuelType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FuelEmissionCalculatorTest {

    @Mock
    private EmissionFactorRegistry emissionFactorRegistry;

    private FuelEmissionCalculator buildCalculator() {
        return new FuelEmissionCalculator(emissionFactorRegistry);
    }

    private Activity fuelActivity(FuelType fuelType, BigDecimal quantity) {
        Map<String, Object> hints = Map.of(
                "activityType", "FUEL",
                "fuelType", fuelType.name(),
                "fuelUnit", "LITRE"
        );
        return Activity.builder()
                .id("act-fuel-001")
                .category(ActivityCategory.FUEL)
                .merchant("Shell")
                .amount(quantity)
                .unit("litres")
                .metadata(Map.of("carbonHints", hints))
                .occurredAt(Instant.parse("2026-01-15T10:00:00Z"))
                .build();
    }

    private EmissionFactor petrolFactor() {
        return EmissionFactor.builder()
                .id("ef-petrol-001")
                .value(BigDecimal.valueOf(2.31))
                .unit("litre")
                .category(ActivityCategory.FUEL)
                .fuelType(FuelType.PETROL)
                .source("DEFRA 2024")
                .version("2024-v1")
                .methodName("Tier 1 — Default EF")
                .validFrom(Instant.parse("2024-01-01T00:00:00Z"))
                .build();
    }

    // ── supports() ────────────────────────────────────────────────────────

    @Test
    void supports_fuelCategoryWithFuelType_returnsTrue() {
        Activity activity = fuelActivity(FuelType.PETROL, BigDecimal.valueOf(40));

        assertThat(buildCalculator().supports(activity)).isTrue();
    }

    @Test
    void supports_nonFuelCategory_returnsFalse() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.ELECTRICITY)
                .build();

        assertThat(buildCalculator().supports(activity)).isFalse();
    }

    @Test
    void supports_fuelCategoryButNoCarbonHints_returnsFalse() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.FUEL)
                .metadata(Map.of())
                .build();

        assertThat(buildCalculator().supports(activity)).isFalse();
    }

    @Test
    void supports_fuelCategoryButNoFuelType_returnsFalse() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.FUEL)
                .metadata(Map.of("carbonHints", Map.of("activityType", "FUEL")))
                .build();

        assertThat(buildCalculator().supports(activity)).isFalse();
    }

    @Test
    void supports_fuelCategoryWithUnknownFuelType_returnsTrue() {
        Activity activity = fuelActivity(FuelType.UNKNOWN, BigDecimal.valueOf(40));

        assertThat(buildCalculator().supports(activity)).isTrue();
    }

    @Test
    void supports_fuelCategoryWithNullMetadata_returnsFalse() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.FUEL)
                .build();

        assertThat(buildCalculator().supports(activity)).isFalse();
    }

    @Test
    void supports_fuelCategoryWithInvalidFuelTypeString_returnsFalse() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.FUEL)
                .metadata(Map.of("carbonHints", Map.of("fuelType", "JET_FUEL")))
                .build();

        assertThat(buildCalculator().supports(activity)).isFalse();
    }

    // ── calculate() ───────────────────────────────────────────────────────

    @Test
    void calculate_petrolActivity_returnsCorrectEmission() {
        FuelType fuelType = FuelType.PETROL;
        BigDecimal quantity = BigDecimal.valueOf(40);
        Activity activity = fuelActivity(fuelType, quantity);
        EmissionFactor factor = petrolFactor();

        when(emissionFactorRegistry.find(eq(ActivityCategory.FUEL), eq(fuelType), any(Instant.class)))
                .thenReturn(Optional.of(factor));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        assertThat(result.getCarbonKg()).isEqualByComparingTo(BigDecimal.valueOf(92.4));
        assertThat(result.getActivityId()).isEqualTo("act-fuel-001");
        assertThat(result.getEmissionFactor()).isEqualTo(factor);
        assertThat(result.getActivityQuantity()).isEqualByComparingTo(quantity);
        assertThat(result.getActivityUnit()).isEqualTo("litre");
        assertThat(result.getMethodology()).isEqualTo("Tier 1 — Default EF");
        assertThat(result.getCalculatedAt()).isNotNull();
    }

    @Test
    void calculate_dieselActivity_usesDieselFactor() {
        FuelType fuelType = FuelType.DIESEL;
        BigDecimal quantity = BigDecimal.valueOf(50);
        Activity activity = fuelActivity(fuelType, quantity);

        EmissionFactor dieselFactor = EmissionFactor.builder()
                .id("ef-diesel-001")
                .value(BigDecimal.valueOf(2.68))
                .unit("litre")
                .category(ActivityCategory.FUEL)
                .fuelType(FuelType.DIESEL)
                .source("DEFRA 2024")
                .version("2024-v1")
                .methodName("Tier 1 — Default EF")
                .validFrom(Instant.parse("2024-01-01T00:00:00Z"))
                .build();

        when(emissionFactorRegistry.find(eq(ActivityCategory.FUEL), eq(fuelType), any(Instant.class)))
                .thenReturn(Optional.of(dieselFactor));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        assertThat(result.getCarbonKg()).isEqualByComparingTo(BigDecimal.valueOf(134.0));
    }

    @Test
    void calculate_noCarbonHints_returnsEmpty() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.FUEL)
                .amount(BigDecimal.valueOf(40))
                .metadata(Map.of())
                .build();

        assertThat(buildCalculator().calculate(activity)).isEmpty();
    }

    @Test
    void calculate_noFuelType_returnsEmpty() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.FUEL)
                .amount(BigDecimal.valueOf(40))
                .metadata(Map.of("carbonHints", Map.of("activityType", "FUEL")))
                .build();

        assertThat(buildCalculator().calculate(activity)).isEmpty();
    }

    @Test
    void calculate_nullQuantity_returnsEmpty() {
        Activity activity = fuelActivity(FuelType.PETROL, null);

        assertThat(buildCalculator().calculate(activity)).isEmpty();
        verify(emissionFactorRegistry, never()).find(any(ActivityCategory.class), any(FuelType.class), any(Instant.class));
    }

    @Test
    void calculate_zeroQuantity_returnsEmpty() {
        Activity activity = fuelActivity(FuelType.PETROL, BigDecimal.ZERO);

        assertThat(buildCalculator().calculate(activity)).isEmpty();
        verify(emissionFactorRegistry, never()).find(any(ActivityCategory.class), any(FuelType.class), any(Instant.class));
    }

    @Test
    void calculate_negativeQuantity_returnsEmpty() {
        Activity activity = fuelActivity(FuelType.PETROL, BigDecimal.valueOf(-10));

        assertThat(buildCalculator().calculate(activity)).isEmpty();
        verify(emissionFactorRegistry, never()).find(any(ActivityCategory.class), any(FuelType.class), any(Instant.class));
    }

    @Test
    void calculate_noEmissionFactor_returnsEmpty() {
        Activity activity = fuelActivity(FuelType.CNG, BigDecimal.valueOf(20));

        when(emissionFactorRegistry.find(eq(ActivityCategory.FUEL), eq(FuelType.CNG), any(Instant.class)))
                .thenReturn(Optional.empty());

        assertThat(buildCalculator().calculate(activity)).isEmpty();
    }

    @Test
    void calculate_usesActivityOccurredAtForFactorLookup() {
        Instant occurredAt = Instant.parse("2025-06-15T08:00:00Z");
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.FUEL)
                .amount(BigDecimal.valueOf(30))
                .occurredAt(occurredAt)
                .metadata(Map.of("carbonHints", Map.of(
                        "activityType", "FUEL",
                        "fuelType", "LPG")))
                .build();

        EmissionFactor lpgFactor = EmissionFactor.builder()
                .id("ef-lpg-001")
                .value(BigDecimal.valueOf(1.51))
                .unit("litre")
                .category(ActivityCategory.FUEL)
                .fuelType(FuelType.LPG)
                .source("DEFRA 2024")
                .version("2024-v1")
                .methodName("Tier 1 — Default EF")
                .validFrom(Instant.parse("2024-01-01T00:00:00Z"))
                .build();

        when(emissionFactorRegistry.find(eq(ActivityCategory.FUEL), eq(FuelType.LPG), eq(occurredAt)))
                .thenReturn(Optional.of(lpgFactor));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        assertThat(result.getCarbonKg()).isEqualByComparingTo(BigDecimal.valueOf(45.3));
        verify(emissionFactorRegistry).find(eq(ActivityCategory.FUEL), eq(FuelType.LPG), eq(occurredAt));
    }

    @Test
    void calculate_noOccurredAt_usesNowForFactorLookup() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.FUEL)
                .amount(BigDecimal.valueOf(25))
                .metadata(Map.of("carbonHints", Map.of(
                        "activityType", "FUEL",
                        "fuelType", "PETROL")))
                .build();

        EmissionFactor factor = petrolFactor();

        when(emissionFactorRegistry.find(eq(ActivityCategory.FUEL), eq(FuelType.PETROL), any(Instant.class)))
                .thenReturn(Optional.of(factor));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        assertThat(result.getCarbonKg()).isEqualByComparingTo(BigDecimal.valueOf(57.75));
    }

    // ── getOrder() ────────────────────────────────────────────────────────

    @Test
    void getOrder_returns10() {
        assertThat(buildCalculator().getOrder()).isEqualTo(10);
    }

    // ── Result metadata ───────────────────────────────────────────────────

    @Test
    void calculate_resultHasEmptyBreakdown() {
        Activity activity = fuelActivity(FuelType.PETROL, BigDecimal.valueOf(40));

        when(emissionFactorRegistry.find(eq(ActivityCategory.FUEL), eq(FuelType.PETROL), any(Instant.class)))
                .thenReturn(Optional.of(petrolFactor()));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        assertThat(result.getBreakdown()).isEmpty();
        assertThat(result.hasBreakdown()).isFalse();
    }

    @Test
    void calculate_resultConvenienceMethodsWork() {
        Activity activity = fuelActivity(FuelType.PETROL, BigDecimal.valueOf(40));

        when(emissionFactorRegistry.find(eq(ActivityCategory.FUEL), eq(FuelType.PETROL), any(Instant.class)))
                .thenReturn(Optional.of(petrolFactor()));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        assertThat(result.getEmissionFactorValue()).isEqualByComparingTo(BigDecimal.valueOf(2.31));
        assertThat(result.getEmissionFactorSource()).isEqualTo("DEFRA 2024");
    }

    // ── Does not modify input ─────────────────────────────────────────────

    @Test
    void calculate_doesNotModifyInputActivity() {
        Activity activity = fuelActivity(FuelType.PETROL, BigDecimal.valueOf(40));

        when(emissionFactorRegistry.find(eq(ActivityCategory.FUEL), eq(FuelType.PETROL), any(Instant.class)))
                .thenReturn(Optional.of(petrolFactor()));

        buildCalculator().calculate(activity);

        assertThat(activity.getId()).isEqualTo("act-fuel-001");
        assertThat(activity.getCategory()).isEqualTo(ActivityCategory.FUEL);
        assertThat(activity.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(40));
        assertThat(activity.getUnit()).isEqualTo("litres");
    }
}
