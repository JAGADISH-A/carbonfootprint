package com.carbonfootprint.platform.carbon.calculation.model;

import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.ingestion.enrichment.model.FuelType;
import com.carbonfootprint.platform.ingestion.enrichment.model.TransportMode;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/**
 * An immutable emission factor that maps a unit of activity to its carbon dioxide
 * equivalent (CO<sub>2</sub>e) emissions.
 *
 * <h3>Purpose</h3>
 * An {@code EmissionFactor} encodes the scientific or regulatory relationship between
 * an activity (e.g., burning one litre of petrol, consuming one kWh of electricity)
 * and the resulting greenhouse-gas emissions expressed in kilograms of CO<sub>2</sub>e.
 *
 * <h3>Specificity dimensions</h3>
 * Emission factors are not one-size-fits-all. Different regions, fuel types, transport
 * modes, and time periods have different factors. This model captures those dimensions:
 * <ul>
 *   <li>{@link #category}       — the high-level activity category (FUEL, ELECTRICITY, …)</li>
 *   <li>{@link #fuelType}       — fuel variant (PETROL, DIESEL, …), nullable</li>
 *   <li>{@link #transportMode}  — transport variant (BUS, TRAIN, …), nullable</li>
 *   <li>{@link #region}         — geographic scope (e.g., "IN", "US", "GLOBAL"), nullable</li>
 * </ul>
 *
 * <h3>Validity period</h3>
 * Emission factors change over time as grids get cleaner or methodologies improve.
 * Each factor carries a {@link #validFrom} and optional {@link #validTo} timestamp
 * so the engine can select the factor that was active at the time of the activity.
 *
 * <h3>Audit trail</h3>
 * Every factor records its {@link #source} (e.g., "IPCC 2006", "India MoEFCC 2023"),
 * {@link #version} for deduplication, and {@link #methodName} describing the
 * calculation methodology (e.g., "Tier 1", "EFDB").
 *
 * <h3>Immutability</h3>
 * This class is immutable ({@code @Value}) and thread-safe by construction.
 *
 * <h3>Usage</h3>
 * Instances are created by {@link EmissionFactorRegistry} lookups and consumed by
 * the carbon calculation engine (not yet implemented).
 *
 * @see EmissionResult
 * @see EmissionFactorRegistry
 */
@Value
@Builder
public class EmissionFactor {

    // ════════════════════════════════════════════════════════════════════════
    //  Identity
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Unique identifier for this emission factor (e.g., UUID or a composite key
     * such {@code "IN:ELECTRICITY:GRID:2024-v2"}).
     */
    String id;

    // ════════════════════════════════════════════════════════════════════════
    //  Value
    // ════════════════════════════════════════════════════════════════════════

    /**
     * The emission factor value: kilograms of CO<sub>2</sub>e per unit of activity.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code 0.82} kg CO₂e per kWh (Indian grid electricity, 2024)</li>
     *   <li>{@code 2.31} kg CO₂e per litre (petrol, well-to-wheel)</li>
     *   <li>{@code 0.14} kg CO₂e per passenger-km (Indian Railways, Sleeper)</li>
     * </ul>
     */
    BigDecimal value;

    /**
     * The unit that the emission factor applies to, expressed as a human-readable
     * string (e.g., {@code "kWh"}, {@code "litre"}, {@code "passenger-km"},
     * {@code "kg"}, {@code "km"}).
     *
     * <p>This is the denominator of the factor: {@code value kg CO₂e / unit}.
     */
    String unit;

    // ════════════════════════════════════════════════════════════════════════
    //  Specificity dimensions
    // ════════════════════════════════════════════════════════════════════════

    /**
     * The high-level activity category this factor applies to.
     * Required — every factor must target at least one category.
     */
    ActivityCategory category;

    /**
     * Fuel type variant, or {@code null} if the factor is not fuel-specific.
     *
     * <p>Used to disambiguate within {@link ActivityCategory#FUEL}:
     * petrol has a different emission factor than diesel or CNG.
     */
    FuelType fuelType;

    /**
     * Transport mode variant, or {@code null} if the factor is not transport-specific.
     *
     * <p>Used to disambiguate within {@link ActivityCategory#TRANSPORT}:
     * a bus emits differently per passenger-km than a train or taxi.
     */
    TransportMode transportMode;

    /**
     * Geographic region this factor applies to, or {@code null} for global defaults.
     *
     * <p>Region codes follow ISO 3166-1 alpha-2 (e.g., {@code "IN"}, {@code "US"})
     * or use {@code "GLOBAL"} for worldwide averages.
     */
    String region;

    // ════════════════════════════════════════════════════════════════════════
    //  Validity period
    // ════════════════════════════════════════════════════════════════════════

    /**
     * The instant from which this factor is valid (inclusive).
     * Used to select the correct factor for an activity's {@code occurredAt} timestamp.
     */
    Instant validFrom;

    /**
     * The instant until which this factor is valid (inclusive), or {@code null}
     * if this factor has no expiration (still current).
     */
    Instant validTo;

    // ════════════════════════════════════════════════════════════════════════
    //  Audit / provenance
    // ════════════════════════════════════════════════════════════════════════

    /**
     * The authoritative source of this factor (e.g., {@code "IPCC AR6"},
     * {@code "India MoEFCC 2023"}, {@code "DEFRA 2024"}).
     */
    String source;

    /**
     * Version identifier for deduplication and tracking (e.g., {@code "2024-v2"},
     * {@code "AR6-WGIII"}).
     */
    String version;

    /**
     * The calculation methodology used to derive this factor
     * (e.g., {@code "Tier 1 — Default emission factors"}, {@code "EFDB"}).
     */
    String methodName;

    // ════════════════════════════════════════════════════════════════════════
    //  Derived helpers
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Returns {@code true} if this factor is currently valid (its validity period
     * includes the current instant).
     *
     * @return true if {@link #validFrom} is in the past and {@link #validTo} is
     *         null or in the future
     */
    public boolean isCurrentlyValid() {
        Instant now = Instant.now();
        return !validFrom.isAfter(now)
                && (validTo == null || !validTo.isBefore(now));
    }

    /**
     * Returns {@code true} if this factor was valid at the given timestamp.
     *
     * @param at the instant to check
     * @return true if the factor's validity period includes the given instant
     */
    public boolean isValidAt(Instant at) {
        return !validFrom.isAfter(at)
                && (validTo == null || !validTo.isBefore(at));
    }

    /**
     * Returns the fuel type as an Optional, useful for fluent null-safe access.
     *
     * @return the fuel type, or empty if not fuel-specific
     */
    public Optional<FuelType> getFuelType() {
        return Optional.ofNullable(fuelType);
    }

    /**
     * Returns the transport mode as an Optional, useful for fluent null-safe access.
     *
     * @return the transport mode, or empty if not transport-specific
     */
    public Optional<TransportMode> getTransportMode() {
        return Optional.ofNullable(transportMode);
    }

    /**
     * Returns the region as an Optional, useful for fluent null-safe access.
     *
     * @return the region code, or empty if global/default
     */
    public Optional<String> getRegion() {
        return Optional.ofNullable(region);
    }
}
