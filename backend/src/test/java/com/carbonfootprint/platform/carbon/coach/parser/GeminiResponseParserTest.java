package com.carbonfootprint.platform.carbon.coach.parser;

import com.carbonfootprint.platform.carbon.coach.model.AICarbonCoachResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiResponseParserTest {

    // ── Valid JSON ────────────────────────────────────────────────────────

    @Test
    void parse_validJson_parsesAllFields() {
        String json = """
                {
                  "summary": "Great progress on reducing your footprint!",
                  "strengths": ["Low emissions this month", "Good use of public transport"],
                  "concerns": ["FUEL is your highest category"],
                  "recommendations": ["Consider an electric vehicle"],
                  "weeklyChallenge": "Walk to work once this week",
                  "motivation": "Every step counts!"
                }
                """;

        AICarbonCoachResponse response = GeminiResponseParser.parse(json, true);

        assertThat(response.getSummary()).isEqualTo("Great progress on reducing your footprint!");
        assertThat(response.getStrengths()).containsExactly("Low emissions this month", "Good use of public transport");
        assertThat(response.getConcerns()).containsExactly("FUEL is your highest category");
        assertThat(response.getRecommendations()).containsExactly("Consider an electric vehicle");
        assertThat(response.getWeeklyChallenge()).isEqualTo("Walk to work once this week");
        assertThat(response.getMotivation()).isEqualTo("Every step counts!");
        assertThat(response.isAiGenerated()).isTrue();
    }

    @Test
    void parse_validJson_setsAiGeneratedCorrectly() {
        String json = """
                {
                  "summary": "Summary",
                  "strengths": [],
                  "concerns": [],
                  "recommendations": [],
                  "weeklyChallenge": "Challenge",
                  "motivation": "Motivation"
                }
                """;

        AICarbonCoachResponse response = GeminiResponseParser.parse(json, false);

        assertThat(response.isAiGenerated()).isFalse();
    }

    // ── Markdown fences ───────────────────────────────────────────────────

    @Test
    void parse_jsonWithMarkdownFences_stripsAndParses() {
        String json = """
                ```json
                {
                  "summary": "Fenced JSON",
                  "strengths": ["S1"],
                  "concerns": [],
                  "recommendations": [],
                  "weeklyChallenge": "C",
                  "motivation": "M"
                }
                ```
                """;

        AICarbonCoachResponse response = GeminiResponseParser.parse(json, true);

        assertThat(response.getSummary()).isEqualTo("Fenced JSON");
        assertThat(response.getStrengths()).containsExactly("S1");
    }

    @Test
    void parse_jsonWithGenericFences_stripsAndParses() {
        String json = """
                ```
                {
                  "summary": "Generic fenced",
                  "strengths": [],
                  "concerns": [],
                  "recommendations": [],
                  "weeklyChallenge": "",
                  "motivation": ""
                }
                ```
                """;

        AICarbonCoachResponse response = GeminiResponseParser.parse(json, true);

        assertThat(response.getSummary()).isEqualTo("Generic fenced");
    }

    // ── Missing fields ────────────────────────────────────────────────────

    @Test
    void parse_missingSummary_emptyString() {
        String json = """
                {
                  "strengths": ["S1"],
                  "concerns": [],
                  "recommendations": [],
                  "weeklyChallenge": "",
                  "motivation": ""
                }
                """;

        AICarbonCoachResponse response = GeminiResponseParser.parse(json, true);

        assertThat(response.getSummary()).isEmpty();
    }

    @Test
    void parse_missingStrengths_emptyList() {
        String json = """
                {
                  "summary": "Summary",
                  "concerns": [],
                  "recommendations": [],
                  "weeklyChallenge": "",
                  "motivation": ""
                }
                """;

        AICarbonCoachResponse response = GeminiResponseParser.parse(json, true);

        assertThat(response.getStrengths()).isEmpty();
    }

    @Test
    void parse_missingConcerns_emptyList() {
        String json = """
                {
                  "summary": "Summary",
                  "strengths": [],
                  "recommendations": [],
                  "weeklyChallenge": "",
                  "motivation": ""
                }
                """;

        AICarbonCoachResponse response = GeminiResponseParser.parse(json, true);

        assertThat(response.getConcerns()).isEmpty();
    }

    @Test
    void parse_missingRecommendations_emptyList() {
        String json = """
                {
                  "summary": "Summary",
                  "strengths": [],
                  "concerns": [],
                  "weeklyChallenge": "",
                  "motivation": ""
                }
                """;

        AICarbonCoachResponse response = GeminiResponseParser.parse(json, true);

        assertThat(response.getRecommendations()).isEmpty();
    }

    @Test
    void parse_missingWeeklyChallenge_emptyString() {
        String json = """
                {
                  "summary": "Summary",
                  "strengths": [],
                  "concerns": [],
                  "recommendations": [],
                  "motivation": ""
                }
                """;

        AICarbonCoachResponse response = GeminiResponseParser.parse(json, true);

        assertThat(response.getWeeklyChallenge()).isEmpty();
    }

    @Test
    void parse_missingMotivation_emptyString() {
        String json = """
                {
                  "summary": "Summary",
                  "strengths": [],
                  "concerns": [],
                  "recommendations": [],
                  "weeklyChallenge": ""
                }
                """;

        AICarbonCoachResponse response = GeminiResponseParser.parse(json, true);

        assertThat(response.getMotivation()).isEmpty();
    }

    // ── Null values in lists ──────────────────────────────────────────────

    @Test
    void parse_nullItemsInList_skipsNulls() {
        String json = """
                {
                  "summary": "Summary",
                  "strengths": ["Valid", null, "Also valid"],
                  "concerns": [],
                  "recommendations": [],
                  "weeklyChallenge": "",
                  "motivation": ""
                }
                """;

        AICarbonCoachResponse response = GeminiResponseParser.parse(json, true);

        assertThat(response.getStrengths()).containsExactly("Valid", "Also valid");
    }

    @Test
    void parse_emptyStringItems_skipsBlanks() {
        String json = """
                {
                  "summary": "Summary",
                  "strengths": ["Valid", "   ", "Also valid"],
                  "concerns": [],
                  "recommendations": [],
                  "weeklyChallenge": "",
                  "motivation": ""
                }
                """;

        AICarbonCoachResponse response = GeminiResponseParser.parse(json, true);

        assertThat(response.getStrengths()).containsExactly("Valid", "Also valid");
    }

    @Test
    void parse_nonArrayField_returnsEmptyList() {
        String json = """
                {
                  "summary": "Summary",
                  "strengths": "not an array",
                  "concerns": [],
                  "recommendations": [],
                  "weeklyChallenge": "",
                  "motivation": ""
                }
                """;

        AICarbonCoachResponse response = GeminiResponseParser.parse(json, true);

        assertThat(response.getStrengths()).isEmpty();
    }

    // ── Malformed JSON ────────────────────────────────────────────────────

    @Test
    void parse_malformedJson_returnsEmptyResponse() {
        AICarbonCoachResponse response = GeminiResponseParser.parse("not valid json {{{", true);

        assertThat(response.getSummary()).isEmpty();
        assertThat(response.getStrengths()).isEmpty();
        assertThat(response.getConcerns()).isEmpty();
        assertThat(response.getRecommendations()).isEmpty();
        assertThat(response.getWeeklyChallenge()).isEmpty();
        assertThat(response.getMotivation()).isEmpty();
        assertThat(response.isAiGenerated()).isTrue();
    }

    @Test
    void parse_emptyString_returnsEmptyResponse() {
        AICarbonCoachResponse response = GeminiResponseParser.parse("", true);

        assertThat(response.getSummary()).isEmpty();
        assertThat(response.isAiGenerated()).isTrue();
    }

    @Test
    void parse_nullInput_returnsEmptyResponse() {
        AICarbonCoachResponse response = GeminiResponseParser.parse(null, false);

        assertThat(response.getSummary()).isEmpty();
        assertThat(response.getStrengths()).isEmpty();
        assertThat(response.isAiGenerated()).isFalse();
    }

    @Test
    void parse_blankInput_returnsEmptyResponse() {
        AICarbonCoachResponse response = GeminiResponseParser.parse("   ", true);

        assertThat(response.getSummary()).isEmpty();
        assertThat(response.isAiGenerated()).isTrue();
    }

    // ── Partial JSON ──────────────────────────────────────────────────────

    @Test
    void parse_partialJson_parsesWhatItCan() {
        String json = """
                {
                  "summary": "Partial data",
                  "strengths": ["One strength"]
                }
                """;

        AICarbonCoachResponse response = GeminiResponseParser.parse(json, true);

        assertThat(response.getSummary()).isEqualTo("Partial data");
        assertThat(response.getStrengths()).containsExactly("One strength");
        assertThat(response.getConcerns()).isEmpty();
        assertThat(response.getRecommendations()).isEmpty();
    }

    @Test
    void parse_emptyArrayFields_returnsEmptyLists() {
        String json = """
                {
                  "summary": "All empty lists",
                  "strengths": [],
                  "concerns": [],
                  "recommendations": [],
                  "weeklyChallenge": "",
                  "motivation": ""
                }
                """;

        AICarbonCoachResponse response = GeminiResponseParser.parse(json, true);

        assertThat(response.getStrengths()).isEmpty();
        assertThat(response.getConcerns()).isEmpty();
        assertThat(response.getRecommendations()).isEmpty();
    }

    // ── Extra fields ──────────────────────────────────────────────────────

    @Test
    void parse_extraFields_ignoresThemGracefully() {
        String json = """
                {
                  "summary": "Summary",
                  "strengths": ["S1"],
                  "concerns": [],
                  "recommendations": [],
                  "weeklyChallenge": "C",
                  "motivation": "M",
                  "extraField": "ignored",
                  "anotherExtra": 42
                }
                """;

        AICarbonCoachResponse response = GeminiResponseParser.parse(json, true);

        assertThat(response.getSummary()).isEqualTo("Summary");
        assertThat(response.getStrengths()).containsExactly("S1");
    }

    // ── Numbers in text fields ────────────────────────────────────────────

    @Test
    void parse_numbersInStrengths_returnsAsStrings() {
        String json = """
                {
                  "summary": "Your 45.00 kg footprint",
                  "strengths": ["Below 100 kg threshold", "Average 2.5 kg per activity"],
                  "concerns": [],
                  "recommendations": [],
                  "weeklyChallenge": "",
                  "motivation": ""
                }
                """;

        AICarbonCoachResponse response = GeminiResponseParser.parse(json, true);

        assertThat(response.getSummary()).contains("45.00 kg");
        assertThat(response.getStrengths()).contains("Below 100 kg threshold");
    }
}
