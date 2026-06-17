package com.carbonfootprint.platform.carbon.calculation.calculator;

import com.carbonfootprint.platform.activity.model.Activity;
import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionFactor;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionFactorRegistry;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionResult;
import com.carbonfootprint.platform.ingestion.enrichment.model.CabinClass;
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
class FlightEmissionCalculatorTest {

    @Mock
    private EmissionFactorRegistry emissionFactorRegistry;

    private FlightEmissionCalculator buildCalculator() {
        return new FlightEmissionCalculator(emissionFactorRegistry);
    }

    private Activity flightActivity(BigDecimal distance, CabinClass cabinClass) {
        var hintsBuilder = Map.<String, Object>ofEntries(
                Map.entry("activityType", "FLIGHT"),
                Map.entry("estimatedDistance", distance)
        );
        var hints = new java.util.HashMap<>(hintsBuilder);
        if (cabinClass != null) {
            hints.put("cabinClass", cabinClass.name());
        }
        return Activity.builder()
                .id("act-flight-001")
                .category(ActivityCategory.FLIGHT)
                .merchant("IndiGo")
                .metadata(Map.of("carbonHints", Map.copyOf(hints)))
                .occurredAt(Instant.parse("2026-01-15T10:00:00Z"))
                .build();
    }

    private Activity flightActivityWithPassengerCount(BigDecimal distance, CabinClass cabinClass, int passengerCount) {
        var hintsBuilder = Map.<String, Object>ofEntries(
                Map.entry("activityType", "FLIGHT"),
                Map.entry("estimatedDistance", distance),
                Map.entry("passengerCount", passengerCount)
        );
        var hints = new java.util.HashMap<>(hintsBuilder);
        if (cabinClass != null) {
            hints.put("cabinClass", cabinClass.name());
        }
        return Activity.builder()
                .id("act-flight-001")
                .category(ActivityCategory.FLIGHT)
                .merchant("IndiGo")
                .metadata(Map.of("carbonHints", Map.copyOf(hints)))
                .occurredAt(Instant.parse("2026-01-15T10:00:00Z"))
                .build();
    }

    private EmissionFactor flightFactor() {
        return EmissionFactor.builder()
                .id("ef-flight-001")
                .value(new BigDecimal("0.255"))
                .unit("passenger-km")
                .category(ActivityCategory.FLIGHT)
                .source("DEFRA 2024")
                .version("2024-v1")
                .methodName("Tier 1 — Default EF")
                .validFrom(Instant.parse("2024-01-01T00:00:00Z"))
                .build();
    }

    // ── supports() ────────────────────────────────────────────────────────

    @Test
    void supports_flightCategoryWithFlightHints_returnsTrue() {
        Activity activity = flightActivity(BigDecimal.valueOf(1500), CabinClass.ECONOMY);

        assertThat(buildCalculator().supports(activity)).isTrue();
    }

    @Test
    void supports_flightCategoryWithoutCabinClass_returnsTrue() {
        Activity activity = flightActivity(BigDecimal.valueOf(1500), null);

        assertThat(buildCalculator().supports(activity)).isTrue();
    }

    @Test
    void supports_nonFlightCategory_returnsFalse() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.FUEL)
                .build();

        assertThat(buildCalculator().supports(activity)).isFalse();
    }

    @Test
    void supports_flightCategoryButNoCarbonHints_returnsFalse() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.FLIGHT)
                .metadata(Map.of())
                .build();

        assertThat(buildCalculator().supports(activity)).isFalse();
    }

    @Test
    void supports_flightCategoryButWrongActivityType_returnsFalse() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.FLIGHT)
                .metadata(Map.of("carbonHints", Map.of("activityType", "FUEL")))
                .build();

        assertThat(buildCalculator().supports(activity)).isFalse();
    }

    @Test
    void supports_flightCategoryWithNullMetadata_returnsFalse() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.FLIGHT)
                .build();

        assertThat(buildCalculator().supports(activity)).isFalse();
    }

    @Test
    void supports_flightCategoryWithNoActivityTypeInHints_returnsFalse() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.FLIGHT)
                .metadata(Map.of("carbonHints", Map.of("estimatedDistance", BigDecimal.valueOf(1500))))
                .build();

        assertThat(buildCalculator().supports(activity)).isFalse();
    }

    // ── calculate() — happy path ──────────────────────────────────────────

    @Test
    void calculate_economyFlight_returnsCorrectEmission() {
        BigDecimal distance = BigDecimal.valueOf(1500);
        Activity activity = flightActivity(distance, CabinClass.ECONOMY);
        EmissionFactor factor = flightFactor();

        when(emissionFactorRegistry.find(eq(ActivityCategory.FLIGHT), any(Instant.class)))
                .thenReturn(Optional.of(factor));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        // 1500 × 0.255 × 1.0 × 1 = 382.5
        assertThat(result.getCarbonKg()).isEqualByComparingTo(new BigDecimal("382.5000"));
        assertThat(result.getActivityId()).isEqualTo("act-flight-001");
        assertThat(result.getEmissionFactor()).isEqualTo(factor);
        assertThat(result.getActivityQuantity()).isEqualByComparingTo(distance);
        assertThat(result.getActivityUnit()).isEqualTo("km");
        assertThat(result.getMethodology()).isEqualTo("Tier 1 — Default EF");
        assertThat(result.getCalculatedAt()).isNotNull();
    }

    @Test
    void calculate_premiumEconomyFlight_applies1point5Multiplier() {
        BigDecimal distance = BigDecimal.valueOf(1500);
        Activity activity = flightActivity(distance, CabinClass.PREMIUM_ECONOMY);
        EmissionFactor factor = flightFactor();

        when(emissionFactorRegistry.find(eq(ActivityCategory.FLIGHT), any(Instant.class)))
                .thenReturn(Optional.of(factor));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        // 1500 × 0.255 × 1.5 × 1 = 573.75
        assertThat(result.getCarbonKg()).isEqualByComparingTo(new BigDecimal("573.7500"));
    }

    @Test
    void calculate_businessFlight_applies2xMultiplier() {
        BigDecimal distance = BigDecimal.valueOf(1500);
        Activity activity = flightActivity(distance, CabinClass.BUSINESS);
        EmissionFactor factor = flightFactor();

        when(emissionFactorRegistry.find(eq(ActivityCategory.FLIGHT), any(Instant.class)))
                .thenReturn(Optional.of(factor));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        // 1500 × 0.255 × 2.0 × 1 = 765.0
        assertThat(result.getCarbonKg()).isEqualByComparingTo(new BigDecimal("765.0000"));
    }

    @Test
    void calculate_firstClassFlight_applies3xMultiplier() {
        BigDecimal distance = BigDecimal.valueOf(1500);
        Activity activity = flightActivity(distance, CabinClass.FIRST);
        EmissionFactor factor = flightFactor();

        when(emissionFactorRegistry.find(eq(ActivityCategory.FLIGHT), any(Instant.class)))
                .thenReturn(Optional.of(factor));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        // 1500 × 0.255 × 3.0 × 1 = 1147.5
        assertThat(result.getCarbonKg()).isEqualByComparingTo(new BigDecimal("1147.5000"));
    }

    @Test
    void calculate_flightWithNoCabinClass_defaultsToEconomy() {
        BigDecimal distance = BigDecimal.valueOf(1500);
        Activity activity = flightActivity(distance, null);
        EmissionFactor factor = flightFactor();

        when(emissionFactorRegistry.find(eq(ActivityCategory.FLIGHT), any(Instant.class)))
                .thenReturn(Optional.of(factor));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        // Same as economy: 1500 × 0.255 × 1.0 × 1 = 382.5
        assertThat(result.getCarbonKg()).isEqualByComparingTo(new BigDecimal("382.5000"));
    }

    @Test
    void calculate_flightWithMultiplePassengers_multipliesByPassengerCount() {
        BigDecimal distance = BigDecimal.valueOf(1500);
        Activity activity = flightActivityWithPassengerCount(distance, CabinClass.ECONOMY, 2);
        EmissionFactor factor = flightFactor();

        when(emissionFactorRegistry.find(eq(ActivityCategory.FLIGHT), any(Instant.class)))
                .thenReturn(Optional.of(factor));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        // 1500 × 0.255 × 1.0 × 2 = 765.0
        assertThat(result.getCarbonKg()).isEqualByComparingTo(new BigDecimal("765.0000"));
    }

    @Test
    void calculate_longHaulBusinessFlight() {
        BigDecimal distance = BigDecimal.valueOf(8000);
        Activity activity = flightActivity(distance, CabinClass.BUSINESS);
        EmissionFactor factor = flightFactor();

        when(emissionFactorRegistry.find(eq(ActivityCategory.FLIGHT), any(Instant.class)))
                .thenReturn(Optional.of(factor));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        // 8000 × 0.255 × 2.0 × 1 = 4080.0
        assertThat(result.getCarbonKg()).isEqualByComparingTo(new BigDecimal("4080.0000"));
    }

    @Test
    void calculate_businessFlightWithThreePassengers() {
        BigDecimal distance = BigDecimal.valueOf(2000);
        Activity activity = flightActivityWithPassengerCount(distance, CabinClass.BUSINESS, 3);
        EmissionFactor factor = flightFactor();

        when(emissionFactorRegistry.find(eq(ActivityCategory.FLIGHT), any(Instant.class)))
                .thenReturn(Optional.of(factor));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        // 2000 × 0.255 × 2.0 × 3 = 3060.0
        assertThat(result.getCarbonKg()).isEqualByComparingTo(new BigDecimal("3060.0000"));
    }

    // ── calculate() — error cases ─────────────────────────────────────────

    @Test
    void calculate_noCarbonHints_returnsEmpty() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.FLIGHT)
                .metadata(Map.of())
                .build();

        assertThat(buildCalculator().calculate(activity)).isEmpty();
    }

    @Test
    void calculate_noDistance_returnsEmpty() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.FLIGHT)
                .metadata(Map.of("carbonHints", Map.of(
                        "activityType", "FLIGHT",
                        "cabinClass", "ECONOMY")))
                .build();

        assertThat(buildCalculator().calculate(activity)).isEmpty();
        verify(emissionFactorRegistry, never()).find(any(ActivityCategory.class), any(Instant.class));
    }

    @Test
    void calculate_zeroDistance_returnsEmpty() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.FLIGHT)
                .metadata(Map.of("carbonHints", Map.of(
                        "activityType", "FLIGHT",
                        "estimatedDistance", BigDecimal.ZERO)))
                .build();

        assertThat(buildCalculator().calculate(activity)).isEmpty();
    }

    @Test
    void calculate_negativeDistance_returnsEmpty() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.FLIGHT)
                .metadata(Map.of("carbonHints", Map.of(
                        "activityType", "FLIGHT",
                        "estimatedDistance", BigDecimal.valueOf(-500))))
                .build();

        assertThat(buildCalculator().calculate(activity)).isEmpty();
    }

    @Test
    void calculate_noEmissionFactor_returnsEmpty() {
        Activity activity = flightActivity(BigDecimal.valueOf(1500), CabinClass.ECONOMY);

        when(emissionFactorRegistry.find(eq(ActivityCategory.FLIGHT), any(Instant.class)))
                .thenReturn(Optional.empty());

        assertThat(buildCalculator().calculate(activity)).isEmpty();
    }

    @Test
    void calculate_unknownCabinClassString_defaultsToEconomy() {
        BigDecimal distance = BigDecimal.valueOf(1500);
        Activity activity = Activity.builder()
                .id("act-flight-001")
                .category(ActivityCategory.FLIGHT)
                .merchant("IndiGo")
                .metadata(Map.of("carbonHints", Map.of(
                        "activityType", "FLIGHT",
                        "estimatedDistance", distance,
                        "cabinClass", "UNKNOWN_CLASS")))
                .occurredAt(Instant.parse("2026-01-15T10:00:00Z"))
                .build();

        EmissionFactor factor = flightFactor();

        when(emissionFactorRegistry.find(eq(ActivityCategory.FLIGHT), any(Instant.class)))
                .thenReturn(Optional.of(factor));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        // Defaults to economy: 1500 × 0.255 × 1.0 = 382.5
        assertThat(result.getCarbonKg()).isEqualByComparingTo(new BigDecimal("382.5000"));
    }

    @Test
    void calculate_cabinClassAsEnum_defaultsToEconomyWhenInvalid() {
        BigDecimal distance = BigDecimal.valueOf(1500);
        Activity activity = Activity.builder()
                .id("act-flight-001")
                .category(ActivityCategory.FLIGHT)
                .merchant("IndiGo")
                .metadata(Map.of("carbonHints", Map.of(
                        "activityType", "FLIGHT",
                        "estimatedDistance", distance,
                        "cabinClass", CabinClass.BUSINESS)))
                .occurredAt(Instant.parse("2026-01-15T10:00:00Z"))
                .build();

        EmissionFactor factor = flightFactor();

        when(emissionFactorRegistry.find(eq(ActivityCategory.FLIGHT), any(Instant.class)))
                .thenReturn(Optional.of(factor));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        // Business: 1500 × 0.255 × 2.0 = 765.0
        assertThat(result.getCarbonKg()).isEqualByComparingTo(new BigDecimal("765.0000"));
    }

    @Test
    void calculate_invalidPassengerCountDefaultsToOne() {
        BigDecimal distance = BigDecimal.valueOf(1500);
        Activity activity = Activity.builder()
                .id("act-flight-001")
                .category(ActivityCategory.FLIGHT)
                .merchant("IndiGo")
                .metadata(Map.of("carbonHints", Map.of(
                        "activityType", "FLIGHT",
                        "estimatedDistance", distance,
                        "cabinClass", "ECONOMY",
                        "passengerCount", -1)))
                .occurredAt(Instant.parse("2026-01-15T10:00:00Z"))
                .build();

        EmissionFactor factor = flightFactor();

        when(emissionFactorRegistry.find(eq(ActivityCategory.FLIGHT), any(Instant.class)))
                .thenReturn(Optional.of(factor));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        // Negative passenger count defaults to 1: 1500 × 0.255 × 1.0 × 1 = 382.5
        assertThat(result.getCarbonKg()).isEqualByComparingTo(new BigDecimal("382.5000"));
    }

    @Test
    void calculate_usesActivityOccurredAtForFactorLookup() {
        Instant occurredAt = Instant.parse("2025-06-15T08:00:00Z");
        Activity activity = Activity.builder()
                .id("act-flight-001")
                .category(ActivityCategory.FLIGHT)
                .merchant("IndiGo")
                .occurredAt(occurredAt)
                .metadata(Map.of("carbonHints", Map.of(
                        "activityType", "FLIGHT",
                        "estimatedDistance", BigDecimal.valueOf(1500),
                        "cabinClass", "ECONOMY")))
                .build();

        EmissionFactor factor = flightFactor();

        when(emissionFactorRegistry.find(eq(ActivityCategory.FLIGHT), eq(occurredAt)))
                .thenReturn(Optional.of(factor));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        assertThat(result.getCarbonKg()).isEqualByComparingTo(new BigDecimal("382.5000"));
        verify(emissionFactorRegistry).find(eq(ActivityCategory.FLIGHT), eq(occurredAt));
    }

    @Test
    void calculate_noOccurredAt_usesNowForFactorLookup() {
        Activity activity = flightActivity(BigDecimal.valueOf(1500), CabinClass.ECONOMY);
        EmissionFactor factor = flightFactor();

        when(emissionFactorRegistry.find(eq(ActivityCategory.FLIGHT), any(Instant.class)))
                .thenReturn(Optional.of(factor));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        assertThat(result.getCarbonKg()).isEqualByComparingTo(new BigDecimal("382.5000"));
    }

    // ── getOrder() ────────────────────────────────────────────────────────

    @Test
    void getOrder_returns10() {
        assertThat(buildCalculator().getOrder()).isEqualTo(10);
    }

    // ── Result metadata ───────────────────────────────────────────────────

    @Test
    void calculate_resultHasEmptyBreakdown() {
        Activity activity = flightActivity(BigDecimal.valueOf(1500), CabinClass.ECONOMY);

        when(emissionFactorRegistry.find(eq(ActivityCategory.FLIGHT), any(Instant.class)))
                .thenReturn(Optional.of(flightFactor()));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        assertThat(result.getBreakdown()).isEmpty();
        assertThat(result.hasBreakdown()).isFalse();
    }

    @Test
    void calculate_resultConvenienceMethodsWork() {
        Activity activity = flightActivity(BigDecimal.valueOf(1500), CabinClass.ECONOMY);

        when(emissionFactorRegistry.find(eq(ActivityCategory.FLIGHT), any(Instant.class)))
                .thenReturn(Optional.of(flightFactor()));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        assertThat(result.getEmissionFactorValue()).isEqualByComparingTo(new BigDecimal("0.255"));
        assertThat(result.getEmissionFactorSource()).isEqualTo("DEFRA 2024");
    }

    // ── Does not modify input ─────────────────────────────────────────────

    @Test
    void calculate_doesNotModifyInputActivity() {
        Activity activity = flightActivity(BigDecimal.valueOf(1500), CabinClass.ECONOMY);

        when(emissionFactorRegistry.find(eq(ActivityCategory.FLIGHT), any(Instant.class)))
                .thenReturn(Optional.of(flightFactor()));

        buildCalculator().calculate(activity);

        assertThat(activity.getId()).isEqualTo("act-flight-001");
        assertThat(activity.getCategory()).isEqualTo(ActivityCategory.FLIGHT);
        assertThat(activity.getMerchant()).isEqualTo("IndiGo");
    }
}
