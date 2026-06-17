package com.carbonfootprint.platform.carbon.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Domain object representing the calculated environmental impact of an Activity.
 *
 * <h3>Design Rationale</h3>
 * Carbon calculations are separated from {@link com.carbonfootprint.platform.activity.model.Activity}
 * for these reasons:
 * <ul>
 *   <li><strong>Independent lifecycle</strong> — emission factors are updated
 *       periodically. A new assessment can be created for any existing activity
 *       without mutating the factual record.</li>
 *   <li><strong>Auditability</strong> — each calculation is traceable to a
 *       specific methodology and emission factor version.</li>
 *   <li><strong>Multiple assessments</strong> — the same activity may be assessed
 *       by different methodologies (GHG Protocol, IPCC, custom).</li>
 * </ul>
 *
 * <h3>Future recalculation strategy</h3>
 * When emission factors change, create a new {@code CarbonAssessment} record.
 * Do NOT modify or delete the previous assessment — keep full history.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarbonAssessment {

    /**
     * Globally unique identifier for this assessment (UUID v4).
     */
    private String assessmentId;

    /**
     * Reference to the {@link com.carbonfootprint.platform.activity.model.Activity}
     * this assessment belongs to.
     */
    private String activityId;

    /**
     * Reference to the authenticated user who owns this assessment.
     */
    private String userId;

    /**
     * Calculated carbon dioxide equivalent in kilograms (kg CO₂e).
     * The primary output of the carbon calculation engine.
     */
    private BigDecimal carbonKg;

    /**
     * Name of the calculation methodology applied.
     * Examples: "GHG_PROTOCOL_SCOPE_2", "IPCC_AR6", "DEFRA_2024", "CUSTOM".
     */
    private String methodology;

    /**
     * Emission factor dataset version used for this calculation.
     * Examples: "DEFRA-2024-v1.0", "IEA-GRID-2024-IN".
     * Enables reproducibility and audit trails.
     */
    private String emissionFactorVersion;

    /**
     * The specific emission factor value applied (kg CO₂e per unit).
     * Stored for full transparency and audit trails.
     */
    private BigDecimal emissionFactorValue;

    /**
     * Unit of the emission factor (e.g., "kg CO2e/kWh", "kg CO2e/litre").
     */
    private String emissionFactorUnit;

    /**
     * Timestamp when this assessment was calculated.
     */
    private Instant calculatedAt;

    /**
     * Extensible metadata for assessment-specific details.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code {"gridRegion": "IN-South", "renewablePercentage": 0.12}}</li>
     *   <li>{@code {"flightDistance_km": 6700, "radiativeForcing": true}}</li>
     * </ul>
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
