package com.carbonfootprint.platform.carbon.analytics.service;

import com.carbonfootprint.platform.activity.model.Activity;
import com.carbonfootprint.platform.activity.port.out.ActivityRepository;
import com.carbonfootprint.platform.carbon.analytics.model.CarbonAnalyticsResponse;
import com.carbonfootprint.platform.carbon.analytics.model.CategoryEmissionSummary;
import com.carbonfootprint.platform.carbon.analytics.model.MonthlyEmissionTrend;
import com.carbonfootprint.platform.carbon.analytics.model.TopEmissionActivity;
import com.carbonfootprint.platform.carbon.port.in.CarbonAnalyticsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Read-only aggregation service for carbon emission analytics.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Loads all calculated activities for a user from {@link ActivityRepository}</li>
 *   <li>Extracts carbon data from {@code Activity.metadata["carbonAssessment"]}</li>
 *   <li>Aggregates: total, category-wise, monthly trend, top 5, average daily</li>
 * </ul>
 *
 * <h3>Read-only guarantee</h3>
 * This service never writes to the repository. All data flows are one-way:
 * repository → service → response DTO.
 *
 * @see CarbonAnalyticsUseCase
 * @see ActivityRepository
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CarbonAnalyticsService implements CarbonAnalyticsUseCase {

    private static final String CARBON_ASSESSMENT_KEY = "carbonAssessment";
    private static final String CARBON_KG_KEY = "carbonKg";
    private static final String METHODOLOGY_KEY = "methodology";
    private static final int TOP_ACTIVITIES_LIMIT = 5;

    private final ActivityRepository activityRepository;

    @Override
    public Optional<CarbonAnalyticsResponse> getAnalytics(String userId) {
        List<Activity> activities = activityRepository.findByUserId(userId);
        return buildResponse(activities, null, null);
    }

    @Override
    public Optional<CarbonAnalyticsResponse> getAnalytics(String userId, Instant from, Instant to) {
        List<Activity> activities = activityRepository.findByUserIdAndOccurredAtBetween(userId, from, to);
        return buildResponse(activities, from, to);
    }

    // ── Private aggregation logic ──────────────────────────────────────────

    private Optional<CarbonAnalyticsResponse> buildResponse(
            List<Activity> activities, Instant from, Instant to) {

        List<Activity> assessed = activities.stream()
                .filter(this::hasCarbonAssessment)
                .toList();

        if (assessed.isEmpty()) {
            log.debug("CarbonAnalyticsService — no assessed activities found");
            return Optional.empty();
        }

        BigDecimal totalCarbonKg = assessed.stream()
                .map(this::extractCarbonKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategoryEmissionSummary> categoryTotals = buildCategoryTotals(assessed, totalCarbonKg);
        List<MonthlyEmissionTrend> monthlyTrend = buildMonthlyTrend(assessed);
        List<TopEmissionActivity> topActivities = buildTopActivities(assessed);
        BigDecimal averageDailyKg = calculateAverageDaily(assessed, from, to);

        CarbonAnalyticsResponse response = CarbonAnalyticsResponse.builder()
                .totalCarbonKg(totalCarbonKg)
                .categoryTotals(categoryTotals)
                .monthlyTrend(monthlyTrend)
                .topActivities(topActivities)
                .averageDailyKg(averageDailyKg)
                .activityCount(assessed.size())
                .periodStart(from)
                .periodEnd(to)
                .build();

        log.debug("CarbonAnalyticsService — built analytics: totalKg={} activities={} categories={}",
                totalCarbonKg, assessed.size(), categoryTotals.size());

        return Optional.of(response);
    }

    private List<CategoryEmissionSummary> buildCategoryTotals(List<Activity> assessed, BigDecimal totalCarbonKg) {
        Map<String, BigDecimal> categorySums = new LinkedHashMap<>();
        Map<String, Integer> categoryCounts = new LinkedHashMap<>();

        for (Activity activity : assessed) {
            String category = activity.getCategory() != null ? activity.getCategory().name() : "OTHER";
            BigDecimal carbonKg = extractCarbonKg(activity);

            categorySums.merge(category, carbonKg, BigDecimal::add);
            categoryCounts.merge(category, 1, Integer::sum);
        }

        List<CategoryEmissionSummary> summaries = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : categorySums.entrySet()) {
            String category = entry.getKey();
            BigDecimal carbonKg = entry.getValue();
            int count = categoryCounts.get(category);

            BigDecimal percentage = totalCarbonKg.compareTo(BigDecimal.ZERO) > 0
                    ? carbonKg.multiply(BigDecimal.valueOf(100))
                            .divide(totalCarbonKg, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            summaries.add(CategoryEmissionSummary.builder()
                    .category(category)
                    .carbonKg(carbonKg)
                    .activityCount(count)
                    .percentageOfTotal(percentage)
                    .build());
        }

        summaries.sort(Comparator.comparing(CategoryEmissionSummary::getCarbonKg).reversed());
        return summaries;
    }

    private List<MonthlyEmissionTrend> buildMonthlyTrend(List<Activity> assessed) {
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
                .withZone(ZoneOffset.UTC);

        Map<String, BigDecimal> monthSums = new LinkedHashMap<>();
        Map<String, Integer> monthCounts = new LinkedHashMap<>();

        for (Activity activity : assessed) {
            Instant occurredAt = activity.getOccurredAt();
            if (occurredAt == null) continue;

            String monthKey = monthFormatter.format(occurredAt);
            BigDecimal carbonKg = extractCarbonKg(activity);

            monthSums.merge(monthKey, carbonKg, BigDecimal::add);
            monthCounts.merge(monthKey, 1, Integer::sum);
        }

        List<MonthlyEmissionTrend> trend = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : monthSums.entrySet()) {
            trend.add(MonthlyEmissionTrend.builder()
                    .month(entry.getKey())
                    .carbonKg(entry.getValue())
                    .activityCount(monthCounts.get(entry.getKey()))
                    .build());
        }

        trend.sort(Comparator.comparing(MonthlyEmissionTrend::getMonth));
        return trend;
    }

    private List<TopEmissionActivity> buildTopActivities(List<Activity> assessed) {
        return assessed.stream()
                .sorted(Comparator.comparing(this::extractCarbonKg).reversed())
                .limit(TOP_ACTIVITIES_LIMIT)
                .map(activity -> TopEmissionActivity.builder()
                        .activityId(activity.getId())
                        .category(activity.getCategory() != null ? activity.getCategory().name() : "OTHER")
                        .merchant(activity.getMerchant())
                        .carbonKg(extractCarbonKg(activity))
                        .methodology(extractMethodology(activity))
                        .occurredAt(activity.getOccurredAt())
                        .build())
                .toList();
    }

    private BigDecimal calculateAverageDaily(List<Activity> assessed, Instant from, Instant to) {
        if (from == null || to == null) {
            return BigDecimal.ZERO;
        }

        long daysBetween = java.time.Duration.between(from, to).toDays();
        if (daysBetween <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalCarbonKg = assessed.stream()
                .map(this::extractCarbonKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalCarbonKg.divide(BigDecimal.valueOf(daysBetween), 4, RoundingMode.HALF_UP);
    }

    // ── Metadata extraction helpers ────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private boolean hasCarbonAssessment(Activity activity) {
        Map<String, Object> metadata = activity.getMetadata();
        if (metadata == null) return false;
        return metadata.get(CARBON_ASSESSMENT_KEY) instanceof Map;
    }

    @SuppressWarnings("unchecked")
    private BigDecimal extractCarbonKg(Activity activity) {
        Map<String, Object> metadata = activity.getMetadata();
        if (metadata == null) return BigDecimal.ZERO;

        Object assessmentObj = metadata.get(CARBON_ASSESSMENT_KEY);
        if (!(assessmentObj instanceof Map)) return BigDecimal.ZERO;

        Map<String, Object> assessment = (Map<String, Object>) assessmentObj;
        Object carbonKg = assessment.get(CARBON_KG_KEY);
        if (carbonKg instanceof BigDecimal) return (BigDecimal) carbonKg;
        if (carbonKg instanceof Number) return BigDecimal.valueOf(((Number) carbonKg).doubleValue());
        if (carbonKg instanceof String) {
            try {
                return new BigDecimal((String) carbonKg);
            } catch (NumberFormatException e) {
                log.debug("CarbonAnalyticsService — malformed carbonKg string '{}', treating as zero", carbonKg);
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    @SuppressWarnings("unchecked")
    private String extractMethodology(Activity activity) {
        Map<String, Object> metadata = activity.getMetadata();
        if (metadata == null) return null;

        Object assessmentObj = metadata.get(CARBON_ASSESSMENT_KEY);
        if (!(assessmentObj instanceof Map)) return null;

        Map<String, Object> assessment = (Map<String, Object>) assessmentObj;
        Object methodology = assessment.get(METHODOLOGY_KEY);
        return methodology instanceof String ? (String) methodology : null;
    }
}
