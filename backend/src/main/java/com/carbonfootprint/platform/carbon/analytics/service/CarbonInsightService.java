package com.carbonfootprint.platform.carbon.analytics.service;

import com.carbonfootprint.platform.activity.model.Activity;
import com.carbonfootprint.platform.carbon.analytics.model.CarbonAnalyticsResponse;
import com.carbonfootprint.platform.carbon.analytics.model.CarbonInsightResponse;
import com.carbonfootprint.platform.carbon.analytics.model.CategoryEmissionSummary;
import com.carbonfootprint.platform.carbon.analytics.model.MonthlyEmissionTrend;
import com.carbonfootprint.platform.carbon.analytics.model.TopEmissionActivity;
import com.carbonfootprint.platform.carbon.port.in.CarbonInsightUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Deterministic insight generation for carbon emission data.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Analyses {@link CarbonAnalyticsResponse} to produce human-readable insights</li>
 *   <li>Identifies achievements, warnings, and recommendations</li>
 *   <li>Generates a concise summary of the user's carbon footprint</li>
 * </ul>
 *
 * <h3>Design</h3>
 * Fully deterministic — no external service calls (Gemini integration deferred).
 * All logic is pure computation over the input data.
 *
 * @see CarbonInsightUseCase
 * @see CarbonAnalyticsResponse
 */
@Slf4j
@Service
public class CarbonInsightService implements CarbonInsightUseCase {

    private static final BigDecimal HIGH_EMISSION_THRESHOLD_KG = BigDecimal.valueOf(100);
    private static final BigDecimal LOW_EMISSION_PER_ACTIVITY_KG = BigDecimal.valueOf(5);
    private static final int HIGH_CONCENTRATION_PERCENTAGE = 60;
    private static final int TREND_SIGNIFICANT_INCREASE_PERCENTAGE = 20;

    @Override
    public Optional<CarbonInsightResponse> generateInsights(
            Optional<CarbonAnalyticsResponse> analyticsResponse,
            List<Activity> recentActivities) {

        if (analyticsResponse.isEmpty()) {
            log.debug("CarbonInsightService — no analytics data, skipping insight generation");
            return Optional.empty();
        }

        CarbonAnalyticsResponse analytics = analyticsResponse.get();

        if (analytics.getActivityCount() == 0) {
            log.debug("CarbonInsightService — zero activities, skipping insight generation");
            return Optional.empty();
        }

        String summary = buildSummary(analytics);
        List<String> achievements = buildAchievements(analytics);
        List<String> warnings = buildWarnings(analytics);
        List<String> recommendations = buildRecommendations(analytics, recentActivities);
        List<String> insights = buildInsights(analytics);

        CarbonInsightResponse response = CarbonInsightResponse.builder()
                .summary(summary)
                .achievements(achievements)
                .warnings(warnings)
                .recommendations(recommendations)
                .insights(insights)
                .build();

        log.debug("CarbonInsightService — generated insights: achievements={} warnings={} recommendations={} insights={}",
                achievements.size(), warnings.size(), recommendations.size(), insights.size());

        return Optional.of(response);
    }

    // ── Summary ───────────────────────────────────────────────────────────

    private String buildSummary(CarbonAnalyticsResponse analytics) {
        BigDecimal totalKg = analytics.getTotalCarbonKg();
        int count = analytics.getActivityCount();
        BigDecimal avgPerActivity = safeDivide(totalKg, count);

        CategoryEmissionSummary highest = firstCategory(analytics);
        String highestCategoryText = highest != null
                ? highest.getCategory() + " (" + formatPct(highest.getPercentageOfTotal()) + "%)"
                : "N/A";

        return String.format(
                "You have logged %d activity/activities with a total carbon footprint of %.2f kg CO₂e "
                        + "(average %.2f kg per activity). Your highest emission category is %s.",
                count,
                totalKg,
                avgPerActivity,
                highestCategoryText);
    }

    // ── Achievements ──────────────────────────────────────────────────────

    private List<String> buildAchievements(CarbonAnalyticsResponse analytics) {
        List<String> achievements = new ArrayList<>();

        // Check if total emissions are below threshold
        if (analytics.getTotalCarbonKg().compareTo(HIGH_EMISSION_THRESHOLD_KG) < 0) {
            achievements.add(String.format(
                    "Your total emissions of %.2f kg CO₂e are below the %.0f kg threshold — great job keeping your footprint low!",
                    analytics.getTotalCarbonKg(), HIGH_EMISSION_THRESHOLD_KG));
        }

        // Check if average per activity is low
        BigDecimal avgPerActivity = safeDivide(analytics.getTotalCarbonKg(), analytics.getActivityCount());
        if (avgPerActivity.compareTo(LOW_EMISSION_PER_ACTIVITY_KG) < 0) {
            achievements.add(String.format(
                    "Your average emission per activity is %.2f kg CO₂e, which is below the %.0f kg benchmark.",
                    avgPerActivity, LOW_EMISSION_PER_ACTIVITY_KG));
        }

        // Check for declining monthly trend
        MonthlyEmissionTrend trend = monthlyTrendDirection(analytics);
        if (trend != null && isDecliningTrend(analytics)) {
            achievements.add("Your emissions have been declining over recent months — keep it up!");
        }

        // Check if emissions are well-distributed (no single category dominates)
        CategoryEmissionSummary highest = firstCategory(analytics);
        if (highest != null && highest.getPercentageOfTotal() != null
                && highest.getPercentageOfTotal().intValue() < HIGH_CONCENTRATION_PERCENTAGE) {
            achievements.add("Your emissions are well-distributed across multiple categories.");
        }

        return achievements;
    }

    // ── Warnings ──────────────────────────────────────────────────────────

    private List<String> buildWarnings(CarbonAnalyticsResponse analytics) {
        List<String> warnings = new ArrayList<>();

        // High total emissions
        if (analytics.getTotalCarbonKg().compareTo(HIGH_EMISSION_THRESHOLD_KG) >= 0) {
            warnings.add(String.format(
                    "Your total emissions of %.2f kg CO₂e exceed the %.0f kg threshold.",
                    analytics.getTotalCarbonKg(), HIGH_EMISSION_THRESHOLD_KG));
        }

        // Highest emission category concentration
        CategoryEmissionSummary highest = firstCategory(analytics);
        if (highest != null && highest.getPercentageOfTotal() != null
                && highest.getPercentageOfTotal().intValue() >= HIGH_CONCENTRATION_PERCENTAGE) {
            warnings.add(String.format(
                    "Category '%s' accounts for %s%% of your total emissions — this is highly concentrated.",
                    highest.getCategory(), formatPct(highest.getPercentageOfTotal())));
        }

        // Increasing monthly trend
        if (isIncreasingTrend(analytics)) {
            warnings.add("Your monthly emissions have been increasing over recent months.");
        }

        // Large single activity
        TopEmissionActivity largest = firstTopActivity(analytics);
        if (largest != null) {
            BigDecimal activityPct = safePercentage(largest.getCarbonKg(), analytics.getTotalCarbonKg());
            if (activityPct.intValue() >= 30) {
                warnings.add(String.format(
                        "Activity '%s' (%s) accounts for %s%% of your total emissions.",
                        largest.getMerchant() != null ? largest.getMerchant() : largest.getActivityId(),
                        largest.getCategory(),
                        formatPct(activityPct)));
            }
        }

        return warnings;
    }

    // ── Recommendations ───────────────────────────────────────────────────

    private List<String> buildRecommendations(CarbonAnalyticsResponse analytics, List<Activity> recentActivities) {
        List<String> recommendations = new ArrayList<>();

        CategoryEmissionSummary highest = firstCategory(analytics);
        if (highest != null) {
            recommendations.addAll(recommendationsForCategory(highest.getCategory()));
        }

        // If average is high, suggest reducing frequency
        BigDecimal avgPerActivity = safeDivide(analytics.getTotalCarbonKg(), analytics.getActivityCount());
        if (avgPerActivity.compareTo(LOW_EMISSION_PER_ACTIVITY_KG) >= 0) {
            recommendations.add("Consider consolidating activities to reduce per-transaction emissions.");
        }

        // If trend is increasing, suggest reviewing habits
        if (isIncreasingTrend(analytics)) {
            recommendations.add("Review your recent lifestyle changes — your emissions are trending upward.");
        }

        return recommendations;
    }

    // ── Insights ──────────────────────────────────────────────────────────

    private List<String> buildInsights(CarbonAnalyticsResponse analytics) {
        List<String> insights = new ArrayList<>();

        // Total and average
        BigDecimal avgPerActivity = safeDivide(analytics.getTotalCarbonKg(), analytics.getActivityCount());
        insights.add(String.format(
                "Total emissions: %.2f kg CO₂e across %d activities (average %.2f kg/activity).",
                analytics.getTotalCarbonKg(), analytics.getActivityCount(), avgPerActivity));

        // Highest category
        CategoryEmissionSummary highest = firstCategory(analytics);
        if (highest != null) {
            insights.add(String.format(
                    "Highest emission category: %s at %.2f kg CO₂e (%s%% of total).",
                    highest.getCategory(), highest.getCarbonKg(), formatPct(highest.getPercentageOfTotal())));
        }

        // Lowest category
        CategoryEmissionSummary lowest = lastCategory(analytics);
        if (lowest != null && analytics.getCategoryTotals().size() > 1) {
            insights.add(String.format(
                    "Lowest emission category: %s at %.2f kg CO₂e.",
                    lowest.getCategory(), lowest.getCarbonKg()));
        }

        // Largest single activity
        TopEmissionActivity largest = firstTopActivity(analytics);
        if (largest != null) {
            insights.add(String.format(
                    "Largest single activity: %s (%s) at %.2f kg CO₂e.",
                    largest.getMerchant() != null ? largest.getMerchant() : largest.getActivityId(),
                    largest.getCategory(),
                    largest.getCarbonKg()));
        }

        // Monthly trend direction
        String trendDesc = describeTrend(analytics);
        if (trendDesc != null) {
            insights.add(trendDesc);
        }

        // Number of categories
        if (analytics.getCategoryTotals() != null && !analytics.getCategoryTotals().isEmpty()) {
            insights.add(String.format(
                    "Emissions span %d distinct categories.",
                    analytics.getCategoryTotals().size()));
        }

        return insights;
    }

    // ── Trend helpers ─────────────────────────────────────────────────────

    private boolean isDecliningTrend(CarbonAnalyticsResponse analytics) {
        List<MonthlyEmissionTrend> trend = analytics.getMonthlyTrend();
        if (trend == null || trend.size() < 2) return false;

        MonthlyESecondLastAndLast two = lastTwoMonths(trend);
        if (two == null) return false;

        return two.last.getCarbonKg().compareTo(two.secondLast.getCarbonKg()) < 0;
    }

    private boolean isIncreasingTrend(CarbonAnalyticsResponse analytics) {
        List<MonthlyEmissionTrend> trend = analytics.getMonthlyTrend();
        if (trend == null || trend.size() < 2) return false;

        MonthlyESecondLastAndLast two = lastTwoMonths(trend);
        if (two == null) return false;

        if (two.secondLast.getCarbonKg().compareTo(BigDecimal.ZERO) == 0) return false;

        BigDecimal changePct = two.last.getCarbonKg()
                .subtract(two.secondLast.getCarbonKg())
                .multiply(BigDecimal.valueOf(100))
                .divide(two.secondLast.getCarbonKg(), 1, RoundingMode.HALF_UP);

        return changePct.intValue() >= TREND_SIGNIFICANT_INCREASE_PERCENTAGE;
    }

    private MonthlyEmissionTrend monthlyTrendDirection(CarbonAnalyticsResponse analytics) {
        List<MonthlyEmissionTrend> trend = analytics.getMonthlyTrend();
        if (trend == null || trend.isEmpty()) return null;
        return trend.get(trend.size() - 1);
    }

    private String describeTrend(CarbonAnalyticsResponse analytics) {
        List<MonthlyEmissionTrend> trend = analytics.getMonthlyTrend();
        if (trend == null || trend.size() < 2) return null;

        MonthlyESecondLastAndLast two = lastTwoMonths(trend);
        if (two == null) return null;

        if (two.secondLast.getCarbonKg().compareTo(BigDecimal.ZERO) == 0) {
            return String.format("Monthly trend: %s at %.2f kg CO₂e (first month with data).",
                    two.last.getMonth(), two.last.getCarbonKg());
        }

        BigDecimal changePct = two.last.getCarbonKg()
                .subtract(two.secondLast.getCarbonKg())
                .multiply(BigDecimal.valueOf(100))
                .divide(two.secondLast.getCarbonKg(), 1, RoundingMode.HALF_UP);

        String direction = changePct.compareTo(BigDecimal.ZERO) > 0 ? "increased" : "decreased";
        return String.format("Monthly trend: emissions %s by %s%% from %s (%.2f kg) to %s (%.2f kg).",
                direction, formatPct(changePct.abs()),
                two.secondLast.getMonth(), two.secondLast.getCarbonKg(),
                two.last.getMonth(), two.last.getCarbonKg());
    }

    // ── Category recommendations ──────────────────────────────────────────

    private List<String> recommendationsForCategory(String category) {
        List<String> recs = new ArrayList<>();
        switch (category) {
            case "FUEL" -> {
                recs.add("Consider switching to electric or hybrid vehicles to reduce fuel emissions.");
                recs.add("Combine trips and use public transport where possible.");
            }
            case "ELECTRICITY" -> {
                recs.add("Switch to renewable energy sources or green electricity tariffs.");
                recs.add("Improve home insulation and use energy-efficient appliances.");
            }
            case "FLIGHT" -> {
                recs.add("Consider train alternatives for short-haul flights.");
                recs.add("Offset flight emissions through verified carbon offset programs.");
            }
            case "TRANSPORT" -> {
                recs.add("Use public transport, cycling, or walking for daily commutes.");
                recs.add("Carpool or use ride-sharing services with electric vehicles.");
            }
            case "SHOPPING" -> {
                recs.add("Buy locally produced goods to reduce supply-chain emissions.");
                recs.add("Choose products with lower carbon footprints and minimal packaging.");
            }
            case "FOOD" -> {
                recs.add("Reduce meat and dairy consumption — plant-based diets have lower emissions.");
                recs.add("Buy seasonal and locally sourced food.");
            }
            case "GAS" -> {
                recs.add("Improve home insulation to reduce gas heating consumption.");
                recs.add("Consider heat pumps as an alternative to gas boilers.");
            }
            default -> recs.add("Review this category for opportunities to reduce emissions.");
        }
        return recs;
    }

    // ── Utility methods ───────────────────────────────────────────────────

    private CategoryEmissionSummary firstCategory(CarbonAnalyticsResponse analytics) {
        List<CategoryEmissionSummary> cats = analytics.getCategoryTotals();
        return (cats != null && !cats.isEmpty()) ? cats.get(0) : null;
    }

    private CategoryEmissionSummary lastCategory(CarbonAnalyticsResponse analytics) {
        List<CategoryEmissionSummary> cats = analytics.getCategoryTotals();
        return (cats != null && !cats.isEmpty()) ? cats.get(cats.size() - 1) : null;
    }

    private TopEmissionActivity firstTopActivity(CarbonAnalyticsResponse analytics) {
        List<TopEmissionActivity> tops = analytics.getTopActivities();
        return (tops != null && !tops.isEmpty()) ? tops.get(0) : null;
    }

    private MonthlyESecondLastAndLast lastTwoMonths(List<MonthlyEmissionTrend> trend) {
        if (trend.size() < 2) return null;
        return new MonthlyESecondLastAndLast(trend.get(trend.size() - 2), trend.get(trend.size() - 1));
    }

    private BigDecimal safeDivide(BigDecimal numerator, int denominator) {
        if (denominator == 0) return BigDecimal.ZERO;
        return numerator.divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal safePercentage(BigDecimal part, BigDecimal whole) {
        if (whole.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return part.multiply(BigDecimal.valueOf(100))
                .divide(whole, 1, RoundingMode.HALF_UP);
    }

    private String formatPct(BigDecimal pct) {
        if (pct == null) return "0";
        return pct.setScale(1, RoundingMode.HALF_UP).toString();
    }

    // ── Internal record ───────────────────────────────────────────────────

    private record MonthlyESecondLastAndLast(
            MonthlyEmissionTrend secondLast,
            MonthlyEmissionTrend last) {}
}
