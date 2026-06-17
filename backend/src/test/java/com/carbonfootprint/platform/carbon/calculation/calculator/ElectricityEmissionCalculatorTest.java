package com.carbonfootprint.platform.carbon.calculation.calculator;

import com.carbonfootprint.platform.activity.model.Activity;
import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionFactor;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionFactorRegistry;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionResult;
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
class ElectricityEmissionCalculatorTest {

    @Mock
    private EmissionFactorRegistry emissionFactorRegistry;

    private ElectricityEmissionCalculator buildCalculator() {
        return new ElectricityEmissionCalculator(emissionFactorRegistry);
    }

    private Activity electricityActivity(String energySource, BigDecimal quantity) {
        Map<String, Object> hints = Map.of(
                "activityType", "ELECTRICITY",
                "energySource", energySource,
                "electricityUnit", "KWH"
        );
        return Activity.builder()
                .id("act-elec-001")
                .category(ActivityCategory.ELECTRICITY)
                .merchant("BESCOM")
                .amount(quantity)
                .unit("kWh")
                .metadata(Map.of("carbonHints", hints))
                .occurredAt(Instant.parse("2026-01-15T10:00:00Z"))
                .build();
    }

    private EmissionFactor gridFactor() {
        return EmissionFactor.builder()
                .id("ef-elec-grid-001")
                .value(BigDecimal.valueOf(0.82))
                .unit("kWh")
                .category(ActivityCategory.ELECTRICITY)
                .source("India MoEFCC 2024")
                .version("2024-v1")
                .methodName("Tier 1 — Default EF")
                .validFrom(Instant.parse("2024-01-01T00:00:00Z"))
                .build();
    }

    // ── supports() ────────────────────────────────────────────────────────

    @Test
    void supports_electricityCategoryWithEnergySource_returnsTrue() {
        Activity activity = electricityActivity("GRID", BigDecimal.valueOf(250));

        assertThat(buildCalculator().supports(activity)).isTrue();
    }

    @Test
    void supports_nonElectricityCategory_returnsFalse() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.FUEL)
                .build();

        assertThat(buildCalculator().supports(activity)).isFalse();
    }

    @Test
    void supports_electricityCategoryButNoCarbonHints_returnsFalse() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.ELECTRICITY)
                .metadata(Map.of())
                .build();

        assertThat(buildCalculator().supports(activity)).isFalse();
    }

    @Test
    void supports_electricityCategoryButNoEnergySource_returnsFalse() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.ELECTRICITY)
                .metadata(Map.of("carbonHints", Map.of("activityType", "ELECTRICITY")))
                .build();

        assertThat(buildCalculator().supports(activity)).isFalse();
    }

    @Test
    void supports_electricityCategoryButWrongActivityType_returnsFalse() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.ELECTRICITY)
                .metadata(Map.of("carbonHints", Map.of("activityType", "FUEL", "energySource", "GRID")))
                .build();

        assertThat(buildCalculator().supports(activity)).isFalse();
    }

    @Test
    void supports_electricityCategoryWithNullMetadata_returnsFalse() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.ELECTRICITY)
                .build();

        assertThat(buildCalculator().supports(activity)).isFalse();
    }

    @Test
    void supports_solarEnergySource_returnsTrue() {
        Activity activity = electricityActivity("SOLAR", BigDecimal.valueOf(100));

        assertThat(buildCalculator().supports(activity)).isTrue();
    }

    // ── calculate() ───────────────────────────────────────────────────────

    @Test
    void calculate_gridActivity_returnsCorrectEmission() {
        BigDecimal quantity = BigDecimal.valueOf(250);
        Activity activity = electricityActivity("GRID", quantity);
        EmissionFactor factor = gridFactor();

        when(emissionFactorRegistry.find(eq(ActivityCategory.ELECTRICITY), any(Instant.class)))
                .thenReturn(Optional.of(factor));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        assertThat(result.getCarbonKg()).isEqualByComparingTo(BigDecimal.valueOf(205.0));
        assertThat(result.getActivityId()).isEqualTo("act-elec-001");
        assertThat(result.getEmissionFactor()).isEqualTo(factor);
        assertThat(result.getActivityQuantity()).isEqualByComparingTo(quantity);
        assertThat(result.getActivityUnit()).isEqualTo("kWh");
        assertThat(result.getMethodology()).isEqualTo("Tier 1 — Default EF");
        assertThat(result.getCalculatedAt()).isNotNull();
    }

    @Test
    void calculate_solarActivity_usesSameGridFactor() {
        BigDecimal quantity = BigDecimal.valueOf(100);
        Activity activity = electricityActivity("SOLAR", quantity);

        EmissionFactor factor = gridFactor();

        when(emissionFactorRegistry.find(eq(ActivityCategory.ELECTRICITY), any(Instant.class)))
                .thenReturn(Optional.of(factor));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        assertThat(result.getCarbonKg()).isEqualByComparingTo(BigDecimal.valueOf(82.0));
    }

    @Test
    void calculate_noCarbonHints_returnsEmpty() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.ELECTRICITY)
                .amount(BigDecimal.valueOf(250))
                .metadata(Map.of())
                .build();

        assertThat(buildCalculator().calculate(activity)).isEmpty();
    }

    @Test
    void calculate_noEnergySource_returnsEmpty() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.ELECTRICITY)
                .amount(BigDecimal.valueOf(250))
                .metadata(Map.of("carbonHints", Map.of("activityType", "ELECTRICITY")))
                .build();

        assertThat(buildCalculator().calculate(activity)).isEmpty();
    }

    @Test
    void calculate_nullQuantity_returnsEmpty() {
        Activity activity = electricityActivity("GRID", null);

        assertThat(buildCalculator().calculate(activity)).isEmpty();
        verify(emissionFactorRegistry, never()).find(any(ActivityCategory.class), any(Instant.class));
    }

    @Test
    void calculate_zeroQuantity_returnsEmpty() {
        Activity activity = electricityActivity("GRID", BigDecimal.ZERO);

        assertThat(buildCalculator().calculate(activity)).isEmpty();
        verify(emissionFactorRegistry, never()).find(any(ActivityCategory.class), any(Instant.class));
    }

    @Test
    void calculate_negativeQuantity_returnsEmpty() {
        Activity activity = electricityActivity("GRID", BigDecimal.valueOf(-50));

        assertThat(buildCalculator().calculate(activity)).isEmpty();
        verify(emissionFactorRegistry, never()).find(any(ActivityCategory.class), any(Instant.class));
    }

    @Test
    void calculate_noEmissionFactor_returnsEmpty() {
        Activity activity = electricityActivity("GRID", BigDecimal.valueOf(250));

        when(emissionFactorRegistry.find(eq(ActivityCategory.ELECTRICITY), any(Instant.class)))
                .thenReturn(Optional.empty());

        assertThat(buildCalculator().calculate(activity)).isEmpty();
    }

    @Test
    void calculate_usesActivityOccurredAtForFactorLookup() {
        Instant occurredAt = Instant.parse("2025-06-15T08:00:00Z");
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.ELECTRICITY)
                .amount(BigDecimal.valueOf(200))
                .occurredAt(occurredAt)
                .metadata(Map.of("carbonHints", Map.of(
                        "activityType", "ELECTRICITY",
                        "energySource", "GRID")))
                .build();

        EmissionFactor factor = gridFactor();

        when(emissionFactorRegistry.find(eq(ActivityCategory.ELECTRICITY), eq(occurredAt)))
                .thenReturn(Optional.of(factor));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        assertThat(result.getCarbonKg()).isEqualByComparingTo(BigDecimal.valueOf(164.0));
        verify(emissionFactorRegistry).find(eq(ActivityCategory.ELECTRICITY), eq(occurredAt));
    }

    @Test
    void calculate_noOccurredAt_usesNowForFactorLookup() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.ELECTRICITY)
                .amount(BigDecimal.valueOf(300))
                .metadata(Map.of("carbonHints", Map.of(
                        "activityType", "ELECTRICITY",
                        "energySource", "GRID")))
                .build();

        EmissionFactor factor = gridFactor();

        when(emissionFactorRegistry.find(eq(ActivityCategory.ELECTRICITY), any(Instant.class)))
                .thenReturn(Optional.of(factor));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        assertThat(result.getCarbonKg()).isEqualByComparingTo(BigDecimal.valueOf(246.0));
    }

    // ── getOrder() ────────────────────────────────────────────────────────

    @Test
    void getOrder_returns10() {
        assertThat(buildCalculator().getOrder()).isEqualTo(10);
    }

    // ── Result metadata ───────────────────────────────────────────────────

    @Test
    void calculate_resultHasEmptyBreakdown() {
        Activity activity = electricityActivity("GRID", BigDecimal.valueOf(250));

        when(emissionFactorRegistry.find(eq(ActivityCategory.ELECTRICITY), any(Instant.class)))
                .thenReturn(Optional.of(gridFactor()));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        assertThat(result.getBreakdown()).isEmpty();
        assertThat(result.hasBreakdown()).isFalse();
    }

    @Test
    void calculate_resultConvenienceMethodsWork() {
        Activity activity = electricityActivity("GRID", BigDecimal.valueOf(250));

        when(emissionFactorRegistry.find(eq(ActivityCategory.ELECTRICITY), any(Instant.class)))
                .thenReturn(Optional.of(gridFactor()));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        assertThat(result.getEmissionFactorValue()).isEqualByComparingTo(BigDecimal.valueOf(0.82));
        assertThat(result.getEmissionFactorSource()).isEqualTo("India MoEFCC 2024");
    }

    // ── Does not modify input ─────────────────────────────────────────────

    @Test
    void calculate_doesNotModifyInputActivity() {
        Activity activity = electricityActivity("GRID", BigDecimal.valueOf(250));

        when(emissionFactorRegistry.find(eq(ActivityCategory.ELECTRICITY), any(Instant.class)))
                .thenReturn(Optional.of(gridFactor()));

        buildCalculator().calculate(activity);

        assertThat(activity.getId()).isEqualTo("act-elec-001");
        assertThat(activity.getCategory()).isEqualTo(ActivityCategory.ELECTRICITY);
        assertThat(activity.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(250));
        assertThat(activity.getUnit()).isEqualTo("kWh");
    }
}
