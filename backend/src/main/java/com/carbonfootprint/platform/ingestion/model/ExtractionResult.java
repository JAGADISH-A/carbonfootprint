package com.carbonfootprint.platform.ingestion.model;

import com.carbonfootprint.platform.activity.model.ActivityCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Data Transfer Object carrying parsed field values extracted from a {@link RawDocument}.
 *
 * <h3>Architectural Role</h3>
 * {@code ExtractionResult} is the firewall between {@link com.carbonfootprint.platform.ingestion.port.out.DocumentParser}
 * implementations and the {@link com.carbonfootprint.platform.activity.model.Activity} domain model.
 *
 * <p>This decoupling means:
 * <ul>
 *   <li>The {@link com.carbonfootprint.platform.activity.model.Activity} domain model has zero
 *       dependency on Gemini, OCR, or any AI provider.</li>
 *   <li>Parsers (Gemini, Regex, Rule-based) all produce the same DTO — the pipeline
 *       does not need to know which parser ran.</li>
 *   <li>The normalization layer converts {@code ExtractionResult} into a fully-formed
 *       {@link com.carbonfootprint.platform.activity.model.Activity} with validated,
 *       canonical values.</li>
 * </ul>
 *
 * <h3>Design Constraints</h3>
 * <ul>
 *   <li>This is NOT a domain entity. It is not persisted.</li>
 *   <li>Fields may be null when a parser cannot determine a value.</li>
 *   <li>Null fields are handled by the normalization and validation layers —
 *       parsers must NOT guess or default-fill values.</li>
 * </ul>
 *
 * <h3>Pipeline position</h3>
 * {@code RawDocument → DocumentParser → ExtractionResult → Validation → Normalization → Activity}
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionResult {

    /**
     * Normalised merchant or vendor name as extracted from the document.
     * May require further normalisation (casing, alias resolution) by
     * {@link com.carbonfootprint.platform.ingestion.normalization.impl.MerchantNormalizer}.
     */
    private String merchant;

    /**
     * Transaction amount as extracted. May be a raw string like "₹3,200" — the
     * {@link com.carbonfootprint.platform.ingestion.normalization.impl.CurrencyNormalizer}
     * will clean this.
     */
    private BigDecimal amount;

    /**
     * Raw currency string extracted from the document (e.g., "Rs", "₹", "USD").
     * Normalised to ISO 4217 by {@link com.carbonfootprint.platform.ingestion.normalization.impl.CurrencyNormalizer}.
     */
    private String currency;

    /**
     * The inferred activity category.
     * Null if the parser could not determine the category — defaults to
     * {@link com.carbonfootprint.platform.activity.model.ActivityCategory#OTHER} during normalisation.
     */
    private ActivityCategory category;

    /**
     * Physical quantity unit as extracted (e.g., "KWH", "L", "litres").
     * Normalised to canonical form by {@link com.carbonfootprint.platform.ingestion.normalization.impl.UnitNormalizer}.
     */
    private String unit;

    /**
     * Location string as extracted (e.g., "MUMBAI", "bangalore, india").
     * Normalised by {@link com.carbonfootprint.platform.ingestion.normalization.impl.LocationNormalizer}.
     */
    private String location;

    /**
     * Parsed timestamp of when the real-world event occurred.
     * Null if the parser could not extract or parse a date.
     * Normalised to UTC by {@link com.carbonfootprint.platform.ingestion.normalization.impl.DateNormalizer}.
     */
    private Instant occurredAt;

    /**
     * Human-readable description of the activity as inferred by the parser.
     */
    private String description;

    /**
     * Parser confidence in the overall extraction, in range [0.0, 1.0].
     * <ul>
     *   <li>1.0 — all fields extracted with high certainty.</li>
     *   <li>0.5 — partial extraction; some fields are missing.</li>
     *   <li>0.0 — extraction failed; all fields are null.</li>
     * </ul>
     * Used by {@link com.carbonfootprint.platform.ingestion.validation.impl.ExtractionConfidenceValidator}.
     */
    private Double confidence;

    /**
     * Name of the parser that produced this result (e.g., "GeminiDocumentParser").
     * Used for audit trails and metrics — never used in business logic.
     */
    private String parserName;

    /**
     * Flexible metadata for parser-specific or category-specific extracted fields.
     *
     * <p>Examples:
     * <ul>
     *   <li>ELECTRICITY: {@code {"tariff": "peak", "meterReading": 12345}}</li>
     *   <li>FLIGHT: {@code {"origin": "BOM", "destination": "LHR", "cabin": "economy"}}</li>
     *   <li>FUEL: {@code {"fuelType": "petrol", "litres": 40.5}}</li>
     * </ul>
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    // ── Factory methods ────────────────────────────────────────────────────

    /**
     * Returns an empty result indicating total extraction failure.
     * The pipeline will flag this for human review.
     */
    public static ExtractionResult empty(String parserName) {
        return ExtractionResult.builder()
                .parserName(parserName)
                .confidence(0.0)
                .build();
    }

    /**
     * Returns {@code true} if the minimum required fields for a valid activity
     * were extracted. Used as a quick pre-validation check.
     */
    public boolean hasMinimumRequiredFields() {
        return category != null || (merchant != null && !merchant.isBlank());
    }
}
