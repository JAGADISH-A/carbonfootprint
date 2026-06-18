package com.carbonfootprint.platform.carbon.calculation.adapter.out.firestore;

import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionFactor;
import com.carbonfootprint.platform.carbon.calculation.port.out.EmissionFactorRepository;
import com.carbonfootprint.platform.ingestion.enrichment.model.FuelType;
import com.carbonfootprint.platform.ingestion.enrichment.model.TransportMode;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Firestore implementation of {@link EmissionFactorRepository}.
 *
 * <h3>Collection path</h3>
 * {@code carbon.firestore.emission-factors-collection} (default: "emission_factors")
 *
 * <h3>Document structure</h3>
 * Each emission factor is stored as a Firestore document with the factor ID as
 * the document ID. Fields match the {@link EmissionFactor} domain model.
 *
 * <h3>Profile</h3>
 * Active only when the {@code stub} profile is NOT active.
 */
@Slf4j
@Repository
@Profile("!stub")
public class FirestoreEmissionFactorRepository implements EmissionFactorRepository {

    private final Firestore firestore;
    private final String collectionName;

    public FirestoreEmissionFactorRepository(
            Firestore firestore,
            @Value("${carbon.firestore.emission-factors-collection:emission_factors}") String collectionName
    ) {
        this.firestore = firestore;
        this.collectionName = collectionName;
        log.info("FirestoreEmissionFactorRepository initialised. collection='{}'", collectionName);
    }

    @Override
    public List<EmissionFactor> findAll() {
        log.debug("FirestoreEmissionFactorRepository.findAll()");
        try {
            List<EmissionFactor> results = new ArrayList<>();
            for (QueryDocumentSnapshot doc : firestore.collection(collectionName)
                    .get().get().getDocuments()) {
                results.add(fromFirestoreMap(doc.getId(), doc.getData()));
            }
            log.debug("FirestoreEmissionFactorRepository.findAll() — loaded {} factors", results.size());
            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore findAll emission factors interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Firestore findAll emission factors failed", e.getCause());
        }
    }

    @Override
    public List<EmissionFactor> findByCategory(ActivityCategory category) {
        log.debug("FirestoreEmissionFactorRepository.findByCategory() — category={}", category);
        try {
            List<EmissionFactor> results = new ArrayList<>();
            for (QueryDocumentSnapshot doc : firestore.collection(collectionName)
                    .whereEqualTo("category", category.name())
                    .get().get().getDocuments()) {
                results.add(fromFirestoreMap(doc.getId(), doc.getData()));
            }
            log.debug("FirestoreEmissionFactorRepository.findByCategory() — loaded {} factors for {}",
                    results.size(), category);
            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore findByCategory emission factors interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Firestore findByCategory emission factors failed", e.getCause());
        }
    }

    // ── Mapping helpers ────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private EmissionFactor fromFirestoreMap(String id, Map<String, Object> data) {
        if (data == null) return EmissionFactor.builder().id(id).build();
        return EmissionFactor.builder()
                .id(id)
                .value(data.get("value") instanceof Number
                        ? BigDecimal.valueOf(((Number) data.get("value")).doubleValue())
                        : data.get("value") instanceof String
                                ? new BigDecimal((String) data.get("value"))
                                : BigDecimal.ZERO)
                .unit((String) data.get("unit"))
                .category(parseEnum(ActivityCategory.class, (String) data.get("category")))
                .fuelType(parseEnum(FuelType.class, (String) data.get("fuelType")))
                .transportMode(parseEnum(TransportMode.class, (String) data.get("transportMode")))
                .region((String) data.get("region"))
                .validFrom(parseInstant((String) data.get("validFrom")))
                .validTo(parseInstant((String) data.get("validTo")))
                .source((String) data.get("source"))
                .version((String) data.get("version"))
                .methodName((String) data.get("methodName"))
                .build();
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        if (value == null) return null;
        try { return Enum.valueOf(type, value); } catch (IllegalArgumentException e) { return null; }
    }

    private Instant parseInstant(String value) {
        if (value == null) return null;
        try { return Instant.parse(value); } catch (Exception e) { return null; }
    }
}
