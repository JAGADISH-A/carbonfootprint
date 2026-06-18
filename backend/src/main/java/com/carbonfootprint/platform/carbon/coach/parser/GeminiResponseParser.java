package com.carbonfootprint.platform.carbon.coach.parser;

import com.carbonfootprint.platform.carbon.coach.model.AICarbonCoachResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses Gemini's JSON response into an {@link AICarbonCoachResponse}.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Parse JSON string from Gemini</li>
 *   <li>Map fields: summary, strengths, concerns, recommendations, weeklyChallenge, motivation</li>
 *   <li>Handle malformed JSON gracefully (return empty response)</li>
 *   <li>Handle missing or null fields gracefully</li>
 * </ul>
 *
 * <h3>Design</h3>
 * Stateless utility — all methods are static. Never throws; returns empty lists/strings on failure.
 */
@Slf4j
public class GeminiResponseParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private GeminiResponseParser() {
    }

    /**
     * Parses a JSON string into an {@link AICarbonCoachResponse}.
     *
     * @param json           the raw JSON response from Gemini
     * @param aiGenerated    whether the response was AI-generated (always true when called from service)
     * @return the parsed response, or a safe empty response if parsing fails
     */
    public static AICarbonCoachResponse parse(String json, boolean aiGenerated) {
        if (json == null || json.isBlank()) {
            log.debug("GeminiResponseParser — empty input, returning empty response");
            return emptyResponse(aiGenerated);
        }

        try {
            String cleaned = stripMarkdownFences(json.trim());
            JsonNode root = objectMapper.readTree(cleaned);

            String summary = getField(root, "summary");
            List<String> strengths = getStringList(root, "strengths");
            List<String> concerns = getStringList(root, "concerns");
            List<String> recommendations = getStringList(root, "recommendations");
            String weeklyChallenge = getField(root, "weeklyChallenge");
            String motivation = getField(root, "motivation");

            return AICarbonCoachResponse.builder()
                    .summary(summary)
                    .strengths(strengths)
                    .concerns(concerns)
                    .recommendations(recommendations)
                    .weeklyChallenge(weeklyChallenge)
                    .motivation(motivation)
                    .aiGenerated(aiGenerated)
                    .build();
        } catch (Exception e) {
            log.warn("GeminiResponseParser — failed to parse JSON: {}", e.getMessage());
            return emptyResponse(aiGenerated);
        }
    }

    /**
     * Strips markdown code fences (```json ... ```) from the response.
     */
    private static String stripMarkdownFences(String text) {
        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        return text.trim();
    }

    private static String getField(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return "";
        }
        return node.asText("");
    }

    private static List<String> getStringList(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || !node.isArray()) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        for (JsonNode item : node) {
            if (item != null && !item.isNull() && item.isTextual()) {
                String text = item.asText("");
                if (!text.isBlank()) {
                    result.add(text);
                }
            }
        }
        return result;
    }

    private static AICarbonCoachResponse emptyResponse(boolean aiGenerated) {
        return AICarbonCoachResponse.builder()
                .summary("")
                .strengths(List.of())
                .concerns(List.of())
                .recommendations(List.of())
                .weeklyChallenge("")
                .motivation("")
                .aiGenerated(aiGenerated)
                .build();
    }
}
