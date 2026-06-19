package com.carbonfootprint.platform.carbon.coach.parser;

import com.carbonfootprint.platform.carbon.coach.model.AICarbonCoachResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiResponseParserTest {

    // ── Pure JSON ─────────────────────────────────────────────────────────

    @Test
    void parse_pureJson_parsesAllFields() {
        String json = """
                {
                  "summary": "Your footprint is 45 kg CO2e.",
                  "strengths": ["Used public transport"],
                  "concerns": ["High car usage"],
                  "recommendations": ["Try cycling"],
                  "weeklyChallenge": "Walk to work once",
                  "motivation": "Great progress!"
                }
                """;

        AICarbonCoachResponse response = AiResponseParser.parse(json, true);

        assertThat(response.getSummary()).isEqualTo("Your footprint is 45 kg CO2e.");
        assertThat(response.getStrengths()).containsExactly("Used public transport");
        assertThat(response.getConcerns()).containsExactly("High car usage");
        assertThat(response.getRecommendations()).containsExactly("Try cycling");
        assertThat(response.getWeeklyChallenge()).isEqualTo("Walk to work once");
        assertThat(response.getMotivation()).isEqualTo("Great progress!");
        assertThat(response.isAiGenerated()).isTrue();
    }

    // ── Markdown-fenced JSON ──────────────────────────────────────────────

    @Test
    void parse_markdownFencedJson_parsesCorrectly() {
        String json = """
                ```json
                {
                  "summary": "Fenced response",
                  "strengths": ["S1"],
                  "concerns": [],
                  "recommendations": [],
                  "weeklyChallenge": "C",
                  "motivation": "M"
                }
                ```
                """;

        AICarbonCoachResponse response = AiResponseParser.parse(json, true);

        assertThat(response.getSummary()).isEqualTo("Fenced response");
        assertThat(response.isAiGenerated()).isTrue();
    }

    @Test
    void parse_markdownFenceWithoutLang_parsesCorrectly() {
        String json = """
                ```
                {
                  "summary": "Fenced without lang",
                  "strengths": [],
                  "concerns": [],
                  "recommendations": [],
                  "weeklyChallenge": "C",
                  "motivation": "M"
                }
                ```
                """;

        AICarbonCoachResponse response = AiResponseParser.parse(json, true);

        assertThat(response.getSummary()).isEqualTo("Fenced without lang");
    }

    // ── Chat completion envelope ──────────────────────────────────────────

    @Test
    void parse_chatCompletionEnvelope_extractsContent() {
        String envelope = """
                {
                  "id": "chatcmpl-123",
                  "object": "chat.completion",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "{\\"summary\\": \\"Envelope content\\", \\"strengths\\": [], \\"concerns\\": [], \\"recommendations\\": [], \\"weeklyChallenge\\": \\"C\\", \\"motivation\\": \\"M\\"}"
                      }
                    }
                  ]
                }
                """;

        AICarbonCoachResponse response = AiResponseParser.parse(envelope, true);

        assertThat(response.getSummary()).isEqualTo("Envelope content");
        assertThat(response.isAiGenerated()).isTrue();
    }

    @Test
    void parse_envelopeWithFencedContent_parsesCorrectly() {
        String envelope = """
                {
                  "id": "chatcmpl-456",
                  "choices": [
                    {
                      "message": {
                        "content": "```json\\n{\\"summary\\": \\"Fenced in envelope\\", \\"strengths\\": [], \\"concerns\\": [], \\"recommendations\\": [], \\"weeklyChallenge\\": \\"C\\", \\"motivation\\": \\"M\\"}\\n```"
                      }
                    }
                  ]
                }
                """;

        AICarbonCoachResponse response = AiResponseParser.parse(envelope, true);

        assertThat(response.getSummary()).isEqualTo("Fenced in envelope");
    }

    @Test
    void parse_envelopeWithTextBeforeJson_parsesCorrectly() {
        String envelope = """
                {
                  "id": "chatcmpl-789",
                  "choices": [
                    {
                      "message": {
                        "content": "Here is your carbon coaching analysis:\\n{\\"summary\\": \\"Text before JSON\\", \\"strengths\\": [], \\"concerns\\": [], \\"recommendations\\": [], \\"weeklyChallenge\\": \\"C\\", \\"motivation\\": \\"M\\"}\\n\\nHope this helps!"
                      }
                    }
                  ]
                }
                """;

        AICarbonCoachResponse response = AiResponseParser.parse(envelope, true);

        assertThat(response.getSummary()).isEqualTo("Text before JSON");
    }

    @Test
    void parse_envelopeWithFencedContentAndSurroundingText_parsesCorrectly() {
        String envelope = """
                {
                  "id": "chatcmpl-abc",
                  "choices": [
                    {
                      "message": {
                        "content": "Based on your data, here is my analysis:\\n```json\\n{\\"summary\\": \\"Fenced with surrounding text\\", \\"strengths\\": [], \\"concerns\\": [], \\"recommendations\\": [], \\"weeklyChallenge\\": \\"C\\", \\"motivation\\": \\"M\\"}\\n```\\n\\nLet me know if you have questions."
                      }
                    }
                  ]
                }
                """;

        AICarbonCoachResponse response = AiResponseParser.parse(envelope, true);

        assertThat(response.getSummary()).isEqualTo("Fenced with surrounding text");
    }

    // ── Envelope error handling ───────────────────────────────────────────

    @Test
    void parse_envelopeWithErrorBlock_returnsEmpty() {
        String envelope = """
                {
                  "error": {
                    "message": "Rate limit exceeded",
                    "type": "rate_limit_error"
                  }
                }
                """;

        AICarbonCoachResponse response = AiResponseParser.parse(envelope, true);

        assertThat(response.getSummary()).isEmpty();
        assertThat(response.isAiGenerated()).isTrue();
    }

    @Test
    void parse_envelopeWithNullContent_returnsEmpty() {
        String envelope = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": null
                      }
                    }
                  ]
                }
                """;

        AICarbonCoachResponse response = AiResponseParser.parse(envelope, true);

        assertThat(response.getSummary()).isEmpty();
    }

    // ── Text before/after JSON ────────────────────────────────────────────

    @Test
    void parse_textBeforeAndAfterJson_extractsJson() {
        String text = """
                Here is your analysis based on the data:

                {
                  "summary": "Mixed text response",
                  "strengths": ["Good"],
                  "concerns": [],
                  "recommendations": [],
                  "weeklyChallenge": "C",
                  "motivation": "M"
                }

                Let me know if you need anything else.
                """;

        AICarbonCoachResponse response = AiResponseParser.parse(text, true);

        assertThat(response.getSummary()).isEqualTo("Mixed text response");
        assertThat(response.getStrengths()).containsExactly("Good");
    }

    @Test
    void parse_textBeforeFencedJson_extractsJson() {
        String text = """
                Here is your analysis:

                ```json
                {
                  "summary": "Fenced in mixed text",
                  "strengths": [],
                  "concerns": [],
                  "recommendations": [],
                  "weeklyChallenge": "C",
                  "motivation": "M"
                }
                ```

                Hope this helps!
                """;

        AICarbonCoachResponse response = AiResponseParser.parse(text, true);

        assertThat(response.getSummary()).isEqualTo("Fenced in mixed text");
    }

    // ── Field name alternates ─────────────────────────────────────────────

    @Test
    void parse_alternateFieldNames_parsesCorrectly() {
        String json = """
                {
                  "summary": "Alternate names",
                  "strength": ["S1"],
                  "concern": ["C1"],
                  "recommendation": ["R1"],
                  "weekly_challenge": "Challenge",
                  "encouragement": "Keep going!"
                }
                """;

        AICarbonCoachResponse response = AiResponseParser.parse(json, true);

        assertThat(response.getSummary()).isEqualTo("Alternate names");
        assertThat(response.getStrengths()).containsExactly("S1");
        assertThat(response.getConcerns()).containsExactly("C1");
        assertThat(response.getRecommendations()).containsExactly("R1");
        assertThat(response.getWeeklyChallenge()).isEqualTo("Challenge");
        assertThat(response.getMotivation()).isEqualTo("Keep going!");
    }

    // ── Empty/missing fields ──────────────────────────────────────────────

    @Test
    void parse_emptyArrays_returnsEmptyLists() {
        String json = """
                {
                  "summary": "Empty lists",
                  "strengths": [],
                  "concerns": [],
                  "recommendations": [],
                  "weeklyChallenge": "",
                  "motivation": ""
                }
                """;

        AICarbonCoachResponse response = AiResponseParser.parse(json, true);

        assertThat(response.getSummary()).isEqualTo("Empty lists");
        assertThat(response.getStrengths()).isEmpty();
        assertThat(response.getConcerns()).isEmpty();
        assertThat(response.getRecommendations()).isEmpty();
    }

    @Test
    void parse_missingOptionalFields_returnsEmptyLists() {
        String json = """
                {
                  "summary": "Only summary"
                }
                """;

        AICarbonCoachResponse response = AiResponseParser.parse(json, true);

        assertThat(response.getSummary()).isEqualTo("Only summary");
        assertThat(response.getStrengths()).isEmpty();
        assertThat(response.getConcerns()).isEmpty();
        assertThat(response.getRecommendations()).isEmpty();
        assertThat(response.getWeeklyChallenge()).isEmpty();
        assertThat(response.getMotivation()).isEmpty();
    }

    // ── Blank summary ─────────────────────────────────────────────────────

    @Test
    void parse_blankSummary_returnsBlankSummary() {
        String json = """
                {
                  "summary": "",
                  "strengths": ["S1"],
                  "concerns": [],
                  "recommendations": [],
                  "weeklyChallenge": "C",
                  "motivation": "M"
                }
                """;

        AICarbonCoachResponse response = AiResponseParser.parse(json, true);

        assertThat(response.getSummary()).isEmpty();
        assertThat(response.getStrengths()).containsExactly("S1");
        assertThat(response.isAiGenerated()).isTrue();
    }

    // ── Invalid JSON ──────────────────────────────────────────────────────

    @Test
    void parse_invalidJson_returnsEmpty() {
        AICarbonCoachResponse response = AiResponseParser.parse("not valid json {{{", true);

        assertThat(response.getSummary()).isEmpty();
        assertThat(response.isAiGenerated()).isTrue();
    }

    @Test
    void parse_emptyInput_returnsEmpty() {
        AICarbonCoachResponse response = AiResponseParser.parse("", true);

        assertThat(response.getSummary()).isEmpty();
    }

    @Test
    void parse_nullInput_returnsEmpty() {
        AICarbonCoachResponse response = AiResponseParser.parse(null, true);

        assertThat(response.getSummary()).isEmpty();
    }

    @Test
    void parse_blankInput_returnsEmpty() {
        AICarbonCoachResponse response = AiResponseParser.parse("   \n  \t  ", true);

        assertThat(response.getSummary()).isEmpty();
    }

    // ── Nested JSON in content ────────────────────────────────────────────

    @Test
    void parse_nestedJsonInEnvelope_parsesCorrectly() {
        String envelope = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"summary\\": \\"Nested\\", \\"strengths\\": [], \\"concerns\\": [], \\"recommendations\\": [], \\"weeklyChallenge\\": \\"C\\", \\"motivation\\": \\"M\\"}"
                      }
                    }
                  ]
                }
                """;

        AICarbonCoachResponse response = AiResponseParser.parse(envelope, true);

        assertThat(response.getSummary()).isEqualTo("Nested");
    }

    // ── Whitespace handling ───────────────────────────────────────────────

    @Test
    void parse_extraWhitespace_parsesCorrectly() {
        String json = """
                {
                    "summary"   :   "Whitespace handled"  ,
                    "strengths" :   [  "S1"  ]  ,
                    "concerns"  :   [  ]  ,
                    "recommendations" : [  ]  ,
                    "weeklyChallenge" :  "C"  ,
                    "motivation" :  "M"
                }
                """;

        AICarbonCoachResponse response = AiResponseParser.parse(json, true);

        assertThat(response.getSummary()).isEqualTo("Whitespace handled");
        assertThat(response.getStrengths()).containsExactly("S1");
    }

    // ── aiGenerated flag ──────────────────────────────────────────────────

    @Test
    void parse_aiGeneratedFalse_setsFlagCorrectly() {
        String json = """
                {
                  "summary": "Test",
                  "strengths": [],
                  "concerns": [],
                  "recommendations": [],
                  "weeklyChallenge": "C",
                  "motivation": "M"
                }
                """;

        AICarbonCoachResponse response = AiResponseParser.parse(json, false);

        assertThat(response.isAiGenerated()).isFalse();
    }

    // ── Before/after examples ─────────────────────────────────────────────

    /*
     * BEFORE (old parser — fails):
     *
     * Input: Chat completion envelope where content has text + JSON:
     * {
     *   "choices": [{
     *     "message": {
     *       "content": "Here is your analysis:\\n{\\"summary\\": \\"Text before JSON\\", ...}\\nHope this helps!"
     *     }
     *   }]
     * }
     *
     * extractFromEnvelope() → stripMarkdownFences(content) →
     *   "Here is your analysis:\\n{\\"summary\\": \\"Text before JSON\\", ...}\\nHope this helps!"
     *   (fences not at start/end, so stripMarkdownFences is a no-op)
     *
     * Back in extractContent(), Strategy 1 returns this string.
     * parse() calls objectMapper.readTree() → fails (not valid JSON) → falls back to empty response.
     *
     * AFTER (new parser — succeeds):
     *
     * extractFromEnvelope() → returns raw content string (no stripping yet)
     * extractContent() → isValidJson(envelopeContent) = false →
     *   extractJsonObject(envelopeContent) → finds {...} → isValidJson(extracted) = true →
     *   returns the extracted JSON object
     *
     * parse() → objectMapper.readTree() succeeds → all fields parsed correctly.
     */

    @Test
    void before_after_envelopeWithTextBeforeJson() {
        // This is the exact scenario that caused frequent fallbacks with the old parser
        String envelope = """
                {
                  "id": "chatcmpl-example",
                  "object": "chat.completion",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "Based on your recent activities, here is my analysis:\\n{\\"summary\\": \\"Your carbon footprint is trending downward this week.\\", \\"strengths\\": [\\"Consistent use of public transport\\"], \\"concerns\\": [\\"Weekend car trips still high\\"], \\"recommendations\\": [\\"Try carpooling on weekends\\"], \\"weeklyChallenge\\": \\"Replace one car trip with cycling\\", \\"motivation\\": \\"You are making great progress! Keep it up.\\"}\\n\\nLet me know if you would like more details."
                      }
                    }
                  ]
                }
                """;

        AICarbonCoachResponse response = AiResponseParser.parse(envelope, true);

        assertThat(response.getSummary()).isEqualTo("Your carbon footprint is trending downward this week.");
        assertThat(response.getStrengths()).containsExactly("Consistent use of public transport");
        assertThat(response.getConcerns()).containsExactly("Weekend car trips still high");
        assertThat(response.getRecommendations()).containsExactly("Try carpooling on weekends");
        assertThat(response.getWeeklyChallenge()).isEqualTo("Replace one car trip with cycling");
        assertThat(response.getMotivation()).isEqualTo("You are making great progress! Keep it up.");
        assertThat(response.isAiGenerated()).isTrue();
    }

    @Test
    void before_after_envelopeWithFencedContentAndText() {
        String envelope = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "Here is your weekly carbon report:\\n```json\\n{\\"summary\\": \\"Your weekly summary shows improvement.\\", \\"strengths\\": [\\"Bike to work 3 times\\"], \\"concerns\\": [], \\"recommendations\\": [\\"Keep cycling\\"], \\"weeklyChallenge\\": \\"Try vegetarian meals\\", \\"motivation\\": \\"Fantastic effort!\\"}\\n```\\n\\nKeep up the momentum!"
                      }
                    }
                  ]
                }
                """;

        AICarbonCoachResponse response = AiResponseParser.parse(envelope, true);

        assertThat(response.getSummary()).isEqualTo("Your weekly summary shows improvement.");
        assertThat(response.getStrengths()).containsExactly("Bike to work 3 times");
    }
}
