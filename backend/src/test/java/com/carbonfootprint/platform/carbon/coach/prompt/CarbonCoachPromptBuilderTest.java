package com.carbonfootprint.platform.carbon.coach.prompt;

import com.carbonfootprint.platform.carbon.analytics.model.CarbonAnalyticsResponse;
import com.carbonfootprint.platform.carbon.analytics.model.CarbonInsightResponse;
import com.carbonfootprint.platform.carbon.analytics.model.CategoryEmissionSummary;
import com.carbonfootprint.platform.carbon.analytics.model.TopEmissionActivity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CarbonCoachPromptBuilderTest {

    private final CarbonCoachPromptBuilder builder = new CarbonCoachPromptBuilder();

    // ── System prompt (separate from build) ───────────────────────────────

    @Test
    void getSystemPrompt_returnsNonNull() {
        assertThat(builder.getSystemPrompt()).isNotNull();
    }

    @Test
    void getSystemPrompt_containsCoachRole() {
        assertThat(builder.getSystemPrompt()).contains("sustainability coach");
    }

    @Test
    void getSystemPrompt_containsNeverInventRule() {
        assertThat(builder.getSystemPrompt()).contains("Never invent numbers");
    }

    @Test
    void getSystemPrompt_containsNeverModifyRule() {
        assertThat(builder.getSystemPrompt()).contains("ONLY supplied data");
    }

    @Test
    void getSystemPrompt_doesNotContainConfidenceInstruction() {
        assertThat(builder.getSystemPrompt()).doesNotContain("confidence");
    }

    // ── build() returns user message only ─────────────────────────────────

    @Test
    void build_returnsNonNull() {
        String result = builder.build(null);

        assertThat(result).isNotNull();
    }

    @Test
    void build_doesNotContainSystemPrompt() {
        String result = builder.build(null);

        assertThat(result).doesNotContain("EcoBuddy");
    }

    // ── Analytics section (compact key-value) ─────────────────────────────

    @Test
    void build_withAnalytics_includesKeyValueFormat() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .analytics(CarbonAnalyticsResponse.builder()
                        .activityCount(5)
                        .totalCarbonKg(new BigDecimal("45.0"))
                        .categoryTotals(List.of(
                                CategoryEmissionSummary.builder()
                                        .category("SHOPPING")
                                        .carbonKg(new BigDecimal("27.0"))
                                        .percentageOfTotal(new BigDecimal("60.0"))
                                        .build()))
                        .topActivities(List.of(
                                TopEmissionActivity.builder()
                                        .merchant("Ashoka")
                                        .carbonKg(new BigDecimal("27.0"))
                                        .build()))
                        .build())
                .build();

        String result = builder.build(insight);

        assertThat(result).contains("act:5");
        assertThat(result).contains("kg:45.0");
        assertThat(result).contains("cat:SHOPPING");
        assertThat(result).contains("merchant:Ashoka");
    }

    @Test
    void build_withNullAnalytics_skipsAnalyticsSection() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .analytics(null)
                .build();

        String result = builder.build(insight);

        assertThat(result).doesNotContain("#DATA");
    }

    // ── Achievements section ──────────────────────────────────────────────

    @Test
    void build_withPopulatedAchievements_includesAchievementsSection() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .achievements(List.of("Below 100 kg threshold", "Low average per activity"))
                .build();

        String result = builder.build(insight);

        assertThat(result).contains("#ACHV");
        assertThat(result).contains("Below 100 kg threshold");
        assertThat(result).contains("Low average per activity");
    }

    @Test
    void build_withEmptyAchievements_doesNotIncludeAchievementsSection() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .achievements(List.of())
                .build();

        String result = builder.build(insight);

        assertThat(result).doesNotContain("#ACHV");
    }

    @Test
    void build_withNullAchievements_doesNotIncludeAchievementsSection() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .achievements(null)
                .build();

        String result = builder.build(insight);

        assertThat(result).doesNotContain("#ACHV");
    }

    // ── Warnings section (mapped to Concerns) ────────────────────────────

    @Test
    void build_withPopulatedWarnings_includesConcernsSection() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .warnings(List.of("FUEL is highest", "Above average emissions"))
                .build();

        String result = builder.build(insight);

        assertThat(result).contains("#CONC");
        assertThat(result).contains("FUEL is highest");
        assertThat(result).contains("Above average emissions");
    }

    @Test
    void build_withEmptyWarnings_doesNotIncludeConcernsSection() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .warnings(List.of())
                .build();

        String result = builder.build(insight);

        assertThat(result).doesNotContain("#CONC");
    }

    // ── Recommendations section ───────────────────────────────────────────

    @Test
    void build_withPopulatedRecommendations_includesRecommendationsSection() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .recommendations(List.of("Switch to electric vehicles", "Use public transport"))
                .build();

        String result = builder.build(insight);

        assertThat(result).contains("#REC");
        assertThat(result).contains("Switch to electric vehicles");
        assertThat(result).contains("Use public transport");
    }

    @Test
    void build_withEmptyRecommendations_doesNotIncludeRecommendationsSection() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .recommendations(List.of())
                .build();

        String result = builder.build(insight);

        assertThat(result).doesNotContain("#REC");
    }

    // ── Output schema ─────────────────────────────────────────────────────

    @Test
    void build_containsReturnOnlyJsonInstruction() {
        String result = builder.build(null);

        assertThat(result).contains("#OUT");
    }

    @Test
    void build_containsJsonSchema() {
        String result = builder.build(null);

        assertThat(result).contains("\"summary\":");
        assertThat(result).contains("\"strengths\":");
        assertThat(result).contains("\"concerns\":");
        assertThat(result).contains("\"recommendations\":");
        assertThat(result).contains("\"weeklyChallenge\":");
        assertThat(result).contains("\"motivation\":");
        assertThat(result).doesNotContain("\"confidence\":");
    }

    // ── Full prompt with all data ─────────────────────────────────────────

    @Test
    void build_withPopulatedInsight_includesAllSections() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .analytics(CarbonAnalyticsResponse.builder()
                        .activityCount(5)
                        .totalCarbonKg(new BigDecimal("45.0"))
                        .build())
                .achievements(List.of("Below 100 kg threshold"))
                .warnings(List.of("FUEL is highest"))
                .recommendations(List.of("Switch to EV"))
                .build();

        String result = builder.build(insight);

        assertThat(result).contains("act:5");
        assertThat(result).contains("Below 100 kg threshold");
        assertThat(result).contains("FUEL is highest");
        assertThat(result).contains("Switch to EV");
    }

    @Test
    void build_alwaysReturnsSamePrompt() {
        CarbonInsightResponse insight = CarbonInsightResponse.builder()
                .analytics(CarbonAnalyticsResponse.builder()
                        .activityCount(3)
                        .totalCarbonKg(new BigDecimal("10.0"))
                        .build())
                .build();

        String first = builder.build(insight);
        String second = builder.build(insight);

        assertThat(first).isEqualTo(second);
    }
}
