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
class ShoppingEmissionCalculatorTest {

    @Mock
    private EmissionFactorRegistry emissionFactorRegistry;

    private ShoppingEmissionCalculator buildCalculator() {
        return new ShoppingEmissionCalculator(emissionFactorRegistry);
    }

    private Activity shoppingActivity(BigDecimal spend) {
        return Activity.builder()
                .id("act-shopping-001")
                .category(ActivityCategory.SHOPPING)
                .merchant("Amazon")
                .amount(spend)
                .unit("INR")
                .metadata(Map.of("carbonHints", Map.of(
                        "activityType", "SHOPPING",
                        "merchantIndustry", "ELECTRONICS")))
                .occurredAt(Instant.parse("2026-01-15T10:00:00Z"))
                .build();
    }

    private EmissionFactor shoppingFactor() {
        return EmissionFactor.builder()
                .id("ef-shopping-001")
                .value(BigDecimal.valueOf(0.00085))
                .unit("INR")
                .category(ActivityCategory.SHOPPING)
                .source("DEFRA 2024")
                .version("2024-v1")
                .methodName("Spend-based — Default EF")
                .validFrom(Instant.parse("2024-01-01T00:00:00Z"))
                .build();
    }

    // ── supports() ────────────────────────────────────────────────────────

    @Test
    void supports_shoppingCategory_returnsTrue() {
        Activity activity = shoppingActivity(BigDecimal.valueOf(1500));

        assertThat(buildCalculator().supports(activity)).isTrue();
    }

    @Test
    void supports_nonShoppingCategory_returnsFalse() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.FUEL)
                .build();

        assertThat(buildCalculator().supports(activity)).isFalse();
    }

    // ── calculate() ───────────────────────────────────────────────────────

    @Test
    void calculate_validSpend_returnsCorrectEmission() {
        BigDecimal spend = BigDecimal.valueOf(5000);
        Activity activity = shoppingActivity(spend);
        EmissionFactor factor = shoppingFactor();

        when(emissionFactorRegistry.find(eq(ActivityCategory.SHOPPING), any(Instant.class)))
                .thenReturn(Optional.of(factor));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        assertThat(result.getCarbonKg()).isEqualByComparingTo(BigDecimal.valueOf(4.25));
        assertThat(result.getActivityId()).isEqualTo("act-shopping-001");
        assertThat(result.getEmissionFactor()).isEqualTo(factor);
        assertThat(result.getActivityQuantity()).isEqualByComparingTo(spend);
        assertThat(result.getActivityUnit()).isEqualTo("INR");
        assertThat(result.getMethodology()).isEqualTo("Spend-based — Default EF");
        assertThat(result.getCalculatedAt()).isNotNull();
    }

    @Test
    void calculate_differentSpend_usesSameFactor() {
        BigDecimal spend = BigDecimal.valueOf(10000);
        Activity activity = shoppingActivity(spend);

        EmissionFactor factor = shoppingFactor();

        when(emissionFactorRegistry.find(eq(ActivityCategory.SHOPPING), any(Instant.class)))
                .thenReturn(Optional.of(factor));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        assertThat(result.getCarbonKg()).isEqualByComparingTo(BigDecimal.valueOf(8.5));
    }

    @Test
    void calculate_noEmissionFactor_returnsEmpty() {
        Activity activity = shoppingActivity(BigDecimal.valueOf(2000));

        when(emissionFactorRegistry.find(eq(ActivityCategory.SHOPPING), any(Instant.class)))
                .thenReturn(Optional.empty());

        assertThat(buildCalculator().calculate(activity)).isEmpty();
    }

    @Test
    void calculate_nullAmount_returnsEmpty() {
        Activity activity = shoppingActivity(null);

        assertThat(buildCalculator().calculate(activity)).isEmpty();
        verify(emissionFactorRegistry, never()).find(any(ActivityCategory.class), any(Instant.class));
    }

    @Test
    void calculate_zeroAmount_returnsEmpty() {
        Activity activity = shoppingActivity(BigDecimal.ZERO);

        assertThat(buildCalculator().calculate(activity)).isEmpty();
        verify(emissionFactorRegistry, never()).find(any(ActivityCategory.class), any(Instant.class));
    }

    @Test
    void calculate_negativeAmount_returnsEmpty() {
        Activity activity = shoppingActivity(BigDecimal.valueOf(-500));

        assertThat(buildCalculator().calculate(activity)).isEmpty();
        verify(emissionFactorRegistry, never()).find(any(ActivityCategory.class), any(Instant.class));
    }

    @Test
    void calculate_nonShoppingCategory_returnsEmpty() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.FUEL)
                .amount(BigDecimal.valueOf(2000))
                .build();

        assertThat(buildCalculator().calculate(activity)).isEmpty();
    }

    @Test
    void calculate_usesActivityOccurredAtForFactorLookup() {
        Instant occurredAt = Instant.parse("2025-06-15T08:00:00Z");
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.SHOPPING)
                .amount(BigDecimal.valueOf(3000))
                .occurredAt(occurredAt)
                .build();

        EmissionFactor factor = shoppingFactor();

        when(emissionFactorRegistry.find(eq(ActivityCategory.SHOPPING), eq(occurredAt)))
                .thenReturn(Optional.of(factor));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        assertThat(result.getCarbonKg()).isEqualByComparingTo(BigDecimal.valueOf(2.55));
        verify(emissionFactorRegistry).find(eq(ActivityCategory.SHOPPING), eq(occurredAt));
    }

    @Test
    void calculate_noOccurredAt_usesNowForFactorLookup() {
        Activity activity = Activity.builder()
                .id("act-001")
                .category(ActivityCategory.SHOPPING)
                .amount(BigDecimal.valueOf(2500))
                .build();

        EmissionFactor factor = shoppingFactor();

        when(emissionFactorRegistry.find(eq(ActivityCategory.SHOPPING), any(Instant.class)))
                .thenReturn(Optional.of(factor));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        assertThat(result.getCarbonKg()).isEqualByComparingTo(BigDecimal.valueOf(2.125));
    }

    // ── getOrder() ────────────────────────────────────────────────────────

    @Test
    void getOrder_returns10() {
        assertThat(buildCalculator().getOrder()).isEqualTo(10);
    }

    // ── Result metadata ───────────────────────────────────────────────────

    @Test
    void calculate_resultHasEmptyBreakdown() {
        Activity activity = shoppingActivity(BigDecimal.valueOf(5000));

        when(emissionFactorRegistry.find(eq(ActivityCategory.SHOPPING), any(Instant.class)))
                .thenReturn(Optional.of(shoppingFactor()));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        assertThat(result.getBreakdown()).isEmpty();
        assertThat(result.hasBreakdown()).isFalse();
    }

    @Test
    void calculate_resultConvenienceMethodsWork() {
        Activity activity = shoppingActivity(BigDecimal.valueOf(5000));

        when(emissionFactorRegistry.find(eq(ActivityCategory.SHOPPING), any(Instant.class)))
                .thenReturn(Optional.of(shoppingFactor()));

        EmissionResult result = buildCalculator().calculate(activity).orElseThrow();

        assertThat(result.getEmissionFactorValue()).isEqualByComparingTo(BigDecimal.valueOf(0.00085));
        assertThat(result.getEmissionFactorSource()).isEqualTo("DEFRA 2024");
    }

    // ── Does not modify input ─────────────────────────────────────────────

    @Test
    void calculate_doesNotModifyInputActivity() {
        Activity activity = shoppingActivity(BigDecimal.valueOf(5000));

        when(emissionFactorRegistry.find(eq(ActivityCategory.SHOPPING), any(Instant.class)))
                .thenReturn(Optional.of(shoppingFactor()));

        buildCalculator().calculate(activity);

        assertThat(activity.getId()).isEqualTo("act-shopping-001");
        assertThat(activity.getCategory()).isEqualTo(ActivityCategory.SHOPPING);
        assertThat(activity.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(activity.getUnit()).isEqualTo("INR");
    }
}
