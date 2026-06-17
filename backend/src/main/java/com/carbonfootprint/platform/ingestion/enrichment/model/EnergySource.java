package com.carbonfootprint.platform.ingestion.enrichment.model;

/**
 * Electricity energy source inferred for {@link CarbonActivityType#ELECTRICITY} activities.
 *
 * <p>The emission factor for electricity depends heavily on whether the power
 * comes from the conventional grid, solar, wind, or other renewable sources.
 *
 * <p>Serialization: {@link #name()} is stored in {@code metadata.carbonHints.energySource}.
 */
public enum EnergySource {

    /** Conventional grid electricity (coal, gas, hydro mix). */
    GRID,

    /** Solar power (rooftop or utility-scale). */
    SOLAR,

    /** Wind power. */
    WIND
}
