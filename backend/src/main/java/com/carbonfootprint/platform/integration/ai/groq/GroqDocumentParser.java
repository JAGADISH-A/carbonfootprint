package com.carbonfootprint.platform.integration.ai.groq;

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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Document parser implementation using the Groq Chat Completions API.
 *
 * <h3>Role</h3>
 * Implements {@link DocumentParser} — the only interface the application
 * core knows about. Business logic NEVER references this class directly.
 *
 * <h3>Responsibility</h3>
 * Sends the raw document text to Groq with a structured prompt asking
 * it to extract fields (merchant, amount, currency, category, etc.).
 * Returns an {@link ExtractionResult} that the normalisation layer will use.
 *
 * <h3>Selection</h3>
 * Activated when {@code ai.provider=groq}.
 * Only active outside the {@code stub} profile.
 *
 * <h3>Configuration</h3>
 * Enabled: {@code ai.provider=groq}.
 * API key: {@code carbon.groq.api-key} (from environment variable GROQ_API_KEY).
 * Receipt model: {@code carbon.groq.receipt-model} (default: openai/gpt-oss-20b).
 */
@Slf4j
@Component
@Profile("!stub")
@ConditionalOnProperty(name = "ai.provider", havingValue = "groq")
public class GroqDocumentParser implements DocumentParser {

    private final String apiKey;
    private final String receiptModel;
    private final GroqClient groqClient;
    private final ObjectMapper objectMapper;

    public GroqDocumentParser(
            @Value("${carbon.groq.api-key:}") String apiKey,
            @Value("${carbon.groq.receipt-model:openai/gpt-oss-20b}") String receiptModel,
            GroqClient groqClient,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey;
        this.receiptModel = receiptModel;
        this.groqClient = groqClient;
        this.objectMapper = objectMapper;
        log.info("GroqDocumentParser initialised — apiKeyPresent={} receiptModel={}",
                apiKey != null && !apiKey.isBlank(), receiptModel);
    }

    @Override
    public boolean supports(RawDocument document) {
        if (apiKey == null || apiKey.isBlank()) {
            return false;
        }
        return document.getRawText() != null && !document.getRawText().isBlank();
    }

    @Override
    public ExtractionResult parse(RawDocument document) throws IngestionException {
        log.info("GroqDocumentParser.parse() called — documentId={} source={}",
                document.getId(), document.getSource());

        String prompt = buildExtractionPrompt(document.getRawText(), document.getSource());

        // ── First attempt ────────────────────────────────────────────────
        String rawResponse = callGroq(prompt);
        String validatedJson = validateAndExtractContent(rawResponse);

        if (validatedJson == null) {
            // ── Retry once on malformed JSON ─────────────────────────────
            log.warn("Groq returned invalid JSON — retrying once for documentId={}", document.getId());
            rawResponse = callGroq(prompt);
            validatedJson = validateAndExtractContent(rawResponse);

            if (validatedJson == null) {
                throw new AiParsingException(
                        "Groq returned invalid JSON twice for documentId=" + document.getId()
                                + " — first 200 chars: " + truncate(rawResponse));
            }
        }

        // ── Deserialise validated JSON ──────────────────────────────────
        try {
            ExtractionResult result = mapGroqResponse(validatedJson, document);
            log.info("Groq extraction completed:\nmerchant={}\ncategory={}\namount={}\nconfidence={}",
                    result.getMerchant(), result.getCategory(), result.getAmount(), result.getConfidence());
            return result;
        } catch (AiParsingException e) {
            throw e;
        } catch (Exception e) {
            throw new IngestionException("Failed to map Groq response to ExtractionResult: " + e.getMessage(), e);
        }
    }

    /**
     * Calls the Groq API. Typed AI exceptions (AiQuotaExceededException,
     * AiTimeoutException, AiProviderException) propagate directly since they
     * already extend {@link IngestionException}.
     */
    private String callGroq(String prompt) throws IngestionException {
        return groqClient.generateContent(receiptModel, prompt);
    }

    /**
     * Validates the raw Groq response at two levels:
     * <ol>
     *   <li><strong>Outer JSON</strong> — the Groq ChatCompletion envelope must parse.</li>
     *   <li><strong>Inner JSON</strong> — the {@code choices[0].message.content} string must itself be valid JSON.</li>
     * </ol>
     *
     * <p>Returns the validated inner JSON string, or {@code null} if either level fails.
     *
     * @param responseJson the raw response from the Groq API
     * @return validated inner JSON content, or {@code null} if validation fails
     */
    private String validateAndExtractContent(String responseJson) {
        if (responseJson == null || responseJson.isBlank()) {
            log.warn("validateAndExtractContent: response is null or blank");
            return null;
        }

        // ── Level 1: outer JSON ──────────────────────────────────────────
        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(responseJson);
        } catch (Exception e) {
            log.warn("validateAndExtractContent: outer JSON parse failed — {}", e.getMessage());
            return null;
        }

        if (rootNode.has("error")) {
            String errMsg = rootNode.path("error").path("message").asText();
            log.warn("validateAndExtractContent: Groq API error block — {}", errMsg);
            return null;
        }

        // ── Extract content string ───────────────────────────────────────
        String contentText = rootNode.path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText(null);

        if (contentText == null || contentText.isBlank()) {
            log.warn("validateAndExtractContent: content field is null or blank");
            return null;
        }

        // ── Strip markdown fences ────────────────────────────────────────
        contentText = contentText.trim();
        if (contentText.startsWith("```json")) {
            contentText = contentText.substring(7);
        } else if (contentText.startsWith("```")) {
            contentText = contentText.substring(3);
        }
        if (contentText.endsWith("```")) {
            contentText = contentText.substring(0, contentText.length() - 3);
        }
        contentText = contentText.trim();

        // ── Level 2: inner JSON ──────────────────────────────────────────
        try {
            objectMapper.readTree(contentText);
        } catch (Exception e) {
            log.warn("validateAndExtractContent: inner JSON parse failed — {}", e.getMessage());
            return null;
        }

        return contentText;
    }

    private String buildExtractionPrompt(String rawText, ActivitySource source) {
        return """
                Extract from %s receipt. Return ONLY valid JSON:
                {"merchant":"str","merchantType":"Restaurant|Supermarket|Hotel|Fuel Station|Airline|Taxi|Retail|Hospital|Utility|Entertainment|Shopping Mall|Government|Other","amount":0,"currency":"INR|USD|EUR|GBP","taxAmount":0,"subtotal":0,"discount":0,"invoiceNumber":"str","paymentMethod":"cash|card|upi|netbanking","category":"ELECTRICITY|FOOD|FUEL|FLIGHT|SHOPPING|TRANSPORT|ACCOMMODATION|WATER|GAS|OTHER","subcategory":"str","unit":"str","quantity":0,"location":"str","city":"str","country":"str","occurredAt":"ISO8601","description":"str","items":[{"name":"str","quantity":1,"unitPrice":0,"totalPrice":0,"category":"str"}],"confidence":0.0}
                Use null for unknowns. amount=total paid(tax incl). subtotal=before tax. taxAmount=all taxes.
                Numeric rules: Every numeric field must be a final number. Never output arithmetic expressions (e.g. use 1440.48, never 1375.00 + 32.74 + 32.74). Compute all sums internally.
                Document:
                %s
                """.formatted(source, rawText);
    }

    /**
     * Maps already-validated inner JSON (from {@link #validateAndExtractContent})
     * to an {@link ExtractionResult}.
     *
     * <p>Pre-condition: {@code extractedJson} is guaranteed to be valid JSON
     * — no outer-envelope parsing or markdown stripping is needed.
     */
    private ExtractionResult mapGroqResponse(String extractedJson, RawDocument document) throws AiParsingException {
        JsonNode extractedNode;
        try {
            extractedNode = objectMapper.readTree(extractedJson);
        } catch (Exception e) {
            throw new AiParsingException("Failed to deserialise validated JSON: " + e.getMessage(), e);
        }

        String merchant = extractedNode.hasNonNull("merchant") ? extractedNode.get("merchant").asText() : null;

        BigDecimal amount = null;
        if (extractedNode.hasNonNull("amount")) {
            try {
                amount = new BigDecimal(extractedNode.get("amount").asText());
            } catch (NumberFormatException e) {
                log.warn("Failed to parse amount from Groq: {}", extractedNode.get("amount").asText());
            }
        }

        String currency = extractedNode.hasNonNull("currency") ? extractedNode.get("currency").asText() : null;

        ActivityCategory category = null;
        String rawCategoryStr = null;
        if (extractedNode.hasNonNull("category")) {
            String catStr = extractedNode.get("category").asText();
            try {
                category = ActivityCategory.valueOf(catStr.toUpperCase().trim());
            } catch (IllegalArgumentException e) {
                log.warn("Failed to map category '{}' to ActivityCategory enum — storing as rawCategory for normalizer", catStr);
                rawCategoryStr = catStr;
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
                    if (value.isArray() || value.isObject()) {
                        metadata.put(key, objectMapper.convertValue(value, Object.class));
                    } else if (value.isNumber()) {
                        metadata.put(key, value.numberValue());
                    } else if (value.isBoolean()) {
                        metadata.put(key, value.booleanValue());
                    } else {
                        metadata.put(key, value.asText());
                    }
                }
            }
        });

        if (rawCategoryStr != null) {
            metadata.put("rawCategory", rawCategoryStr);
        }

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
        // Use AI-provided confidence if present, otherwise calculate from field presence
        double confidence;
        if (extractedNode.hasNonNull("confidence")) {
            try {
                confidence = Math.max(0.0, Math.min(1.0, extractedNode.get("confidence").asDouble()));
            } catch (Exception e) {
                confidence = Math.min(1.0, coreFieldsExtracted * 0.25);
            }
        } else {
            confidence = Math.min(1.0, coreFieldsExtracted * 0.25);
        }

        return ExtractionResult.builder()
                .parserName("GroqDocumentParser")
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

    private static String truncate(String s) {
        if (s == null) return "null";
        return s.length() <= 200 ? s : s.substring(0, 200) + "...";
    }
}
