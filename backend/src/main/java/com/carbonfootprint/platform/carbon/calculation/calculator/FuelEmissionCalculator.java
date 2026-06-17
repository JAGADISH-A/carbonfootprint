package com.carbonfootprint.platform.carbon.calculation.calculator;

import com.carbonfootprint.platform.activity.model.Activity;
import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionCalculator;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionFactor;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionFactorRegistry;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionResult;
import com.carbonfootprint.platform.ingestion.enrichment.model.FuelType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * {@link EmissionCalculator} for {@link ActivityCategory#FUEL} activities.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Reads {@link com.carbonfootprint.platform.ingestion.model.CarbonHints} from
 *       {@code activity.metadata["carbonHints"]}</li>
 *   <li>Extracts the {@link FuelType} (PETROL, DIESEL, LPG, CNG)</li>
 *   <li>Reads the fuel quantity (litres) from {@link Activity#getAmount()}</li>
 *   <li>Looks up the appropriate {@link EmissionFactor} from the
 *       {@link EmissionFactorRegistry}</li>
 *   <li>Computes: {@code Emission = litres × emission factor}</li>
 * </ul>
 *
 * <h3>Ordering</h3>
 * Runs at order 10 — the highest-priority calculator for FUEL activities, as it
 * uses a known quantity and fuel-specific emission factor.
 *
 * @see EmissionFactorRegistry
 * @see EmissionResult
 */
@Slf4j
@Component
public class FuelEmissionCalculator implements EmissionCalculator {

    private static final int ORDER = 10;
    private static final String CARBON_HINTS_KEY = "carbonHints";
    private static final String FUEL_TYPE_KEY = "fuelType";

    private final EmissionFactorRegistry emissionFactorRegistry;

    @Autowired
    public FuelEmissionCalculator(@Lazy EmissionFactorRegistry emissionFactorRegistry) {
        this.emissionFactorRegistry = emissionFactorRegistry;
    }

    @Override
    public boolean supports(Activity activity) {
        if (activity.getCategory() != ActivityCategory.FUEL) {
            return false;
        }
        return extractFuelType(activity).isPresent();
    }

    @Override
    public Optional<EmissionResult> calculate(Activity activity) {
        Optional<FuelType> fuelTypeOpt = extractFuelType(activity);
        if (fuelTypeOpt.isEmpty()) {
            log.debug("FuelEmissionCalculator — no fuelType in carbonHints for activity {}", activity.getId());
            return Optional.empty();
        }

        BigDecimal quantity = activity.getAmount();
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("FuelEmissionCalculator — invalid or missing quantity for activity {}", activity.getId());
            return Optional.empty();
        }

        FuelType fuelType = fuelTypeOpt.get();
        Instant validAt = activity.getOccurredAt() != null ? activity.getOccurredAt() : Instant.now();

        Optional<EmissionFactor> factorOpt = emissionFactorRegistry.find(
                ActivityCategory.FUEL, fuelType, validAt);
        if (factorOpt.isEmpty()) {
            log.debug("FuelEmissionCalculator — no emission factor found for fuelType={} at {} for activity {}",
                    fuelType, validAt, activity.getId());
            return Optional.empty();
        }

        EmissionFactor factor = factorOpt.get();
        BigDecimal carbonKg = quantity.multiply(factor.getValue());

        log.debug("FuelEmissionCalculator — calculated: activityId={} fuelType={} quantity={} × {} = {} kg CO₂e",
                activity.getId(), fuelType, quantity, factor.getValue(), carbonKg);

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
     * Extracts the {@link FuelType} from the activity's carbon hints metadata.
     *
     * @return the fuel type, or empty if not present or unparseable
     */
    @SuppressWarnings("unchecked")
    private Optional<FuelType> extractFuelType(Activity activity) {
        Map<String, Object> metadata = activity.getMetadata();
        if (metadata == null) {
            return Optional.empty();
        }

        Object hintsObj = metadata.get(CARBON_HINTS_KEY);
        if (!(hintsObj instanceof Map)) {
            return Optional.empty();
        }

        Map<String, Object> hints = (Map<String, Object>) hintsObj;
        Object fuelTypeValue = hints.get(FUEL_TYPE_KEY);
        if (fuelTypeValue == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(FuelType.valueOf(fuelTypeValue.toString()));
        } catch (IllegalArgumentException e) {
            log.debug("FuelEmissionCalculator — unknown fuelType '{}' in carbonHints", fuelTypeValue);
            return Optional.empty();
        }
    }
}
