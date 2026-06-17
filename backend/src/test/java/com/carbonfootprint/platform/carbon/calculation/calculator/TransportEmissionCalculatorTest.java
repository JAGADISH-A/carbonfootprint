package com.carbonfootprint.platform.carbon.calculation.calculator;

import com.carbonfootprint.platform.activity.model.Activity;
import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionFactor;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionFactorRegistry;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionResult;
import com.carbonfootprint.platform.ingestion.enrichment.model.TransportMode;
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
class TransportEmissionCalculatorTest {

    @Mock
    private EmissionFactorRegistry emissionFactorRegistry;

    private TransportEmissionCalculator buildCalculator() {
        return new TransportEmissionCalculator(emissionFactorRegistry);
    }

    private Activity transportActivity(TransportMode transportMode, BigDecimal quantity) {
        Map<String, Object> hints = Map.of(
                "activityType", "TRANSPORT",
                "transportMode", transportMode.name(),
                "transportUnit", "KM"
        );
        return Activity.builder()
                .id("act-transport-001")
                .category(ActivityCategory.TRANSPORT)
                .merchant("Uber")
                .amount(quantity)
                .unit("km")
                .metadata(Map.of("carbonHints", hints))
                .occurredAt(Instant.parse("2026-01-15T10:00:00Z"))
                .build();
    }

    private EmissionFactor taxiFactor() {
        return EmissionFactor.builder()
                .id("ef-taxi-001")
                .value(BigDecimal.valueOf(0.21))
                .unit("km")
                .category(ActivityCategory.TRANSPORT)
                .transportMode(TransportMode.TAXI)
                .source("DEFRA 2024")
                .version("2024-v1")
                .methodName("Tier 1 — Default EF")
                .validFrom(Instant.parse("2024-01-01T00:00:00Z"))
                .build();
    }

    private EmissionFactor busFactor() {
        return EmissionFactor.builder()
                .id("ef-bus-001")
                .value(BigDecimal.valueOf(0.089))
                .unit("km")
                .category(ActivityCategory.TRANSPORT)
                .transportMode(TransportMode.BUS)
                .source("DEFRA 2024")
                .version("2024-v1")
                .methodName("Tier 1 — Default EF")
                .validFrom(Instant.parse("2024-01-01T00:00:00Z"))
                .build();
    }

    // ── supports() ────────────────────────────────────────────────────────

    @Test
    void supports_transportCategoryWithTransportMode_returnsTrue() {
        Activity activity = transportActivity(TransportMode.TAXI, BigDecimal.valueOf(15));

        assertThat(buildCalculator().supports(activity)).isTrue();
    }

    @Test
    void supports_nonTransportCategory_returnsFalse() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.FUEL)
                .build();

        assertThat(buildCalculator().supports(activity)).isFalse();
    }

    @Test
    void supports_transportCategoryButNoCarbonHints_returnsFalse() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.TRANSPORT)
                .metadata(Map.of())
                .build();

        assertThat(buildCalculator().supports(activity)).isFalse();
    }

    @Test
    void supports_transportCategoryButNoTransportMode_returnsFalse() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.TRANSPORT)
                .metadata(Map.of("carbonHints", Map.of("activityType", "TRANSPORT")))
                .build();

        assertThat(buildCalculator().supports(activity)).isFalse();
    }

    @Test
    void supports_transportCategoryWithNullMetadata_returnsFalse() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.TRANSPORT)
                .build();

        assertThat(buildCalculator().supports(activity)).isFalse();
    }

    @Test
    void supports_transportCategoryWithInvalidTransportModeString_returnsFalse() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.TRANSPORT)
                .metadata(Map.of("carbonHints", Map.of("transportMode", "INVALID_MODE")))
                .build();

        assertThat(buildCalculator().supports(activity)).isFalse();
    }

    @Test
    void supports_allValidTransportModes_returnsTrue() {
        for (TransportMode mode : TransportMode.values()) {
            Activity activity = transportActivity(mode, BigDecimal.valueOf(10));
            assertThat(buildCalculator().supports(activity))
                    .as("supports(%s)", mode)
                    .isTrue();
        }
    }

    // ── calculate() ───────────────────────────────────────────────────────

    @Test
    void calculate_taxiActivity_returnsCorrectEmission() {
        TransportMode transportMode = TransportMode.TAXI;
        BigDecimal quantity = BigDecimal.valueOf(20);
        Activity activity = transportActivity(transportMode, quantity);
        EmissionFactor factor = taxiFactor();

        when(emissionFactorRegistry.find(eq(ActivityCategory.TRANSPORT), eq(transportMode), any(Instant.class)))
                .thenReturn(Optional.of(factor));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        assertThat(result.getCarbonKg()).isEqualByComparingTo(BigDecimal.valueOf(4.2));
        assertThat(result.getActivityId()).isEqualTo("act-transport-001");
        assertThat(result.getEmissionFactor()).isEqualTo(factor);
        assertThat(result.getActivityQuantity()).isEqualByComparingTo(quantity);
        assertThat(result.getActivityUnit()).isEqualTo("km");
        assertThat(result.getMethodology()).isEqualTo("Tier 1 — Default EF");
        assertThat(result.getCalculatedAt()).isNotNull();
    }

    @Test
    void calculate_busActivity_usesBusFactor() {
        TransportMode transportMode = TransportMode.BUS;
        BigDecimal quantity = BigDecimal.valueOf(100);
        Activity activity = transportActivity(transportMode, quantity);

        EmissionFactor factor = busFactor();

        when(emissionFactorRegistry.find(eq(ActivityCategory.TRANSPORT), eq(transportMode), any(Instant.class)))
                .thenReturn(Optional.of(factor));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        assertThat(result.getCarbonKg()).isEqualByComparingTo(BigDecimal.valueOf(8.9));
    }

    @Test
    void calculate_noCarbonHints_returnsEmpty() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.TRANSPORT)
                .amount(BigDecimal.valueOf(15))
                .metadata(Map.of())
                .build();

        assertThat(buildCalculator().calculate(activity)).isEmpty();
    }

    @Test
    void calculate_noTransportMode_returnsEmpty() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.TRANSPORT)
                .amount(BigDecimal.valueOf(15))
                .metadata(Map.of("carbonHints", Map.of("activityType", "TRANSPORT")))
                .build();

        assertThat(buildCalculator().calculate(activity)).isEmpty();
    }

    @Test
    void calculate_nullQuantity_returnsEmpty() {
        Activity activity = transportActivity(TransportMode.TAXI, null);

        assertThat(buildCalculator().calculate(activity)).isEmpty();
        verify(emissionFactorRegistry, never()).find(any(ActivityCategory.class), any(TransportMode.class), any(Instant.class));
    }

    @Test
    void calculate_zeroQuantity_returnsEmpty() {
        Activity activity = transportActivity(TransportMode.TAXI, BigDecimal.ZERO);

        assertThat(buildCalculator().calculate(activity)).isEmpty();
        verify(emissionFactorRegistry, never()).find(any(ActivityCategory.class), any(TransportMode.class), any(Instant.class));
    }

    @Test
    void calculate_negativeQuantity_returnsEmpty() {
        Activity activity = transportActivity(TransportMode.TAXI, BigDecimal.valueOf(-10));

        assertThat(buildCalculator().calculate(activity)).isEmpty();
        verify(emissionFactorRegistry, never()).find(any(ActivityCategory.class), any(TransportMode.class), any(Instant.class));
    }

    @Test
    void calculate_noEmissionFactor_returnsEmpty() {
        Activity activity = transportActivity(TransportMode.TAXI, BigDecimal.valueOf(50));

        when(emissionFactorRegistry.find(eq(ActivityCategory.TRANSPORT), eq(TransportMode.TAXI), any(Instant.class)))
                .thenReturn(Optional.empty());

        assertThat(buildCalculator().calculate(activity)).isEmpty();
    }

    @Test
    void calculate_usesActivityOccurredAtForFactorLookup() {
        Instant occurredAt = Instant.parse("2025-06-15T08:00:00Z");
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.TRANSPORT)
                .amount(BigDecimal.valueOf(30))
                .occurredAt(occurredAt)
                .metadata(Map.of("carbonHints", Map.of(
                        "activityType", "TRANSPORT",
                        "transportMode", "TAXI")))
                .build();

        EmissionFactor factor = taxiFactor();

        when(emissionFactorRegistry.find(eq(ActivityCategory.TRANSPORT), eq(TransportMode.TAXI), eq(occurredAt)))
                .thenReturn(Optional.of(factor));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        assertThat(result.getCarbonKg()).isEqualByComparingTo(BigDecimal.valueOf(6.3));
        verify(emissionFactorRegistry).find(eq(ActivityCategory.TRANSPORT), eq(TransportMode.TAXI), eq(occurredAt));
    }

    @Test
    void calculate_noOccurredAt_usesNowForFactorLookup() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.TRANSPORT)
                .amount(BigDecimal.valueOf(25))
                .metadata(Map.of("carbonHints", Map.of(
                        "activityType", "TRANSPORT",
                        "transportMode", "TAXI")))
                .build();

        EmissionFactor factor = taxiFactor();

        when(emissionFactorRegistry.find(eq(ActivityCategory.TRANSPORT), eq(TransportMode.TAXI), any(Instant.class)))
                .thenReturn(Optional.of(factor));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        assertThat(result.getCarbonKg()).isEqualByComparingTo(BigDecimal.valueOf(5.25));
    }

    // ── getOrder() ────────────────────────────────────────────────────────

    @Test
    void getOrder_returns10() {
        assertThat(buildCalculator().getOrder()).isEqualTo(10);
    }

    // ── Result metadata ───────────────────────────────────────────────────

    @Test
    void calculate_resultHasEmptyBreakdown() {
        Activity activity = transportActivity(TransportMode.TAXI, BigDecimal.valueOf(20));

        when(emissionFactorRegistry.find(eq(ActivityCategory.TRANSPORT), eq(TransportMode.TAXI), any(Instant.class)))
                .thenReturn(Optional.of(taxiFactor()));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        assertThat(result.getBreakdown()).isEmpty();
        assertThat(result.hasBreakdown()).isFalse();
    }

    @Test
    void calculate_resultConvenienceMethodsWork() {
        Activity activity = transportActivity(TransportMode.TAXI, BigDecimal.valueOf(20));

        when(emissionFactorRegistry.find(eq(ActivityCategory.TRANSPORT), eq(TransportMode.TAXI), any(Instant.class)))
                .thenReturn(Optional.of(taxiFactor()));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        assertThat(result.getEmissionFactorValue()).isEqualByComparingTo(BigDecimal.valueOf(0.21));
        assertThat(result.getEmissionFactorSource()).isEqualTo("DEFRA 2024");
    }

    // ── Does not modify input ─────────────────────────────────────────────

    @Test
    void calculate_doesNotModifyInputActivity() {
        Activity activity = transportActivity(TransportMode.TAXI, BigDecimal.valueOf(20));

        when(emissionFactorRegistry.find(eq(ActivityCategory.TRANSPORT), eq(TransportMode.TAXI), any(Instant.class)))
                .thenReturn(Optional.of(taxiFactor()));

        buildCalculator().calculate(activity);

        assertThat(activity.getId()).isEqualTo("act-transport-001");
        assertThat(activity.getCategory()).isEqualTo(ActivityCategory.TRANSPORT);
        assertThat(activity.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(20));
        assertThat(activity.getUnit()).isEqualTo("km");
    }
}
