package com.carbonfootprint.platform.ingestion.enrichment.model;

/**
 * Vehicle type inferred for {@link CarbonActivityType#FUEL} or
 * {@link CarbonActivityType#TRANSPORT} activities.
 *
 * <p>Carbon emission factors vary by vehicle type due to engine size,
 * fuel efficiency, and passenger capacity differences.
 *
 * <p>Serialization: {@link #name()} is stored in {@code metadata.carbonHints.vehicleType}.
 */
public enum VehicleType {

    /** Passenger car (sedan, hatchback, SUV). */
    CAR,

    /** Motorbike, scooter, or motorcycle. */
    MOTORBIKE,

    /** Truck, lorry, or heavy goods vehicle. */
    TRUCK,

    /** Auto-rickshaw (three-wheeler). */
    AUTO_RICKSHAW
}
