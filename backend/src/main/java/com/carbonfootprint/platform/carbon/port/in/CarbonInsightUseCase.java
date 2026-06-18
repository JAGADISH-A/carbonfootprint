package com.carbonfootprint.platform.carbon.port.in;

import com.carbonfootprint.platform.carbon.analytics.model.CarbonInsightResponse;

import java.time.Instant;
import java.util.Optional;

/**
 * Inbound port: deterministic insight generation for carbon emission data.
 *
 * <h3>Clean Architecture role</h3>
 * This is a <em>driving port</em> — the REST controller depends on this
 * abstraction. The service implementation provides the concrete logic.
 *
 * <h3>Design</h3 * Purely read-only and deterministic. No external calls (e.g., Gemini).
 * Internally delegates to {@link CarbonAnalyticsUseCase} to reuse the existing
 * analytics pipeline — no aggregation logic is duplicated.
 */
public interface CarbonInsightUseCase {

    /**
     * Generates deterministic insights for the given user.
     *
     * @param userId the authenticated user's identifier
     * @return insight response, or empty if no data to derive insights from
     */
    Optional<CarbonInsightResponse> generateInsights(String userId);

    /**
     * Generates deterministic insights for the given user within a time window.
     *
     * @param userId the authenticated user's identifier
     * @param from   start of the window (inclusive)
     * @param to     end of the window (inclusive)
     * @return insight response, or empty if no data to derive insights from
     */
    Optional<CarbonInsightResponse> generateInsights(String userId, Instant from, Instant to);
}
