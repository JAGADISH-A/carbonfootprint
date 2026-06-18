package com.carbonfootprint.platform.carbon.calculation.adapter.out.firestore;

import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionFactor;
import com.carbonfootprint.platform.ingestion.enrichment.model.FuelType;
import com.carbonfootprint.platform.ingestion.enrichment.model.TransportMode;
import com.carbonfootprint.platform.shared.constant.ApiConstants;
import com.google.cloud.firestore.Firestore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Seeds the Firestore {@code emission_factors} collection with default values
 * on application startup.
 *
 * <h3>Behaviour</h3>
 * <ul>
 *   <li>Runs once at startup via {@link CommandLineRunner}.</li>
 *   <li>Checks whether the collection is already populated.</li>
 *   <li>If empty, inserts default emission factors for all supported categories.</li>
 *   <li>If non-empty, does nothing — existing data is never overwritten.</li>
 * </ul>
 *
 * <h3>Profile</h3>
 * Active only when the {@code stub} profile is NOT active (same as Firestore).
 */
@Slf4j
@Component
@Profile("!stub")
public class EmissionFactorSeeder implements CommandLineRunner {

    private final Firestore firestore;
    private final String collectionName;

    public EmissionFactorSeeder(Firestore firestore) {
        this.firestore = firestore;
        this.collectionName = ApiConstants.COLLECTION_EMISSION_FACTORS;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("EmissionFactorSeeder — checking if '{}' collection needs seeding...", collectionName);

        if (isCollectionPopulated()) {
            log.info("EmissionFactorSeeder — collection '{}' already has data, skipping seed.",
                    collectionName);
            return;
        }

        List<EmissionFactor> defaults = defaultEmissionFactors();
        insertAll(defaults);
        log.info("EmissionFactorSeeder — seeded {} default emission factors into '{}'.",
                defaults.size(), collectionName);
    }

    // ── Collection check ──────────────────────────────────────────────────

    private boolean isCollectionPopulated() {
        try {
            return !firestore.collection(collectionName).limit(1).get().get().isEmpty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("EmissionFactorSeeder — interrupted while checking collection, assuming empty.");
            return false;
        } catch (ExecutionException e) {
            log.error("EmissionFactorSeeder — failed to check collection: {}", e.getCause().getMessage());
            return false;
        }
    }

    // ── Insert ────────────────────────────────────────────────────────────

    private void insertAll(List<EmissionFactor> factors) {
        for (EmissionFactor factor : factors) {
            try {
                firestore.collection(collectionName)
                        .document(factor.getId())
                        .set(toFirestoreMap(factor))
                        .get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("EmissionFactorSeeder — interrupted while inserting factor '{}'", factor.getId());
                throw new RuntimeException("Seeder interrupted", e);
            } catch (ExecutionException e) {
                log.error("EmissionFactorSeeder — failed to insert factor '{}': {}",
                        factor.getId(), e.getCause().getMessage());
                throw new RuntimeException("Seeder insert failed for " + factor.getId(), e.getCause());
            }
        }
    }

    // ── Default emission factors ──────────────────────────────────────────

    static List<EmissionFactor> defaultEmissionFactors() {
        Instant validFrom = Instant.parse("2024-01-01T00:00:00Z");

        return List.of(
                // ── FUEL ──────────────────────────────────────────────
                EmissionFactor.builder()
                        .id("ef-fuel-petrol")
                        .category(ActivityCategory.FUEL)
                        .fuelType(FuelType.PETROL)
                        .value(new BigDecimal("2.31"))
                        .unit("litre")
                        .source("DEFRA 2024")
                        .version("2024-v1")
                        .methodName("Tier 1 — Default emission factors")
                        .validFrom(validFrom)
                        .build(),
                EmissionFactor.builder()
                        .id("ef-fuel-diesel")
                        .category(ActivityCategory.FUEL)
                        .fuelType(FuelType.DIESEL)
                        .value(new BigDecimal("2.68"))
                        .unit("litre")
                        .source("DEFRA 2024")
                        .version("2024-v1")
                        .methodName("Tier 1 — Default emission factors")
                        .validFrom(validFrom)
                        .build(),
                EmissionFactor.builder()
                        .id("ef-fuel-lpg")
                        .category(ActivityCategory.FUEL)
                        .fuelType(FuelType.LPG)
                        .value(new BigDecimal("1.51"))
                        .unit("litre")
                        .source("DEFRA 2024")
                        .version("2024-v1")
                        .methodName("Tier 1 — Default emission factors")
                        .validFrom(validFrom)
                        .build(),
                EmissionFactor.builder()
                        .id("ef-fuel-cng")
                        .category(ActivityCategory.FUEL)
                        .fuelType(FuelType.CNG)
                        .value(new BigDecimal("2.50"))
                        .unit("kg")
                        .source("DEFRA 2024")
                        .version("2024-v1")
                        .methodName("Tier 1 — Default emission factors")
                        .validFrom(validFrom)
                        .build(),

                // ── ELECTRICITY ───────────────────────────────────────
                EmissionFactor.builder()
                        .id("ef-electricity-grid")
                        .category(ActivityCategory.ELECTRICITY)
                        .value(new BigDecimal("0.82"))
                        .unit("kWh")
                        .source("India MoEFCC 2024")
                        .version("2024-v1")
                        .methodName("Tier 1 — Grid electricity emission factor")
                        .validFrom(validFrom)
                        .build(),

                // ── FLIGHT ────────────────────────────────────────────
                EmissionFactor.builder()
                        .id("ef-flight-economy")
                        .category(ActivityCategory.FLIGHT)
                        .value(new BigDecimal("0.14"))
                        .unit("passenger-km")
                        .source("DEFRA 2024")
                        .version("2024-v1")
                        .methodName("Tier 1 — Default emission factors")
                        .validFrom(validFrom)
                        .build(),
                EmissionFactor.builder()
                        .id("ef-flight-business")
                        .category(ActivityCategory.FLIGHT)
                        .value(new BigDecimal("0.42"))
                        .unit("passenger-km")
                        .source("DEFRA 2024")
                        .version("2024-v1")
                        .methodName("Tier 1 — Default emission factors")
                        .validFrom(validFrom)
                        .build(),

                // ── TRANSPORT ─────────────────────────────────────────
                EmissionFactor.builder()
                        .id("ef-transport-bus")
                        .category(ActivityCategory.TRANSPORT)
                        .transportMode(TransportMode.BUS)
                        .value(new BigDecimal("0.089"))
                        .unit("passenger-km")
                        .source("DEFRA 2024")
                        .version("2024-v1")
                        .methodName("Tier 1 — Default emission factors")
                        .validFrom(validFrom)
                        .build(),
                EmissionFactor.builder()
                        .id("ef-transport-metro")
                        .category(ActivityCategory.TRANSPORT)
                        .transportMode(TransportMode.METRO)
                        .value(new BigDecimal("0.05"))
                        .unit("passenger-km")
                        .source("DEFRA 2024")
                        .version("2024-v1")
                        .methodName("Tier 1 — Default emission factors")
                        .validFrom(validFrom)
                        .build(),
                EmissionFactor.builder()
                        .id("ef-transport-train")
                        .category(ActivityCategory.TRANSPORT)
                        .transportMode(TransportMode.TRAIN)
                        .value(new BigDecimal("0.04"))
                        .unit("passenger-km")
                        .source("DEFRA 2024")
                        .version("2024-v1")
                        .methodName("Tier 1 — Default emission factors")
                        .validFrom(validFrom)
                        .build(),
                EmissionFactor.builder()
                        .id("ef-transport-taxi")
                        .category(ActivityCategory.TRANSPORT)
                        .transportMode(TransportMode.TAXI)
                        .value(new BigDecimal("0.21"))
                        .unit("passenger-km")
                        .source("DEFRA 2024")
                        .version("2024-v1")
                        .methodName("Tier 1 — Default emission factors")
                        .validFrom(validFrom)
                        .build(),

                // ── SHOPPING ──────────────────────────────────────────
                EmissionFactor.builder()
                        .id("ef-shopping-default")
                        .category(ActivityCategory.SHOPPING)
                        .value(new BigDecimal("0.00085"))
                        .unit("INR")
                        .source("DEFRA 2024")
                        .version("2024-v1")
                        .methodName("Spend-based — Default emission factors")
                        .validFrom(validFrom)
                        .build()
        );
    }

    // ── Mapping ───────────────────────────────────────────────────────────

    static java.util.Map<String, Object> toFirestoreMap(EmissionFactor f) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", f.getId());
        map.put("category", f.getCategory() != null ? f.getCategory().name() : null);
        map.put("fuelType", f.getFuelType().orElse(null) != null
                ? f.getFuelType().get().name() : null);
        map.put("transportMode", f.getTransportMode().orElse(null) != null
                ? f.getTransportMode().get().name() : null);
        map.put("region", f.getRegion().orElse(null));
        map.put("value", f.getValue().doubleValue());
        map.put("unit", f.getUnit());
        map.put("source", f.getSource());
        map.put("version", f.getVersion());
        map.put("methodName", f.getMethodName());
        map.put("validFrom", f.getValidFrom().toString());
        map.put("validTo", f.getValidTo() != null ? f.getValidTo().toString() : null);
        return map;
    }
}
