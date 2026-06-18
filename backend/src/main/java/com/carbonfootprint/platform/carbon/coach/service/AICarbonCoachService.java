package com.carbonfootprint.platform.carbon.coach.service;

import com.carbonfootprint.platform.carbon.analytics.model.CarbonInsightResponse;
import com.carbonfootprint.platform.carbon.coach.model.AICarbonCoachResponse;
import com.carbonfootprint.platform.carbon.coach.parser.GeminiResponseParser;
import com.carbonfootprint.platform.carbon.coach.prompt.CarbonCoachPromptBuilder;
import com.carbonfootprint.platform.carbon.port.in.AICarbonCoachUseCase;
import com.carbonfootprint.platform.carbon.port.in.CarbonInsightUseCase;
import com.carbonfootprint.platform.integration.ai.gemini.GeminiClient;
import com.carbonfootprint.platform.integration.ai.gemini.GeminiClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * AI-powered carbon emission coaching service.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Delegates to {@link CarbonInsightUseCase} for deterministic data</li>
 *   <li>Builds a prompt via {@link CarbonCoachPromptBuilder}</li>
 *   <li>Calls {@link GeminiClient} for AI-generated coaching summary</li>
 *   <li>Falls back to deterministic response on any Gemini failure</li>
 * </ul>
 *
 * @see AICarbonCoachUseCase
 * @see CarbonInsightUseCase
 * @see GeminiClient
 * @see CarbonCoachPromptBuilder
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AICarbonCoachService implements AICarbonCoachUseCase {

    private static final String DEFAULT_WEEKLY_CHALLENGE = "Walk instead of driving once this week.";
    private static final String DEFAULT_MOTIVATION = "Every small step counts.";

    private final CarbonInsightUseCase carbonInsightUseCase;
    private final GeminiClient geminiClient;
    private final CarbonCoachPromptBuilder carbonCoachPromptBuilder;

    @Override
    public Optional<AICarbonCoachResponse> generateCoach(String userId) {
        log.debug("AICarbonCoachService — generating coach for userId={}", userId);

        Optional<CarbonInsightResponse> insight = carbonInsightUseCase.generateInsights(userId);
        return insight.map(this::buildCoachResponse);
    }

    @Override
    public Optional<AICarbonCoachResponse> generateCoach(String userId, LocalDate from, LocalDate to) {
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusNanos(1);

        log.debug("AICarbonCoachService — generating coach for userId={} from={} to={}", userId, from, to);

        Optional<CarbonInsightResponse> insight = carbonInsightUseCase.generateInsights(userId, fromInstant, toInstant);
        return insight.map(this::buildCoachResponse);
    }

    /**
     * Builds a coach response from deterministic insights.
     *
     * <p>Builds the prompt, calls Gemini for AI-generated summary, and
     * combines it with deterministic fields. Falls back to deterministic
     * response on any Gemini failure.</p>
     *
     * @param insight the deterministic insight response
     * @return the coach response
     */
    private AICarbonCoachResponse buildCoachResponse(CarbonInsightResponse insight) {
        String prompt = carbonCoachPromptBuilder.build(insight);

        log.debug("AICarbonCoachService — generated prompt:\n{}", prompt);

        try {
            String geminiResponse = geminiClient.generateContent(prompt);

            log.debug("AICarbonCoachService — Gemini response received ({} chars)", geminiResponse.length());

            AICarbonCoachResponse parsed = GeminiResponseParser.parse(geminiResponse, true);

            if (parsed.getSummary().isBlank()) {
                log.warn("AICarbonCoachService — parsed response has empty summary, falling back");
                return fallbackResponse(insight);
            }

            return parsed;
        } catch (GeminiClientException e) {
            log.warn("AICarbonCoachService — Gemini call failed: {}, falling back to deterministic", e.getMessage());
            return fallbackResponse(insight);
        } catch (Exception e) {
            log.error("AICarbonCoachService — unexpected error: {}, falling back to deterministic", e.getMessage(), e);
            return fallbackResponse(insight);
        }
    }

    /**
     * Builds a deterministic fallback response from the insight data.
     *
     * <p>Always returns a complete response with {@code aiGenerated = false}.
     * Never throws.</p>
     */
    private AICarbonCoachResponse fallbackResponse(CarbonInsightResponse insight) {
        return AICarbonCoachResponse.builder()
                .summary(insight.getSummary())
                .strengths(insight.getAchievements())
                .concerns(insight.getWarnings())
                .recommendations(insight.getRecommendations())
                .weeklyChallenge(DEFAULT_WEEKLY_CHALLENGE)
                .motivation(DEFAULT_MOTIVATION)
                .aiGenerated(false)
                .build();
    }
}
