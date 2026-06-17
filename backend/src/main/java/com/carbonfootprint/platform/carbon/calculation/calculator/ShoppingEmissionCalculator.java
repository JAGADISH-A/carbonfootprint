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
import java.util.Optional;

/**
 * {@link EmissionCalculator} for {@link ActivityCategory#SHOPPING} activities.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Reads the spend amount from {@link Activity#getAmount()}</li>
 *   <li>Looks up the category-level {@link EmissionFactor} from the
 *       {@link EmissionFactorRegistry}</li>
 *   <li>Computes: {@code Emission = spend × emission factor}</li>
 * </ul>
 *
 * <h3>Prototype scope</h3>
 * This is a spend-based estimation. The emission factor represents kg CO₂e per
 * unit of currency spent. Merchant industry specificity is not yet applied —
 * a single factor covers all SHOPPING activities.
 *
 * @see EmissionFactorRegistry
 * @see EmissionResult
 */
@Slf4j
@Component
public class ShoppingEmissionCalculator implements EmissionCalculator {

    private static final int ORDER = 10;

    private final EmissionFactorRegistry emissionFactorRegistry;

    @Autowired
    public ShoppingEmissionCalculator(@Lazy EmissionFactorRegistry emissionFactorRegistry) {
        this.emissionFactorRegistry = emissionFactorRegistry;
    }

    @Override
    public boolean supports(Activity activity) {
        return activity.getCategory() == ActivityCategory.SHOPPING;
    }

    @Override
    public Optional<EmissionResult> calculate(Activity activity) {
        if (!supports(activity)) {
            return Optional.empty();
        }

        BigDecimal spend = activity.getAmount();
        if (spend == null || spend.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("ShoppingEmissionCalculator — invalid or missing spend amount for activity {}",
                    activity.getId());
            return Optional.empty();
        }

        Instant validAt = activity.getOccurredAt() != null ? activity.getOccurredAt() : Instant.now();

        Optional<EmissionFactor> factorOpt = emissionFactorRegistry.find(
                ActivityCategory.SHOPPING, validAt);
        if (factorOpt.isEmpty()) {
            log.debug("ShoppingEmissionCalculator — no emission factor found for SHOPPING at {} for activity {}",
                    validAt, activity.getId());
            return Optional.empty();
        }

        EmissionFactor factor = factorOpt.get();
        BigDecimal carbonKg = spend.multiply(factor.getValue());

        log.debug("ShoppingEmissionCalculator — calculated: activityId={} spend={} × {} = {} kg CO₂e",
                activity.getId(), spend, factor.getValue(), carbonKg);

        EmissionResult result = EmissionResult.builder()
                .carbonKg(carbonKg)
                .activityId(activity.getId())
                .emissionFactor(factor)
                .activityQuantity(spend)
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
}
