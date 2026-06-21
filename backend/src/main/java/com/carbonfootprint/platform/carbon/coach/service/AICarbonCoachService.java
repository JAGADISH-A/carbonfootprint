package com.carbonfootprint.platform.carbon.coach.service;

import com.carbonfootprint.platform.carbon.analytics.model.CarbonAnalyticsResponse;
import com.carbonfootprint.platform.carbon.analytics.model.CarbonInsightResponse;
import com.carbonfootprint.platform.carbon.coach.model.AICarbonCoachResponse;
import com.carbonfootprint.platform.carbon.coach.parser.AiResponseParser;
import com.carbonfootprint.platform.carbon.coach.prompt.CarbonCoachPromptBuilder;
import com.carbonfootprint.platform.carbon.port.in.AICarbonCoachUseCase;
import com.carbonfootprint.platform.carbon.port.in.CarbonInsightUseCase;
import com.carbonfootprint.platform.integration.ai.groq.GroqClient;
import com.carbonfootprint.platform.platform.exception.IngestionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
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
 *   <li>Checks {@link CoachCacheService} for cached AI responses</li>
 *   <li>Builds a prompt via {@link CarbonCoachPromptBuilder}</li>
 *   <li>Calls {@link GroqClient} with coach-model for AI-generated coaching summary</li>
 *   <li>Overrides AI confidence with deterministic score from {@link CoachConfidenceCalculator}</li>
 *   <li>Caches only successful AI responses (never fallbacks)</li>
 *   <li>Falls back to deterministic response on any AI failure</li>
 * </ul>
 *
 * @see AICarbonCoachUseCase
 * @see CarbonInsightUseCase
 * @see GroqClient
 * @see CarbonCoachPromptBuilder
 * @see CoachCacheService
 * @see CoachConfidenceCalculator
 */
@Slf4j
@Service
@Profile("!stub")
public class AICarbonCoachService implements AICarbonCoachUseCase {

    private static final String DEFAULT_WEEKLY_CHALLENGE = "Here's a fun one: try going car-free for just one day this week and see how it feels.";
    private static final String DEFAULT_MOTIVATION = "You're already doing better than you think. Every small choice adds up — keep going!";

    private final CarbonInsightUseCase carbonInsightUseCase;
    private final GroqClient groqClient;
    private final CarbonCoachPromptBuilder carbonCoachPromptBuilder;
    private final CoachCacheService coachCacheService;
    private final CoachConfidenceCalculator coachConfidenceCalculator;
    private final String coachModel;

    public AICarbonCoachService(
            CarbonInsightUseCase carbonInsightUseCase,
            GroqClient groqClient,
            CarbonCoachPromptBuilder carbonCoachPromptBuilder,
            CoachCacheService coachCacheService,
            CoachConfidenceCalculator coachConfidenceCalculator,
            @Value("${carbon.groq.coach-model:llama-3.3-70b-versatile}") String coachModel
    ) {
        this.carbonInsightUseCase = carbonInsightUseCase;
        this.groqClient = groqClient;
        this.carbonCoachPromptBuilder = carbonCoachPromptBuilder;
        this.coachCacheService = coachCacheService;
        this.coachConfidenceCalculator = coachConfidenceCalculator;
        this.coachModel = coachModel;
        log.info("AICarbonCoachService initialised — coachModel={}", coachModel);
    }

    @Override
    public Optional<AICarbonCoachResponse> generateCoach(String userId) {
        log.debug("AICarbonCoachService — generating coach for userId={}", userId);

        Optional<CarbonInsightResponse> insight = carbonInsightUseCase.generateInsights(userId);
        return insight.map(i -> buildCoachResponse(userId, i));
    }

    @Override
    public Optional<AICarbonCoachResponse> generateCoach(String userId, LocalDate from, LocalDate to) {
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusNanos(1);

        log.debug("AICarbonCoachService — generating coach for userId={} from={} to={}", userId, from, to);

        Optional<CarbonInsightResponse> insight = carbonInsightUseCase.generateInsights(userId, fromInstant, toInstant);
        return insight.map(i -> buildCoachResponse(userId, i));
    }

    /**
     * Builds a coach response from deterministic insights.
     *
     * <p>Checks the cache first. On a miss, builds the prompt, calls the AI provider,
     * and stores only successful AI responses. Falls back to deterministic response
     * on any AI failure — fallback responses are never cached.</p>
     *
     * <p>Confidence is always computed deterministically via {@link CoachConfidenceCalculator},
     * regardless of whether the AI response was successful or fallback was used.</p>
     *
     * @param userId  the user identifier (for cache key)
     * @param insight the deterministic insight response
     * @return the coach response
     */
    private AICarbonCoachResponse buildCoachResponse(String userId, CarbonInsightResponse insight) {
        int confidence = coachConfidenceCalculator.calculate(insight);

        // ── Cache lookup ───────────────────────────────────────────────────
        CarbonAnalyticsResponse analytics = insight.getAnalytics();
        AICarbonCoachResponse cached = coachCacheService.get(userId, analytics);
        if (cached != null) {
            return withConfidence(cached, confidence);
        }

        // ── Cache miss — call Groq ────────────────────────────────────────
        String prompt = carbonCoachPromptBuilder.build(insight);

        log.debug("AICarbonCoachService — generated prompt:\n{}", prompt);

        try {
            String aiResponse = groqClient.generateContent(
                    coachModel, carbonCoachPromptBuilder.getSystemPrompt(), prompt);

            log.debug("AICarbonCoachService — AI response received ({} chars)", aiResponse.length());
            log.trace("AICarbonCoachService — raw AI response:\n{}", aiResponse);

            AICarbonCoachResponse parsed = AiResponseParser.parse(aiResponse, true);

            if (parsed == null || parsed.getSummary() == null || parsed.getSummary().isBlank()) {
                log.warn("AICarbonCoachService — parsed response has empty summary, falling back. " +
                        "Raw response (first 500 chars): {}",
                        aiResponse.length() > 500 ? aiResponse.substring(0, 500) + "..." : aiResponse);
                return fallbackResponse(insight, confidence);
            }

            AICarbonCoachResponse response = withConfidence(parsed, confidence);

            // ── Cache only successful AI responses ─────────────────────────
            coachCacheService.put(userId, analytics, response);

            return response;
        } catch (IngestionException e) {
            log.warn("AICarbonCoachService — AI call failed: {}, falling back to deterministic", e.getMessage());
            return fallbackResponse(insight, confidence);
        } catch (Exception e) {
            log.error("AICarbonCoachService — unexpected error: {}, falling back to deterministic", e.getMessage(), e);
            return fallbackResponse(insight, confidence);
        }
    }

    /**
     * Builds a deterministic fallback response from the insight data.
     *
     * <p>Always returns a complete response with {@code aiGenerated = false}.
     * Never throws. Never cached.</p>
     */
    private AICarbonCoachResponse fallbackResponse(CarbonInsightResponse insight, int confidence) {
        return AICarbonCoachResponse.builder()
                .summary(insight.getSummary())
                .strengths(insight.getAchievements())
                .concerns(insight.getWarnings())
                .recommendations(insight.getRecommendations())
                .weeklyChallenge(DEFAULT_WEEKLY_CHALLENGE)
                .motivation(DEFAULT_MOTIVATION)
                .confidence(confidence)
                .aiGenerated(false)
                .build();
    }

    /**
     * Returns a copy of the response with the deterministic confidence value set.
     * The AI-generated confidence (if any) is always overridden.
     */
    private AICarbonCoachResponse withConfidence(AICarbonCoachResponse response, int confidence) {
        return AICarbonCoachResponse.builder()
                .summary(response.getSummary())
                .strengths(response.getStrengths())
                .concerns(response.getConcerns())
                .recommendations(response.getRecommendations())
                .weeklyChallenge(response.getWeeklyChallenge())
                .motivation(response.getMotivation())
                .confidence(confidence)
                .aiGenerated(response.isAiGenerated())
                .build();
    }
}
