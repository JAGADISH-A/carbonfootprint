package com.carbonfootprint.platform.carbon.analytics.service;

import com.carbonfootprint.platform.carbon.analytics.model.CarbonAnalyticsResponse;
import com.carbonfootprint.platform.carbon.analytics.model.CarbonInsightResponse;
import com.carbonfootprint.platform.carbon.analytics.model.CategoryEmissionSummary;
import com.carbonfootprint.platform.carbon.analytics.model.MonthlyEmissionTrend;
import com.carbonfootprint.platform.carbon.analytics.model.TopEmissionActivity;
import com.carbonfootprint.platform.carbon.port.in.CarbonAnalyticsUseCase;
import com.carbonfootprint.platform.carbon.port.in.CarbonInsightUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Deterministic insight generation for carbon emission data.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Delegates to {@link CarbonAnalyticsUseCase} for aggregation (no duplication)</li>
 *   <li>Analyses {@link CarbonAnalyticsResponse} to produce human-readable insights</li>
 *   <li>Identifies achievements, warnings, and recommendations</li>
 *   <li>Generates a concise summary of the user's carbon footprint</li>
 * </ul>
 *
 * <h3>Design</h3>
 * Fully deterministic — no external service calls (Gemini integration deferred).
 * Reuses the existing analytics pipeline via {@link CarbonAnalyticsUseCase}.
 *
 * @see CarbonInsightUseCase
 * @see CarbonAnalyticsUseCase
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CarbonInsightService implements CarbonInsightUseCase {

    private static final BigDecimal HIGH_EMISSION_THRESHOLD_KG = BigDecimal.valueOf(100);
    private static final BigDecimal LOW_EMISSION_PER_ACTIVITY_KG = BigDecimal.valueOf(5);
    private static final int HIGH_CONCENTRATION_PERCENTAGE = 60;
    private static final int TREND_SIGNIFICANT_INCREASE_PERCENTAGE = 20;

    private final CarbonAnalyticsUseCase carbonAnalyticsUseCase;

    @Override
    public Optional<CarbonInsightResponse> generateInsights(String userId) {
        Optional<CarbonAnalyticsResponse> analyticsResponse = carbonAnalyticsUseCase.getAnalytics(userId);
        return buildInsightsFromAnalytics(analyticsResponse);
    }

    @Override
    public Optional<CarbonInsightResponse> generateInsights(String userId, Instant from, Instant to) {
        Optional<CarbonAnalyticsResponse> analyticsResponse = carbonAnalyticsUseCase.getAnalytics(userId, from, to);
        return buildInsightsFromAnalytics(analyticsResponse);
    }

    private Optional<CarbonInsightResponse> buildInsightsFromAnalytics(
            Optional<CarbonAnalyticsResponse> analyticsResponse) {

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
        List<String> recommendations = buildRecommendations(analytics);
        List<String> insights = buildInsights(analytics);

        CarbonInsightResponse response = CarbonInsightResponse.builder()
                .analytics(analytics)
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
        String highestCategoryName = highest != null
                ? formatCategoryName(highest.getCategory())
                : null;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("I looked at your %d activit%s ", count, count == 1 ? "y" : "ies"));
        sb.append(String.format("and your total footprint is %.1f kg CO\u2082e. ", totalKg));

        if (highestCategoryName != null) {
            sb.append(String.format("Most of your emissions came from %s, ", highestCategoryName));
            sb.append(String.format("which makes up %s%% of your total. ", formatPct(highest.getPercentageOfTotal())));
        }

        sb.append(String.format("On average, each activity produces %.1f kg CO\u2082e.", avgPerActivity));

        return sb.toString();
    }

    // ── Achievements ──────────────────────────────────────────────────────

    private List<String> buildAchievements(CarbonAnalyticsResponse analytics) {
        List<String> achievements = new ArrayList<>();

        BigDecimal totalKg = analytics.getTotalCarbonKg();
        if (totalKg == null) return achievements;

        if (totalKg.compareTo(HIGH_EMISSION_THRESHOLD_KG) < 0) {
            achievements.add(String.format(
                    "Nice work — your total of %.1f kg CO\u2082e is well below the 100 kg threshold. You're keeping your footprint low!",
                    analytics.getTotalCarbonKg()));
        }

        BigDecimal avgPerActivity = safeDivide(analytics.getTotalCarbonKg(), analytics.getActivityCount());
        if (avgPerActivity.compareTo(LOW_EMISSION_PER_ACTIVITY_KG) < 0) {
            achievements.add(String.format(
                    "Your average of %.1f kg per activity is under 5 kg — that's a sign you're making thoughtful choices.",
                    avgPerActivity));
        }

        MonthlyEmissionTrend trend = monthlyTrendDirection(analytics);
        if (trend != null && isDecliningTrend(analytics)) {
            achievements.add("I can see your emissions going down over the past few months — you're on a great path.");
        }

        CategoryEmissionSummary highest = firstCategory(analytics);
        if (highest != null && highest.getPercentageOfTotal() != null
                && highest.getPercentageOfTotal().intValue() < HIGH_CONCENTRATION_PERCENTAGE) {
            achievements.add("Your emissions are spread across different areas rather than concentrated in one — that shows balanced habits.");
        }

        return achievements;
    }

    // ── Warnings ──────────────────────────────────────────────────────────

    private List<String> buildWarnings(CarbonAnalyticsResponse analytics) {
        List<String> warnings = new ArrayList<>();

        BigDecimal totalKg = analytics.getTotalCarbonKg();
        if (totalKg == null) return warnings;

        if (totalKg.compareTo(HIGH_EMISSION_THRESHOLD_KG) >= 0) {
            warnings.add(String.format(
                    "Your total of %.1f kg CO\u2082e is above 100 kg — there's room to bring that down.",
                    analytics.getTotalCarbonKg()));
        }

        CategoryEmissionSummary highest = firstCategory(analytics);
        if (highest != null && highest.getPercentageOfTotal() != null
                && highest.getPercentageOfTotal().intValue() >= HIGH_CONCENTRATION_PERCENTAGE) {
            warnings.add(String.format(
                    "I noticed that %s makes up %s%% of your emissions — that's quite concentrated. "
                            + "One small change in this area could make a big difference.",
                    formatCategoryName(highest.getCategory()), formatPct(highest.getPercentageOfTotal())));
        }

        if (isIncreasingTrend(analytics)) {
            warnings.add("Your emissions have been creeping up over the past few months — worth keeping an eye on.");
        }

        TopEmissionActivity largest = firstTopActivity(analytics);
        if (largest != null) {
            BigDecimal activityPct = safePercentage(largest.getCarbonKg(), analytics.getTotalCarbonKg());
            if (activityPct.intValue() >= 30) {
                String merchantName = largest.getMerchant() != null ? largest.getMerchant() : "a " + formatCategoryName(largest.getCategory()) + " activity";
                warnings.add(String.format(
                        "One activity — %s — accounts for %s%% of your total. "
                                + "If that's a regular expense, even a small reduction would help.",
                        merchantName, formatPct(activityPct)));
            }
        }

        return warnings;
    }

    // ── Recommendations ───────────────────────────────────────────────────

    private List<String> buildRecommendations(CarbonAnalyticsResponse analytics) {
        List<String> recommendations = new ArrayList<>();

        CategoryEmissionSummary highest = firstCategory(analytics);
        if (highest != null) {
            recommendations.addAll(conversationalRecommendations(highest.getCategory()));
        }

        BigDecimal avgPerActivity = safeDivide(analytics.getTotalCarbonKg(), analytics.getActivityCount());
        if (avgPerActivity.compareTo(LOW_EMISSION_PER_ACTIVITY_KG) >= 0) {
            recommendations.add("Try combining a few smaller errands into one trip — it reduces the per-activity impact.");
        }

        if (isIncreasingTrend(analytics)) {
            recommendations.add("Take a quick look at what changed recently — sometimes small habit shifts add up.");
        }

        return recommendations;
    }

    // ── Insights ──────────────────────────────────────────────────────────

    private List<String> buildInsights(CarbonAnalyticsResponse analytics) {
        List<String> insights = new ArrayList<>();

        BigDecimal avgPerActivity = safeDivide(analytics.getTotalCarbonKg(), analytics.getActivityCount());
        insights.add(String.format(
                "You've logged %d activit%s totalling %.1f kg CO\u2082e, averaging %.1f kg each.",
                analytics.getActivityCount(),
                analytics.getActivityCount() == 1 ? "y" : "ies",
                analytics.getTotalCarbonKg(), avgPerActivity));

        CategoryEmissionSummary highest = firstCategory(analytics);
        if (highest != null) {
            insights.add(String.format(
                    "Your biggest source is %s at %.1f kg (%s%% of your total).",
                    formatCategoryName(highest.getCategory()), highest.getCarbonKg(), formatPct(highest.getPercentageOfTotal())));
        }

        CategoryEmissionSummary lowest = lastCategory(analytics);
        if (lowest != null && analytics.getCategoryTotals().size() > 1) {
            insights.add(String.format(
                    "Your lowest is %s at just %.1f kg — that's the one you're doing best on.",
                    formatCategoryName(lowest.getCategory()), lowest.getCarbonKg()));
        }

        TopEmissionActivity largest = firstTopActivity(analytics);
        if (largest != null) {
            String merchantLabel = largest.getMerchant() != null ? largest.getMerchant() : formatCategoryName(largest.getCategory());
            insights.add(String.format(
                    "Your highest single activity is %s (%s) at %.1f kg CO\u2082e.",
                    merchantLabel, formatCategoryName(largest.getCategory()), largest.getCarbonKg()));
        }

        String trendDesc = describeTrend(analytics);
        if (trendDesc != null) {
            insights.add(trendDesc);
        }

        return insights;
    }

    // ── Trend helpers ─────────────────────────────────────────────────────

    private boolean isDecliningTrend(CarbonAnalyticsResponse analytics) {
        List<MonthlyEmissionTrend> trend = analytics.getMonthlyTrend();
        if (trend == null || trend.size() < 2) return false;

        MonthlyESecondLastAndLast two = lastTwoMonths(trend);
        if (two == null || two.last.getCarbonKg() == null || two.secondLast.getCarbonKg() == null) return false;

        return two.last.getCarbonKg().compareTo(two.secondLast.getCarbonKg()) < 0;
    }

    private boolean isIncreasingTrend(CarbonAnalyticsResponse analytics) {
        List<MonthlyEmissionTrend> trend = analytics.getMonthlyTrend();
        if (trend == null || trend.size() < 2) return false;

        MonthlyESecondLastAndLast two = lastTwoMonths(trend);
        if (two == null || two.last.getCarbonKg() == null || two.secondLast.getCarbonKg() == null) return false;

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
            return String.format("This is your first month with data (%s at %.1f kg) — I'll have more to compare next time.",
                    two.last.getMonth(), two.last.getCarbonKg());
        }

        BigDecimal changePct = two.last.getCarbonKg()
                .subtract(two.secondLast.getCarbonKg())
                .multiply(BigDecimal.valueOf(100))
                .divide(two.secondLast.getCarbonKg(), 1, RoundingMode.HALF_UP);

        if (changePct.compareTo(BigDecimal.ZERO) > 0) {
            return String.format("Your emissions went up by %s%% from %s to %s — worth seeing what changed.",
                    formatPct(changePct.abs()), two.secondLast.getMonth(), two.last.getMonth());
        } else {
            return String.format("Nice — your emissions dropped by %s%% from %s to %s. You're heading in the right direction.",
                    formatPct(changePct.abs()), two.secondLast.getMonth(), two.last.getMonth());
        }
    }

    // ── Category recommendations ──────────────────────────────────────────

    private List<String> conversationalRecommendations(String category) {
        List<String> recs = new ArrayList<>();
        switch (category) {
            case "FUEL" -> {
                recs.add("If you can, try combining errands into one trip — it cuts fuel use more than you'd think.");
                recs.add("Even one bus or metro ride instead of driving makes a real difference over time.");
            }
            case "ELECTRICITY" -> {
                recs.add("Switching to LED bulbs is one of the easiest wins — they use 75% less energy.");
                recs.add("Unplugging devices when they're not in use can quietly shave off emissions.");
            }
            case "FLIGHT" -> {
                recs.add("For shorter distances, trains are often just as fast and way lower in emissions.");
                recs.add("If you do fly, choosing economy class actually has a smaller footprint per person.");
            }
            case "TRANSPORT" -> {
                recs.add("Walking or cycling for even one trip a week adds up to meaningful savings.");
                recs.add("Carpooling is a great way to halve your commute emissions without changing much.");
            }
            case "SHOPPING" -> {
                recs.add("Buying local products cuts down on shipping emissions — and often supports local businesses.");
                recs.add("Next time you shop, try picking something with less packaging — it helps more than you'd expect.");
            }
            case "FOOD" -> {
                recs.add("Even swapping one meat meal for a plant-based option each week makes a measurable difference.");
                recs.add("Buying seasonal produce is not just greener — it's often fresher and tastier too.");
            }
            case "GAS" -> {
                recs.add("A quick check on your home insulation can significantly reduce gas usage.");
                recs.add("If you're considering upgrades, heat pumps are becoming a great alternative to gas.");
            }
            default -> recs.add("Take a look at this category — there might be a simple swap that cuts your footprint.");
        }
        return recs;
    }

    private String formatCategoryName(String category) {
        if (category == null) return "other";
        return category.charAt(0) + category.substring(1).toLowerCase();
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
