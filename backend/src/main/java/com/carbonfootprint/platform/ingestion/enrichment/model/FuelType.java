package com.carbonfootprint.platform.ingestion.enrichment.model;

/**
 * Fuel variant inferred for {@link CarbonActivityType#FUEL} activities.
 *
 * <p>Serialization: {@link #name()} is stored in {@code metadata.carbonHints.fuelType}.
 */
public enum FuelType {

    /** Petrol / gasoline. */
    PETROL,

    /** Diesel / HSD (High Speed Diesel). */
    DIESEL,

    /** Liquefied Petroleum Gas. */
    LPG,

    /** Compressed Natural Gas. */
    CNG,

    /** Fuel station detected but specific fuel type could not be determined. */
    UNKNOWN
}
