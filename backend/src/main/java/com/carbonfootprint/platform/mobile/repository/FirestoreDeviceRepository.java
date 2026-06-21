package com.carbonfootprint.platform.mobile.repository;

import com.carbonfootprint.platform.mobile.model.Device;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Firestore implementation of {@link DeviceRepository}.
 */
@Slf4j
@Repository
@Profile("!stub")
public class FirestoreDeviceRepository implements DeviceRepository {

    private final Firestore firestore;
    private final String collectionName;

    public FirestoreDeviceRepository(
            Firestore firestore,
            @Value("${carbon.firestore.devices-collection:devices}") String collectionName
    ) {
        this.firestore = firestore;
        this.collectionName = collectionName;
        log.info("FirestoreDeviceRepository initialised. collection='{}'", collectionName);
    }

    @Override
    public Device save(Device device) {
        if (device.getId() == null) {
            device.setId(UUID.randomUUID().toString());
        }
        log.debug("FirestoreDeviceRepository.save() — id={}", device.getId());
        try {
            Map<String, Object> data = toFirestoreMap(device);
            firestore.collection(collectionName)
                     .document(device.getId())
                     .set(data)
                     .get(); // Block until write completes
            return device;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore save interrupted for deviceId=" + device.getId(), e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Firestore save failed for deviceId=" + device.getId(), e.getCause());
        }
    }

    @Override
    public Optional<Device> findById(String id) {
        log.debug("FirestoreDeviceRepository.findById() — id={}", id);
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
    public Optional<Device> findByDeviceId(String deviceId) {
        log.debug("FirestoreDeviceRepository.findByDeviceId() — deviceId={}", deviceId);
        try {
            List<QueryDocumentSnapshot> docs = firestore.collection(collectionName)
                    .whereEqualTo("deviceId", deviceId)
                    .limit(1)
                    .get().get().getDocuments();
            
            if (docs.isEmpty()) {
                return Optional.empty();
            }
            QueryDocumentSnapshot doc = docs.get(0);
            return Optional.of(fromFirestoreMap(doc.getId(), doc.getData()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore findByDeviceId interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Firestore findByDeviceId failed", e.getCause());
        }
    }

    @Override
    public Optional<Device> findByRefreshTokenHash(String refreshTokenHash) {
        log.debug("FirestoreDeviceRepository.findByRefreshTokenHash() — refreshTokenHash={}", refreshTokenHash);
        try {
            List<QueryDocumentSnapshot> docs = firestore.collection(collectionName)
                    .whereEqualTo("refreshTokenHash", refreshTokenHash)
                    .limit(1)
                    .get().get().getDocuments();
            
            if (docs.isEmpty()) {
                return Optional.empty();
            }
            QueryDocumentSnapshot doc = docs.get(0);
            return Optional.of(fromFirestoreMap(doc.getId(), doc.getData()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore findByRefreshTokenHash interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Firestore findByRefreshTokenHash failed", e.getCause());
        }
    }

    @Override
    public List<Device> findByUserId(String userId) {
        log.debug("FirestoreDeviceRepository.findByUserId() — userId={}", userId);
        try {
            List<QueryDocumentSnapshot> docs = firestore.collection(collectionName)
                    .whereEqualTo("userId", userId)
                    .get().get().getDocuments();

            return docs.stream()
                    .map(doc -> fromFirestoreMap(doc.getId(), doc.getData()))
                    .toList();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore findByUserId interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Firestore findByUserId failed", e.getCause());
        }
    }

    @Override
    public void deleteById(String id) {
        log.debug("FirestoreDeviceRepository.deleteById() — id={}", id);
        try {
            firestore.collection(collectionName).document(id).delete().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore delete interrupted for id=" + id, e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Firestore delete failed for id=" + id, e.getCause());
        }
    }

    private Map<String, Object> toFirestoreMap(Device d) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", d.getId());
        map.put("userId", d.getUserId());
        map.put("deviceId", d.getDeviceId());
        map.put("deviceName", d.getDeviceName());
        map.put("manufacturer", d.getManufacturer());
        map.put("model", d.getModel());
        map.put("androidVersion", d.getAndroidVersion());
        map.put("appVersion", d.getAppVersion());
        map.put("refreshTokenHash", d.getRefreshTokenHash());
        map.put("refreshTokenExpiry", d.getRefreshTokenExpiry() != null ? d.getRefreshTokenExpiry().toString() : null);
        map.put("createdAt", d.getCreatedAt() != null ? d.getCreatedAt().toString() : null);
        map.put("lastSeenAt", d.getLastSeenAt() != null ? d.getLastSeenAt().toString() : null);
        map.put("lastSyncAt", d.getLastSyncAt() != null ? d.getLastSyncAt().toString() : null);
        map.put("lastUploadStatus", d.getLastUploadStatus());
        return map;
    }

    private Device fromFirestoreMap(String id, Map<String, Object> data) {
        if (data == null) {
            return Device.builder().id(id).build();
        }
        return Device.builder()
                .id(id)
                .userId((String) data.get("userId"))
                .deviceId((String) data.get("deviceId"))
                .deviceName((String) data.get("deviceName"))
                .manufacturer((String) data.get("manufacturer"))
                .model((String) data.get("model"))
                .androidVersion((String) data.get("androidVersion"))
                .appVersion((String) data.get("appVersion"))
                .refreshTokenHash((String) data.get("refreshTokenHash"))
                .refreshTokenExpiry(parseInstant((String) data.get("refreshTokenExpiry")))
                .createdAt(parseInstant((String) data.get("createdAt")))
                .lastSeenAt(parseInstant((String) data.get("lastSeenAt")))
                .lastSyncAt(parseInstant((String) data.get("lastSyncAt")))
                .lastUploadStatus((String) data.get("lastUploadStatus"))
                .build();
    }


    private Instant parseInstant(String value) {
        if (value == null) return null;
        try { return Instant.parse(value); } catch (Exception e) { return null; }
    }
}
