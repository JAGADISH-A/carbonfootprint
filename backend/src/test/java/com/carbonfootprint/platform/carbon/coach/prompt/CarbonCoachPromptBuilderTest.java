package com.carbonfootprint.platform.carbon.coach.prompt;

import com.carbonfootprint.platform.carbon.analytics.model.CarbonInsightResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CarbonCoachPromptBuilderTest {

    private final CarbonCoachPromptBuilder builder = new CarbonCoachPromptBuilder();

    // ── System prompt content ─────────────────────────────────────────────

    @Test
    void build_returnsNonNull() {
        String result = builder.build(null);

        assertThat(result).isNotNull();
    }

    @Test
    void build_containsSustainabilityCoachRole() {
        String result = builder.build(null);

        assertThat(result).contains("experienced sustainability coach");
    }

    @Test
    void build_containsNeverInventRule() {
        String result = builder.build(null);

        assertThat(result).contains("Never invent numbers");
    }

    @Test
    void build_containsNeverModifyRule() {
        String result = builder.build(null);

        assertThat(result).contains("Never modify numbers");
    }

    @Test
    void build_containsNeverCalculateRule() {
        String result = builder.build(null);

        assertThat(result).contains("Never perform calculations");
    }

    @Test
    void build_containsUseOnlySuppliedInfoRule() {
        String result = builder.build(null);

        assertThat(result).contains("Use only supplied information");
    }

    @Test
    void build_containsBeEncouragingRule() {
        String result = builder.build(null);

        assertThat(result).contains("Be encouraging");
    }

    @Test
    void build_containsMaxWordLimitRule() {
        String result = builder.build(null);

        assertThat(result).contains("Maximum 250 words");
    }

    // ── Summary section ───────────────────────────────────────────────────

    @Test
    void build_withValidSummary_appendsSummarySection() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .summary("Your total footprint is 45.00 kg CO₂e across 5 activities.")
                .build();

        String result = builder.build(insight);

        assertThat(result).contains("Summary\n-------\nYour total footprint is 45.00 kg CO₂e across 5 activities.");
    }

    @Test
    void build_withNullSummary_doesNotAppendSummarySection() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .summary(null)
                .build();

        String result = builder.build(insight);

        assertThat(result).doesNotContain("Summary\n-------");
    }

    @Test
    void build_withBlankSummary_doesNotAppendSummarySection() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .summary("   ")
                .build();

        String result = builder.build(insight);

        assertThat(result).doesNotContain("Summary\n-------");
    }

    @Test
    void build_withNullInsight_doesNotAppendSummarySection() {
        String result = builder.build(null);

        assertThat(result).doesNotContain("Summary\n-------");
    }

    // ── Achievements section ──────────────────────────────────────────────

    @Test
    void build_withPopulatedAchievements_appendsAchievementsSection() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .summary("Summary")
                .achievements(List.of("Below 100 kg threshold", "Low average per activity"))
                .build();

        String result = builder.build(insight);

        assertThat(result).contains("Achievements\n------------\n- Below 100 kg threshold\n- Low average per activity\n");
    }

    @Test
    void build_withEmptyAchievements_doesNotAppendAchievementsSection() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .summary("Summary")
                .achievements(List.of())
                .build();

        String result = builder.build(insight);

        assertThat(result).doesNotContain("Achievements\n------------");
    }

    @Test
    void build_withNullAchievements_doesNotAppendAchievementsSection() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .summary("Summary")
                .achievements(null)
                .build();

        String result = builder.build(insight);

        assertThat(result).doesNotContain("Achievements\n------------");
    }

    // ── Warnings section ─────────────────────────────────────────────────

    @Test
    void build_withPopulatedWarnings_appendsWarningsSection() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .summary("Summary")
                .warnings(List.of("FUEL is highest", "Above average emissions"))
                .build();

        String result = builder.build(insight);

        assertThat(result).contains("Warnings\n--------\n- FUEL is highest\n- Above average emissions\n");
    }

    @Test
    void build_withEmptyWarnings_doesNotAppendWarningsSection() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .summary("Summary")
                .warnings(List.of())
                .build();

        String result = builder.build(insight);

        assertThat(result).doesNotContain("Warnings\n--------");
    }

    @Test
    void build_withNullWarnings_doesNotAppendWarningsSection() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .summary("Summary")
                .warnings(null)
                .build();

        String result = builder.build(insight);

        assertThat(result).doesNotContain("Warnings\n--------");
    }

    // ── Recommendations section ───────────────────────────────────────────

    @Test
    void build_withPopulatedRecommendations_appendsRecommendationsSection() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .summary("Summary")
                .recommendations(List.of("Switch to electric vehicles", "Use public transport"))
                .build();

        String result = builder.build(insight);

        assertThat(result).contains("Recommendations\n---------------\n- Switch to electric vehicles\n- Use public transport\n");
    }

    @Test
    void build_withEmptyRecommendations_doesNotAppendRecommendationsSection() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .summary("Summary")
                .recommendations(List.of())
                .build();

        String result = builder.build(insight);

        assertThat(result).doesNotContain("Recommendations\n---------------");
    }

    @Test
    void build_withNullRecommendations_doesNotAppendRecommendationsSection() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .summary("Summary")
                .recommendations(null)
                .build();

        String result = builder.build(insight);

        assertThat(result).doesNotContain("Recommendations\n---------------");
    }

    // ── Insights section ──────────────────────────────────────────────────

    @Test
    void build_withPopulatedInsights_appendsInsightsSection() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .summary("Summary")
                .insights(List.of("Total emissions: 45.00 kg CO₂e", "Highest category: FUEL"))
                .build();

        String result = builder.build(insight);

        assertThat(result).contains("Insights\n--------\n- Total emissions: 45.00 kg CO₂e\n- Highest category: FUEL\n");
    }

    @Test
    void build_withEmptyInsights_doesNotAppendInsightsSection() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .summary("Summary")
                .insights(List.of())
                .build();

        String result = builder.build(insight);

        assertThat(result).doesNotContain("Insights\n--------");
    }

    @Test
    void build_withNullInsights_doesNotAppendInsightsSection() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .summary("Summary")
                .insights(null)
                .build();

        String result = builder.build(insight);

        assertThat(result).doesNotContain("Insights\n--------");
    }

    // ── Final instruction ─────────────────────────────────────────────────

    @Test
    void build_containsReturnOnlyJsonInstruction() {
        String result = builder.build(null);

        assertThat(result).contains("Return ONLY valid JSON");
    }

    @Test
    void build_containsNoMarkdownInstruction() {
        String result = builder.build(null);

        assertThat(result).contains("No markdown");
    }

    @Test
    void build_containsNoCodeFencesInstruction() {
        String result = builder.build(null);

        assertThat(result).contains("No code fences");
    }

    @Test
    void build_containsNoExplanationsInstruction() {
        String result = builder.build(null);

        assertThat(result).contains("No explanations");
    }

    @Test
    void build_containsNeverInventNumbersInstruction() {
        String result = builder.build(null);

        assertThat(result).contains("Never invent numbers");
    }

    @Test
    void build_containsNeverModifyNumbersInstruction() {
        String result = builder.build(null);

        assertThat(result).contains("Never modify numerical values");
    }

    @Test
    void build_containsNeverCalculateEmissionsInstruction() {
        String result = builder.build(null);

        assertThat(result).contains("Never calculate emissions");
    }

    @Test
    void build_containsUseOnlySuppliedResponseInstruction() {
        String result = builder.build(null);

        assertThat(result).contains("Use only the supplied CarbonInsightResponse");
    }

    @Test
    void build_containsJsonSchema() {
        String result = builder.build(null);

        assertThat(result).contains("\"summary\": \"string\"");
        assertThat(result).contains("\"strengths\": [\"string\"]");
        assertThat(result).contains("\"concerns\": [\"string\"]");
        assertThat(result).contains("\"recommendations\": [\"string\"]");
        assertThat(result).contains("\"weeklyChallenge\": \"string\"");
        assertThat(result).contains("\"motivation\": \"string\"");
    }

    // ── Ignores non-summary fields ────────────────────────────────────────

    @Test
    void build_withPopulatedInsight_ignoresNonSummaryFields() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .summary("Your footprint is 45.00 kg CO₂e.")
                .achievements(List.of("Below 100 kg threshold"))
                .warnings(List.of("FUEL is highest"))
                .recommendations(List.of("Switch to EV"))
                .insights(List.of("Total: 45.00 kg"))
                .build();

        String result = builder.build(insight);

        assertThat(result).contains("experienced sustainability coach");
        assertThat(result).contains("Your footprint is 45.00 kg CO₂e.");
        assertThat(result).contains("Below 100 kg threshold");
        assertThat(result).contains("FUEL is highest");
        assertThat(result).contains("Switch to EV");
        assertThat(result).contains("Total: 45.00 kg");
    }

    @Test
    void build_alwaysReturnsSamePrompt() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .summary("Test summary")
                .build();

        String first = builder.build(insight);
        String second = builder.build(insight);

        assertThat(first).isEqualTo(second);
    }
}
