package com.carbonfootprint.platform.activity.model;

/**
 * High-level category of the real-world carbon-generating activity.
 *
 * <p>Categories are intentionally broad — sub-categorisation (e.g., domestic
 * vs long-haul flight) is expressed through {@code Activity.metadata} to
 * keep this enum stable.
 */
public enum ActivityCategory {

    /** Electricity consumption (home, office, EV charging). */
    ELECTRICITY,

    /** Food and beverage purchases. */
    FOOD,

    /** Petrol, diesel, LPG, or other fuel purchases. */
    FUEL,

    /** Air travel. */
    FLIGHT,

    /** Retail shopping (clothing, electronics, home goods). */
    SHOPPING,

    /** Ground transport (cab, train, bus, rideshare). */
    TRANSPORT,

    /** Accommodation (hotel, Airbnb). */
    ACCOMMODATION,

    /** Natural gas or LPG home heating / cooking. */
    GAS,

    /** Water consumption. */
    WATER,

    /** Category could not be determined — requires manual review. */
    OTHER
}
