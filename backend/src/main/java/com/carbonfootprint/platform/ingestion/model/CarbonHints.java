package com.carbonfootprint.platform.ingestion.model;

import com.carbonfootprint.platform.ingestion.enrichment.model.CabinClass;
import com.carbonfootprint.platform.ingestion.enrichment.model.CarbonActivityType;
import com.carbonfootprint.platform.ingestion.enrichment.model.EnergySource;
import com.carbonfootprint.platform.ingestion.enrichment.model.FuelType;
import com.carbonfootprint.platform.ingestion.enrichment.model.MerchantIndustry;
import com.carbonfootprint.platform.ingestion.enrichment.model.TransportMode;
import com.carbonfootprint.platform.ingestion.enrichment.model.VehicleType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * Immutable snapshot of carbon-domain hints inferred from an {@link ExtractionResult}
 * by the {@link com.carbonfootprint.platform.ingestion.enrichment.CarbonHintEngine}.
 *
 * <h3>Design constraints</h3>
 * <ul>
 *   <li>This is a <strong>pure DTO</strong> — it carries data only, with no merge or
 *       business logic. Merge logic lives in
 *       {@link com.carbonfootprint.platform.ingestion.enrichment.CarbonHintMerger}.</li>
 *   <li>All fields are nullable — a provider sets only the fields it can infer.</li>
 *   <li>Instances are immutable ({@code @Value}) and thread-safe.</li>
 *   <li>Domain-specific fields use type-safe enums instead of raw strings.</li>
 * </ul>
 *
 * <h3>Serialization</h3>
 * This object is serialized into {@code ExtractionResult.metadata["carbonHints"]} as a
 * plain {@code Map<String, Object>} by
 * {@link com.carbonfootprint.platform.ingestion.normalization.ActivityCarbonEnricher}.
 * Enum fields are serialized via {@link Enum#name()} so downstream consumers see the
 * same string values as before (backward compatible).
 *
 * <h3>Field reference</h3>
 * <ul>
 *   <li>{@code activityType}      — high-level carbon activity (FUEL, FLIGHT, TRANSPORT, …)</li>
 *   <li>{@code transportMode}     — ground transport variant (BUS, METRO, TAXI, TRAIN, AUTO, BIKE)</li>
 *   <li>{@code fuelType}          — fuel variant (PETROL, DIESEL, LPG, CNG, UNKNOWN)</li>
 *   <li>{@code energySource}      — electricity origin (GRID, SOLAR, WIND)</li>
 *   <li>{@code electricityUnit}   — unit of electricity consumed ("KWH")</li>
 *   <li>{@code fuelUnit}          — unit of fuel consumed ("LITRE", "KG")</li>
 *   <li>{@code estimatedDistance} — distance in kilometres</li>
 *   <li>{@code passengerCount}    — number of passengers (flights)</li>
 *   <li>{@code cabinClass}        — flight cabin (ECONOMY, BUSINESS, FIRST, PREMIUM_ECONOMY)</li>
 *   <li>{@code vehicleType}       — vehicle category (CAR, MOTORBIKE, TRUCK, AUTO_RICKSHAW)</li>
 *   <li>{@code merchantIndustry}  — retail segment (GROCERY, CLOTHING, ELECTRONICS, …)</li>
 *   <li>{@code confidence}        — mean provider confidence in range [0.0, 1.0]</li>
 * </ul>
 */
@Value
@Builder(toBuilder = true)
public class CarbonHints {

    /** High-level carbon activity type. */
    CarbonActivityType activityType;

    /** Ground-transport mode inferred for TRANSPORT activities. */
    TransportMode transportMode;

    /** Fuel variant inferred for FUEL activities. */
    FuelType fuelType;

    /** Energy source inferred for ELECTRICITY activities. */
    EnergySource energySource;

    /** Canonical electricity consumption unit. Almost always {@code "KWH"}. */
    String electricityUnit;

    /** Canonical fuel volume or mass unit ("LITRE", "KG"). */
    String fuelUnit;

    /** Estimated travel distance in kilometres, if determinable. */
    BigDecimal estimatedDistance;

    /** Number of passengers on a flight booking. */
    Integer passengerCount;

    /** Flight cabin class. */
    CabinClass cabinClass;

    /** Vehicle type for transport or fuel activities. */
    VehicleType vehicleType;

    /** Merchant industry segment for SHOPPING activities. */
    MerchantIndustry merchantIndustry;

    /**
     * Mean confidence across all inferred fields, in range [0.0, 1.0].
     * Computed exclusively by
     * {@link com.carbonfootprint.platform.ingestion.enrichment.CarbonHintEngine} —
     * providers set per-inference confidence which the engine averages.
     */
    Double confidence;

    // ── Factory ─────────────────────────────────────────────────────────────

    /** Returns an empty (all-null) instance — useful as a merge seed. */
    public static CarbonHints empty() {
        return CarbonHints.builder().build();
    }
}
