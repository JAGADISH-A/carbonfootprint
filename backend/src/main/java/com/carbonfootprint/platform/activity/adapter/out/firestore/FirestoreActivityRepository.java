package com.carbonfootprint.platform.activity.adapter.out.firestore;

import com.carbonfootprint.platform.activity.model.Activity;
import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.activity.model.ActivitySource;
import com.carbonfootprint.platform.activity.port.out.ActivityRepository;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Firestore implementation of {@link ActivityRepository}.
 *
 * <h3>Collection path</h3>
 * {@code carbon.firestore.activities-collection} (default: "activities")
 *
 * <h3>Document structure</h3>
 * Each activity is stored as a Firestore document with the activity ID as the
 * document ID. No nested collections are used for simplicity and query efficiency.
 *
 * <h3>Note on async</h3>
 * The Firestore SDK returns {@code ApiFuture<T>}. This adapter blocks using
 * {@code .get()} to keep the interface synchronous. A future refactoring may
 * introduce reactive types (Project Reactor) when throughput demands it.
 *
 * <p>TODO (Phase 1): Implement full CRUD when Firestore is connected.
 */
@Slf4j
@Repository
@Profile("!stub")
public class FirestoreActivityRepository implements ActivityRepository {

    private final Firestore firestore;
    private final String collectionName;

    public FirestoreActivityRepository(
            Firestore firestore,
            @Value("${carbon.firestore.activities-collection:activities}") String collectionName
    ) {
        this.firestore = firestore;
        this.collectionName = collectionName;
        log.info("FirestoreActivityRepository initialised. collection='{}'", collectionName);
    }

    @Override
    public Activity save(Activity activity) {
        log.debug("FirestoreActivityRepository.save() — id={}", activity.getId());
        try {
            Map<String, Object> data = toFirestoreMap(activity);
            firestore.collection(collectionName)
                     .document(activity.getId())
                     .set(data)
                     .get(); // Block until write completes
            return activity;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore save interrupted for activityId=" + activity.getId(), e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Firestore save failed for activityId=" + activity.getId(), e.getCause());
        }
    }

    @Override
    public Optional<Activity> findById(String id) {
        log.debug("FirestoreActivityRepository.findById() — id={}", id);
        try {
            var snapshot = firestore.collection(collectionName).document(id).get().get();
            if (!snapshot.exists()) {
                return Optional.empty();
            }
            return Optional.of(fromFirestoreMap(id, snapshot.getData()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore findById interrupted for id=" + id, e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Firestore findById failed for id=" + id, e.getCause());
        }
    }

    @Override
    public List<Activity> findByUserId(String userId) {
        log.debug("FirestoreActivityRepository.findByUserId() — userId={}", userId);
        try {
            List<Activity> results = new ArrayList<>();
            for (QueryDocumentSnapshot doc : firestore.collection(collectionName)
                         .whereEqualTo("userId", userId)
                         .orderBy("occurredAt")
                         .get().get().getDocuments()) {
                results.add(fromFirestoreMap(doc.getId(), doc.getData()));
            }
            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore findByUserId interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Firestore findByUserId failed", e.getCause());
        }
    }

    @Override
    public List<Activity> findByUserIdAndOccurredAtBetween(String userId, Instant from, Instant to) {
        log.debug("FirestoreActivityRepository.findByUserIdAndOccurredAtBetween() — userId={}", userId);
        try {
            List<Activity> results = new ArrayList<>();
            for (QueryDocumentSnapshot doc : firestore.collection(collectionName)
                         .whereEqualTo("userId", userId)
                         .whereGreaterThanOrEqualTo("occurredAt", from.toString())
                         .whereLessThanOrEqualTo("occurredAt", to.toString())
                         .get().get().getDocuments()) {
                results.add(fromFirestoreMap(doc.getId(), doc.getData()));
            }
            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore range query interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Firestore range query failed", e.getCause());
        }
    }

    @Override
    public boolean existsByUserIdAndRawDocumentId(String userId, String rawDocumentId) {
        log.debug("FirestoreActivityRepository.existsByUserIdAndRawDocumentId() — rawDocumentId={}", rawDocumentId);
        try {
            return !firestore.collection(collectionName)
                             .whereEqualTo("userId", userId)
                             .whereEqualTo("rawDocumentId", rawDocumentId)
                             .limit(1)
                             .get().get()
                             .isEmpty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore duplicate check interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Firestore duplicate check failed", e.getCause());
        }
    }

    @Override
    public void deleteById(String id) {
        log.debug("FirestoreActivityRepository.deleteById() — id={}", id);
        try {
            firestore.collection(collectionName).document(id).delete().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore delete interrupted for id=" + id, e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Firestore delete failed for id=" + id, e.getCause());
        }
    }

    // ── Mapping helpers ────────────────────────────────────────────────────

    private Map<String, Object> toFirestoreMap(Activity a) {
        Map<String, Object> map = new HashMap<>();
        map.put("id",            a.getId());
        map.put("userId",        a.getUserId());
        map.put("source",        a.getSource() != null ? a.getSource().name() : null);
        map.put("category",      a.getCategory() != null ? a.getCategory().name() : null);
        map.put("merchant",      a.getMerchant());
        map.put("amount",        a.getAmount() != null ? a.getAmount().toPlainString() : null);
        map.put("currency",      a.getCurrency());
        map.put("unit",          a.getUnit());
        map.put("location",      a.getLocation());
        map.put("occurredAt",    a.getOccurredAt() != null ? a.getOccurredAt().toString() : null);
        map.put("description",   a.getDescription());
        map.put("rawDocumentId", a.getRawDocumentId());
        map.put("metadata",      a.getMetadata());
        map.put("createdAt",     a.getCreatedAt() != null ? a.getCreatedAt().toString() : null);
        map.put("updatedAt",     a.getUpdatedAt() != null ? a.getUpdatedAt().toString() : null);
        return map;
    }

    @SuppressWarnings("unchecked")
    private Activity fromFirestoreMap(String id, Map<String, Object> data) {
        if (data == null) return Activity.builder().id(id).build();
        return Activity.builder()
                .id(id)
                .userId((String) data.get("userId"))
                .source(parseEnum(ActivitySource.class, (String) data.get("source")))
                .category(parseEnum(ActivityCategory.class, (String) data.get("category")))
                .merchant((String) data.get("merchant"))
                .amount(data.get("amount") != null ? new BigDecimal((String) data.get("amount")) : null)
                .currency((String) data.get("currency"))
                .unit((String) data.get("unit"))
                .location((String) data.get("location"))
                .occurredAt(parseInstant((String) data.get("occurredAt")))
                .description((String) data.get("description"))
                .rawDocumentId((String) data.get("rawDocumentId"))
                .metadata(data.get("metadata") instanceof Map
                        ? (Map<String, Object>) data.get("metadata")
                        : new HashMap<>())
                .createdAt(parseInstant((String) data.get("createdAt")))
                .updatedAt(parseInstant((String) data.get("updatedAt")))
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
