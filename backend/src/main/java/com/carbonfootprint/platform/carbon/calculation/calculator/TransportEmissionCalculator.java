package com.carbonfootprint.platform.carbon.calculation.calculator;

import com.carbonfootprint.platform.activity.model.Activity;
import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionCalculator;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionFactor;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionFactorRegistry;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionResult;
import com.carbonfootprint.platform.ingestion.enrichment.model.TransportMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * {@link EmissionCalculator} for {@link ActivityCategory#TRANSPORT} activities.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Reads {@link com.carbonfootprint.platform.ingestion.model.CarbonHints} from
 *       {@code activity.metadata["carbonHints"]}</li>
 *   <li>Extracts the {@link TransportMode} (BUS, METRO, TRAIN, TAXI, AUTO, BIKE)</li>
 *   <li>Reads the distance/quantity (km) from {@link Activity#getAmount()}</li>
 *   <li>Looks up the transport-mode-specific {@link EmissionFactor} from the
 *       {@link EmissionFactorRegistry}</li>
 *   <li>Computes: {@code Emission = distance × emission factor}</li>
 * </ul>
 *
 * <h3>Ordering</h3 * Runs at order 10 — the highest-priority calculator for TRANSPORT activities.
 *
 * @see EmissionFactorRegistry
 * @see EmissionResult
 */
@Slf4j
@Component
public class TransportEmissionCalculator implements EmissionCalculator {

    private static final int ORDER = 10;
    private static final String CARBON_HINTS_KEY = "carbonHints";
    private static final String TRANSPORT_MODE_KEY = "transportMode";

    private final EmissionFactorRegistry emissionFactorRegistry;

    @Autowired
    public TransportEmissionCalculator(@Lazy EmissionFactorRegistry emissionFactorRegistry) {
        this.emissionFactorRegistry = emissionFactorRegistry;
    }

    @Override
    public boolean supports(Activity activity) {
        if (activity.getCategory() != ActivityCategory.TRANSPORT) {
            return false;
        }
        return extractTransportMode(activity).isPresent();
    }

    @Override
    public Optional<EmissionResult> calculate(Activity activity) {
        Optional<TransportMode> transportModeOpt = extractTransportMode(activity);
        if (transportModeOpt.isEmpty()) {
            log.debug("TransportEmissionCalculator — no transportMode in carbonHints for activity {}",
                    activity.getId());
            return Optional.empty();
        }

        BigDecimal quantity = activity.getAmount();
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("TransportEmissionCalculator — invalid or missing quantity for activity {}",
                    activity.getId());
            return Optional.empty();
        }

        TransportMode transportMode = transportModeOpt.get();
        Instant validAt = activity.getOccurredAt() != null ? activity.getOccurredAt() : Instant.now();

        Optional<EmissionFactor> factorOpt = emissionFactorRegistry.find(
                ActivityCategory.TRANSPORT, transportMode, validAt);
        if (factorOpt.isEmpty()) {
            log.debug("TransportEmissionCalculator — no emission factor found for transportMode={} at {} for activity {}",
                    transportMode, validAt, activity.getId());
            return Optional.empty();
        }

        EmissionFactor factor = factorOpt.get();
        BigDecimal carbonKg = quantity.multiply(factor.getValue());

        log.debug("TransportEmissionCalculator — calculated: activityId={} transportMode={} quantity={} × {} = {} kg CO₂e",
                activity.getId(), transportMode, quantity, factor.getValue(), carbonKg);

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
     * Extracts the {@link TransportMode} from the activity's carbon hints metadata.
     *
     * @return the transport mode, or empty if not present or unparseable
     */
    @SuppressWarnings("unchecked")
    private Optional<TransportMode> extractTransportMode(Activity activity) {
        Map<String, Object> metadata = activity.getMetadata();
        if (metadata == null) {
            return Optional.empty();
        }

        Object hintsObj = metadata.get(CARBON_HINTS_KEY);
        if (!(hintsObj instanceof Map)) {
            return Optional.empty();
        }

        Map<String, Object> hints = (Map<String, Object>) hintsObj;
        Object transportModeValue = hints.get(TRANSPORT_MODE_KEY);
        if (transportModeValue == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(TransportMode.valueOf(transportModeValue.toString()));
        } catch (IllegalArgumentException e) {
            log.debug("TransportEmissionCalculator — unknown transportMode '{}' in carbonHints", transportModeValue);
            return Optional.empty();
        }
    }
}
