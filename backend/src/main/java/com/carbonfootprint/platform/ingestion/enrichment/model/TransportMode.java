package com.carbonfootprint.platform.ingestion.enrichment.model;

/**
 * Ground-transport mode inferred for {@link CarbonActivityType#TRANSPORT} activities.
 *
 * <p>Serialization: {@link #name()} is stored in {@code metadata.carbonHints.transportMode}.
 */
public enum TransportMode {

    /** Public bus (city bus, intercity bus, KSRTC, BMTC, RedBus). */
    BUS,

    /** Metro rail (DMRC, BMRC, Hyderabad Metro, etc.). */
    METRO,

    /** Railway (IRCTC, Indian Railways). */
    TRAIN,

    /** Taxi / ride-share (Uber, Ola, Meru, cab). */
    TAXI,

    /** Auto-rickshaw. */
    AUTO,

    /** Bicycle / cycle-share (Yulu, Bounce). */
    BIKE
}
