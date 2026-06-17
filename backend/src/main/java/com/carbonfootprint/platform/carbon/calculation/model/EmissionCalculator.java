package com.carbonfootprint.platform.carbon.calculation.model;

import com.carbonfootprint.platform.activity.model.Activity;
import com.carbonfootprint.platform.activity.model.ActivityCategory;

import java.util.Optional;

/**
 * Strategy interface for a single carbon-emission calculator.
 *
 * <h3>Strategy Pattern</h3>
 * Each implementation encapsulates the emission-calculation logic for one
 * {@link ActivityCategory} or a specific subset of activities (e.g., fuel
 * purchases, electricity bills, flights, ground transport).
 * Calculators are discovered by Spring and injected as
 * {@code List<EmissionCalculator>} into the future
 * {@code CarbonCalculationEngine}, which orchestrates them.
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li>{@link #supports(Activity)} must be called before {@link #calculate(Activity)}.
 *       The engine only invokes calculators that claim support.</li>
 *   <li>{@link #calculate(Activity)} receives a fully normalised, validated
 *       {@link Activity} — it must NOT modify the activity.</li>
 *   <li>Returns an {@link Optional} — empty when the calculator cannot produce
 *       a result (e.g., missing quantity, unknown emission factor).</li>
 *   <li>Never returns {@code null} — return {@link Optional#empty()} instead.</li>
 * </ul>
 *
 * <h3>Determinism</h3>
 * Given the same input, a calculator must always produce the same output.
 * Calculators must be stateless and thread-safe.
 *
 * <h3>Ordering</h3>
 * When multiple calculators support the same activity, the one with the lowest
 * {@link #getOrder()} wins. This allows a specialised calculator (e.g.,
 * "IndianRailwaysTransportCalculator") to take precedence over a generic one
 * (e.g., "GenericTransportCalculator").
 *
 * <h3>Extensibility</h3>
 * To add calculation logic for a new category or a category-specific variant,
 * create a new {@code @Component} implementing this interface. The engine
 * picks it up automatically — no existing code needs to change.
 *
 * @see EmissionFactor
 * @see EmissionFactorRegistry
 * @see EmissionResult
 */
public interface EmissionCalculator {

    // ════════════════════════════════════════════════════════════════════════
    //  Strategy methods
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Returns {@code true} if this calculator can handle the given activity.
     *
     * <p>Implementations should inspect the activity's {@link Activity#getCategory()},
     * {@link Activity#getUnit()}, {@link Activity#getMetadata()}, and any other
     * relevant fields to determine applicability.
     *
     * <p>The engine calls this method first and only invokes
     * {@link #calculate(Activity)} for calculators that return {@code true}.
     *
     * @param activity the normalised activity to evaluate (never null)
     * @return {@code true} if this calculator supports the activity
     */
    boolean supports(Activity activity);

    /**
     * Calculates the carbon-dioxide-equivalent emissions for the given activity.
     *
     * <p>Contract:
     * <ul>
     *   <li>Must only be called after {@link #supports(Activity)} returned {@code true}.</li>
     *   <li>Must NOT modify the input {@link Activity}.</li>
     *   <li>Must return a fully populated {@link EmissionResult} with the emission
     *       value, emission factor audit trail, and calculation metadata.</li>
     *   <li>Return {@link Optional#empty()} when the calculation cannot be performed
     *       (e.g., missing quantity in metadata, no matching emission factor).</li>
     *   <li>Must NOT return {@code null}.</li>
     * </ul>
     *
     * @param activity the normalised activity to calculate emissions for (never null)
     * @return an {@link Optional} containing the calculation result, or empty if
     *         the calculation cannot be performed
     */
    Optional<EmissionResult> calculate(Activity activity);

    /**
     * Determines the execution order of this calculator within the engine.
     *
     * <p>Lower values run first. When multiple calculators support the same activity,
     * the one with the lowest order wins.
     *
     * <p>Recommended ordering:
     * <ol>
     *   <li>10 — Category-specific calculators with high confidence
     *       (e.g., fuel purchase with known litres)</li>
     *   <li>20 — Category-specific calculators with moderate confidence
     *       (e.g., electricity with kWh unit)</li>
     *   <li>30 — Category-specific calculators with lower confidence
     *       (e.g., transport from category only)</li>
     *   <li>50 — Generic fallback calculators
     *       (e.g., amount-based estimation)</li>
     *   <li>100 — Last-resort calculators
     *       (e.g., category-default average)</li>
     * </ol>
     *
     * @return execution order (lower = higher priority)
     */
    int getOrder();
}
