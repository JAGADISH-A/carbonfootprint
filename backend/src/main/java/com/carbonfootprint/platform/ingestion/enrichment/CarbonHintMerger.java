package com.carbonfootprint.platform.ingestion.enrichment;

import com.carbonfootprint.platform.ingestion.model.CarbonHints;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Merges multiple partial {@link CarbonHints} instances into a single result using
 * a deterministic <em>first-non-null-wins</em> rule per field.
 *
 * <h3>Single Responsibility</h3>
 * This class owns all merge logic. {@link CarbonHints} is a pure immutable DTO with
 * no merge behavior. The {@link CarbonHintEngine} delegates merging here.
 *
 * <h3>Merge semantics</h3>
 * <ul>
 *   <li>Fields already set in the seed (from pre-existing {@code metadata.carbonHints})
 *       are never overwritten — the non-overwrite guarantee.</li>
 *   <li>Among provider partials, the provider with the lowest
 *       {@link CarbonHintProvider#getOrder()} wins because it appears first in the list.</li>
 *   <li>{@link CarbonHints#getConfidence()} is <strong>not</strong> merged here.
 *       The engine computes the final confidence separately.</li>
 * </ul>
 *
 * <h3>Thread safety</h3>
 * This class is stateless and thread-safe.
 *
 * @see CarbonHintEngine
 */
@Slf4j
@Component
public class CarbonHintMerger {

    /**
     * Merges a list of partial {@link CarbonHints} into a seed using
     * first-non-null-wins per field. Confidence is excluded from the merge.
     *
     * @param seed     the seed hints (typically from pre-existing {@code metadata.carbonHints});
     *                 may be {@link CarbonHints#empty()}
     * @param partials the ordered list of partial hints from providers (lowest order first)
     * @return a new merged {@link CarbonHints} with all non-null fields resolved
     */
    public CarbonHints merge(CarbonHints seed, List<CarbonHints> partials) {
        CarbonHints result = seed != null ? seed : CarbonHints.empty();

        for (CarbonHints partial : partials) {
            if (partial == null) {
                continue;
            }
            result = mergeTwo(result, partial);
        }

        log.debug("CarbonHintMerger — merge complete: activityType={} fuelType={} " +
                        "transportMode={} cabinClass={} merchantIndustry={}",
                result.getActivityType(), result.getFuelType(),
                result.getTransportMode(), result.getCabinClass(),
                result.getMerchantIndustry());

        return result;
    }

    // ── Private ─────────────────────────────────────────────────────────────

    /**
     * Merges two hints: fields already set in {@code base} are kept;
     * null fields adopt the value from {@code other}.
     * Confidence is intentionally excluded — the engine computes it.
     */
    private CarbonHints mergeTwo(CarbonHints base, CarbonHints other) {
        return CarbonHints.builder()
                .activityType(      firstNonNull(base.getActivityType(),      other.getActivityType()))
                .transportMode(     firstNonNull(base.getTransportMode(),     other.getTransportMode()))
                .fuelType(          firstNonNull(base.getFuelType(),          other.getFuelType()))
                .energySource(      firstNonNull(base.getEnergySource(),      other.getEnergySource()))
                .electricityUnit(   firstNonNull(base.getElectricityUnit(),   other.getElectricityUnit()))
                .fuelUnit(          firstNonNull(base.getFuelUnit(),          other.getFuelUnit()))
                .estimatedDistance( firstNonNull(base.getEstimatedDistance(), other.getEstimatedDistance()))
                .passengerCount(    firstNonNull(base.getPassengerCount(),    other.getPassengerCount()))
                .cabinClass(        firstNonNull(base.getCabinClass(),        other.getCabinClass()))
                .vehicleType(      firstNonNull(base.getVehicleType(),       other.getVehicleType()))
                .merchantIndustry(  firstNonNull(base.getMerchantIndustry(),  other.getMerchantIndustry()))
                // confidence intentionally omitted — engine computes final value
                .build();
    }

    private static <T> T firstNonNull(T a, T b) {
        return a != null ? a : b;
    }
}
