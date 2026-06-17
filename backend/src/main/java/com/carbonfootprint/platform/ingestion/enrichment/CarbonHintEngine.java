package com.carbonfootprint.platform.ingestion.enrichment;

import com.carbonfootprint.platform.ingestion.enrichment.model.CabinClass;
import com.carbonfootprint.platform.ingestion.enrichment.model.CarbonActivityType;
import com.carbonfootprint.platform.ingestion.enrichment.model.EnergySource;
import com.carbonfootprint.platform.ingestion.enrichment.model.FuelType;
import com.carbonfootprint.platform.ingestion.enrichment.model.MerchantIndustry;
import com.carbonfootprint.platform.ingestion.enrichment.model.TransportMode;
import com.carbonfootprint.platform.ingestion.enrichment.model.VehicleType;
import com.carbonfootprint.platform.ingestion.model.CarbonHints;
import com.carbonfootprint.platform.ingestion.model.ExtractionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Orchestrates all {@link CarbonHintProvider} strategies to produce a merged,
 * deterministic {@link CarbonHints} from a normalized {@link ExtractionResult}.
 *
 * <h3>Strategy Pattern</h3>
 * The engine is the <em>Context</em> in the Strategy pattern. It holds a
 * {@code List<CarbonHintProvider>} injected by Spring and executed in
 * ascending order of {@link CarbonHintProvider#getOrder()}.
 *
 * <h3>Merge algorithm</h3>
 * <ol>
 *   <li>Providers are sorted by {@link CarbonHintProvider#getOrder()} (ascending).</li>
 *   <li>A single {@link CarbonHintContext} is built from the extraction result and
 *       passed to every provider — corpus is constructed once, lower-cased once.</li>
 *   <li>Each provider returns a partial {@link CarbonHints}.</li>
 *   <li>Partials are merged via {@link CarbonHintMerger} using first-non-null-wins
 *       per field, so lower-order providers have higher priority.</li>
 *   <li>Any values already present in {@code metadata.carbonHints} are read and
 *       placed into the seed before merging — preserving pre-existing hints.</li>
 *   <li>The final {@code confidence} is computed as the mean of all provider
 *       confidences, owning this calculation in exactly one place.</li>
 * </ol>
 *
 * <h3>Extensibility</h3>
 * To add a new provider, implement {@link CarbonHintProvider} and annotate with
 * {@code @Component}. The engine picks it up automatically — no code change required here.
 */
@Slf4j
@Component
public class CarbonHintEngine {

    private static final String METADATA_KEY = "carbonHints";

    private final List<CarbonHintProvider> sortedProviders;
    private final CarbonHintMerger merger;

    /**
     * Constructs the engine with the full list of providers, sorted by order once at startup.
     *
     * @param providers all {@link CarbonHintProvider} beans discovered by Spring
     * @param merger    the merge strategy component
     */
    public CarbonHintEngine(List<CarbonHintProvider> providers, CarbonHintMerger merger) {
        this.merger = merger;
        this.sortedProviders = providers.stream()
                .sorted(Comparator.comparingInt(CarbonHintProvider::getOrder))
                .collect(Collectors.toUnmodifiableList());

        log.info("CarbonHintEngine initialized with {} provider(s): {}",
                sortedProviders.size(),
                sortedProviders.stream()
                        .map(p -> p.getClass().getSimpleName() + "[order=" + p.getOrder() + "]")
                        .collect(Collectors.joining(", ")));
    }

    /**
     * Runs all providers against the given extraction result, merges the partial hints,
     * respects any pre-existing {@code carbonHints} in metadata, and returns the final
     * merged {@link CarbonHints}.
     *
     * <p>This method never modifies {@code result}.
     *
     * @param result the normalized extraction result (must not be mutated)
     * @return the merged, final {@link CarbonHints} — never {@code null}
     */
    public CarbonHints computeHints(ExtractionResult result) {
        log.debug("CarbonHintEngine — starting enrichment: merchant='{}' category={} confidence={}",
                result.getMerchant(), result.getCategory(), result.getConfidence());

        // ── Build context once for all providers ───────────────────────────
        CarbonHintContext context = CarbonHintContext.from(result);

        // ── Seed: read any pre-existing carbonHints from metadata ─────────
        CarbonHints seed = readExistingHints(result);

        // ── Execute providers and collect partials ─────────────────────────
        List<CarbonHints> partials = sortedProviders.stream()
                .map(provider -> {
                    CarbonHints partial = safeProvide(provider, context);
                    log.debug("CarbonHintEngine — provider={} returned: activityType={} confidence={}",
                            provider.getClass().getSimpleName(),
                            partial.getActivityType(),
                            partial.getConfidence());
                    return partial;
                })
                .collect(Collectors.toList());

        // ── Merge via dedicated merger ────────────────────────────────────
        CarbonHints merged = merger.merge(seed, partials);

        // ── Recompute confidence as mean of all non-null provider confidences ─
        double finalConfidence = computeFinalConfidence(partials);
        if (merged.getConfidence() == null || seed.getConfidence() == null) {
            merged = merged.toBuilder().confidence(finalConfidence > 0 ? finalConfidence : null).build();
        }

        log.info("CarbonHintEngine — enrichment complete: activityType={} transportMode={} " +
                        "fuelType={} cabinClass={} merchantIndustry={} confidence={}",
                merged.getActivityType(), merged.getTransportMode(), merged.getFuelType(),
                merged.getCabinClass(), merged.getMerchantIndustry(), merged.getConfidence());

        return merged;
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Reads {@code metadata.carbonHints} from the result and reconstructs a
     * {@link CarbonHints} seed from it. This ensures the non-overwrite guarantee:
     * pre-existing hints take priority in the merge (they are placed in the seed
     * and therefore have implicit priority over all providers).
     *
     * <p>String values from metadata are safely converted to their corresponding
     * enum types using {@link Enum#valueOf(Class, String)} with fallback to {@code null}.
     */
    @SuppressWarnings("unchecked")
    private CarbonHints readExistingHints(ExtractionResult result) {
        if (result.getMetadata() == null) {
            return CarbonHints.empty();
        }

        Object existing = result.getMetadata().get(METADATA_KEY);
        if (!(existing instanceof Map)) {
            return CarbonHints.empty();
        }

        Map<String, Object> existingMap = (Map<String, Object>) existing;

        CarbonHints.CarbonHintsBuilder builder = CarbonHints.builder();

        applyEnum(existingMap, "activityType",     CarbonActivityType.class, builder::activityType);
        applyEnum(existingMap, "transportMode",    TransportMode.class,      builder::transportMode);
        applyEnum(existingMap, "fuelType",         FuelType.class,           builder::fuelType);
        applyEnum(existingMap, "energySource",     EnergySource.class,       builder::energySource);
        applyEnum(existingMap, "cabinClass",       CabinClass.class,         builder::cabinClass);
        applyEnum(existingMap, "vehicleType",      VehicleType.class,        builder::vehicleType);
        applyEnum(existingMap, "merchantIndustry", MerchantIndustry.class,   builder::merchantIndustry);

        applyString(existingMap, "electricityUnit", builder::electricityUnit);
        applyString(existingMap, "fuelUnit",        builder::fuelUnit);

        Object pc = existingMap.get("passengerCount");
        if (pc instanceof Integer i) {
            builder.passengerCount(i);
        } else if (pc instanceof Number n) {
            builder.passengerCount(n.intValue());
        }

        Object dist = existingMap.get("estimatedDistance");
        if (dist instanceof java.math.BigDecimal bd) {
            builder.estimatedDistance(bd);
        } else if (dist instanceof Number n) {
            builder.estimatedDistance(java.math.BigDecimal.valueOf(n.doubleValue()));
        }

        Object conf = existingMap.get("confidence");
        if (conf instanceof Double d) {
            builder.confidence(d);
        } else if (conf instanceof Number n) {
            builder.confidence(n.doubleValue());
        }

        CarbonHints seed = builder.build();

        log.debug("CarbonHintEngine — loaded pre-existing carbonHints seed from metadata: " +
                        "activityType={} fuelType={} confidence={}",
                seed.getActivityType(), seed.getFuelType(), seed.getConfidence());

        return seed;
    }

    /**
     * Safely converts a String value from metadata to the given enum type.
     * If the value is absent, blank, or does not match any enum constant, the setter is not called.
     */
    private <E extends Enum<E>> void applyEnum(Map<String, Object> map, String key,
                                               Class<E> enumClass,
                                               java.util.function.Consumer<E> setter) {
        Object val = map.get(key);
        if (val instanceof String s && !s.isBlank()) {
            try {
                setter.accept(Enum.valueOf(enumClass, s.trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // Unknown enum value in metadata — skip gracefully
            }
        }
    }

    private void applyString(Map<String, Object> map, String key, java.util.function.Consumer<String> setter) {
        Object val = map.get(key);
        if (val instanceof String s && !s.isBlank()) {
            setter.accept(s);
        }
    }

    /**
     * Calls the provider safely, catching and logging any exception without
     * halting the pipeline. Returns {@link CarbonHints#empty()} on failure.
     */
    private CarbonHints safeProvide(CarbonHintProvider provider, CarbonHintContext context) {
        try {
            CarbonHints partial = provider.provide(context);
            return partial != null ? partial : CarbonHints.empty();
        } catch (Exception e) {
            log.error("CarbonHintEngine — provider {} threw an unexpected exception, skipping: {}",
                    provider.getClass().getSimpleName(), e.getMessage(), e);
            return CarbonHints.empty();
        }
    }

    /**
     * Computes the mean confidence across all partials that have a non-null confidence.
     * Returns 0.0 if no provider set a confidence.
     */
    private double computeFinalConfidence(List<CarbonHints> partials) {
        List<Double> confidences = partials.stream()
                .map(CarbonHints::getConfidence)
                .filter(c -> c != null && c > 0.0)
                .collect(Collectors.toList());

        if (confidences.isEmpty()) {
            return 0.0;
        }

        double mean = confidences.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        log.debug("CarbonHintEngine — final confidence computed: mean={} from {} provider(s)", mean, confidences.size());
        return mean;
    }
}
