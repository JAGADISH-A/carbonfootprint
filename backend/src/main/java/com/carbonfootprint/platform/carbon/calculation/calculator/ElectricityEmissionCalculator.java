package com.carbonfootprint.platform.carbon.calculation.calculator;

import com.carbonfootprint.platform.activity.model.Activity;
import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionCalculator;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionFactor;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionFactorRegistry;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * {@link EmissionCalculator} for {@link ActivityCategory#ELECTRICITY} activities.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Reads {@link com.carbonfootprint.platform.ingestion.model.CarbonHints} from
 *       {@code activity.metadata["carbonHints"]}</li>
 *   <li>Confirms {@code activityType == ELECTRICITY} and {@code energySource} is present</li>
 *   <li>Reads the electricity quantity (kWh) from {@link Activity#getAmount()}</li>
 *   <li>Looks up the GRID {@link EmissionFactor} from the
 *       {@link EmissionFactorRegistry}</li>
 *   <li>Computes: {@code Emission = kWh × emission factor}</li>
 * </ul>
 *
 * <h3>Ordering</h3>
 * Runs at order 10 — the highest-priority calculator for ELECTRICITY activities.
 *
 * @see EmissionFactorRegistry
 * @see EmissionResult
 */
@Slf4j
@Component
public class ElectricityEmissionCalculator implements EmissionCalculator {

    private static final int ORDER = 10;
    private static final String CARBON_HINTS_KEY = "carbonHints";
    private static final String ACTIVITY_TYPE_KEY = "activityType";
    private static final String ENERGY_SOURCE_KEY = "energySource";

    private final EmissionFactorRegistry emissionFactorRegistry;

    @Autowired
    public ElectricityEmissionCalculator(@Lazy EmissionFactorRegistry emissionFactorRegistry) {
        this.emissionFactorRegistry = emissionFactorRegistry;
    }

    @Override
    public boolean supports(Activity activity) {
        if (activity.getCategory() != ActivityCategory.ELECTRICITY) {
            return false;
        }
        return extractEnergySource(activity).isPresent();
    }

    @Override
    public Optional<EmissionResult> calculate(Activity activity) {
        if (extractEnergySource(activity).isEmpty()) {
            log.debug("ElectricityEmissionCalculator — no energySource in carbonHints for activity {}",
                    activity.getId());
            return Optional.empty();
        }

        BigDecimal quantity = activity.getAmount();
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("ElectricityEmissionCalculator — invalid or missing quantity for activity {}",
                    activity.getId());
            return Optional.empty();
        }

        Instant validAt = activity.getOccurredAt() != null ? activity.getOccurredAt() : Instant.now();

        Optional<EmissionFactor> factorOpt = emissionFactorRegistry.find(
                ActivityCategory.ELECTRICITY, validAt);
        if (factorOpt.isEmpty()) {
            log.debug("ElectricityEmissionCalculator — no emission factor found at {} for activity {}",
                    validAt, activity.getId());
            return Optional.empty();
        }

        EmissionFactor factor = factorOpt.get();
        BigDecimal carbonKg = quantity.multiply(factor.getValue());

        log.debug("ElectricityEmissionCalculator — calculated: activityId={} quantity={} × {} = {} kg CO₂e",
                activity.getId(), quantity, factor.getValue(), carbonKg);

        EmissionResult result = EmissionResult.builder()
                .carbonKg(carbonKg)
                .activityId(activity.getId())
                .emissionFactor(factor)
                .activityQuantity(quantity)
                .activityUnit(factor.getUnit())
                .methodology(factor.getMethodName())
                .calculatedAt(Instant.now())
                .build();

        return Optional.of(result);
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Extracts the {@code energySource} from the activity's carbon hints metadata.
     *
     * @return the energy source string, or empty if not present
     */
    @SuppressWarnings("unchecked")
    private Optional<String> extractEnergySource(Activity activity) {
        Map<String, Object> metadata = activity.getMetadata();
        if (metadata == null) {
            return Optional.empty();
        }

        Object hintsObj = metadata.get(CARBON_HINTS_KEY);
        if (!(hintsObj instanceof Map)) {
            return Optional.empty();
        }

        Map<String, Object> hints = (Map<String, Object>) hintsObj;
        Object activityTypeValue = hints.get(ACTIVITY_TYPE_KEY);
        if (!"ELECTRICITY".equals(activityTypeValue)) {
            return Optional.empty();
        }

        Object energySource = hints.get(ENERGY_SOURCE_KEY);
        if (energySource == null) {
            return Optional.empty();
        }

        return Optional.of(energySource.toString());
    }
}
