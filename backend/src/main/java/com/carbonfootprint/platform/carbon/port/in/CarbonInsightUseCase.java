package com.carbonfootprint.platform.carbon.port.in;

import com.carbonfootprint.platform.carbon.analytics.model.CarbonAnalyticsResponse;
import com.carbonfootprint.platform.carbon.analytics.model.CarbonInsightResponse;

import java.util.List;
import java.util.Optional;

/**
 * Inbound port: deterministic insight generation for carbon emission data.
 *
 * <h3>Clean Architecture role</h3>
 * This is a <em>driving port</em> — the REST controller depends on this
 * abstraction. The service implementation provides the concrete logic.
 *
 * <h3>Design</h3>
 * Purely read-only and deterministic. No external calls (e.g., Gemini).
 * Insights are derived solely from the provided analytics response and activities.
 */
public interface CarbonInsightUseCase {

    /**
     * Generates deterministic insights from pre-computed analytics.
     *
     * @param analyticsResponse the aggregated analytics (may be empty)
     * @param recentActivities  the raw activities used to compute analytics
     * @return insight response, or empty if no data to derive insights from
     */
    Optional<CarbonInsightResponse> generateInsights(
            Optional<CarbonAnalyticsResponse> analyticsResponse,
            List<com.carbonfootprint.platform.activity.model.Activity> recentActivities);
}
