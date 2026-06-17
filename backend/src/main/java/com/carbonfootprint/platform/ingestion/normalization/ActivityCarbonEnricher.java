package com.carbonfootprint.platform.ingestion.normalization;

import com.carbonfootprint.platform.ingestion.enrichment.CarbonHintEngine;
import com.carbonfootprint.platform.ingestion.model.CarbonHints;
import com.carbonfootprint.platform.ingestion.model.ExtractionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pipeline adapter that enriches a normalized {@link ExtractionResult} with
 * carbon-domain hints inferred by {@link CarbonHintEngine}.
 *
 * <h3>Pipeline position</h3>
 * <pre>
 * ExtractionResultNormalizer
 *   → [ActivityCarbonEnricher]         ← this class (Step 6b)
 *   → ExtractionResultValidator chain
 *   → ExtractionResultToActivityConverter
 *   → ActivityNormalizer chain
 *   → Activity
 * </pre>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Delegates all inference to {@link CarbonHintEngine}.</li>
 *   <li>Converts the resulting {@link CarbonHints} into a {@code Map<String, Object>}
 *       and places it under the key {@value #CARBON_HINTS_KEY} in a new metadata map.</li>
 *   <li>Never modifies the input {@link ExtractionResult}.</li>
 *   <li>Never overwrites an existing {@code carbonHints} entry — the engine already
 *       reads and preserves it as a seed; this class simply persists the final merged value.</li>
 *   <li>Preserves all original metadata keys.</li>
 * </ul>
 *
 * <h3>Serialization</h3>
 * Enum fields are serialized via {@link Enum#name()} to produce the same plain-string
 * values as before (backward compatible with downstream consumers).
 *
 * <h3>Immutability</h3>
 * This class always returns a new {@link ExtractionResult} built via the builder.
 * The original {@code result} is never mutated.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityCarbonEnricher {

    static final String CARBON_HINTS_KEY = "carbonHints";

    private final CarbonHintEngine carbonHintEngine;

    /**
     * Enriches the given normalized {@link ExtractionResult} with carbon hints.
     *
     * <p>The returned result is a new instance that contains all original fields
     * plus a new (or preserved) {@code "carbonHints"} entry in its metadata.
     *
     * @param normalized the normalized extraction result (must not be mutated)
     * @return a new {@link ExtractionResult} with {@code metadata.carbonHints} populated
     */
    public ExtractionResult enrich(ExtractionResult normalized) {
        log.debug("ActivityCarbonEnricher — starting enrichment: merchant='{}' category={}",
                normalized.getMerchant(), normalized.getCategory());

        // ── Delegate inference to engine ───────────────────────────────────
        CarbonHints hints = carbonHintEngine.computeHints(normalized);

        // ── Skip if no hints were inferred at all ──────────────────────────
        if (isEffectivelyEmpty(hints)) {
            log.debug("ActivityCarbonEnricher — no carbon hints inferred, returning original result unchanged");
            return normalized;
        }

        // ── Build enriched metadata ────────────────────────────────────────
        Map<String, Object> enrichedMetadata = mergeIntoMetadata(normalized.getMetadata(), hints);

        // ── Return new immutable ExtractionResult with enriched metadata ───
        ExtractionResult enriched = ExtractionResult.builder()
                .parserName(normalized.getParserName())
                .merchant(normalized.getMerchant())
                .amount(normalized.getAmount())
                .currency(normalized.getCurrency())
                .category(normalized.getCategory())
                .unit(normalized.getUnit())
                .location(normalized.getLocation())
                .occurredAt(normalized.getOccurredAt())
                .description(normalized.getDescription())
                .confidence(normalized.getConfidence())
                .metadata(enrichedMetadata)
                .build();

        log.info("ActivityCarbonEnricher — enrichment complete: merchant='{}' category={} " +
                        "carbonHints.activityType={} carbonHints.confidence={}",
                enriched.getMerchant(), enriched.getCategory(),
                hints.getActivityType(), hints.getConfidence());

        return enriched;
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Merges the inferred {@link CarbonHints} into a copy of the existing metadata map.
     *
     * <p>All original keys are preserved. The {@value #CARBON_HINTS_KEY} entry is
     * replaced with a new map derived from the final merged hints (which already
     * incorporated any pre-existing carbonHints via the engine's seed logic).
     */
    private Map<String, Object> mergeIntoMetadata(Map<String, Object> existingMetadata, CarbonHints hints) {
        Map<String, Object> merged = new HashMap<>();
        if (existingMetadata != null) {
            merged.putAll(existingMetadata);
        }

        Map<String, Object> hintsMap = toMap(hints);
        merged.put(CARBON_HINTS_KEY, hintsMap);

        return Collections.unmodifiableMap(merged);
    }

    /**
     * Converts a {@link CarbonHints} instance to a {@link Map} containing only
     * non-null fields. Enum values are serialized via {@link Enum#name()} to produce
     * the same plain strings as before (backward compatible).
     */
    private Map<String, Object> toMap(CarbonHints hints) {
        Map<String, Object> map = new LinkedHashMap<>();
        putEnumIfNotNull(map, "activityType",      hints.getActivityType());
        putEnumIfNotNull(map, "transportMode",     hints.getTransportMode());
        putEnumIfNotNull(map, "fuelType",          hints.getFuelType());
        putEnumIfNotNull(map, "energySource",      hints.getEnergySource());
        putIfNotNull(map,     "electricityUnit",   hints.getElectricityUnit());
        putIfNotNull(map,     "fuelUnit",          hints.getFuelUnit());
        putIfNotNull(map,     "estimatedDistance", hints.getEstimatedDistance());
        putIfNotNull(map,     "passengerCount",    hints.getPassengerCount());
        putEnumIfNotNull(map, "cabinClass",        hints.getCabinClass());
        putEnumIfNotNull(map, "vehicleType",       hints.getVehicleType());
        putEnumIfNotNull(map, "merchantIndustry",  hints.getMerchantIndustry());
        putIfNotNull(map,     "confidence",        hints.getConfidence());
        return Collections.unmodifiableMap(map);
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private void putEnumIfNotNull(Map<String, Object> map, String key, Enum<?> value) {
        if (value != null) {
            map.put(key, value.name());
        }
    }

    /**
     * Returns {@code true} if no meaningful field was inferred (all nullable fields are null).
     * Confidence alone is not sufficient to consider hints non-empty.
     */
    private boolean isEffectivelyEmpty(CarbonHints hints) {
        return hints.getActivityType()      == null
                && hints.getTransportMode() == null
                && hints.getFuelType()      == null
                && hints.getEnergySource()  == null
                && hints.getElectricityUnit()== null
                && hints.getFuelUnit()      == null
                && hints.getCabinClass()    == null
                && hints.getVehicleType()   == null
                && hints.getMerchantIndustry()== null
                && hints.getPassengerCount()== null
                && hints.getEstimatedDistance()== null;
    }
}
