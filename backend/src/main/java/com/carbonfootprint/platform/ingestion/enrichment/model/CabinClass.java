package com.carbonfootprint.platform.ingestion.enrichment.model;

/**
 * Flight cabin class inferred for {@link CarbonActivityType#FLIGHT} activities.
 *
 * <p>Carbon emission factors vary significantly by cabin class because business and
 * first class seats occupy more physical space per passenger.
 *
 * <p>Serialization: {@link #name()} is stored in {@code metadata.carbonHints.cabinClass}.
 */
public enum CabinClass {

    /** Standard economy cabin. */
    ECONOMY,

    /** Premium economy (extra legroom, upgraded service). */
    PREMIUM_ECONOMY,

    /** Business class. */
    BUSINESS,

    /** First class. */
    FIRST
}
