package com.carbonfootprint.platform.ingestion.enrichment;

import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.ingestion.model.ExtractionResult;
import lombok.Value;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable, pre-computed context passed to every {@link CarbonHintProvider}.
 *
 * <h3>Purpose</h3>
 * Eliminates the duplicated corpus-building logic that previously existed in every
 * provider. The {@link CarbonHintEngine} builds a single {@code CarbonHintContext}
 * from the normalized {@link ExtractionResult} and passes it to all providers — the
 * search corpus is constructed once, lower-cased once, and metadata is traversed once.
 *
 * <h3>Thread safety</h3>
 * This class is immutable ({@code @Value}) and thread-safe by construction.
 * All string fields are pre-lowercased and can be compared directly without
 * additional allocations.
 *
 * <h3>Convenience methods</h3>
 * Providers use the lookup methods ({@link #contains}, {@link #merchantContains},
 * {@link #anyMatch}, {@link #findFirstMatch}) instead of raw string operations,
 * keeping provider code focused on inference logic.
 *
 * @see CarbonHintEngine#computeHints(ExtractionResult)
 * @see CarbonHintProvider#provide(CarbonHintContext)
 */
@Value
public class CarbonHintContext {

    /**
     * The original normalized extraction result. Providers can access any field
     * on this object, but should prefer the pre-computed fields below for
     * string comparisons.
     */
    ExtractionResult extractionResult;

    /**
     * Full lower-case searchable corpus built from merchant, description, unit,
     * and all string values in metadata (including nested lists and maps).
     * Never null — empty string when no text is available.
     */
    String corpus;

    /**
     * Lower-case merchant name, or empty string if merchant is null/blank.
     */
    String merchant;

    /**
     * Lower-case description, or empty string if description is null/blank.
     */
    String description;

    /**
     * Lower-case unit, or empty string if unit is null/blank.
     */
    String unit;

    /**
     * The extraction result's category, or {@code null} if not set.
     */
    ActivityCategory category;

    // ════════════════════════════════════════════════════════════════════════
    //  Lookup methods
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Returns {@code true} if the full corpus contains the given keyword.
     *
     * @param keyword lower-case keyword to search for
     * @return true if found in the corpus
     */
    public boolean contains(String keyword) {
        return corpus.contains(keyword);
    }

    /**
     * Returns {@code true} if the merchant name specifically contains the given keyword.
     *
     * @param keyword lower-case keyword to search for
     * @return true if found in the merchant name
     */
    public boolean merchantContains(String keyword) {
        return !merchant.isEmpty() && merchant.contains(keyword);
    }

    /**
     * Returns {@code true} if the category matches the expected value.
     *
     * @param expected the expected category
     * @return true if the category equals the expected value
     */
    public boolean hasCategory(ActivityCategory expected) {
        return expected.equals(category);
    }

    /**
     * Returns {@code true} if the category is null or matches the expected value.
     * Useful for providers that should run when the category is unset (null) or
     * matches their domain.
     *
     * @param expected the expected category
     * @return true if category is null or equals the expected value
     */
    public boolean hasCategoryOrNull(ActivityCategory expected) {
        return category == null || expected.equals(category);
    }

    /**
     * Returns {@code true} if any keyword in the collection is found in the corpus.
     *
     * @param keywords collection of lower-case keywords to search for
     * @return true if at least one keyword matches
     */
    public boolean anyMatch(Collection<String> keywords) {
        for (String keyword : keywords) {
            if (corpus.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the first keyword from the collection that is found in the corpus.
     *
     * @param keywords collection of lower-case keywords to search for
     * @return the first matching keyword, or empty if none match
     */
    public Optional<String> findFirstKeyword(Collection<String> keywords) {
        for (String keyword : keywords) {
            if (corpus.contains(keyword)) {
                return Optional.of(keyword);
            }
        }
        return Optional.empty();
    }

    /**
     * Iterates the given keyword-to-value map in insertion order and returns the
     * first entry whose key is found in the corpus.
     *
     * <p>Callers should use a {@link java.util.LinkedHashMap} to control match
     * precedence (more specific keywords before generic ones).
     *
     * @param keywordMap ordered map of lower-case keywords to domain values
     * @param <T>        the mapped value type
     * @return the first matching entry, or empty if none match
     */
    public <T> Optional<Map.Entry<String, T>> findFirstMatch(Map<String, T> keywordMap) {
        for (Map.Entry<String, T> entry : keywordMap.entrySet()) {
            if (corpus.contains(entry.getKey())) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Factory
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Builds a {@link CarbonHintContext} from a normalized {@link ExtractionResult}.
     *
     * <p>The corpus is constructed by concatenating merchant, description, unit, and
     * all string values found in metadata (including values inside nested lists and maps).
     * The entire corpus is then lower-cased once for efficient keyword matching.
     *
     * @param result the normalized extraction result (must not be null)
     * @return a new immutable context
     */
    public static CarbonHintContext from(ExtractionResult result) {
        StringBuilder sb = new StringBuilder();
        appendIfPresent(sb, result.getMerchant());
        appendIfPresent(sb, result.getDescription());
        appendIfPresent(sb, result.getUnit());
        appendMetadata(sb, result.getMetadata());

        return new CarbonHintContext(
                result,
                sb.toString().toLowerCase(),
                safeToLower(result.getMerchant()),
                safeToLower(result.getDescription()),
                safeToLower(result.getUnit()),
                result.getCategory()
        );
    }

    // ── Corpus construction helpers ─────────────────────────────────────────

    private static String safeToLower(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toLowerCase();
    }

    private static void appendIfPresent(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(' ').append(value);
        }
    }

    @SuppressWarnings("unchecked")
    private static void appendMetadata(StringBuilder sb, Map<String, Object> metadata) {
        if (metadata == null) {
            return;
        }
        for (Object value : metadata.values()) {
            if (value instanceof String s) {
                sb.append(' ').append(s);
            } else if (value instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof String s) {
                        sb.append(' ').append(s);
                    } else if (item instanceof Map<?, ?> map) {
                        map.values().forEach(v -> {
                            if (v instanceof String s) {
                                sb.append(' ').append(s);
                            }
                        });
                    }
                }
            }
        }
    }
}
