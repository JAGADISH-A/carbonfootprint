package com.carbonfootprint.platform.carbon.coach.service;

import com.carbonfootprint.platform.carbon.analytics.model.CarbonAnalyticsResponse;
import com.carbonfootprint.platform.carbon.analytics.model.CarbonInsightResponse;
import com.carbonfootprint.platform.carbon.analytics.model.CategoryEmissionSummary;
import com.carbonfootprint.platform.carbon.analytics.model.MonthlyEmissionTrend;
import com.carbonfootprint.platform.carbon.analytics.model.TopEmissionActivity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CoachConfidenceCalculatorTest {

    private final CoachConfidenceCalculator calculator = new CoachConfidenceCalculator();

    // ── Null / empty inputs ──────────────────────────────────────────────

    @Test
    void calculate_nullInsight_returnsZero() {
        assertThat(calculator.calculate(null)).isEqualTo(0);
    }

    @Test
    void calculate_nullAnalytics_returnsZero() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .analytics(null)
                .build();

        assertThat(calculator.calculate(insight)).isEqualTo(0);
    }

    @Test
    void calculate_emptyAnalytics_returnsZero() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .analytics(CarbonAnalyticsResponse.builder()
                        .activityCount(0)
                        .build())
                .build();

        assertThat(calculator.calculate(insight)).isEqualTo(0);
    }

    // ── One activity ─────────────────────────────────────────────────────

    @Test
    void calculate_oneActivity_noExtras_returnsLowScore() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .analytics(CarbonAnalyticsResponse.builder()
                        .activityCount(1)
                        .totalCarbonKg(new BigDecimal("5.0"))
                        .build())
                .build();

        int score = calculator.calculate(insight);

        // 1 activity = 5, totalCarbonKg present = 3, freshness unknown = 0 → 8
        assertThat(score).isBetween(5, 15);
    }

    // ── Many activities ──────────────────────────────────────────────────

    @Test
    void calculate_manyActivities_fullAnalytics_returnsHighScore() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .analytics(CarbonAnalyticsResponse.builder()
                        .activityCount(15)
                        .totalCarbonKg(new BigDecimal("120.0"))
                        .averageDailyKg(new BigDecimal("4.0"))
                        .categoryTotals(List.of(
                                CategoryEmissionSummary.builder().category("FUEL").build(),
                                CategoryEmissionSummary.builder().category("SHOPPING").build(),
                                CategoryEmissionSummary.builder().category("ELECTRICITY").build(),
                                CategoryEmissionSummary.builder().category("FLIGHT").build(),
                                CategoryEmissionSummary.builder().category("FOOD").build()
                        ))
                        .monthlyTrend(List.of(
                                MonthlyEmissionTrend.builder().month("2026-01").build(),
                                MonthlyEmissionTrend.builder().month("2026-02").build(),
                                MonthlyEmissionTrend.builder().month("2026-03").build()
                        ))
                        .topActivities(List.of(
                                TopEmissionActivity.builder().merchant("Shell").build()
                        ))
                        .periodEnd(Instant.now().minus(2, ChronoUnit.DAYS))
                        .build())
                .build();

        int score = calculator.calculate(insight);

        // 15 acts=35, 5 cats=25, 3 months=20, 5 fields=15, fresh=5 → 100
        assertThat(score).isEqualTo(100);
    }

    // ── Incomplete analytics ─────────────────────────────────────────────

    @Test
    void calculate_incompleteAnalytics_mediumScore() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .analytics(CarbonAnalyticsResponse.builder()
                        .activityCount(5)
                        .totalCarbonKg(new BigDecimal("30.0"))
                        .categoryTotals(List.of(
                                CategoryEmissionSummary.builder().category("FUEL").build(),
                                CategoryEmissionSummary.builder().category("SHOPPING").build()
                        ))
                        .build())
                .build();

        int score = calculator.calculate(insight);

        // 5 acts=25, 2 cats=15, 0 months=0, 2 fields=6, no freshness=0 → 46
        assertThat(score).isBetween(40, 55);
    }

    // ── Complete analytics ───────────────────────────────────────────────

    @Test
    void calculate_completeAnalytics_highScore() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .analytics(CarbonAnalyticsResponse.builder()
                        .activityCount(10)
                        .totalCarbonKg(new BigDecimal("80.0"))
                        .averageDailyKg(new BigDecimal("2.7"))
                        .categoryTotals(List.of(
                                CategoryEmissionSummary.builder().category("FUEL").build(),
                                CategoryEmissionSummary.builder().category("SHOPPING").build(),
                                CategoryEmissionSummary.builder().category("ELECTRICITY").build()
                        ))
                        .monthlyTrend(List.of(
                                MonthlyEmissionTrend.builder().month("2026-01").build(),
                                MonthlyEmissionTrend.builder().month("2026-02").build()
                        ))
                        .topActivities(List.of(
                                TopEmissionActivity.builder().merchant("Ashoka").build()
                        ))
                        .periodEnd(Instant.now().minus(5, ChronoUnit.DAYS))
                        .build())
                .build();

        int score = calculator.calculate(insight);

        // 10 acts=35, 3 cats=20, 2 months=14, 5 fields=15, fresh=5 → 89
        assertThat(score).isBetween(80, 100);
    }

    // ── Category diversity scoring ───────────────────────────────────────

    @Test
    void calculate_singleCategory_lowerThanMultiCategory() {
        CarbonInsightResponse singleCat = CarbonInsightResponse.builder()
                .analytics(CarbonAnalyticsResponse.builder()
                        .activityCount(10)
                        .totalCarbonKg(new BigDecimal("50.0"))
                        .categoryTotals(List.of(
                                CategoryEmissionSummary.builder().category("FUEL").build()
                        ))
                        .build())
                .build();

        CarbonInsightResponse multiCat = CarbonInsightResponse.builder()
                .analytics(CarbonAnalyticsResponse.builder()
                        .activityCount(10)
                        .totalCarbonKg(new BigDecimal("50.0"))
                        .categoryTotals(List.of(
                                CategoryEmissionSummary.builder().category("FUEL").build(),
                                CategoryEmissionSummary.builder().category("SHOPPING").build(),
                                CategoryEmissionSummary.builder().category("ELECTRICITY").build()
                        ))
                        .build())
                .build();

        assertThat(calculator.calculate(multiCat))
                .isGreaterThan(calculator.calculate(singleCat));
    }

    // ── Temporal coverage scoring ────────────────────────────────────────

    @Test
    void calculate_moreMonths_higherScore() {
        CarbonInsightResponse oneMonth = CarbonInsightResponse.builder()
                .analytics(CarbonAnalyticsResponse.builder()
                        .activityCount(10)
                        .monthlyTrend(List.of(
                                MonthlyEmissionTrend.builder().month("2026-01").build()
                        ))
                        .build())
                .build();

        CarbonInsightResponse threeMonths = CarbonInsightResponse.builder()
                .analytics(CarbonAnalyticsResponse.builder()
                        .activityCount(10)
                        .monthlyTrend(List.of(
                                MonthlyEmissionTrend.builder().month("2026-01").build(),
                                MonthlyEmissionTrend.builder().month("2026-02").build(),
                                MonthlyEmissionTrend.builder().month("2026-03").build()
                        ))
                        .build())
                .build();

        assertThat(calculator.calculate(threeMonths))
                .isGreaterThan(calculator.calculate(oneMonth));
    }

    // ── Freshness scoring ────────────────────────────────────────────────

    @Test
    void calculate_recentData_higherThanStale() {
        CarbonInsightResponse recent = CarbonInsightResponse.builder()
                .analytics(CarbonAnalyticsResponse.builder()
                        .activityCount(5)
                        .periodEnd(Instant.now().minus(2, ChronoUnit.DAYS))
                        .build())
                .build();

        CarbonInsightResponse stale = CarbonInsightResponse.builder()
                .analytics(CarbonAnalyticsResponse.builder()
                        .activityCount(5)
                        .periodEnd(Instant.now().minus(60, ChronoUnit.DAYS))
                        .build())
                .build();

        assertThat(calculator.calculate(recent))
                .isGreaterThan(calculator.calculate(stale));
    }

    // ── Score bounds ─────────────────────────────────────────────────────

    @Test
    void calculate_scoreClampedTo100_max() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .analytics(CarbonAnalyticsResponse.builder()
                        .activityCount(100)
                        .totalCarbonKg(new BigDecimal("500.0"))
                        .averageDailyKg(new BigDecimal("16.7"))
                        .categoryTotals(List.of(
                                CategoryEmissionSummary.builder().category("A").build(),
                                CategoryEmissionSummary.builder().category("B").build(),
                                CategoryEmissionSummary.builder().category("C").build(),
                                CategoryEmissionSummary.builder().category("D").build(),
                                CategoryEmissionSummary.builder().category("E").build()
                        ))
                        .monthlyTrend(List.of(
                                MonthlyEmissionTrend.builder().month("1").build(),
                                MonthlyEmissionTrend.builder().month("2").build(),
                                MonthlyEmissionTrend.builder().month("3").build()
                        ))
                        .topActivities(List.of(
                                TopEmissionActivity.builder().build()
                        ))
                        .periodEnd(Instant.now())
                        .build())
                .build();

        assertThat(calculator.calculate(insight)).isEqualTo(100);
    }

    @Test
    void calculate_scoreNeverNegative() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .analytics(CarbonAnalyticsResponse.builder()
                        .activityCount(-5)
                        .build())
                .build();

        assertThat(calculator.calculate(insight)).isGreaterThanOrEqualTo(0);
    }

    // ── Activity count thresholds ────────────────────────────────────────

    @Test
    void calculate_activityThresholds_stepChanges() {
        CarbonInsightResponse zero = insightWithActivities(0);
        CarbonInsightResponse one = insightWithActivities(1);
        CarbonInsightResponse two = insightWithActivities(2);
        CarbonInsightResponse four = insightWithActivities(4);
        CarbonInsightResponse ten = insightWithActivities(10);

        assertThat(calculator.calculate(zero)).isEqualTo(0);
        assertThat(calculator.calculate(one)).isGreaterThan(0);
        assertThat(calculator.calculate(two)).isGreaterThan(calculator.calculate(one));
        assertThat(calculator.calculate(four)).isGreaterThan(calculator.calculate(two));
        assertThat(calculator.calculate(ten)).isGreaterThan(calculator.calculate(four));
    }

    private CarbonInsightResponse insightWithActivities(int count) {
        return CarbonInsightResponse.builder()
                .analytics(CarbonAnalyticsResponse.builder()
                        .activityCount(count)
                        .build())
                .build();
    }
}
