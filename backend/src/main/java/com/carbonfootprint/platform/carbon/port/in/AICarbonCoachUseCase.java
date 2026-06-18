package com.carbonfootprint.platform.carbon.port.in;

import com.carbonfootprint.platform.carbon.coach.model.AICarbonCoachResponse;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Inbound port: AI-powered carbon emission coaching.
 *
 * <h3>Clean Architecture role</h3>
 * This is a <em>driving port</em> — the REST controller depends on this
 * abstraction. The service implementation provides the concrete logic.
 *
 * <h3>Design</h3>
 * Delegates to {@link CarbonInsightUseCase} for deterministic data, then
 * enriches it with Gemini-generated personalized coaching. If Gemini fails,
 * the fallback returns deterministic insights unchanged.
 */
public interface AICarbonCoachUseCase {

    /**
     * Generates AI-powered coaching for the given user.
     *
     * @param userId the authenticated user's identifier
     * @return coach response, or empty if no data to derive insights from
     */
    Optional<AICarbonCoachResponse> generateCoach(String userId);

    /**
     * Generates AI-powered coaching for the given user within a time window.
     *
     * @param userId the authenticated user's identifier
     * @param from   start of the window (inclusive)
     * @param to     end of the window (inclusive)
     * @return coach response, or empty if no data to derive insights from
     */
    Optional<AICarbonCoachResponse> generateCoach(String userId, LocalDate from, LocalDate to);
}
