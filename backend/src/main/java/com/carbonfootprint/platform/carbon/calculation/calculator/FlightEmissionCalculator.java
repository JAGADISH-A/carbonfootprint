package com.carbonfootprint.platform.carbon.calculation.calculator;

import com.carbonfootprint.platform.activity.model.Activity;
import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionCalculator;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionFactor;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionFactorRegistry;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionResult;
import com.carbonfootprint.platform.ingestion.enrichment.model.CabinClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * {@link EmissionCalculator} for {@link ActivityCategory#FLIGHT} activities.
 *
 * <h3>Formula</h3>
 * {@code carbonKg = distance × emissionFactor × cabinMultiplier}
 *
 * <h3>Cabin class multipliers</h3>
 * <ul>
 *   <li>ECONOMY — 1.0 (baseline)</li>
 *   <li>PREMIUM_ECONOMY — 1.5</li>
 *   <li>BUSINESS — 2.0</li>
 *   <li>FIRST — 3.0</li>
 * </ul>
 *
 * <p>If no cabin class is provided in the carbon hints, ECONOMY (1.0) is used as the default.
 *
 * <h3>Order</h3>
 * Runs at order 10 (same priority as other category-specific calculators).
 */
@Slf4j
@Component
public class FlightEmissionCalculator implements EmissionCalculator {

    private static final int ORDER = 10;
    private static final String CARBON_HINTS_KEY = "carbonHints";
    private static final String ACTIVITY_TYPE_KEY = "activityType";
    private static final String CABIN_CLASS_KEY = "cabinClass";
    private static final String ESTIMATED_DISTANCE_KEY = "estimatedDistance";
    private static final String PASSENGER_COUNT_KEY = "passengerCount";
    private static final String FLIGHT_ACTIVITY_TYPE = "FLIGHT";

    private static final BigDecimal CABIN_MULTIPLIER_ECONOMY = BigDecimal.ONE;
    private static final BigDecimal CABIN_MULTIPLIER_PREMIUM_ECONOMY = new BigDecimal("1.5");
    private static final BigDecimal CABIN_MULTIPLIER_BUSINESS = BigDecimal.valueOf(2);
    private static final BigDecimal CABIN_MULTIPLIER_FIRST = BigDecimal.valueOf(3);

    private final EmissionFactorRegistry emissionFactorRegistry;

    @Autowired
    public FlightEmissionCalculator(@Lazy EmissionFactorRegistry emissionFactorRegistry) {
        this.emissionFactorRegistry = emissionFactorRegistry;
    }

    @Override
    public boolean supports(Activity activity) {
        if (activity.getCategory() != ActivityCategory.FLIGHT) {
            return false;
        }
        return hasFlightHints(activity);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<EmissionResult> calculate(Activity activity) {
        Map<String, Object> hints = extractHints(activity);
        if (hints == null) {
            log.debug("FlightEmissionCalculator — no carbonHints for activity {}", activity.getId());
            return Optional.empty();
        }

        BigDecimal distance = extractDistance(hints);
        if (distance == null || distance.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("FlightEmissionCalculator — missing or non-positive estimatedDistance for activity {}",
                    activity.getId());
            return Optional.empty();
        }

        CabinClass cabinClass = extractCabinClass(hints);
        BigDecimal cabinMultiplier = getCabinMultiplier(cabinClass);

        int passengerCount = 1;
        Object passengerCountObj = hints.get(PASSENGER_COUNT_KEY);
        if (passengerCountObj instanceof Number) {
            int parsed = ((Number) passengerCountObj).intValue();
            if (parsed > 0) {
                passengerCount = parsed;
            }
        }

        Instant validAt = activity.getOccurredAt() != null ? activity.getOccurredAt() : Instant.now();

        Optional<EmissionFactor> factorOpt = emissionFactorRegistry.find(
                ActivityCategory.FLIGHT, validAt);
        if (factorOpt.isEmpty()) {
            log.debug("FlightEmissionCalculator — no emission factor found for FLIGHT at {} for activity {}",
                    validAt, activity.getId());
            return Optional.empty();
        }

        EmissionFactor factor = factorOpt.get();

        BigDecimal carbonKg = distance
                .multiply(factor.getValue())
                .multiply(cabinMultiplier)
                .multiply(BigDecimal.valueOf(passengerCount))
                .setScale(4, RoundingMode.HALF_UP);

        log.debug("FlightEmissionCalculator — calculated: activityId={} distance={}km × factor={} × cabin={} × passengers={} = {} kg CO₂e",
                activity.getId(), distance, factor.getValue(), cabinMultiplier, passengerCount, carbonKg);

        EmissionResult result = EmissionResult.builder()
                .carbonKg(carbonKg)
                .activityId(activity.getId())
                .emissionFactor(factor)
                .activityQuantity(distance)
                .activityUnit("km")
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractHints(Activity activity) {
        Map<String, Object> metadata = activity.getMetadata();
        if (metadata == null) {
            return null;
        }
        Object hintsObj = metadata.get(CARBON_HINTS_KEY);
        if (!(hintsObj instanceof Map)) {
            return null;
        }
        return (Map<String, Object>) hintsObj;
    }

    private boolean hasFlightHints(Activity activity) {
        Map<String, Object> hints = extractHints(activity);
        if (hints == null) {
            return false;
        }
        Object activityType = hints.get(ACTIVITY_TYPE_KEY);
        return FLIGHT_ACTIVITY_TYPE.equals(activityType);
    }

    private BigDecimal extractDistance(Map<String, Object> hints) {
        Object distanceObj = hints.get(ESTIMATED_DISTANCE_KEY);
        if (distanceObj instanceof BigDecimal) {
            return (BigDecimal) distanceObj;
        }
        if (distanceObj instanceof Number) {
            return BigDecimal.valueOf(((Number) distanceObj).doubleValue());
        }
        return null;
    }

    private CabinClass extractCabinClass(Map<String, Object> hints) {
        Object cabinObj = hints.get(CABIN_CLASS_KEY);
        if (cabinObj instanceof CabinClass) {
            return (CabinClass) cabinObj;
        }
        if (cabinObj instanceof String) {
            try {
                return CabinClass.valueOf((String) cabinObj);
            } catch (IllegalArgumentException e) {
                log.debug("FlightEmissionCalculator — unknown cabinClass '{}', defaulting to ECONOMY", cabinObj);
                return CabinClass.ECONOMY;
            }
        }
        return CabinClass.ECONOMY;
    }

    private BigDecimal getCabinMultiplier(CabinClass cabinClass) {
        if (cabinClass == null) {
            return CABIN_MULTIPLIER_ECONOMY;
        }
        return switch (cabinClass) {
            case ECONOMY -> CABIN_MULTIPLIER_ECONOMY;
            case PREMIUM_ECONOMY -> CABIN_MULTIPLIER_PREMIUM_ECONOMY;
            case BUSINESS -> CABIN_MULTIPLIER_BUSINESS;
            case FIRST -> CABIN_MULTIPLIER_FIRST;
        };
    }
}
