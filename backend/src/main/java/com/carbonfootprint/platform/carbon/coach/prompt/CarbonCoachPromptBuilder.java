package com.carbonfootprint.platform.carbon.coach.prompt;

import com.carbonfootprint.platform.carbon.analytics.model.CarbonInsightResponse;
import org.springframework.stereotype.Component;

/**
 * Builds a structured prompt for Gemini from deterministic carbon insights.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Provide a system prompt defining the coach role and strict rules</li>
 *   <li>Enforce: no invented numbers, no modified numbers, no calculations</li>
 * </ul>
 *
 * <h3>Design</h3 *>
 * Stateless Spring bean — no instance state, no external calls.
 * Each data section is appended by its own helper method.
 */
@Component
public class CarbonCoachPromptBuilder {

    private static final String SYSTEM_PROMPT = """
            You are an experienced sustainability coach.

            Rules:
            - Never invent numbers.
            - Never modify numbers.
            - Never perform calculations.
            - Use only supplied information.
            - Be encouraging.
            - Maximum 250 words.
            """;

    private static final String FINAL_INSTRUCTION = """
            Return ONLY valid JSON. No markdown. No code fences. No explanations.
            Never invent numbers. Never modify numerical values. Never calculate emissions.
            Use only the supplied CarbonInsightResponse.

            JSON schema:
            {
              "summary": "string",
              "strengths": ["string"],
              "concerns": ["string"],
              "recommendations": ["string"],
              "weeklyChallenge": "string",
              "motivation": "string"
            }
            """;

    /**
     * Builds a coaching prompt from the given deterministic insights.
     *
     * @param insight the deterministic insight response
     * @return the prompt ready to send to Gemini
     */
    public String build(CarbonInsightResponse insight) {
        StringBuilder sb = new StringBuilder();
        sb.append(SYSTEM_PROMPT);

        if (insight != null) {
            appendSummary(sb, insight);
            appendAchievements(sb, insight);
            appendWarnings(sb, insight);
            appendRecommendations(sb, insight);
            appendInsights(sb, insight);
        }

        sb.append(FINAL_INSTRUCTION);

        return sb.toString();
    }

    private void appendSummary(StringBuilder sb, CarbonInsightResponse insight) {
        appendIfNotBlank(sb, "Summary", "-------", insight.getSummary());
    }

    private void appendAchievements(StringBuilder sb, CarbonInsightResponse insight) {
        appendList(sb, "Achievements", "------------", insight.getAchievements());
    }

    private void appendWarnings(StringBuilder sb, CarbonInsightResponse insight) {
        appendList(sb, "Warnings", "--------", insight.getWarnings());
    }

    private void appendRecommendations(StringBuilder sb, CarbonInsightResponse insight) {
        appendList(sb, "Recommendations", "---------------", insight.getRecommendations());
    }

    private void appendInsights(StringBuilder sb, CarbonInsightResponse insight) {
        appendList(sb, "Insights", "--------", insight.getInsights());
    }

    private void appendIfNotBlank(StringBuilder sb, String label, String separator, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(label).append("\n").append(separator).append("\n").append(value).append("\n");
        }
    }

    private void appendList(StringBuilder sb, String label, String separator, java.util.List<String> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        sb.append(label).append("\n").append(separator).append("\n");
        for (String item : items) {
            if (item != null && !item.isBlank()) {
                sb.append("- ").append(item).append("\n");
            }
        }
    }
}
