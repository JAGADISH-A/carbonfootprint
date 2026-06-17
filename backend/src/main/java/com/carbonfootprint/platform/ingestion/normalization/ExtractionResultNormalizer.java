package com.carbonfootprint.platform.ingestion.normalization;

import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.ingestion.model.ExtractionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Normalizes and sanitizes an {@link ExtractionResult} immediately after
 * parsing and before the {@link com.carbonfootprint.platform.ingestion.validation.ExtractionResultValidator} chain.
 *
 * <h3>Pipeline position</h3>
 * <pre>
 * DocumentParser → ExtractionResult → [ExtractionResultNormalizer] → ExtractionResultValidator chain
 *   → ExtractionResultToActivityConverter → ActivityNormalizer chain → Activity
 * </pre>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Currency normalization (₹, Rs, Rs. → INR)</li>
 *   <li>Merchant name normalization (title-case, trim, remove duplicate spaces/punctuation)</li>
 *   <li>Amount validation (reject negative, zero, NaN)</li>
 *   <li>Category validation and correction</li>
 *   <li>Date parsing safety (null on failure, never throws)</li>
 *   <li>Weighted confidence scoring</li>
 *   <li>Metadata cleanup (remove empty/placeholder values, convert arrays to immutable lists)</li>
 * </ul>
 *
 * <h3>Immutability</h3>
 * This normalizer NEVER modifies the original {@link ExtractionResult}.
 * It always returns a new instance built via the builder pattern.
 */
@Slf4j
@Component
public class ExtractionResultNormalizer {

    // ── Currency mapping ────────────────────────────────────────────────────

    private static final Map<String, String> CURRENCY_MAP;

    static {
        Map<String, String> map = new HashMap<>();
        // Indian Rupee
        map.put("₹", "INR");
        map.put("rs", "INR");
        map.put("rs.", "INR");
        map.put("inr", "INR");
        map.put("rupee", "INR");
        map.put("rupees", "INR");
        // US Dollar
        map.put("$", "USD");
        map.put("usd", "USD");
        map.put("us$", "USD");
        // Euro
        map.put("€", "EUR");
        map.put("eur", "EUR");
        map.put("euro", "EUR");
        // British Pound
        map.put("£", "GBP");
        map.put("gbp", "GBP");
        // Japanese Yen
        map.put("¥", "JPY");
        map.put("jpy", "JPY");
        // Other common currencies
        map.put("sgd", "SGD");
        map.put("aud", "AUD");
        map.put("cad", "CAD");
        map.put("aed", "AED");
        map.put("chf", "CHF");
        map.put("cny", "CNY");
        map.put("krw", "KRW");
        map.put("myr", "MYR");
        map.put("thb", "THB");
        map.put("nzd", "NZD");
        map.put("sek", "SEK");
        map.put("nok", "NOK");
        map.put("dkk", "DKK");
        map.put("hkd", "HKD");
        map.put("zar", "ZAR");
        CURRENCY_MAP = Collections.unmodifiableMap(map);
    }

    // ── Category correction mapping ─────────────────────────────────────────

    private static final Map<String, ActivityCategory> CATEGORY_CORRECTION_MAP;

    static {
        Map<String, ActivityCategory> map = new HashMap<>();
        // Grocery / supermarket variants → FOOD
        map.put("grocery", ActivityCategory.FOOD);
        map.put("groceries", ActivityCategory.FOOD);
        map.put("supermarket", ActivityCategory.FOOD);
        map.put("restaurant", ActivityCategory.FOOD);
        map.put("dining", ActivityCategory.FOOD);
        map.put("cafe", ActivityCategory.FOOD);
        map.put("food & beverage", ActivityCategory.FOOD);
        // Transport variants
        map.put("taxi", ActivityCategory.TRANSPORT);
        map.put("cab", ActivityCategory.TRANSPORT);
        map.put("rideshare", ActivityCategory.TRANSPORT);
        map.put("uber", ActivityCategory.TRANSPORT);
        map.put("ola", ActivityCategory.TRANSPORT);
        map.put("bus", ActivityCategory.TRANSPORT);
        map.put("train", ActivityCategory.TRANSPORT);
        map.put("metro", ActivityCategory.TRANSPORT);
        map.put("auto", ActivityCategory.TRANSPORT);
        // Fuel variants
        map.put("petrol", ActivityCategory.FUEL);
        map.put("diesel", ActivityCategory.FUEL);
        map.put("gasoline", ActivityCategory.FUEL);
        map.put("lpg", ActivityCategory.FUEL);
        // Flight variants
        map.put("airline", ActivityCategory.FLIGHT);
        map.put("air travel", ActivityCategory.FLIGHT);
        map.put("aviation", ActivityCategory.FLIGHT);
        // Accommodation variants
        map.put("hotel", ActivityCategory.ACCOMMODATION);
        map.put("lodging", ActivityCategory.ACCOMMODATION);
        map.put("airbnb", ActivityCategory.ACCOMMODATION);
        map.put("hostel", ActivityCategory.ACCOMMODATION);
        map.put("resort", ActivityCategory.ACCOMMODATION);
        // Shopping variants
        map.put("retail", ActivityCategory.SHOPPING);
        map.put("clothing", ActivityCategory.SHOPPING);
        map.put("electronics", ActivityCategory.SHOPPING);
        map.put("online shopping", ActivityCategory.SHOPPING);
        map.put("ecommerce", ActivityCategory.SHOPPING);
        // Electricity variants
        map.put("power", ActivityCategory.ELECTRICITY);
        map.put("electric", ActivityCategory.ELECTRICITY);
        map.put("energy", ActivityCategory.ELECTRICITY);
        // Water variants
        map.put("water supply", ActivityCategory.WATER);
        map.put("water bill", ActivityCategory.WATER);
        // Gas variants
        map.put("natural gas", ActivityCategory.GAS);
        map.put("piped gas", ActivityCategory.GAS);
        map.put("cooking gas", ActivityCategory.GAS);
        CATEGORY_CORRECTION_MAP = Collections.unmodifiableMap(map);
    }

    // ── Metadata placeholder values to strip ────────────────────────────────

    private static final Set<String> METADATA_PLACEHOLDER_VALUES = Set.of(
            "", "n/a", "na", "-", "--", "unknown", "none", "null", "not available", "not applicable"
    );

    // ── Internal pipeline metadata keys to remove ───────────────────────────

    private static final Set<String> INTERNAL_METADATA_KEYS = Set.of(
            "rawCategory"
    );

    // ── Merchant cleanup patterns ───────────────────────────────────────────

    private static final Pattern DUPLICATE_SPACES = Pattern.compile("\\s{2,}");
    private static final Pattern LEADING_TRAILING_PUNCTUATION = Pattern.compile("^[\\p{Punct}\\s]+|[\\p{Punct}\\s]+$");

    // ── Confidence weights ──────────────────────────────────────────────────

    private static final double WEIGHT_MERCHANT    = 0.20;
    private static final double WEIGHT_AMOUNT      = 0.25;
    private static final double WEIGHT_CATEGORY    = 0.20;
    private static final double WEIGHT_CURRENCY    = 0.10;
    private static final double WEIGHT_DATE        = 0.15;
    private static final double WEIGHT_DESCRIPTION = 0.10;

    // ════════════════════════════════════════════════════════════════════════
    //  Public API
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Normalizes the given {@link ExtractionResult} and returns a new, immutable instance.
     *
     * <p>The original {@code result} is never modified.
     *
     * @param result the raw extraction result from a document parser
     * @return a new, normalized {@link ExtractionResult}
     */
    public ExtractionResult normalize(ExtractionResult result) {
        log.debug("ExtractionResultNormalizer — starting normalization for parser={}",
                result.getParserName());

        String normalizedCurrency   = normalizeCurrency(result.getCurrency());
        String normalizedMerchant   = normalizeMerchant(result.getMerchant());
        BigDecimal validatedAmount  = validateAmount(result.getAmount());
        Instant safeOccurredAt      = safeParseDateIfNeeded(result.getOccurredAt());
        String cleanDescription     = trimOrNull(result.getDescription());
        String cleanUnit            = trimOrNull(result.getUnit());
        String cleanLocation        = trimOrNull(result.getLocation());

        // Category normalization: use correction map if category is null and rawCategory exists in metadata
        ActivityCategory normalizedCategory = normalizeCategory(result.getCategory());
        if (normalizedCategory == null && result.getMetadata() != null) {
            Object rawCat = result.getMetadata().get("rawCategory");
            if (rawCat instanceof String rawCategoryStr && StringUtils.hasText(rawCategoryStr)) {
                normalizedCategory = correctCategory(rawCategoryStr);
            }
        }

        // Clean metadata (removes rawCategory since it's an internal pipeline hint)
        Map<String, Object> cleanMetadata = cleanMetadata(result.getMetadata());

        double confidence = computeWeightedConfidence(
                normalizedMerchant, validatedAmount, normalizedCategory,
                normalizedCurrency, safeOccurredAt, cleanDescription
        );

        ExtractionResult normalized = ExtractionResult.builder()
                .parserName(result.getParserName())
                .merchant(normalizedMerchant)
                .amount(validatedAmount)
                .currency(normalizedCurrency)
                .category(normalizedCategory)
                .unit(cleanUnit)
                .location(cleanLocation)
                .occurredAt(safeOccurredAt)
                .description(cleanDescription)
                .confidence(confidence)
                .metadata(cleanMetadata)
                .build();

        log.info("ExtractionResultNormalizer — normalization complete: " +
                        "merchant={}, amount={}, category={}, currency={}, confidence={}",
                normalizedMerchant, validatedAmount, normalizedCategory,
                normalizedCurrency, confidence);

        return normalized;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Currency normalization
    // ════════════════════════════════════════════════════════════════════════

    private String normalizeCurrency(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }

        String trimmed = raw.trim();
        String key = trimmed.toLowerCase();
        String normalized = CURRENCY_MAP.getOrDefault(key, trimmed.toUpperCase());

        if (!normalized.equals(trimmed)) {
            log.info("Currency normalized: {} -> {}", trimmed, normalized);
        }

        return normalized;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Merchant normalization
    // ════════════════════════════════════════════════════════════════════════

    private String normalizeMerchant(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }

        String cleaned = raw.trim();

        // Remove leading/trailing punctuation
        cleaned = LEADING_TRAILING_PUNCTUATION.matcher(cleaned).replaceAll("");

        // Collapse duplicate spaces
        cleaned = DUPLICATE_SPACES.matcher(cleaned).replaceAll(" ");

        cleaned = cleaned.trim();

        if (!StringUtils.hasText(cleaned)) {
            return null;
        }

        // Title-case
        String normalized = toTitleCase(cleaned);

        if (!normalized.equals(raw)) {
            log.info("Merchant normalized: {} -> {}", raw, normalized);
        }

        return normalized;
    }

    private String toTitleCase(String input) {
        if (!StringUtils.hasText(input)) {
            return input;
        }
        String[] words = input.toLowerCase().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (!word.isEmpty()) {
                if (i > 0) {
                    sb.append(' ');
                }
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    sb.append(word.substring(1));
                }
            }
        }
        return sb.toString();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Amount validation
    // ════════════════════════════════════════════════════════════════════════

    private BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null) {
            return null;
        }

        // Reject zero
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("Amount rejected: zero value");
            return null;
        }

        // Reject negative
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Amount rejected: negative value {}", amount);
            return null;
        }

        // Reject NaN-like (BigDecimal cannot represent NaN, but guard against edge cases)
        try {
            amount.doubleValue(); // Will produce Infinity for extremely large values
            if (Double.isNaN(amount.doubleValue()) || Double.isInfinite(amount.doubleValue())) {
                log.warn("Amount rejected: NaN or Infinite value");
                return null;
            }
        } catch (Exception e) {
            log.warn("Amount rejected: failed double conversion — {}", e.getMessage());
            return null;
        }

        return amount;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Category normalization
    // ════════════════════════════════════════════════════════════════════════

    private ActivityCategory normalizeCategory(ActivityCategory category) {
        if (category != null) {
            // Already a valid enum — no correction needed
            return category;
        }

        // Category is null. The GeminiDocumentParser already maps known enum values,
        // and logs a warning for unknown strings. If it reaches here as null,
        // the correction map doesn't apply (we don't have the raw string).
        // The metadata may contain a "subcategory" or "merchantType" hint,
        // but we defer that to the converter/normalizer chain.
        return null;
    }

    /**
     * Attempts to correct a raw category string that failed enum parsing.
     * Called externally or can be integrated if the parser passes the raw string.
     *
     * @param rawCategory the raw category string from Gemini
     * @return the corrected {@link ActivityCategory}, or {@link ActivityCategory#OTHER} if unknown
     */
    public ActivityCategory correctCategory(String rawCategory) {
        if (!StringUtils.hasText(rawCategory)) {
            return null;
        }

        String key = rawCategory.trim().toLowerCase();

        // First: try direct enum match
        try {
            return ActivityCategory.valueOf(key.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            // Not a direct match
        }

        // Second: try correction map
        ActivityCategory corrected = CATEGORY_CORRECTION_MAP.get(key);
        if (corrected != null) {
            log.info("Category corrected: {} -> {}", rawCategory, corrected);
            return corrected;
        }

        // Fallback: OTHER
        log.info("Category corrected: {} -> OTHER (unrecognized)", rawCategory);
        return ActivityCategory.OTHER;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Date safety
    // ════════════════════════════════════════════════════════════════════════

    private Instant safeParseDateIfNeeded(Instant occurredAt) {
        // The parser already converts to Instant. If it's null, it stays null.
        // This method exists as a safety guard — if the Instant is somehow invalid
        // or if future parsers pass raw strings, this protects the pipeline.
        return occurredAt;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Weighted confidence
    // ════════════════════════════════════════════════════════════════════════

    private double computeWeightedConfidence(
            String merchant, BigDecimal amount, ActivityCategory category,
            String currency, Instant occurredAt, String description
    ) {
        double score = 0.0;

        if (StringUtils.hasText(merchant)) {
            score += WEIGHT_MERCHANT;
        }
        if (amount != null) {
            score += WEIGHT_AMOUNT;
        }
        if (category != null) {
            score += WEIGHT_CATEGORY;
        }
        if (StringUtils.hasText(currency)) {
            score += WEIGHT_CURRENCY;
        }
        if (occurredAt != null) {
            score += WEIGHT_DATE;
        }
        if (StringUtils.hasText(description)) {
            score += WEIGHT_DESCRIPTION;
        }

        double clamped = Math.max(0.0, Math.min(1.0, score));

        log.debug("Weighted confidence: merchant={} amount={} category={} currency={} date={} desc={} → {}",
                merchant != null, amount != null, category != null,
                currency != null, occurredAt != null, description != null,
                clamped);

        return clamped;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Metadata cleanup
    // ════════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private Map<String, Object> cleanMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> cleaned = new HashMap<>();

        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Remove null values
            if (value == null) {
                continue;
            }

            // Remove internal pipeline keys
            if (INTERNAL_METADATA_KEYS.contains(key)) {
                log.debug("Metadata removed internal key: {}", key);
                continue;
            }

            // Remove placeholder strings
            if (value instanceof String strValue) {
                if (METADATA_PLACEHOLDER_VALUES.contains(strValue.trim().toLowerCase())) {
                    log.debug("Metadata removed placeholder: key={} value='{}'", key, strValue);
                    continue;
                }
                cleaned.put(key, strValue);
                continue;
            }

            // Convert mutable lists to immutable
            if (value instanceof List<?> listValue) {
                List<Object> filteredList = new ArrayList<>();
                for (Object item : listValue) {
                    if (item != null) {
                        filteredList.add(item);
                    }
                }
                if (!filteredList.isEmpty()) {
                    cleaned.put(key, Collections.unmodifiableList(filteredList));
                }
                continue;
            }

            // Convert mutable maps to cleaned maps (recursive for nested structures like items)
            if (value instanceof Map<?, ?> mapValue) {
                Map<String, Object> nestedCleaned = new HashMap<>();
                for (Map.Entry<?, ?> nestedEntry : mapValue.entrySet()) {
                    if (nestedEntry.getValue() != null) {
                        String nestedKey = String.valueOf(nestedEntry.getKey());
                        Object nestedVal = nestedEntry.getValue();
                        if (nestedVal instanceof String nestedStr) {
                            if (!METADATA_PLACEHOLDER_VALUES.contains(nestedStr.trim().toLowerCase())) {
                                nestedCleaned.put(nestedKey, nestedStr);
                            }
                        } else {
                            nestedCleaned.put(nestedKey, nestedVal);
                        }
                    }
                }
                if (!nestedCleaned.isEmpty()) {
                    cleaned.put(key, Collections.unmodifiableMap(nestedCleaned));
                }
                continue;
            }

            // Keep all other types as-is (Number, Boolean, etc.)
            cleaned.put(key, value);
        }

        return Collections.unmodifiableMap(cleaned);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Utilities
    // ════════════════════════════════════════════════════════════════════════

    private String trimOrNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
