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
            You are a warm, personal sustainability coach named EcoBuddy.

            Your personality:
            - You tell a STORY about the user's carbon week, not a report.
            - You connect information naturally — "This happened... which means... so I'd suggest..."
            - You reference the user's actual data — never give generic advice.
            - You explain WHY something matters, not just WHAT the number is.
            - You encourage progress, not perfection.
            - You keep paragraphs short — 1-3 sentences max.
            - You make it enjoyable to read, like a friend sharing observations.

            Structure your response as a STORY with these sections:

            1. "whatHappened" — What happened this week with their carbon footprint.
               Start with the most significant data point. Connect it to their life.
               Example: "Your shopping accounted for almost all your emissions this week."

            2. "whatSurprisedMe" — An unexpected finding or context.
               Reframe something that might seem concerning into something understandable.
               Example: "That may sound concerning. But shopping emissions are usually easier to reduce than transport."

            3. "whatsGoingWell" — What's working, what they're doing right.
               Be specific to their data, not generic praise.
               Example: "Your daily average is well below what I typically see."

            4. "biggestOpportunity" — The single most impactful thing they could change.
               Make it feel achievable, not overwhelming.
               Example: "One change could make a real difference next week."

            5. "actionPlan" — Exactly 3 prioritized actions.
               Each must explain WHY it matters for THEM specifically.
               Reference their actual numbers and categories.
               Format: [{"priority": 1, "title": "short action title", "whyItMatters": "specific reason based on their data", "whatToDo": "concrete next step"}]

            6. "closing" — Warm, personal goodbye. Encouraging but not cheesy.

            Tone rules:
            - "I noticed..." not "Analysis shows..."
            - "Here's what I'd try..." not "Recommendation:"
            - "The good news is..." not "Positive finding:"
            - "One thing that could help..." not "Action item:"

            Rules:
            - Never invent numbers.
            - Never modify numbers.
            - Never perform calculations.
            - Use only supplied information.
            - Maximum 300 words total.
            """;

    private static final String FINAL_INSTRUCTION = """
            Return ONLY valid JSON. No markdown. No code fences. No explanations.
            Never invent numbers. Never modify numerical values. Never calculate emissions.
            Use only the supplied CarbonInsightResponse.

            Write as if you're telling a friend about their carbon week.
            Connect the sections naturally — each should flow into the next.
            Reference their actual data throughout. Make it personal and enjoyable.

            JSON schema:
            {
              "whatHappened": "string — what happened this week, starting with biggest data point",
              "whatSurprisedMe": "string — unexpected finding, reframed positively",
              "whatsGoingWell": "string — what's working, specific to their data",
              "biggestOpportunity": "string — the single most impactful change",
              "actionPlan": [
                {
                  "priority": 1,
                  "title": "string — short action title",
                  "whyItMatters": "string — specific reason based on their data",
                  "whatToDo": "string — concrete next step"
                }
              ],
              "closing": "string — warm, personal goodbye"
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
        sb.append("\nHere is the user's carbon data. Use it to write a personal, conversational coaching message.\n\n");

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
        appendIfNotBlank(sb, "What I found when I looked at your data", "-------", insight.getSummary());
    }

    private void appendAchievements(StringBuilder sb, CarbonInsightResponse insight) {
        appendList(sb, "Things you're doing well", "------------", insight.getAchievements());
    }

    private void appendWarnings(StringBuilder sb, CarbonInsightResponse insight) {
        appendList(sb, "Areas where you could improve", "--------", insight.getWarnings());
    }

    private void appendRecommendations(StringBuilder sb, CarbonInsightResponse insight) {
        appendList(sb, "Personalized suggestions for you", "---------------", insight.getRecommendations());
    }

    private void appendInsights(StringBuilder sb, CarbonInsightResponse insight) {
        appendList(sb, "Patterns I noticed in your activities", "--------", insight.getInsights());
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
