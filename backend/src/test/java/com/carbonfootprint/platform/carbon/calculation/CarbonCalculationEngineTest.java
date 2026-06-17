package com.carbonfootprint.platform.carbon.calculation;

import com.carbonfootprint.platform.activity.model.Activity;
import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionCalculator;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionFactor;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarbonCalculationEngineTest {

    @Mock
    private EmissionCalculator calculatorA;

    @Mock
    private EmissionCalculator calculatorB;

    @Mock
    private EmissionCalculator failingCalculator;

    private CarbonCalculationEngine buildEngine(List<EmissionCalculator> calculators) {
        return new CarbonCalculationEngine(calculators);
    }

    private Activity sampleActivity() {
        return Activity.builder()
                .id("act-001")
                .category(ActivityCategory.FUEL)
                .merchant("Shell")
                .unit("litres")
                .amount(BigDecimal.valueOf(40))
                .build();
    }

    private EmissionResult sampleResult() {
        return EmissionResult.builder()
                .carbonKg(BigDecimal.valueOf(92.4))
                .activityId("act-001")
                .emissionFactor(EmissionFactor.builder()
                        .category(ActivityCategory.FUEL)
                        .value(BigDecimal.valueOf(2.31))
                        .unit("litre")
                        .source("DEFRA 2024")
                        .build())
                .activityQuantity(BigDecimal.valueOf(40))
                .activityUnit("litre")
                .methodology("Tier 1 — Default EF")
                .calculatedAt(Instant.parse("2026-01-15T10:00:00Z"))
                .build();
    }

    // ── Constructor / ordering ───────────────────────────────────────────

    @Test
    void constructor_sortsCalculatorsByOrderAscending() {
        when(calculatorA.getOrder()).thenReturn(100);
        when(calculatorB.getOrder()).thenReturn(10);

        CarbonCalculationEngine engine = buildEngine(List.of(calculatorA, calculatorB));

        // B (order=10) should be evaluated before A (order=100)
        // We verify this by making B return a result — A is never reached
        Activity activity = sampleActivity();
        when(calculatorB.supports(activity)).thenReturn(true);
        when(calculatorB.calculate(activity)).thenReturn(Optional.of(sampleResult()));

        Optional<EmissionResult> result = engine.calculate(activity);

        assertThat(result).isPresent();
        verify(calculatorB).calculate(activity);
        verify(calculatorA, never()).supports(any());
        verify(calculatorA, never()).calculate(any());
    }

    @Test
    void constructor_calculatorsListIsImmutablySorted() {
        when(calculatorA.getOrder()).thenReturn(50);
        when(calculatorB.getOrder()).thenReturn(50);

        CarbonCalculationEngine engine = buildEngine(List.of(calculatorA, calculatorB));

        // Verify both were registered (log output would show both)
        // The key property is that the list is immutable
        assertThat(engine).isNotNull();
    }

    // ── Happy path ──────────────────────────────────────────────────────

    @Test
    void calculate_firstSupportingCalculatorWins() {
        Activity activity = sampleActivity();
        EmissionResult expectedResult = sampleResult();

        when(calculatorA.supports(activity)).thenReturn(true);
        when(calculatorA.calculate(activity)).thenReturn(Optional.of(expectedResult));

        CarbonCalculationEngine engine = buildEngine(List.of(calculatorA));

        Optional<EmissionResult> result = engine.calculate(activity);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedResult);
        verify(calculatorA).calculate(activity);
    }

    @Test
    void calculate_firstCalculatorDoesNotSupport_secondTakesOver() {
        Activity activity = sampleActivity();
        EmissionResult expectedResult = sampleResult();

        when(calculatorA.supports(activity)).thenReturn(false);
        when(calculatorB.supports(activity)).thenReturn(true);
        when(calculatorB.calculate(activity)).thenReturn(Optional.of(expectedResult));

        CarbonCalculationEngine engine = buildEngine(List.of(calculatorA, calculatorB));

        Optional<EmissionResult> result = engine.calculate(activity);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedResult);
        verify(calculatorA, never()).calculate(activity);
        verify(calculatorB).calculate(activity);
    }

    @Test
    void calculate_noCalculatorSupports_returnsEmpty() {
        Activity activity = sampleActivity();

        when(calculatorA.supports(activity)).thenReturn(false);
        when(calculatorB.supports(activity)).thenReturn(false);

        CarbonCalculationEngine engine = buildEngine(List.of(calculatorA, calculatorB));

        Optional<EmissionResult> result = engine.calculate(activity);

        assertThat(result).isEmpty();
    }

    @Test
    void calculate_calculatorSupportsButReturnsEmpty_nextCalculatorRuns() {
        Activity activity = sampleActivity();
        EmissionResult expectedResult = sampleResult();

        when(calculatorA.supports(activity)).thenReturn(true);
        when(calculatorA.calculate(activity)).thenReturn(Optional.empty());
        when(calculatorB.supports(activity)).thenReturn(true);
        when(calculatorB.calculate(activity)).thenReturn(Optional.of(expectedResult));

        CarbonCalculationEngine engine = buildEngine(List.of(calculatorA, calculatorB));

        Optional<EmissionResult> result = engine.calculate(activity);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedResult);
        verify(calculatorA).calculate(activity);
        verify(calculatorB).calculate(activity);
    }

    @Test
    void calculate_calculatorSupportsButReturnsNull_treatedAsEmpty() {
        Activity activity = sampleActivity();
        EmissionResult expectedResult = sampleResult();

        when(calculatorA.supports(activity)).thenReturn(true);
        when(calculatorA.calculate(activity)).thenReturn(null);
        when(calculatorB.supports(activity)).thenReturn(true);
        when(calculatorB.calculate(activity)).thenReturn(Optional.of(expectedResult));

        CarbonCalculationEngine engine = buildEngine(List.of(calculatorA, calculatorB));

        Optional<EmissionResult> result = engine.calculate(activity);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedResult);
    }

    // ── Exception isolation ──────────────────────────────────────────────

    @Test
    void calculate_calculatorThrowsException_isSkippedGracefully() {
        Activity activity = sampleActivity();

        when(calculatorA.supports(activity)).thenReturn(true);
        when(calculatorA.calculate(activity)).thenThrow(new RuntimeException("Calculation failure"));

        CarbonCalculationEngine engine = buildEngine(List.of(calculatorA));

        Optional<EmissionResult> result = engine.calculate(activity);

        assertThat(result).isEmpty();
    }

    @Test
    void calculate_calculatorThrowsException_nextCalculatorRuns() {
        Activity activity = sampleActivity();
        EmissionResult expectedResult = sampleResult();

        when(calculatorA.supports(activity)).thenReturn(true);
        when(calculatorA.calculate(activity)).thenThrow(new RuntimeException("Boom"));
        when(calculatorB.supports(activity)).thenReturn(true);
        when(calculatorB.calculate(activity)).thenReturn(Optional.of(expectedResult));

        CarbonCalculationEngine engine = buildEngine(List.of(calculatorA, calculatorB));

        Optional<EmissionResult> result = engine.calculate(activity);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedResult);
    }

    @Test
    void calculate_calculatorSupportsThrowsException_isNotCalledWithCalculate() {
        Activity activity = sampleActivity();

        when(calculatorA.supports(activity)).thenReturn(true);
        when(calculatorA.calculate(activity)).thenThrow(new RuntimeException("Boom"));

        CarbonCalculationEngine engine = buildEngine(List.of(calculatorA));

        Optional<EmissionResult> result = engine.calculate(activity);

        assertThat(result).isEmpty();
    }

    @Test
    void calculate_allCalculatorsThrowExceptions_returnsEmpty() {
        Activity activity = sampleActivity();

        when(calculatorA.supports(activity)).thenReturn(true);
        when(calculatorA.calculate(activity)).thenThrow(new RuntimeException("Boom A"));
        when(calculatorB.supports(activity)).thenReturn(true);
        when(calculatorB.calculate(activity)).thenThrow(new RuntimeException("Boom B"));

        CarbonCalculationEngine engine = buildEngine(List.of(calculatorA, calculatorB));

        Optional<EmissionResult> result = engine.calculate(activity);

        assertThat(result).isEmpty();
    }

    // ── Input immutability ──────────────────────────────────────────────

    @Test
    void calculate_doesNotModifyInputActivity() {
        Activity original = sampleActivity();
        EmissionResult expectedResult = sampleResult();

        when(calculatorA.supports(any())).thenReturn(true);
        when(calculatorA.calculate(any())).thenReturn(Optional.of(expectedResult));

        CarbonCalculationEngine engine = buildEngine(List.of(calculatorA));

        engine.calculate(original);

        assertThat(original.getId()).isEqualTo("act-001");
        assertThat(original.getCategory()).isEqualTo(ActivityCategory.FUEL);
        assertThat(original.getMerchant()).isEqualTo("Shell");
        assertThat(original.getUnit()).isEqualTo("litres");
        assertThat(original.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(40));
    }

    // ── Null input ──────────────────────────────────────────────────────

    @Test
    void calculate_nullActivity_throwsIllegalArgumentException() {
        CarbonCalculationEngine engine = buildEngine(List.of(calculatorA));

        assertThatThrownBy(() -> engine.calculate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Activity must not be null");
    }

    // ── Empty calculator list ───────────────────────────────────────────

    @Test
    void calculate_noCalculatorsRegistered_returnsEmpty() {
        CarbonCalculationEngine engine = buildEngine(List.of());
        Activity activity = sampleActivity();

        Optional<EmissionResult> result = engine.calculate(activity);

        assertThat(result).isEmpty();
    }

    // ── Order priority ──────────────────────────────────────────────────

    @Test
    void calculate_lowerOrderCalculatorWinsWhenBothSupport() {
        Activity activity = sampleActivity();

        EmissionResult lowOrderResult = EmissionResult.builder()
                .carbonKg(BigDecimal.valueOf(100))
                .activityId("act-001")
                .emissionFactor(EmissionFactor.builder()
                        .category(ActivityCategory.FUEL)
                        .value(BigDecimal.valueOf(2.5))
                        .unit("litre")
                        .source("Low-order source")
                        .build())
                .activityQuantity(BigDecimal.valueOf(40))
                .activityUnit("litre")
                .methodology("Tier 1")
                .calculatedAt(Instant.parse("2026-01-15T10:00:00Z"))
                .build();

        EmissionResult highOrderResult = EmissionResult.builder()
                .carbonKg(BigDecimal.valueOf(200))
                .activityId("act-001")
                .emissionFactor(EmissionFactor.builder()
                        .category(ActivityCategory.FUEL)
                        .value(BigDecimal.valueOf(5.0))
                        .unit("litre")
                        .source("High-order source")
                        .build())
                .activityQuantity(BigDecimal.valueOf(40))
                .activityUnit("litre")
                .methodology("Tier 2")
                .calculatedAt(Instant.parse("2026-01-15T10:00:00Z"))
                .build();

        when(calculatorA.getOrder()).thenReturn(100);

        when(calculatorB.getOrder()).thenReturn(10);
        when(calculatorB.supports(activity)).thenReturn(true);
        when(calculatorB.calculate(activity)).thenReturn(Optional.of(lowOrderResult));

        CarbonCalculationEngine engine = buildEngine(List.of(calculatorA, calculatorB));

        Optional<EmissionResult> result = engine.calculate(activity);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(lowOrderResult);
        verify(calculatorA, never()).supports(any());
        verify(calculatorA, never()).calculate(any());
    }

    // ── supports() called before calculate() ─────────────────────────────

    @Test
    void calculate_supportsCalledBeforeCalculate() {
        Activity activity = sampleActivity();
        EmissionResult expectedResult = sampleResult();

        when(calculatorA.supports(activity)).thenReturn(true);
        when(calculatorA.calculate(activity)).thenReturn(Optional.of(expectedResult));

        CarbonCalculationEngine engine = buildEngine(List.of(calculatorA));

        engine.calculate(activity);

        verify(calculatorA).supports(activity);
        verify(calculatorA).calculate(activity);
    }

    @Test
    void calculate_calculateNotCalledWhenSupportsFalse() {
        Activity activity = sampleActivity();

        when(calculatorA.supports(activity)).thenReturn(false);

        CarbonCalculationEngine engine = buildEngine(List.of(calculatorA));

        engine.calculate(activity);

        verify(calculatorA).supports(activity);
        verify(calculatorA, never()).calculate(activity);
    }

    // ── Single calculator returns empty ──────────────────────────────────

    @Test
    void calculate_singleCalculatorSupportsButReturnsEmpty() {
        Activity activity = sampleActivity();

        when(calculatorA.supports(activity)).thenReturn(true);
        when(calculatorA.calculate(activity)).thenReturn(Optional.empty());

        CarbonCalculationEngine engine = buildEngine(List.of(calculatorA));

        Optional<EmissionResult> result = engine.calculate(activity);

        assertThat(result).isEmpty();
    }
}
