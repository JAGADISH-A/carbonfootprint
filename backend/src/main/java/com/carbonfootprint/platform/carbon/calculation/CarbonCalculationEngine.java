package com.carbonfootprint.platform.carbon.calculation;

import com.carbonfootprint.platform.activity.model.Activity;
import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionCalculator;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Orchestrates all {@link EmissionCalculator} strategies to produce an
 * {@link EmissionResult} from a normalised {@link Activity}.
 *
 * <h3>Strategy Pattern</h3>
 * The engine is the <em>Context</em> in the Strategy pattern. It holds a
 * {@code List<EmissionCalculator>} injected by Spring and executed in
 * ascending order of {@link EmissionCalculator#getOrder()}.
 *
 * <h3>Resolution algorithm</h3>
 * <ol>
 *   <li>Calculators are sorted by {@link EmissionCalculator#getOrder()} (ascending)
 *       once at startup.</li>
 *   <li>For each activity, calculators are evaluated in order.</li>
 *   <li>The first calculator that both {@link EmissionCalculator#supports(Activity)}
 *       returns {@code true} for <em>and</em>
 *       {@link EmissionCalculator#calculate(Activity)} returns a present
 *       {@link Optional} wins.</li>
 *   <li>If no calculator supports the activity, or all supporting calculators
 *       return empty, the engine returns {@link Optional#empty()}.</li>
 * </ol>
 *
 * <h3>Error handling</h3>
 * Each calculator invocation is wrapped in a try-catch. A failing calculator
 * is logged and skipped — it does not prevent other calculators from running.
 *
 * <h3>Thread safety</h3>
 * The engine is stateless and thread-safe. The sorted calculator list is
 * immutable after construction.
 *
 * <h3>Extensibility</h3>
 * To add a new calculator, implement {@link EmissionCalculator} and annotate
 * with {@code @Component}. The engine picks it up automatically — no code
 * change required here.
 *
 * @see EmissionCalculator
 * @see EmissionResult
 */
@Slf4j
@Component
public class CarbonCalculationEngine {

    private final List<EmissionCalculator> sortedCalculators;

    /**
     * Constructs the engine with the full list of calculators, sorted by order once at startup.
     *
     * @param calculators all {@link EmissionCalculator} beans discovered by Spring
     */
    public CarbonCalculationEngine(List<EmissionCalculator> calculators) {
        this.sortedCalculators = calculators.stream()
                .sorted(Comparator.comparingInt(EmissionCalculator::getOrder))
                .collect(Collectors.toUnmodifiableList());

        log.info("CarbonCalculationEngine initialized with {} calculator(s): {}",
                sortedCalculators.size(),
                sortedCalculators.stream()
                        .map(c -> c.getClass().getSimpleName() + "[order=" + c.getOrder() + "]")
                        .collect(Collectors.joining(", ")));
    }

    /**
     * Calculates the carbon-dioxide-equivalent emissions for the given activity.
     *
     * <p>The engine evaluates calculators in ascending order of
     * {@link EmissionCalculator#getOrder()}. The first calculator that:
     * <ol>
     *   <li>Returns {@code true} from {@link EmissionCalculator#supports(Activity)}</li>
     *   <li>Returns a present {@link Optional} from {@link EmissionCalculator#calculate(Activity)}</li>
     * </ol>
     * determines the result.
     *
     * <p>This method never modifies the input {@code activity}.
     *
     * @param activity the normalised activity to calculate emissions for (must not be null)
     * @return an {@link Optional} containing the calculation result, or empty if no
     *         calculator could produce a result
     * @throws IllegalArgumentException if activity is null
     */
    public Optional<EmissionResult> calculate(Activity activity) {
        if (activity == null) {
            throw new IllegalArgumentException("Activity must not be null");
        }

        log.debug("CarbonCalculationEngine — calculating emissions: id={} category={} merchant={} unit={} amount={}",
                activity.getId(), activity.getCategory(), activity.getMerchant(),
                activity.getUnit(), activity.getAmount());

        for (EmissionCalculator calculator : sortedCalculators) {
            String calculatorName = calculator.getClass().getSimpleName();

            if (!calculator.supports(activity)) {
                log.debug("CarbonCalculationEngine — {} does not support activity {}",
                        calculatorName, activity.getId());
                continue;
            }

            log.debug("CarbonCalculationEngine — {} supports activity {}, executing calculation",
                    calculatorName, activity.getId());

            Optional<EmissionResult> result = safeCalculate(calculator, activity);
            if (result.isPresent()) {
                EmissionResult emissionResult = result.get();
                log.info("CarbonCalculationEngine — calculation complete: activityId={} calculator={} " +
                                "carbonKg={} emissionFactor={} methodology={}",
                        activity.getId(), calculatorName,
                        emissionResult.getCarbonKg(),
                        emissionResult.getEmissionFactorValue(),
                        emissionResult.getMethodology());
                return result;
            }

            log.debug("CarbonCalculationEngine — {} returned empty for activity {}",
                    calculatorName, activity.getId());
        }

        log.debug("CarbonCalculationEngine — no calculator produced a result for activity {} (category={})",
                activity.getId(), activity.getCategory());

        return Optional.empty();
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Safely invokes the calculator's {@code calculate} method, catching and
     * logging any exception without halting the pipeline.
     *
     * @return the calculator result, or {@link Optional#empty()} on failure
     */
    private Optional<EmissionResult> safeCalculate(EmissionCalculator calculator, Activity activity) {
        try {
            Optional<EmissionResult> result = calculator.calculate(activity);
            return result != null ? result : Optional.empty();
        } catch (Exception e) {
            log.error("CarbonCalculationEngine — calculator {} threw an unexpected exception for activity {}: {}",
                    calculator.getClass().getSimpleName(), activity.getId(), e.getMessage(), e);
            return Optional.empty();
        }
    }
}
