package com.carbonfootprint.platform.integration.ai.gemini;

import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.activity.model.ActivitySource;
import com.carbonfootprint.platform.document.model.RawDocument;
import com.carbonfootprint.platform.ingestion.model.ExtractionResult;
import com.carbonfootprint.platform.ingestion.port.out.DocumentParser;
import com.carbonfootprint.platform.platform.exception.IngestionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Document parser implementation using the Google Gemini API.
 *
 * <h3>Role</h3>
 * Implements {@link DocumentParser} — the only interface the application
 * core knows about. Business logic NEVER references this class directly.
 *
 * <h3>Responsibility</h3>
 * Sends the raw document text to Gemini with a structured prompt asking
 * asking it to extract fields (merchant, amount, currency, category, etc.).
 * Returns an {@link ExtractionResult} that the normalisation layer will use.
 *
 * <h3>Prompt strategy</h3>
 * The extraction prompt is NOT hardcoded — it will be loaded from Firestore
 * configuration collection to allow updates without redeployment.
 *
 * <h3>Configuration</h3>
 * API key: {@code carbon.gemini.api-key} (from environment variable GEMINI_API_KEY).
 * Model: {@code carbon.gemini.model} (default: gemini-2.0-flash).
 *
 * <p>TODO (Phase 1): Implement real Gemini API call.
 * <ol>
 *   <li>Design structured JSON output schema for activity extraction.</li>
 *   <li>Build prompt template (load from Firestore config collection).</li>
 *   <li>Implement HTTP call to Gemini REST API or SDK.</li>
 *   <li>Map JSON response to {@link ExtractionResult} fields.</li>
 *   <li>Handle partial responses (low-confidence fields → null, not guessed).</li>
 * </ol>
 */
@Slf4j
@Component
@Profile("!stub")
public class GeminiDocumentParser implements DocumentParser {

    private final String geminiModel;
    private final String geminiApiKey;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiDocumentParser(
            @Value("${carbon.gemini.model:gemini-2.0-flash}") String geminiModel,
            @Value("${carbon.gemini.api-key:}") String geminiApiKey,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper
    ) {
        this.geminiModel = geminiModel;
        this.geminiApiKey = geminiApiKey;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
        log.info("GeminiDocumentParser initialised with model={}", geminiModel);
    }

    @Override
    public boolean supports(RawDocument document) {
        // Gemini parser is the default — supports all sources with non-empty text
        return document.getRawText() != null && !document.getRawText().isBlank();
    }

    @Override
    public ExtractionResult parse(RawDocument document) throws IngestionException {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new IngestionException("Gemini API key is not configured.");
        }

        log.info("GeminiDocumentParser.parse() called — documentId={} source={} model={}",
                document.getId(), document.getSource(), geminiModel);

        String prompt = buildExtractionPrompt(document.getRawText(), document.getSource());
        
        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> generationConfig = Map.of("responseMimeType", "application/json");
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(content),
            "generationConfig", generationConfig
        );

        String rawResponseBody;
        try {
            log.info("Calling Gemini API...");
            rawResponseBody = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/" + geminiModel + ":generateContent")
                            .queryParam("key", geminiApiKey)
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            throw new IngestionException("Gemini API returned HTTP error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new IngestionException("Connection failure or timeout when calling Gemini: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IngestionException("Unexpected error calling Gemini: " + e.getMessage(), e);
        }

        if (rawResponseBody == null || rawResponseBody.isBlank()) {
            throw new IngestionException("Gemini API returned empty response");
        }

        try {
            ExtractionResult result = mapGeminiResponse(rawResponseBody, document);
            log.info("Gemini extraction completed:\nmerchant={}\ncategory={}\namount={}\nconfidence={}",
                    result.getMerchant(), result.getCategory(), result.getAmount(), result.getConfidence());
            return result;
        } catch (Exception e) {
            throw new IngestionException("Failed to map Gemini response to ExtractionResult: " + e.getMessage(), e);
        }
    }

    private String buildExtractionPrompt(String rawText, ActivitySource source) {
        return """
                Extract the following fields from the document text below.
                
                The fields should be:
                - merchant: String or null (inferred merchant or vendor name)
                - amount: Number or null (numeric transaction amount, do not include currency symbols)
                - currency: String or null (raw currency symbol/abbreviation like Rs, INR, USD, $)
                - unit: String or null (physical quantity unit, e.g. KWH, L, kg)
                - category: String or null (MUST be one of: ELECTRICITY, FOOD, FUEL, FLIGHT, SHOPPING, TRANSPORT, ACCOMMODATION, GAS, WATER, OTHER)
                - location: String or null (physical location/address)
                - occurredAt: String or null (ISO-8601 UTC format, e.g. 2026-06-17T14:19:30Z)
                - description: String or null (brief description of the transaction/activity)
                
                Constraints:
                1. Return ONLY valid JSON.
                2. Do NOT wrap the response inside Markdown.
                3. Do NOT use ```json or ``` code blocks.
                4. Do NOT explain anything.
                5. Do NOT add comments.
                6. If a field cannot be determined, set its value to null. Do NOT guess.
                7. Category MUST be one of the supported ActivityCategory values listed above or null.
                
                Document source type: %s
                
                Document text:
                %s
                """.formatted(source, rawText);
    }

    private ExtractionResult mapGeminiResponse(String responseJson, RawDocument document) throws Exception {
        JsonNode rootNode = objectMapper.readTree(responseJson);
        
        if (rootNode.has("error")) {
            String errMsg = rootNode.path("error").path("message").asText();
            throw new IngestionException("Gemini API returned error block: " + errMsg);
        }
        
        String textJson = rootNode.path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text")
                .asText();

        if (textJson == null || textJson.isBlank()) {
            log.warn("Gemini returned empty text response");
            return ExtractionResult.empty("GeminiDocumentParser");
        }

        textJson = textJson.trim();
        if (textJson.startsWith("```json")) {
            textJson = textJson.substring(7);
        } else if (textJson.startsWith("```")) {
            textJson = textJson.substring(3);
        }
        if (textJson.endsWith("```")) {
            textJson = textJson.substring(0, textJson.length() - 3);
        }
        textJson = textJson.trim();

        JsonNode extractedNode = objectMapper.readTree(textJson);

        String merchant = extractedNode.hasNonNull("merchant") ? extractedNode.get("merchant").asText() : null;

        BigDecimal amount = null;
        if (extractedNode.hasNonNull("amount")) {
            try {
                amount = new BigDecimal(extractedNode.get("amount").asText());
            } catch (NumberFormatException e) {
                log.warn("Failed to parse amount from Gemini: {}", extractedNode.get("amount").asText());
            }
        }

        String currency = extractedNode.hasNonNull("currency") ? extractedNode.get("currency").asText() : null;

        ActivityCategory category = null;
        if (extractedNode.hasNonNull("category")) {
            String catStr = extractedNode.get("category").asText();
            try {
                category = ActivityCategory.valueOf(catStr.toUpperCase().trim());
            } catch (IllegalArgumentException e) {
                log.warn("Failed to map category '{}' to ActivityCategory enum", catStr);
            }
        }

        String unit = extractedNode.hasNonNull("unit") ? extractedNode.get("unit").asText() : null;
        String location = extractedNode.hasNonNull("location") ? extractedNode.get("location").asText() : null;

        Instant occurredAt = null;
        if (extractedNode.hasNonNull("occurredAt")) {
            String dateStr = extractedNode.get("occurredAt").asText();
            try {
                occurredAt = Instant.parse(dateStr);
            } catch (Exception e) {
                log.warn("Failed to parse occurredAt '{}' as Instant", dateStr);
            }
        }

        String description = extractedNode.hasNonNull("description") ? extractedNode.get("description").asText() : null;

        Set<String> excludedKeys = Set.of(
                "merchant", "amount", "currency", "category", "unit", "location", "occurredAt", "description"
        );

        Map<String, Object> metadata = new HashMap<>();
        extractedNode.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            if (!excludedKeys.contains(key)) {
                JsonNode value = entry.getValue();
                if (!value.isNull()) {
                    if (value.isNumber()) {
                        metadata.put(key, value.numberValue());
                    } else if (value.isBoolean()) {
                        metadata.put(key, value.booleanValue());
                    } else {
                        metadata.put(key, value.asText());
                    }
                }
            }
        });

        int coreFieldsExtracted = 0;
        if (merchant != null && !merchant.isBlank()) {
            coreFieldsExtracted++;
        }
        if (amount != null) {
            coreFieldsExtracted++;
        }
        if (category != null) {
            coreFieldsExtracted++;
        }
        if (occurredAt != null) {
            coreFieldsExtracted++;
        }
        double confidence = Math.min(1.0, coreFieldsExtracted * 0.25);

        return ExtractionResult.builder()
                .parserName("GeminiDocumentParser")
                .merchant(merchant)
                .amount(amount)
                .currency(currency)
                .category(category)
                .unit(unit)
                .location(location)
                .occurredAt(occurredAt)
                .description(description)
                .confidence(confidence)
                .metadata(metadata)
                .build();
    }
}
