package com.carbonfootprint.platform.carbon.calculation.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * Immutable snapshot of a single emission calculation result.
 *
 * <h3>Purpose</h3>
 * An {@code EmissionResult} captures the output of converting one {@link com.carbonfootprint.platform.activity.model.Activity}
 * into a carbon-dioxide-equivalent (CO<sub>2</sub>e) mass. It is the primary output
 * of the carbon calculation engine and serves as the input to aggregation, reporting,
 * and storage layers.
 *
 * <h3>What it contains</h3>
 * <ul>
 *   <li><strong>Emission value</strong> — the calculated mass in kilograms of CO<sub>2</sub>e</li>
 *   <li><strong>Activity reference</strong> — links back to the originating activity</li>
 *   <li><strong>Emission factor</strong> — the factor used, with full audit trail (source, version, method)</li>
 *   <li><strong>Breakdown</strong> — optional per-source decomposition for composite activities
 *       (e.g., a flight broken into fuel-burn and radiative-forcing components)</li>
 * </ul>
 *
 * <h3>Immutability</h3>
 * This class is immutable ({@code @Value}) and thread-safe by construction.
 * All fields are set at construction time and cannot be changed.
 *
 * <h3>Relationship to CarbonAssessment</h3>
 * {@code EmissionResult} is the <em>calculation-layer</em> output. The existing
 * {@link com.carbonfootprint.platform.carbon.model.CarbonAssessment} is the
 * <em>persistence-layer</em> output. A future adapter will map
 * {@code EmissionResult → CarbonAssessment} for Firestore storage.
 *
 * @see EmissionFactor
 * @see EmissionFactorRegistry
 */
@Value
@Builder
public class EmissionResult {

    // ════════════════════════════════════════════════════════════════════════
    //  Primary output
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Total greenhouse-gas emissions expressed in kilograms of CO<sub>2</sub>e
     * (carbon-dioxide equivalent).
     *
     * <p>This value accounts for all relevant greenhouse gases (CO₂, CH₄, N₂O, …)
     * converted to CO₂-equivalents using global warming potentials (GWP) from the
     * source methodology.
     */
    BigDecimal carbonKg;

    // ════════════════════════════════════════════════════════════════════════
    //  Activity reference
    // ════════════════════════════════════════════════════════════════════════

    /**
     * The ID of the {@link com.carbonfootprint.platform.activity.model.Activity}
     * that produced this emission. Links the calculation back to the source event.
     */
    String activityId;

    // ════════════════════════════════════════════════════════════════════════
    //  Emission factor audit trail
    // ════════════════════════════════════════════════════════════════════════

    /**
     * The emission factor that was applied to produce this result.
     * Carries the full provenance (source, version, methodology, validity period).
     */
    EmissionFactor emissionFactor;

    /**
     * The quantity of activity that was multiplied by the emission factor.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code 40.0} litres (fuel purchase)</li>
     *   {@code 250.0} kWh (electricity bill)</li>
     *   <li>{@code 1500.0} passenger-km (flight)</li>
     * </ul>
     *
     * <p>The unit is {@link EmissionFactor#getUnit()}.
     */
    BigDecimal activityQuantity;

    /**
     * The unit of the activity quantity (e.g., {@code "kWh"}, {@code "litre"},
     * {@code "passenger-km"}). Redundant with {@link EmissionFactor#getUnit()}
     * but included for self-contained readability.
     */
    String activityUnit;

    // ════════════════════════════════════════════════════════════════════════
    //  Calculation metadata
    // ════════════════════════════════════════════════════════════════════════

    /**
     * The calculation methodology used (e.g., {@code "Tier 1 — Default EF"},
     * {@code "Tier 2 — Country-specific EF"}).
     *
     * <p>Copied from {@link EmissionFactor#getMethodName()} at calculation time
     * for audit trail independence.
     */
    String methodology;

    /**
     * The instant at which this calculation was performed.
     */
    Instant calculatedAt;

    /**
     * Optional per-source breakdown of the emission for composite activities.
     *
     * <p>For simple activities (e.g., a single fuel purchase), this map is empty.
     * For composite activities (e.g., a multi-leg flight), it contains the
     * contribution of each component:
     *
     * <pre>{@code
     * {
     *   "fuel_burn":    120.5,
     *   "lto_cycle":     15.2,
     *   "radiative":     18.3
     * }
     * }</pre>
     *
     * <p>Keys are methodology-specific; values are in kg CO<sub>2</sub>e.
     */
    @Builder.Default
    Map<String, BigDecimal> breakdown = Collections.emptyMap();

    // ════════════════════════════════════════════════════════════════════════
    //  Derived helpers
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Returns {@code true} if this result contains a non-empty breakdown.
     *
     * @return true if the breakdown map has at least one entry
     */
    public boolean hasBreakdown() {
        return breakdown != null && !breakdown.isEmpty();
    }

    /**
     * Returns the emission factor value that was applied (kg CO₂e per unit).
     * Convenience shorthand for {@code emissionFactor.getValue()}.
     *
     * @return the emission factor value
     */
    public BigDecimal getEmissionFactorValue() {
        return emissionFactor.getValue();
    }

    /**
     * Returns the emission factor source that was applied.
     * Convenience shorthand for {@code emissionFactor.getSource()}.
     *
     * @return the source authority
     */
    public String getEmissionFactorSource() {
        return emissionFactor.getSource();
    }
}
