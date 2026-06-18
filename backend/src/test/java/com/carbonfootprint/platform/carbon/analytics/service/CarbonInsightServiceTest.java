package com.carbonfootprint.platform.carbon.analytics.service;

import com.carbonfootprint.platform.activity.model.Activity;
import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.carbon.analytics.model.CarbonAnalyticsResponse;
import com.carbonfootprint.platform.carbon.analytics.model.CarbonInsightResponse;
import com.carbonfootprint.platform.carbon.analytics.model.CategoryEmissionSummary;
import com.carbonfootprint.platform.carbon.analytics.model.MonthlyEmissionTrend;
import com.carbonfootprint.platform.carbon.analytics.model.TopEmissionActivity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CarbonInsightServiceTest {

    private CarbonInsightService service;

    @BeforeEach
    void setUp() {
        service = new CarbonInsightService();
    }

    // ── Empty / no data ───────────────────────────────────────────────────

    @Test
    void generateInsights_emptyAnalytics_returnsEmpty() {
        Optional<CarbonInsightResponse> result = service.generateInsights(
                Optional.empty(), List.of());
        assertThat(result).isEmpty();
    }

    @Test
    void generateInsights_zeroActivities_returnsEmpty() {
        CarbonAnalyticsResponse analytics = analytics(0, List.of(), List.of(), List.of());
        Optional<CarbonInsightResponse> result = service.generateInsights(
                Optional.of(analytics), List.of());
        assertThat(result).isEmpty();
    }

    // ── Summary ───────────────────────────────────────────────────────────

    @Test
    void generateInsights_singleActivity_summaryContainsCountAndTotal() {
        CarbonInsightResponse response = service.generateInsights(
                Optional.of(analytics(1, cats(cat("FUEL", 10, 1, 100)), trends(trend("2026-01", 10, 1)),
                        List.of(top("act-001", "FUEL", "Shell", 10)))),
                List.of()).orElseThrow();
        assertThat(response.getSummary()).contains("1 activity");
        assertThat(response.getSummary()).contains("10.00 kg CO₂e");
        assertThat(response.getSummary()).contains("FUEL");
    }

    @Test
    void generateInsights_multipleActivities_summaryContainsAverage() {
        CarbonInsightResponse response = service.generateInsights(
                Optional.of(analytics(5, cats(cat("FUEL", 50, 5, 100)), trends(trend("2026-01", 50, 5)),
                        List.of(top("act-001", "FUEL", "Shell", 50)))),
                List.of()).orElseThrow();
        assertThat(response.getSummary()).contains("5 activity/activities");
        assertThat(response.getSummary()).contains("average 10.00 kg per activity");
    }

    // ── Achievements ──────────────────────────────────────────────────────

    @Test
    void generateInsights_lowTotal_belowThresholdAchievement() {
        CarbonInsightResponse response = service.generateInsights(
                Optional.of(analytics(10, cats(cat("FUEL", 50, 10, 100)), trends(trend("2026-01", 50, 10)),
                        List.of(top("act-001", "FUEL", "Shell", 50)))),
                List.of()).orElseThrow();
        assertThat(response.getAchievements()).anyMatch(s -> s.contains("below the 100 kg threshold"));
    }

    @Test
    void generateInsights_highTotal_noBelowThresholdAchievement() {
        CarbonInsightResponse response = service.generateInsights(
                Optional.of(analytics(10, cats(cat("FUEL", 150, 10, 100)), trends(trend("2026-01", 150, 10)),
                        List.of(top("act-001", "FUEL", "Shell", 150)))),
                List.of()).orElseThrow();
        assertThat(response.getAchievements()).noneMatch(s -> s.contains("below the 100 kg threshold"));
    }

    @Test
    void generateInsights_lowAverage_achievement() {
        CarbonInsightResponse response = service.generateInsights(
                Optional.of(analytics(10, cats(cat("FUEL", 20, 10, 100)), trends(trend("2026-01", 20, 10)),
                        List.of(top("act-001", "FUEL", "Shell", 20)))),
                List.of()).orElseThrow();
        assertThat(response.getAchievements()).anyMatch(s -> s.contains("below the 5 kg benchmark"));
    }

    @Test
    void generateInsights_decliningTrend_achievement() {
        CarbonInsightResponse response = service.generateInsights(
                Optional.of(analytics(6, cats(cat("FUEL", 30, 6, 100)),
                        trends(trend("2026-01", 20, 3), trend("2026-02", 10, 3)),
                        List.of(top("act-001", "FUEL", "Shell", 20)))),
                List.of()).orElseThrow();
        assertThat(response.getAchievements()).anyMatch(s -> s.contains("declining"));
    }

    @Test
    void generateInsights_distributedCategories_achievement() {
        CarbonInsightResponse response = service.generateInsights(
                Optional.of(analytics(10,
                        cats(cat("FUEL", 40, 4, 40), cat("ELECTRICITY", 30, 3, 30), cat("TRANSPORT", 30, 3, 30)),
                        trends(trend("2026-01", 100, 10)),
                        List.of(top("act-001", "FUEL", "Shell", 40)))),
                List.of()).orElseThrow();
        assertThat(response.getAchievements()).anyMatch(s -> s.contains("well-distributed"));
    }

    // ── Warnings ──────────────────────────────────────────────────────────

    @Test
    void generateInsights_highTotal_warning() {
        CarbonInsightResponse response = service.generateInsights(
                Optional.of(analytics(10, cats(cat("FUEL", 150, 10, 100)), trends(trend("2026-01", 150, 10)),
                        List.of(top("act-001", "FUEL", "Shell", 150)))),
                List.of()).orElseThrow();
        assertThat(response.getWarnings()).anyMatch(s -> s.contains("exceed the 100 kg threshold"));
    }

    @Test
    void generateInsights_concentratedCategory_warning() {
        CarbonInsightResponse response = service.generateInsights(
                Optional.of(analytics(10,
                        cats(cat("FUEL", 80, 8, 80), cat("ELECTRICITY", 20, 2, 20)),
                        trends(trend("2026-01", 100, 10)),
                        List.of(top("act-001", "FUEL", "Shell", 80)))),
                List.of()).orElseThrow();
        assertThat(response.getWarnings()).anyMatch(s -> s.contains("accounts for 80.0%"));
    }

    @Test
    void generateInsights_increasingTrend_warning() {
        CarbonInsightResponse response = service.generateInsights(
                Optional.of(analytics(10, cats(cat("FUEL", 100, 10, 100)),
                        trends(trend("2026-01", 20, 3), trend("2026-02", 80, 7)),
                        List.of(top("act-001", "FUEL", "Shell", 80)))),
                List.of()).orElseThrow();
        assertThat(response.getWarnings()).anyMatch(s -> s.contains("increasing"));
    }

    @Test
    void generateInsights_largeSingleActivity_warning() {
        CarbonInsightResponse response = service.generateInsights(
                Optional.of(analytics(10, cats(cat("FUEL", 100, 10, 100)), trends(trend("2026-01", 100, 10)),
                        List.of(top("act-001", "FUEL", "Shell", 50)))),
                List.of()).orElseThrow();
        assertThat(response.getWarnings()).anyMatch(s -> s.contains("accounts for 50.0%"));
    }

    @Test
    void generateInsights_smallSingleActivity_noLargeWarning() {
        CarbonInsightResponse response = service.generateInsights(
                Optional.of(analytics(10, cats(cat("FUEL", 100, 10, 100)), trends(trend("2026-01", 100, 10)),
                        List.of(top("act-001", "FUEL", "Shell", 20)))),
                List.of()).orElseThrow();
        assertThat(response.getWarnings()).noneMatch(s -> s.contains("Activity 'Shell'"));
    }

    // ── Recommendations ───────────────────────────────────────────────────

    @Test
    void generateInsights_fuelCategory_fuelRecommendations() {
        CarbonInsightResponse response = service.generateInsights(
                Optional.of(analytics(5, cats(cat("FUEL", 50, 5, 100)), trends(trend("2026-01", 50, 5)),
                        List.of(top("act-001", "FUEL", "Shell", 50)))),
                List.of()).orElseThrow();
        assertThat(response.getRecommendations()).anyMatch(s -> s.contains("electric or hybrid"));
        assertThat(response.getRecommendations()).anyMatch(s -> s.contains("public transport"));
    }

    @Test
    void generateInsights_electricityCategory_recommendations() {
        CarbonInsightResponse response = service.generateInsights(
                Optional.of(analytics(5, cats(cat("ELECTRICITY", 50, 5, 100)), trends(trend("2026-01", 50, 5)),
                        List.of(top("act-001", "ELECTRICITY", "BSES", 50)))),
                List.of()).orElseThrow();
        assertThat(response.getRecommendations()).anyMatch(s -> s.contains("renewable energy"));
        assertThat(response.getRecommendations()).anyMatch(s -> s.contains("energy-efficient"));
    }

    @Test
    void generateInsights_flightCategory_recommendations() {
        CarbonInsightResponse response = service.generateInsights(
                Optional.of(analytics(5, cats(cat("FLIGHT", 50, 5, 100)), trends(trend("2026-01", 50, 5)),
                        List.of(top("act-001", "FLIGHT", "IndiGo", 50)))),
                List.of()).orElseThrow();
        assertThat(response.getRecommendations()).anyMatch(s -> s.contains("train alternatives"));
        assertThat(response.getRecommendations()).anyMatch(s -> s.contains("Offset flight"));
    }

    @Test
    void generateInsights_transportCategory_recommendations() {
        CarbonInsightResponse response = service.generateInsights(
                Optional.of(analytics(5, cats(cat("TRANSPORT", 50, 5, 100)), trends(trend("2026-01", 50, 5)),
                        List.of(top("act-001", "TRANSPORT", "Uber", 50)))),
                List.of()).orElseThrow();
        assertThat(response.getRecommendations()).anyMatch(s -> s.contains("public transport, cycling"));
    }

    @Test
    void generateInsights_shoppingCategory_recommendations() {
        CarbonInsightResponse response = service.generateInsights(
                Optional.of(analytics(5, cats(cat("SHOPPING", 50, 5, 100)), trends(trend("2026-01", 50, 5)),
                        List.of(top("act-001", "SHOPPING", "Amazon", 50)))),
                List.of()).orElseThrow();
        assertThat(response.getRecommendations()).anyMatch(s -> s.contains("locally produced"));
    }

    @Test
    void generateInsights_foodCategory_recommendations() {
        CarbonInsightResponse response = service.generateInsights(
                Optional.of(analytics(5, cats(cat("FOOD", 50, 5, 100)), trends(trend("2026-01", 50, 5)),
                        List.of(top("act-001", "FOOD", "Zomato", 50)))),
                List.of()).orElseThrow();
        assertThat(response.getRecommendations()).anyMatch(s -> s.contains("plant-based"));
    }

    @Test
    void generateInsights_gasCategory_recommendations() {
        CarbonInsightResponse response = service.generateInsights(
                Optional.of(analytics(5, cats(cat("GAS", 50, 5, 100)), trends(trend("2026-01", 50, 5)),
                        List.of(top("act-001", "GAS", "Indane", 50)))),
                List.of()).orElseThrow();
        assertThat(response.getRecommendations()).anyMatch(s -> s.contains("home insulation"));
    }

    @Test
    void generateInsights_otherCategory_genericRecommendation() {
        CarbonInsightResponse response = service.generateInsights(
                Optional.of(analytics(5, cats(cat("OTHER", 50, 5, 100)), trends(trend("2026-01", 50, 5)),
                        List.of(top("act-001", "OTHER", "Unknown", 50)))),
                List.of()).orElseThrow();
        assertThat(response.getRecommendations()).anyMatch(s -> s.contains("Review this category"));
    }

    @Test
    void generateInsights_highAverage_consolidationRecommendation() {
        CarbonInsightResponse response = service.generateInsights(
                Optional.of(analytics(5, cats(cat("FUEL", 100, 5, 100)), trends(trend("2026-01", 100, 5)),
                        List.of(top("act-001", "FUEL", "Shell", 100)))),
                List.of()).orElseThrow();
        assertThat(response.getRecommendations()).anyMatch(s -> s.contains("consolidating activities"));
    }

    @Test
    void generateInsights_increasingTrend_reviewRecommendation() {
        CarbonInsightResponse response = service.generateInsights(
                Optional.of(analytics(10, cats(cat("FUEL", 100, 10, 100)),
                        trends(trend("2026-01", 20, 3), trend("2026-02", 80, 7)),
                        List.of(top("act-001", "FUEL", "Shell", 80)))),
                List.of()).orElseThrow();
        assertThat(response.getRecommendations()).anyMatch(s -> s.contains("Review your recent lifestyle"));
    }

    // ── Insights ──────────────────────────────────────────────────────────

    @Test
    void generateInsights_insightsContainTotalAndAverage() {
        CarbonInsightResponse response = service.generateInsights(
                Optional.of(analytics(6, cats(cat("FUEL", 60, 6, 100)), trends(trend("2026-01", 60, 6)),
                        List.of(top("act-001", "FUEL", "Shell", 60)))),
                List.of()).orElseThrow();
        assertThat(response.getInsights()).anyMatch(s -> s.contains("60.00 kg CO₂e"));
        assertThat(response.getInsights()).anyMatch(s -> s.contains("6 activities"));
        assertThat(response.getInsights()).anyMatch(s -> s.contains("average 10.00 kg/activity"));
    }

    @Test
    void generateInsights_insightsContainHighestCategory() {
        CarbonInsightResponse response = service.generateInsights(
                Optional.of(analytics(10,
                        cats(cat("FUEL", 60, 6, 60), cat("ELECTRICITY", 40, 4, 40)),
                        trends(trend("2026-01", 100, 10)),
                        List.of(top("act-001", "FUEL", "Shell", 60)))),
                List.of()).orElseThrow();
        assertThat(response.getInsights()).anyMatch(s -> s.contains("Highest emission category: FUEL"));
    }

    @Test
    void generateInsights_insightsContainLowestCategory() {
        CarbonInsightResponse response = service.generateInsights(
                Optional.of(analytics(10,
                        cats(cat("FUEL", 60, 6, 60), cat("ELECTRICITY", 40, 4, 40)),
                        trends(trend("2026-01", 100, 10)),
                        List.of(top("act-001", "FUEL", "Shell", 60)))),
                List.of()).orElseThrow();
        assertThat(response.getInsights()).anyMatch(s -> s.contains("Lowest emission category: ELECTRICITY"));
    }

    @Test
    void generateInsights_insightsContainLargestActivity() {
        CarbonInsightResponse response = service.generateInsights(
                Optional.of(analytics(10, cats(cat("FUEL", 100, 10, 100)), trends(trend("2026-01", 100, 10)),
                        List.of(top("act-001", "FUEL", "Shell", 50)))),
                List.of()).orElseThrow();
        assertThat(response.getInsights()).anyMatch(s -> s.contains("Largest single activity: Shell"));
        assertThat(response.getInsights()).anyMatch(s -> s.contains("50.00 kg CO₂e"));
    }

    @Test
    void generateInsights_insightsContainMonthlyTrend() {
        CarbonInsightResponse response = service.generateInsights(
                Optional.of(analytics(10, cats(cat("FUEL", 100, 10, 100)),
                        trends(trend("2026-01", 30, 4), trend("2026-02", 70, 6)),
                        List.of(top("act-001", "FUEL", "Shell", 70)))),
                List.of()).orElseThrow();
        assertThat(response.getInsights()).anyMatch(s -> s.contains("Monthly trend"));
        assertThat(response.getInsights()).anyMatch(s -> s.contains("increased"));
    }

    @Test
    void generateInsights_insightsContainCategoryCount() {
        CarbonInsightResponse response = service.generateInsights(
                Optional.of(analytics(10,
                        cats(cat("FUEL", 50, 5, 50), cat("ELECTRICITY", 30, 3, 30), cat("TRANSPORT", 20, 2, 20)),
                        trends(trend("2026-01", 100, 10)),
                        List.of(top("act-001", "FUEL", "Shell", 50)))),
                List.of()).orElseThrow();
        assertThat(response.getInsights()).anyMatch(s -> s.contains("3 distinct categories"));
    }

    // ── Edge cases ────────────────────────────────────────────────────────

    @Test
    void generateInsights_singleCategory_noLowestInsight() {
        CarbonInsightResponse response = service.generateInsights(
                Optional.of(analytics(5, cats(cat("FUEL", 50, 5, 100)), trends(trend("2026-01", 50, 5)),
                        List.of(top("act-001", "FUEL", "Shell", 50)))),
                List.of()).orElseThrow();
        assertThat(response.getInsights()).noneMatch(s -> s.contains("Lowest emission category"));
    }

    @Test
    void generateInsights_singleMonth_noTrendInsight() {
        CarbonInsightResponse response = service.generateInsights(
                Optional.of(analytics(5, cats(cat("FUEL", 50, 5, 100)), trends(trend("2026-01", 50, 5)),
                        List.of(top("act-001", "FUEL", "Shell", 50)))),
                List.of()).orElseThrow();
        assertThat(response.getInsights()).noneMatch(s -> s.contains("Monthly trend"));
        assertThat(response.getWarnings()).noneMatch(s -> s.contains("increasing"));
        assertThat(response.getAchievements()).noneMatch(s -> s.contains("declining"));
    }

    @Test
    void generateInsights_decliningTrend_noIncreasingWarning() {
        CarbonInsightResponse response = service.generateInsights(
                Optional.of(analytics(6, cats(cat("FUEL", 30, 6, 100)),
                        trends(trend("2026-01", 20, 3), trend("2026-02", 10, 3)),
                        List.of(top("act-001", "FUEL", "Shell", 20)))),
                List.of()).orElseThrow();
        assertThat(response.getWarnings()).noneMatch(s -> s.contains("increasing"));
    }

    @Test
    void generateInsights_zeroBaseMonth_firstMonthDescription() {
        CarbonInsightResponse response = service.generateInsights(
                Optional.of(analytics(5, cats(cat("FUEL", 50, 5, 100)),
                        trends(trend("2026-01", 0, 0), trend("2026-02", 50, 5)),
                        List.of(top("act-001", "FUEL", "Shell", 50)))),
                List.of()).orElseThrow();
        assertThat(response.getInsights()).anyMatch(s -> s.contains("first month with data"));
    }

    @Test
    void generateInsights_allListsNonNull() {
        CarbonInsightResponse response = service.generateInsights(
                Optional.of(analytics(1, cats(cat("FUEL", 10, 1, 100)), trends(trend("2026-01", 10, 1)),
                        List.of(top("act-001", "FUEL", "Shell", 10)))),
                List.of()).orElseThrow();
        assertThat(response.getSummary()).isNotNull();
        assertThat(response.getAchievements()).isNotNull();
        assertThat(response.getWarnings()).isNotNull();
        assertThat(response.getRecommendations()).isNotNull();
        assertThat(response.getInsights()).isNotNull();
    }

    @Test
    void generateInsights_nullMerchant_usesActivityId() {
        CarbonInsightResponse response = service.generateInsights(
                Optional.of(analytics(1, cats(cat("FUEL", 10, 1, 100)), trends(trend("2026-01", 10, 1)),
                        List.of(top("act-001", "FUEL", null, 10)))),
                List.of()).orElseThrow();
        assertThat(response.getInsights()).anyMatch(s -> s.contains("act-001"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private CarbonAnalyticsResponse analytics(
            int activityCount,
            List<CategoryEmissionSummary> categoryTotals,
            List<MonthlyEmissionTrend> monthlyTrend,
            List<TopEmissionActivity> topActivities) {

        BigDecimal total = categoryTotals.stream()
                .map(CategoryEmissionSummary::getCarbonKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CarbonAnalyticsResponse.builder()
                .totalCarbonKg(total)
                .activityCount(activityCount)
                .categoryTotals(categoryTotals)
                .monthlyTrend(monthlyTrend)
                .topActivities(topActivities)
                .averageDailyKg(BigDecimal.ZERO)
                .build();
    }

    private List<CategoryEmissionSummary> cats(CategoryEmissionSummary... items) {
        return List.of(items);
    }

    private CategoryEmissionSummary cat(String category, int carbonKg, int count, int pctOfTotal) {
        return CategoryEmissionSummary.builder()
                .category(category)
                .carbonKg(BigDecimal.valueOf(carbonKg))
                .activityCount(count)
                .percentageOfTotal(BigDecimal.valueOf(pctOfTotal))
                .build();
    }

    private List<MonthlyEmissionTrend> trends(MonthlyEmissionTrend... items) {
        return List.of(items);
    }

    private MonthlyEmissionTrend trend(String month, int carbonKg, int count) {
        return MonthlyEmissionTrend.builder()
                .month(month)
                .carbonKg(BigDecimal.valueOf(carbonKg))
                .activityCount(count)
                .build();
    }

    private TopEmissionActivity top(String id, String category, String merchant, int carbonKg) {
        return TopEmissionActivity.builder()
                .activityId(id)
                .category(category)
                .merchant(merchant)
                .carbonKg(BigDecimal.valueOf(carbonKg))
                .methodology("Tier 1")
                .occurredAt(Instant.parse("2026-01-15T10:00:00Z"))
                .build();
    }
}
