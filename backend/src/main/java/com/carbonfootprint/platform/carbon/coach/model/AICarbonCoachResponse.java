package com.carbonfootprint.platform.carbon.coach.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for AI-powered carbon emission coaching.
 *
 * <h3>Fields</h3>
 * <ul>
 *   <li><strong>summary</strong> — personalized executive summary of the user's carbon footprint</li>
 *   <li><strong>strengths</strong> — positive habits and low-emission behaviors to reinforce</li>
 *   <li><strong>concerns</strong> — biggest emission sources and areas needing attention</li>
 *   <li><strong>recommendations</strong> — actionable, personalized suggestions to reduce emissions</li>
 *   <li><strong>weeklyChallenge</strong> — a concrete, achievable weekly goal for emission reduction</li>
 *   <li><strong>motivation</strong> — encouraging conclusion to sustain behavior change</li>
 *   <li><strong>aiGenerated</strong> — whether Gemini enriched the content (false = deterministic fallback)</li>
 * </ul>
 *
 * <h3>Design</h3>
 * All lists are non-null. Empty list means no items of that type.
 * The {@code aiGenerated} flag distinguishes AI-enriched responses from
 * deterministic fallbacks returned when Gemini is unavailable.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AICarbonCoachResponse {

    private String summary;

    private List<String> strengths;

    private List<String> concerns;

    private List<String> recommendations;

    private String weeklyChallenge;

    private String motivation;

    private boolean aiGenerated;
}
