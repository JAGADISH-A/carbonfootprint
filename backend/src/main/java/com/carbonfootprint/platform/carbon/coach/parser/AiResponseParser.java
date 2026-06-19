package com.carbonfootprint.platform.carbon.coach.parser;

import com.carbonfootprint.platform.carbon.coach.model.AICarbonCoachResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses AI provider responses into an {@link AICarbonCoachResponse}.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Extract content from chat completion envelopes (OpenAI-compatible)</li>
 *   <li>Handle markdown fences, mixed text, and nested JSON</li>
 *   <li>Map fields: summary, strengths, concerns, recommendations, weeklyChallenge, motivation</li>
 *   <li>Try alternate field names for resilience against model drift</li>
 *   <li>Log clearly when fields are missing or malformed</li>
 * </ul>
 *
 * <h3>Design</h3>
 * Stateless utility — all methods are static. Never throws; returns empty lists/strings on failure.
 */
@Slf4j
public class AiResponseParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private AiResponseParser() {
    }

    /**
     * Parses an AI response string into an {@link AICarbonCoachResponse}.
     *
     * <p>Handles three input formats:
     * <ol>
     *   <li>Chat completion envelope — extracts {@code choices[0].message.content}</li>
     *   <li>Raw JSON string — parses directly</li>
     *   <li>Markdown-wrapped JSON — strips fences then parses</li>
     * </ol>
     *
     * @param json           the raw response from the AI provider
     * @param aiGenerated    whether the response was AI-generated
     * @return the parsed response, or a safe empty response if parsing fails
     */
    public static AICarbonCoachResponse parse(String json, boolean aiGenerated) {
        if (json == null || json.isBlank()) {
            log.debug("AiResponseParser — empty input, returning empty response");
            return emptyResponse(aiGenerated);
        }

        String contentJson = extractContent(json);
        if (contentJson == null) {
            log.warn("AiResponseParser — could not extract content from response (length={})", json.length());
            return emptyResponse(aiGenerated);
        }

        try {
            JsonNode root = objectMapper.readTree(contentJson);

            String summary = getRequiredField(root, "summary");
            List<String> strengths = getStringList(root, "strengths", "strength");
            List<String> concerns = getStringList(root, "concerns", "concern");
            List<String> recommendations = getStringList(root, "recommendations", "recommendation");
            String weeklyChallenge = getRequiredField(root, "weeklyChallenge", "weekly_challenge", "challenge");
            String motivation = getRequiredField(root, "motivation", "encouragement", "closing");
            int confidence = getIntField(root, "confidence");

            if (summary.isBlank()) {
                log.warn("AiResponseParser — summary field is blank in valid JSON. Keys present: {}", fieldNames(root));
            }

            return AICarbonCoachResponse.builder()
                    .summary(summary)
                    .strengths(strengths)
                    .concerns(concerns)
                    .recommendations(recommendations)
                    .weeklyChallenge(weeklyChallenge)
                    .motivation(motivation)
                    .confidence(confidence)
                    .aiGenerated(aiGenerated)
                    .build();
        } catch (Exception e) {
            log.warn("AiResponseParser — failed to parse JSON: {} | input starts with: {}",
                    e.getMessage(), truncate(contentJson, 80));
            return emptyResponse(aiGenerated);
        }
    }

    // ── Content extraction ────────────────────────────────────────────────

    /**
     * Extracts the JSON content string from the AI response.
     * Tries strategies in order:
     * <ol>
     *   <li>Parse as chat completion envelope → extract choices[0].message.content</li>
     *   <li>Strip markdown fences, then validate</li>
     *   <li>Extract outermost {…} from mixed text</li>
     * </ol>
     * Each strategy is tried on the original input AND on envelope-extracted content,
     * so responses with text surrounding JSON are handled.
     */
    private static String extractContent(String raw) {
        String trimmed = raw.trim();

        // Strategy 1: chat completion envelope
        String envelopeContent = extractFromEnvelope(trimmed);
        if (envelopeContent != null) {
            // Envelope content may itself be valid JSON — return immediately
            if (isValidJson(envelopeContent)) {
                return envelopeContent;
            }
            // Content has text around JSON — try to extract the JSON object
            String extracted = extractJsonObject(envelopeContent);
            if (extracted != null && isValidJson(extracted)) {
                return extracted;
            }
            log.debug("AiResponseParser — envelope content not parseable ({} chars): {}",
                    envelopeContent.length(), truncate(envelopeContent, 200));
        }

        // Strategy 2: direct JSON parse
        if (isValidJson(trimmed)) {
            return trimmed;
        }

        // Strategy 3: strip markdown fences from raw input, then validate
        String fencesStripped = stripMarkdownFences(trimmed);
        if (fencesStripped != trimmed && isValidJson(fencesStripped)) {
            return fencesStripped;
        }

        // Strategy 4: extract outermost {…} from mixed text
        String fromMixed = extractJsonObject(trimmed);
        if (fromMixed != null && isValidJson(fromMixed)) {
            return fromMixed;
        }

        return null;
    }

    /**
     * Checks whether a string is valid JSON (object or array).
     */
    private static boolean isValidJson(String s) {
        if (s == null || s.isBlank()) return false;
        try {
            objectMapper.readTree(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extracts content from an OpenAI-compatible chat completion envelope.
     * Handles both {@code choices[0].message.content} and raw content strings.
     * Returns the raw content string (may contain markdown fences or surrounding text).
     */
    private static String extractFromEnvelope(String trimmed) {
        if (!trimmed.startsWith("{")) return null;

        JsonNode root;
        try {
            root = objectMapper.readTree(trimmed);
        } catch (Exception e) {
            return null;
        }

        // Check for error block
        if (root.has("error")) {
            String errMsg = root.path("error").path("message").asText("unknown");
            log.warn("AiResponseParser — API error in response: {}", errMsg);
            return null;
        }

        // Try choices[0].message.content
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (contentNode.isTextual()) {
            String content = contentNode.asText("").trim();
            if (!content.isEmpty()) {
                return content;
            }
        }

        return null;
    }

    /**
     * Finds and returns the outermost JSON object {@code {...}} in mixed text.
     * Tracks brace depth to handle nested objects correctly.
     */
    private static String extractJsonObject(String text) {
        int start = text.indexOf('{');
        if (start < 0) return null;

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\' && inString) {
                escaped = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (inString) continue;

            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    // ── Markdown handling ─────────────────────────────────────────────────

    /**
     * Strips markdown code fences from around a JSON string.
     * Handles {@code ```json}, {@code ```}, and leading/trailing whitespace.
     * If fences are not at the start/end, returns the original string unchanged.
     */
    private static String stripMarkdownFences(String text) {
        text = text.trim();

        // Strip opening fence
        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }

        // Strip closing fence
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }

        return text.trim();
    }

    // ── Field extraction ──────────────────────────────────────────────────

    /**
     * Gets a required string field, trying alternate names.
     * Returns empty string if none found; logs a warning.
     */
    private static String getRequiredField(JsonNode root, String... fieldNames) {
        for (String name : fieldNames) {
            JsonNode node = root.get(name);
            if (node != null && !node.isNull()) {
                String value = node.asText("").trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        log.warn("AiResponseParser — required field not found: {} | available: {}",
                fieldNames[0], fieldNames(root));
        return "";
    }

    /**
     * Gets a string field, returns empty string if missing/null.
     */
    private static String getField(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return "";
        }
        return node.asText("").trim();
    }

    /**
     * Gets an integer field, returns 0 if missing/null/out-of-range.
     * Clamps to 0-100 range.
     */
    private static int getIntField(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull() || !node.isNumber()) {
            return 0;
        }
        int val = node.asInt(0);
        return Math.max(0, Math.min(100, val));
    }

    /**
     * Gets a list of strings, trying alternate field names.
     */
    private static List<String> getStringList(JsonNode root, String... fieldNames) {
        for (String name : fieldNames) {
            JsonNode node = root.get(name);
            if (node != null && node.isArray()) {
                return parseStringArray(node);
            }
        }
        return List.of();
    }

    private static List<String> parseStringArray(JsonNode arrayNode) {
        List<String> result = new ArrayList<>();
        for (JsonNode item : arrayNode) {
            if (item != null && !item.isNull() && item.isTextual()) {
                String text = item.asText("").trim();
                if (!text.isEmpty()) {
                    result.add(text);
                }
            }
        }
        return result;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static List<String> fieldNames(JsonNode root) {
        List<String> names = new ArrayList<>();
        if (root != null && root.isObject()) {
            root.fieldNames().forEachRemaining(names::add);
        }
        return names;
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "null";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }

    private static AICarbonCoachResponse emptyResponse(boolean aiGenerated) {
        return AICarbonCoachResponse.builder()
                .summary("")
                .strengths(List.of())
                .concerns(List.of())
                .recommendations(List.of())
                .weeklyChallenge("")
                .motivation("")
                .confidence(0)
                .aiGenerated(aiGenerated)
                .build();
    }
}
