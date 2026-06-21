package com.carbonfootprint.platform.carbon.chat;

import com.carbonfootprint.platform.carbon.analytics.model.CategoryEmissionSummary;
import com.carbonfootprint.platform.carbon.analytics.model.CarbonAnalyticsResponse;
import com.carbonfootprint.platform.carbon.analytics.model.CarbonInsightResponse;
import com.carbonfootprint.platform.carbon.analytics.model.MonthlyEmissionTrend;
import com.carbonfootprint.platform.carbon.analytics.model.TopEmissionActivity;
import com.carbonfootprint.platform.carbon.port.in.CarbonAnalyticsUseCase;
import com.carbonfootprint.platform.carbon.port.in.CarbonInsightUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds a structured context string from the user's real analytics data,
 * deterministic insights, and recent activities for injection into the
 * AI chat prompt.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Fetch analytics via {@code CarbonAnalyticsUseCase}</li>
 *   <li>Fetch deterministic insights via {@code CarbonInsightUseCase}</li>
 *   <li>Assemble a concise, structured context block for the LLM system prompt</li>
 * </ul>
 *
 * <h3>Context Format</h3>
 * The output is a newline-delimited key-value block designed for minimal token usage.
 */
@Slf4j
@Component
@Profile("!stub")
@RequiredArgsConstructor
public class PersonalizedContextBuilder {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    private final CarbonAnalyticsUseCase analyticsUseCase;
    private final CarbonInsightUseCase insightUseCase;

    /**
     * Build the user context block for the AI prompt.
     *
     * @param userId the user ID (typically "anonymous" for demo)
     * @return structured context string, or "No data available" placeholder
     */
    public String buildContext(String userId) {
        try {
            var analyticsOpt = analyticsUseCase.getAnalytics(userId);
            var insightsOpt = insightUseCase.generateInsights(userId);

            if (analyticsOpt.isEmpty() && insightsOpt.isEmpty()) {
                return "No personalized data available. Provide general carbon reduction advice.";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("=== USER CARBON DATA ===\n");

            if (analyticsOpt.isPresent()) {
                CarbonAnalyticsResponse analytics = analyticsOpt.get();
                // Analytics summary
                sb.append("Total emissions: ").append(analytics.getTotalCarbonKg()).append(" kg CO2e\n");
                sb.append("Average daily: ").append(analytics.getAverageDailyKg()).append(" kg CO2e\n");
                sb.append("Activity count: ").append(analytics.getActivityCount()).append("\n");

                // Period
                if (analytics.getPeriodStart() != null && analytics.getPeriodEnd() != null) {
                    sb.append("Period: ").append(DATE_FMT.format(analytics.getPeriodStart()))
                      .append(" to ").append(DATE_FMT.format(analytics.getPeriodEnd())).append("\n");
                }

                // Category breakdown
                List<CategoryEmissionSummary> categories = analytics.getCategoryTotals();
                if (categories != null && !categories.isEmpty()) {
                    sb.append("Category breakdown:\n");
                    for (CategoryEmissionSummary cat : categories) {
                        sb.append("  - ").append(cat.getCategory())
                          .append(": ").append(cat.getCarbonKg())
                          .append(" kg (").append(cat.getPercentageOfTotal()).append("%)\n");
                    }
                }

                // Top activities
                List<TopEmissionActivity> topActivities = analytics.getTopActivities();
                if (topActivities != null && !topActivities.isEmpty()) {
                    sb.append("Top emitting activities:\n");
                    for (TopEmissionActivity act : topActivities) {
                        sb.append("  - ").append(act.getCategory())
                          .append(" | ").append(act.getMerchant() != null ? act.getMerchant() : "Unknown")
                          .append(" | ").append(act.getCarbonKg()).append(" kg\n");
                    }
                }

                // Monthly trend
                List<MonthlyEmissionTrend> trend = analytics.getMonthlyTrend();
                if (trend != null && !trend.isEmpty()) {
                    sb.append("Monthly trend:\n");
                    for (MonthlyEmissionTrend month : trend) {
                        sb.append("  - ").append(month.getMonth())
                          .append(": ").append(month.getCarbonKg()).append(" kg\n");
                    }
                }
            }

            // Deterministic insights
            if (insightsOpt.isPresent()) {
                CarbonInsightResponse insights = insightsOpt.get();
                if (insights.getSummary() != null) {
                    sb.append("Insight summary: ").append(insights.getSummary()).append("\n");
                }
                if (insights.getAchievements() != null && !insights.getAchievements().isEmpty()) {
                    sb.append("Achievements: ")
                      .append(insights.getAchievements().stream()
                              .collect(Collectors.joining("; "))).append("\n");
                }
                if (insights.getWarnings() != null && !insights.getWarnings().isEmpty()) {
                    sb.append("Warnings: ")
                      .append(insights.getWarnings().stream()
                              .collect(Collectors.joining("; "))).append("\n");
                }
                if (insights.getRecommendations() != null && !insights.getRecommendations().isEmpty()) {
                    sb.append("Recommendations: ")
                      .append(insights.getRecommendations().stream()
                              .collect(Collectors.joining("; "))).append("\n");
                }
            }

            sb.append("=== END USER DATA ===");
            return sb.toString();

        } catch (Exception e) {
            log.warn("Failed to build personalized context for user {}: {}", userId, e.getMessage());
            return "No personalized data available. Provide general carbon reduction advice.";
        }
    }
}
