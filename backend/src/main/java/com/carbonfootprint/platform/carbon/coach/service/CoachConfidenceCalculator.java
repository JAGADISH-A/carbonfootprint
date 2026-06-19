package com.carbonfootprint.platform.carbon.coach.service;

import com.carbonfootprint.platform.carbon.analytics.model.CarbonAnalyticsResponse;
import com.carbonfootprint.platform.carbon.analytics.model.CarbonInsightResponse;
import com.carbonfootprint.platform.carbon.analytics.model.CategoryEmissionSummary;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Deterministic confidence score calculator for AI Coach responses.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Compute a 0–100 confidence score from measurable data signals</li>
 *   <li>No LLM involvement — purely rule-based computation</li>
 *   <li>Replace the former LLM self-estimated confidence</li>
 * </ul>
 *
 * <h3>Scoring Breakdown</h3>
 * <pre>
 * ┌─────────────────────────┬───────┬──────────────────────────────────────────┐
 * │ Signal                  │ Max   │ Rationale                                │
 * ├─────────────────────────┼───────┼──────────────────────────────────────────┤
 * │ Activity count          │ 35    │ Primary data volume measure              │
 * │ Category diversity      │ 25    │ Breadth of emission sources captured     │
 * │ Temporal coverage       │ 20    │ Months of data available                 │
 * │ Analytics completeness  │ 15    │ How many analytics fields are populated  │
 * │ Data freshness          │ 5     │ Recency of the latest activity           │
 * ├─────────────────────────┼───────┼──────────────────────────────────────────┤
 * │ Total                   │ 100   │                                          │
 * └─────────────────────────┴───────┴──────────────────────────────────────────┘
 * </pre>
 *
 * <h3>Design</h3>
 * Stateless Spring bean — no instance state, no external calls.
 * All methods are pure functions of the input data.
 *
 * @see AICarbonCoachService
 * @see CarbonAnalyticsResponse
 */
@Component
public class CoachConfidenceCalculator {

    private static final int MAX_ACTIVITY_SCORE = 35;
    private static final int MAX_CATEGORY_SCORE = 25;
    private static final int MAX_TEMPORAL_SCORE = 20;
    private static final int MAX_COMPLETENESS_SCORE = 15;
    private static final int MAX_FRESHNESS_SCORE = 5;

    /**
     * Computes a deterministic confidence score (0–100) from insight data.
     *
     * @param insight the deterministic insight response containing analytics
     * @return confidence score clamped to [0, 100]
     */
    public int calculate(CarbonInsightResponse insight) {
        if (insight == null) return 0;

        CarbonAnalyticsResponse analytics = insight.getAnalytics();
        if (analytics == null) return 0;

        int score = 0;
        score += scoreActivityCount(analytics.getActivityCount());
        score += scoreCategoryDiversity(analytics.getCategoryTotals());
        score += scoreTemporalCoverage(analytics.getMonthlyTrend());
        score += scoreAnalyticsCompleteness(analytics);
        score += scoreDataFreshness(analytics.getPeriodEnd());

        return Math.max(0, Math.min(100, score));
    }

    // ── Activity count scoring (35 max) ──────────────────────────────────

    /**
     * Scores based on raw activity count.
     * <pre>
     *   0 → 0
     *   1 → 5
     *   2–3 → 15
     *   4–9 → 25
     *   10+ → 35
     * </pre>
     */
    private int scoreActivityCount(int count) {
        if (count <= 0) return 0;
        if (count == 1) return 5;
        if (count <= 3) return 15;
        if (count <= 9) return 25;
        return MAX_ACTIVITY_SCORE;
    }

    // ── Category diversity scoring (25 max) ──────────────────────────────

    /**
     * Scores based on number of distinct emission categories.
     * <pre>
     *   0 → 0
     *   1 → 8
     *   2 → 15
     *   3–4 → 20
     *   5+ → 25
     * </pre>
     */
    private int scoreCategoryDiversity(List<CategoryEmissionSummary> categories) {
        if (categories == null || categories.isEmpty()) return 0;
        int size = categories.size();
        if (size == 1) return 8;
        if (size == 2) return 15;
        if (size <= 4) return 20;
        return MAX_CATEGORY_SCORE;
    }

    // ── Temporal coverage scoring (20 max) ───────────────────────────────

    /**
     * Scores based on number of months with data.
     * <pre>
     *   0 → 0
     *   1 → 8
     *   2 → 14
     *   3+ → 20
     * </pre>
     */
    private int scoreTemporalCoverage(List<?> monthlyTrend) {
        if (monthlyTrend == null || monthlyTrend.isEmpty()) return 0;
        int months = monthlyTrend.size();
        if (months == 1) return 8;
        if (months == 2) return 14;
        return MAX_TEMPORAL_SCORE;
    }

    // ── Analytics completeness scoring (15 max) ──────────────────────────

    /**
     * Scores based on how many analytics fields are populated.
     * Each of the 5 key fields contributes 3 points:
     * <ul>
     *   <li>totalCarbonKg (non-null, non-zero)</li>
     *   <li>averageDailyKg (non-null, non-zero)</li>
     *   <li>topActivities (non-empty list)</li>
     *   <li>categoryTotals (non-empty list)</li>
     *   <li>monthlyTrend (non-empty list)</li>
     * </ul>
     */
    private int scoreAnalyticsCompleteness(CarbonAnalyticsResponse analytics) {
        int score = 0;
        if (analytics.getTotalCarbonKg() != null && analytics.getTotalCarbonKg().signum() != 0) score += 3;
        if (analytics.getAverageDailyKg() != null && analytics.getAverageDailyKg().signum() != 0) score += 3;
        if (analytics.getTopActivities() != null && !analytics.getTopActivities().isEmpty()) score += 3;
        if (analytics.getCategoryTotals() != null && !analytics.getCategoryTotals().isEmpty()) score += 3;
        if (analytics.getMonthlyTrend() != null && !analytics.getMonthlyTrend().isEmpty()) score += 3;
        return Math.min(score, MAX_COMPLETENESS_SCORE);
    }

    // ── Data freshness scoring (5 max) ───────────────────────────────────

    /**
     * Scores based on how recently data was recorded.
     * <pre>
     *   null periodEnd → 0
     *   within 7 days → 5
     *   within 30 days → 3
     *   older → 1
     * </pre>
     */
    private int scoreDataFreshness(Instant periodEnd) {
        if (periodEnd == null) return 0;
        Duration age = Duration.between(periodEnd, Instant.now());
        if (age.toDays() <= 7) return MAX_FRESHNESS_SCORE;
        if (age.toDays() <= 30) return 3;
        return 1;
    }
}
