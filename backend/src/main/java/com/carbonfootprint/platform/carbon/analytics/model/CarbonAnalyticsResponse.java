package com.carbonfootprint.platform.carbon.analytics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for carbon emission analytics.
 *
 * <h3>Fields</h3>
 * <ul>
 *   <li><strong>totalCarbonKg</strong> — sum of all emissions (kg CO₂e)</li>
 *   <li><strong>categoryTotals</strong> — breakdown by {@code ActivityCategory}</li>
 *   <li><strong>monthlyTrend</strong> — month-by-month emission history</li>
 *   <li><strong>topActivities</strong> — the 5 highest-emitting activities</li>
 *   <li><strong>averageDailyKg</strong> — mean daily emission over the period</li>
 *   <li><strong>activityCount</strong> — number of activities with calculated emissions</li>
 *   <li><strong>periodStart / periodEnd</strong> — the time window queried</li>
 * </ul>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarbonAnalyticsResponse {

    private BigDecimal totalCarbonKg;

    private List<CategoryEmissionSummary> categoryTotals;

    private List<MonthlyEmissionTrend> monthlyTrend;

    private List<TopEmissionActivity> topActivities;

    private BigDecimal averageDailyKg;

    private int activityCount;

    private Instant periodStart;

    private Instant periodEnd;
}
