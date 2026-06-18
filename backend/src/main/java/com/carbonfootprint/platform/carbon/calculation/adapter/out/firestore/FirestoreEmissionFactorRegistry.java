package com.carbonfootprint.platform.carbon.calculation.adapter.out.firestore;

import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionFactor;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionFactorRegistry;
import com.carbonfootprint.platform.carbon.calculation.port.out.EmissionFactorRepository;
import com.carbonfootprint.platform.ingestion.enrichment.model.FuelType;
import com.carbonfootprint.platform.ingestion.enrichment.model.TransportMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Firestore-backed implementation of {@link EmissionFactorRegistry} with
 * in-memory caching.
 *
 * <h3>Design</h3>
 * <ul>
 *   <li>Loads all emission factors from {@link EmissionFactorRepository} at
 *       startup and caches them in a {@link ConcurrentHashMap}.</li>
 *   <li>All {@code find()} methods operate on the in-memory cache — no
 *       Firestore calls on every request.</li>
 *   <li>Cache is built once at construction time. For factor updates,
 *       a future enhancement can add a TTL or manual refresh.</li>
 * </ul>
 *
 * <h3>Lookup priority</h3>
 * When multiple factors match, the most specific one wins:
 * <ol>
 *   <li>Full specificity (category + fuelType + transportMode + region)</li>
 *   <li>Category + fuelType (no region)</li>
 *   <li>Category + transportMode (no region)</li>
 *   <li>Category only</li>
 * </ol>
 *
 * @see EmissionFactorRepository
 * @see EmissionFactorRegistry
 */
@Slf4j
@Component
@Profile("!stub")
public class FirestoreEmissionFactorRegistry implements EmissionFactorRegistry {

    private final EmissionFactorRepository repository;
    private final List<EmissionFactor> allFactors;

    public FirestoreEmissionFactorRegistry(EmissionFactorRepository repository) {
        this.repository = repository;
        this.allFactors = loadAllFactors();
        log.info("FirestoreEmissionFactorRegistry initialised with {} emission factors", allFactors.size());
    }

    @Override
    public Optional<EmissionFactor> find(ActivityCategory category, Instant validAt) {
        return allFactors.stream()
                .filter(f -> f.getCategory() == category)
                .filter(f -> f.isValidAt(validAt))
                .filter(f -> f.getFuelType().isEmpty())
                .filter(f -> f.getTransportMode().isEmpty())
                .min(Comparator.comparing(EmissionFactor::getValidFrom).reversed());
    }

    @Override
    public Optional<EmissionFactor> find(ActivityCategory category, FuelType fuelType, Instant validAt) {
        return allFactors.stream()
                .filter(f -> f.getCategory() == category)
                .filter(f -> f.getFuelType().orElse(null) == fuelType)
                .filter(f -> f.isValidAt(validAt))
                .filter(f -> f.getTransportMode().isEmpty())
                .min(Comparator.comparing(EmissionFactor::getValidFrom).reversed());
    }

    @Override
    public Optional<EmissionFactor> find(ActivityCategory category, TransportMode transportMode, Instant validAt) {
        return allFactors.stream()
                .filter(f -> f.getCategory() == category)
                .filter(f -> f.getTransportMode().orElse(null) == transportMode)
                .filter(f -> f.isValidAt(validAt))
                .filter(f -> f.getFuelType().isEmpty())
                .min(Comparator.comparing(EmissionFactor::getValidFrom).reversed());
    }

    @Override
    public Optional<EmissionFactor> find(ActivityCategory category,
                                          FuelType fuelType,
                                          TransportMode transportMode,
                                          String region,
                                          Instant validAt) {
        return allFactors.stream()
                .filter(f -> f.getCategory() == category)
                .filter(f -> matchesOptional(f.getFuelType(), fuelType))
                .filter(f -> matchesOptional(f.getTransportMode(), transportMode))
                .filter(f -> matchesRegion(f.getRegion(), region))
                .filter(f -> f.isValidAt(validAt))
                .min(Comparator.comparing(EmissionFactor::getValidFrom).reversed());
    }

    @Override
    public List<EmissionFactor> findAll() {
        return List.copyOf(allFactors);
    }

    @Override
    public List<EmissionFactor> findByCategory(ActivityCategory category) {
        return allFactors.stream()
                .filter(f -> f.getCategory() == category)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public int count() {
        return allFactors.size();
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private List<EmissionFactor> loadAllFactors() {
        try {
            List<EmissionFactor> factors = repository.findAll();
            log.info("Loaded {} emission factors from repository", factors.size());
            return List.copyOf(factors);
        } catch (Exception e) {
            log.error("Failed to load emission factors from repository: {}", e.getMessage());
            return List.of();
        }
    }

    private <T> boolean matchesOptional(Optional<T> factorValue, T expected) {
        return factorValue.isEmpty() || factorValue.get() == expected;
    }

    private boolean matchesRegion(Optional<String> factorRegion, String expectedRegion) {
        if (expectedRegion == null) return true;
        return factorRegion.filter(r -> r.equals(expectedRegion)).isPresent();
    }
}
