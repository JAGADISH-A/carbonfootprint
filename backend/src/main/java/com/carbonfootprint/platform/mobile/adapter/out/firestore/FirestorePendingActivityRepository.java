package com.carbonfootprint.platform.mobile.adapter.out.firestore;

import com.carbonfootprint.platform.mobile.model.PendingActivity;
import com.carbonfootprint.platform.mobile.model.PendingActivityStatus;
import com.carbonfootprint.platform.mobile.port.out.PendingActivityRepository;
import com.google.cloud.firestore.Firestore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Slf4j
@Repository
@Profile("!stub")
public class FirestorePendingActivityRepository implements PendingActivityRepository {

    private final Firestore firestore;
    private final String collectionName;

    public FirestorePendingActivityRepository(
            Firestore firestore,
            @Value("${carbon.firestore.pending-activities-collection:pending_activities}") String collectionName
    ) {
        this.firestore = firestore;
        this.collectionName = collectionName;
        log.info("FirestorePendingActivityRepository initialised. collection='{}'", collectionName);
    }

    @Override
    public PendingActivity save(PendingActivity pendingActivity) {
        log.debug("FirestorePendingActivityRepository.save() — id={}", pendingActivity.getId());
        try {
            Map<String, Object> data = toFirestoreMap(pendingActivity);
            firestore.collection(collectionName)
                     .document(pendingActivity.getId())
                     .set(data)
                     .get();
            return pendingActivity;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore save interrupted for id=" + pendingActivity.getId(), e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Firestore save failed for id=" + pendingActivity.getId(), e.getCause());
        }
    }

    @Override
    public Optional<PendingActivity> findById(String id) {
        log.debug("FirestorePendingActivityRepository.findById() — id={}", id);
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
    public boolean exists(String id) {
        log.debug("FirestorePendingActivityRepository.exists() — id={}", id);
        try {
            var snapshot = firestore.collection(collectionName).document(id).get().get();
            return snapshot.exists();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore exists check interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Firestore exists check failed", e.getCause());
        }
    }

    @Override
    public void updateStatus(String id, PendingActivityStatus status) {
        log.debug("FirestorePendingActivityRepository.updateStatus() — id={} status={}", id, status);
        try {
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", status.name());
            if (status == PendingActivityStatus.PROCESSING) {
                updates.put("processingStartedAt", Instant.now().toString());
            } else if (status == PendingActivityStatus.PROCESSED) {
                updates.put("processedAt", Instant.now().toString());
            }

            firestore.collection(collectionName)
                     .document(id)
                     .update(updates)
                     .get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore updateStatus interrupted for id=" + id, e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Firestore updateStatus failed for id=" + id, e.getCause());
        }
    }

    @Override
    public void updateFailure(String id, String errorMessage) {
        log.debug("FirestorePendingActivityRepository.updateFailure() — id={}", id);
        try {
            firestore.runTransaction(transaction -> {
                var docRef = firestore.collection(collectionName).document(id);
                var snapshot = transaction.get(docRef).get();
                if (snapshot.exists()) {
                    long currentRetryCount = snapshot.contains("retryCount") ? snapshot.getLong("retryCount") : 0;
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", PendingActivityStatus.FAILED.name());
                    updates.put("retryCount", currentRetryCount + 1);
                    updates.put("lastError", errorMessage);
                    updates.put("lastRetryAt", Instant.now().toString());
                    transaction.update(docRef, updates);
                }
                return null;
            }).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore updateFailure interrupted for id=" + id, e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Firestore updateFailure failed for id=" + id, e.getCause());
        }
    }

    @Override
    public PendingActivity upsert(PendingActivity pendingActivity) {
        log.debug("FirestorePendingActivityRepository.upsert() — id={}", pendingActivity.getId());
        try {
            firestore.runTransaction(transaction -> {
                var docRef = firestore.collection(collectionName).document(pendingActivity.getId());
                var snapshot = transaction.get(docRef).get();
                if (snapshot.exists()) {
                    // Update existing
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("syncSessionId", pendingActivity.getSyncSessionId());
                    updates.put("rawPayload", pendingActivity.getRawPayload());
                    updates.put("merchant", pendingActivity.getMerchant());
                    updates.put("amount", pendingActivity.getAmount() != null ? pendingActivity.getAmount().toPlainString() : null);
                    updates.put("timestamp", pendingActivity.getTimestamp() != null ? pendingActivity.getTimestamp().toString() : null);
                    // Do not reset status if it is already PROCESSING or PROCESSED, maybe reset to NEW if FAILED
                    String currentStatus = snapshot.getString("status");
                    if (currentStatus == null || currentStatus.equals(PendingActivityStatus.FAILED.name())) {
                        updates.put("status", PendingActivityStatus.NEW.name());
                        updates.put("retryCount", 0);
                        updates.put("lastError", null);
                    }
                    transaction.update(docRef, updates);
                } else {
                    // Create new
                    transaction.set(docRef, toFirestoreMap(pendingActivity));
                }
                return null;
            }).get();
            return pendingActivity;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore upsert interrupted for id=" + pendingActivity.getId(), e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Firestore upsert failed for id=" + pendingActivity.getId(), e.getCause());
        }
    }

    private Map<String, Object> toFirestoreMap(PendingActivity a) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", a.getId());
        map.put("userId", a.getUserId());
        map.put("deviceId", a.getDeviceId());
        map.put("syncSessionId", a.getSyncSessionId());
        map.put("source", a.getSource());
        map.put("rawPayload", a.getRawPayload());
        map.put("merchant", a.getMerchant());
        map.put("amount", a.getAmount() != null ? a.getAmount().toPlainString() : null);
        map.put("timestamp", a.getTimestamp() != null ? a.getTimestamp().toString() : null);
        map.put("retryCount", a.getRetryCount());
        map.put("lastError", a.getLastError());
        map.put("status", a.getStatus() != null ? a.getStatus().name() : null);
        map.put("createdAt", a.getCreatedAt() != null ? a.getCreatedAt().toString() : null);
        map.put("processingStartedAt", a.getProcessingStartedAt() != null ? a.getProcessingStartedAt().toString() : null);
        map.put("processedAt", a.getProcessedAt() != null ? a.getProcessedAt().toString() : null);
        map.put("lastRetryAt", a.getLastRetryAt() != null ? a.getLastRetryAt().toString() : null);
        return map;
    }

    private PendingActivity fromFirestoreMap(String id, Map<String, Object> data) {
        if (data == null) return PendingActivity.builder().id(id).build();
        return PendingActivity.builder()
                .id(id)
                .userId((String) data.get("userId"))
                .deviceId((String) data.get("deviceId"))
                .syncSessionId((String) data.get("syncSessionId"))
                .source((String) data.get("source"))
                .rawPayload((String) data.get("rawPayload"))
                .merchant((String) data.get("merchant"))
                .amount(data.get("amount") != null ? new BigDecimal((String) data.get("amount")) : null)
                .timestamp(parseInstant((String) data.get("timestamp")))
                .retryCount(data.get("retryCount") != null ? ((Number) data.get("retryCount")).intValue() : 0)
                .lastError((String) data.get("lastError"))
                .status(parseEnum(PendingActivityStatus.class, (String) data.get("status")))
                .createdAt(parseInstant((String) data.get("createdAt")))
                .processingStartedAt(parseInstant((String) data.get("processingStartedAt")))
                .processedAt(parseInstant((String) data.get("processedAt")))
                .lastRetryAt(parseInstant((String) data.get("lastRetryAt")))
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
