package com.carbonfootprint.platform.carbon.coach.prompt;

import com.carbonfootprint.platform.carbon.analytics.model.CarbonAnalyticsResponse;
import com.carbonfootprint.platform.carbon.analytics.model.CarbonInsightResponse;
import com.carbonfootprint.platform.carbon.analytics.model.CategoryEmissionSummary;
import com.carbonfootprint.platform.carbon.analytics.model.TopEmissionActivity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Builds a structured prompt for Groq from deterministic carbon insights.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Provide a system prompt defining the coach role and strict rules</li>
 *   <li>Enforce: no invented numbers, no modified numbers, no calculations</li>
 *   <li>Output schema must match {@link com.carbonfootprint.platform.carbon.coach.parser.AiResponseParser}</li>
 * </ul>
 *
 * <h3>Design</h3>
 * Stateless Spring bean — no instance state, no external calls.
 * Uses compact structured format (key-value pairs) instead of prose to minimise token usage.
 */
@Component
public class CarbonCoachPromptBuilder {

    /**
     * System message sent as the first message in the Groq chat.
     * Defines the proactive coaching persona, tone, and output contract.
     */
    static final String SYSTEM_PROMPT = """
            Role: Proactive sustainability coach. Warm, encouraging, conversational, confident.
            Task: Coach the user on their carbon footprint data. Be specific to THEIR numbers.

            Output rules:
            - summary: What happened. Reference their actual data (total kg, category, merchant). 1-2 sentences.
            - strengths: Why their low numbers matter. Connect to real impact. 1 sentence per item.
            - concerns: Identify the ONE highest-impact improvement opportunity. Be specific about what to change.
            - recommendations: Estimate potential reduction in kg if they act. Use their data to calculate.
            - weeklyChallenge: One concrete, achievable action tied to their top emission source.
            - motivation: End with confident encouragement. Reference their potential.

            Hard rules:
            - Never invent numbers. Use ONLY supplied data.
            - Never give generic advice ("eat less meat"). Always reference their specific categories/merchants.
            - Keep total output under 4 short paragraphs across all fields combined.
            """;

    /**
     * Final instruction appended after all data sections.
     * Defines the exact JSON output schema that matches {@code AiResponseParser}.
     */
    static final String OUTPUT_SCHEMA = "Return ONLY valid JSON. Do not include markdown. Do not wrap in ```.\n#OUT\n{\"summary\":\"\",\"strengths\":[],\"concerns\":[],\"recommendations\":[],\"weeklyChallenge\":\"\",\"motivation\":\"\"}";

    /**
     * Builds the user message portion of the coaching prompt.
     * The system message is provided separately via {@link #getSystemPrompt()}.
     *
     * @param insight the deterministic insight response (contains analytics)
     * @return the user message ready to send to Groq
     */
    public String build(CarbonInsightResponse insight) {
        StringBuilder sb = new StringBuilder();

        if (insight != null) {
            CarbonAnalyticsResponse analytics = insight.getAnalytics();
            if (analytics != null) {
                appendAnalytics(sb, analytics);
            }
            appendPipeList(sb, "#ACHV", insight.getAchievements());
            appendPipeList(sb, "#CONC", insight.getWarnings());
            appendPipeList(sb, "#REC", insight.getRecommendations());
        }

        sb.append(OUTPUT_SCHEMA);
        return sb.toString();
    }

    /**
     * Returns the system-level instruction for the coach persona.
     */
    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    // ── Compact analytics formatting ──────────────────────────────────────

    private void appendAnalytics(StringBuilder sb, CarbonAnalyticsResponse a) {
        sb.append("#DATA\n");
        sb.append("act:").append(a.getActivityCount());
        sb.append("|kg:").append(fmt(a.getTotalCarbonKg()));
        sb.append("|avg:").append(fmt(safeDivide(a.getTotalCarbonKg(), a.getActivityCount())));

        List<CategoryEmissionSummary> cats = a.getCategoryTotals();
        if (cats != null && !cats.isEmpty()) {
            CategoryEmissionSummary top = cats.get(0);
            sb.append("|cat:").append(top.getCategory());
            sb.append("(").append(fmt(top.getCarbonKg())).append("kg,").append(fmtPct(top.getPercentageOfTotal())).append("%)");
        }

        List<TopEmissionActivity> tops = a.getTopActivities();
        if (tops != null && !tops.isEmpty()) {
            TopEmissionActivity top = tops.get(0);
            String label = top.getMerchant() != null ? top.getMerchant() : top.getCategory();
            sb.append("|merchant:").append(label);
        }
        sb.append("\n");
    }

    // ── Compact pipe-delimited list formatting ────────────────────────────

    private void appendPipeList(StringBuilder sb, String header, List<String> items) {
        if (items == null || items.isEmpty()) return;
        sb.append(header).append("\n");
        boolean first = true;
        for (String item : items) {
            if (item != null && !item.isBlank()) {
                if (!first) sb.append("|");
                sb.append(compact(item));
                first = false;
            }
        }
        sb.append("\n");
    }

    /**
     * Compacts a prose string by trimming filler words and keeping the core fact.
     * E.g. "Nice work — your total of 45.0 kg CO₂e is well below the 100 kg threshold." → "Total 45.0kg below 100kg threshold"
     */
    private String compact(String text) {
        return text
                .replace("I noticed that ", "")
                .replace("I can see ", "")
                .replace("I looked at your ", "")
                .replace(" that's a sign you're making thoughtful choices.", "")
                .replace(" — that shows balanced habits.", "")
                .replace(" — worth keeping an eye on.", "")
                .replace(" — one small change in this area could make a big difference.", "")
                .replace(" — worth seeing what changed.", "")
                .replace("You're already doing better than you think. Every small choice adds up — keep going!", "Keep going!")
                .trim();
    }

    // ── Utility ───────────────────────────────────────────────────────────

    private String fmt(BigDecimal val) {
        return val == null ? "0" : val.setScale(1, RoundingMode.HALF_UP).toString();
    }

    private String fmtPct(BigDecimal val) {
        return val == null ? "0" : val.setScale(0, RoundingMode.HALF_UP).toString();
    }

    private BigDecimal safeDivide(BigDecimal numerator, int denominator) {
        if (denominator == 0) return BigDecimal.ZERO;
        return numerator.divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
    }
}
