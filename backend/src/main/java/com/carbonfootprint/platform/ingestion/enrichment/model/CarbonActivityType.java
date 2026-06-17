package com.carbonfootprint.platform.ingestion.enrichment.model;

/**
 * High-level carbon activity type inferred by the enrichment layer.
 *
 * <p>These values map to the subset of
 * {@link com.carbonfootprint.platform.activity.model.ActivityCategory} values that are
 * relevant for carbon footprint estimation. The enrichment layer uses this enum rather
 * than {@code ActivityCategory} directly to maintain a clean separation between the
 * domain model and the inference layer.
 *
 * <p>Serialization: {@link #name()} is stored in {@code metadata.carbonHints.activityType}
 * to preserve backward compatibility with downstream consumers that expect plain strings.
 */
public enum CarbonActivityType {

    /** Petrol, diesel, LPG, CNG, or other fuel purchases. */
    FUEL,

    /** Electricity consumption (home, office, EV charging). */
    ELECTRICITY,

    /** Air travel. */
    FLIGHT,

    /** Ground transport (bus, train, metro, taxi, auto, bike). */
    TRANSPORT,

    /** Retail shopping (clothing, electronics, grocery, pharmacy). */
    SHOPPING
}
