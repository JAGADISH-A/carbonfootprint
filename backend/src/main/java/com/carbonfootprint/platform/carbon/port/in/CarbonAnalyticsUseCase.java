package com.carbonfootprint.platform.carbon.port.in;

import com.carbonfootprint.platform.carbon.analytics.model.CarbonAnalyticsResponse;

import java.time.Instant;
import java.util.Optional;

/**
 * Inbound port: read-only analytics contract for carbon emission data.
 *
 * <h3>Clean Architecture role</h3>
 * This is a <em>driving port</em> — the REST controller depends on this
 * abstraction. The service implementation provides the concrete logic.
 *
 * <h3>Design</h3>
 * All methods are purely read-only. No mutations are performed.
 * The service extracts carbon data from {@code Activity.metadata["carbonAssessment"]}
 * and aggregates it for the frontend.
 */
public interface CarbonAnalyticsUseCase {

    /**
     * Returns aggregated carbon analytics for the given user.
     *
     * @param userId the authenticated user's identifier
     * @return the analytics response, or empty if no data exists
     */
    Optional<CarbonAnalyticsResponse> getAnalytics(String userId);

    /**
     * Returns aggregated carbon analytics for the given user within a time window.
     *
     * @param userId the authenticated user's identifier
     * @param from   start of the window (inclusive)
     * @param to     end of the window (inclusive)
     * @return the analytics response, or empty if no data exists in the window
     */
    Optional<CarbonAnalyticsResponse> getAnalytics(String userId, Instant from, Instant to);
}
