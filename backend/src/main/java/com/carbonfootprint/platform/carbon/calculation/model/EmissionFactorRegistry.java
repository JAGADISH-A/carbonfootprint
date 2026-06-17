package com.carbonfootprint.platform.carbon.calculation.model;

import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.ingestion.enrichment.model.FuelType;
import com.carbonfootprint.platform.ingestion.enrichment.model.TransportMode;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Port (interface) for looking up {@link EmissionFactor} instances.
 *
 * <h3>Clean Architecture role</h3>
 * This interface is a <em>driven port</em> in the hexagonal architecture.
 * The carbon calculation engine depends on this abstraction — it does not know
 * how emission factors are stored or sourced. Adapters (e.g., a Firestore-backed
 * implementation, a YAML-config loader, or an in-memory test stub) provide the
 * concrete implementation.
 *
 * <h3>Lookup semantics</h3>
 * Emission factors are multi-dimensional: different regions, fuel types, transport
 * modes, and time periods have different factors. The lookup methods on this
 * interface reflect those dimensions:
 *
 * <ul>
 *   <li>{@link #find(ActivityCategory, Instant)} — broadest lookup by category and time</li>
 *   <li>{@link #find(ActivityCategory, FuelType, Instant)} — fuel-specific lookup</li>
 *   <li>{@link #find(ActivityCategory, TransportMode, Instant)} — transport-specific lookup</li>
 *   <li>{@link #find(ActivityCategory, FuelType, TransportMode, String, Instant)} — full specificity</li>
 * </ul>
 *
 * <p>All methods return the <em>most specific</em> matching factor. If no region-specific
 * factor exists, the engine falls back to a global factor. If no fuel-specific factor
 * exists, it falls back to a category-only factor.
 *
 * <h3>Thread safety</h3>
 * Implementations must be thread-safe. The registry is shared across requests
 * and may be accessed concurrently.
 *
 * <h3>Extensibility</h3>
 * To add a new emission factor source (e.g., a new regulatory database), implement
 * this interface and register as a Spring {@code @Component}. The calculation
 * engine picks it up automatically.
 *
 * @see EmissionFactor
 * @see EmissionResult
 */
public interface EmissionFactorRegistry {

    /**
     * Finds the best-matching emission factor for the given category and time.
     *
     * <p>This is the broadest lookup — it matches on category alone. Use the
     * more specific overloads when fuel type, transport mode, or region are known.
     *
     * @param category  the activity category (required)
     * @param validAt   the instant the factor must be valid at (typically the
     *                  activity's {@code occurredAt} timestamp)
     * @return the best-matching factor, or empty if none found
     */
    Optional<EmissionFactor> find(ActivityCategory category, Instant validAt);

    /**
     * Finds the best-matching emission factor for a fuel-specific activity.
     *
     * @param category  the activity category (typically {@link ActivityCategory#FUEL})
     * @param fuelType  the fuel type variant (required)
     * @param validAt   the instant the factor must be valid at
     * @return the best-matching factor, or empty if none found
     */
    Optional<EmissionFactor> find(ActivityCategory category, FuelType fuelType, Instant validAt);

    /**
     * Finds the best-matching emission factor for a transport-specific activity.
     *
     * @param category      the activity category (typically {@link ActivityCategory#TRANSPORT})
     * @param transportMode the transport mode variant (required)
     * @param validAt       the instant the factor must be valid at
     * @return the best-matching factor, or empty if none found
     */
    Optional<EmissionFactor> find(ActivityCategory category, TransportMode transportMode, Instant validAt);

    /**
     * Finds the best-matching emission factor with full specificity.
     *
     * <p>All dimensions are used for matching. {@code null} values for
     * {@code fuelType}, {@code transportMode}, or {@code region} mean
     * "match any" (i.e., fall back to broader factors).
     *
     * @param category      the activity category (required)
     * @param fuelType      the fuel type, or {@code null} if not applicable
     * @param transportMode the transport mode, or {@code null} if not applicable
     * @param region        the region code, or {@code null} for global default
     * @param validAt       the instant the factor must be valid at
     * @return the best-matching factor, or empty if none found
     */
    Optional<EmissionFactor> find(ActivityCategory category,
                                  FuelType fuelType,
                                  TransportMode transportMode,
                                  String region,
                                  Instant validAt);

    /**
     * Returns all emission factors currently registered in this registry.
     *
     * <p>Useful for admin dashboards, audit exports, and testing.
     * The returned list is unmodifiable — callers must not modify it.
     *
     * @return an unmodifiable list of all registered factors
     */
    List<EmissionFactor> findAll();

    /**
     * Returns all emission factors that match the given category.
     *
     * @param category the activity category to filter by
     * @return an unmodifiable list of matching factors (may be empty)
     */
    List<EmissionFactor> findByCategory(ActivityCategory category);

    /**
     * Returns the total number of emission factors registered.
     *
     * @return the count of registered factors
     */
    int count();
}
