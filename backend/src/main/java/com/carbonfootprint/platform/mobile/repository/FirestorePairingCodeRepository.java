package com.carbonfootprint.platform.mobile.repository;

import com.carbonfootprint.platform.mobile.model.PairingCode;
import com.google.cloud.firestore.Firestore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Firestore implementation of {@link PairingCodeRepository}.
 */
@Slf4j
@Repository
@Profile("!stub")
public class FirestorePairingCodeRepository implements PairingCodeRepository {

    private final Firestore firestore;
    private final String collectionName;

    public FirestorePairingCodeRepository(
            Firestore firestore,
            @Value("${carbon.firestore.pairing-codes-collection:pairing-codes}") String collectionName
    ) {
        this.firestore = firestore;
        this.collectionName = collectionName;
        log.info("FirestorePairingCodeRepository initialised. collection='{}'", collectionName);
    }

    @Override
    public PairingCode save(PairingCode pairingCode) {
        log.debug("FirestorePairingCodeRepository.save() — code={}", pairingCode.getCode());
        try {
            Map<String, Object> data = toFirestoreMap(pairingCode);
            // We use the 'code' as the document ID for quick O(1) lookup
            firestore.collection(collectionName)
                     .document(pairingCode.getCode())
                     .set(data)
                     .get(); // Block until write completes
            return pairingCode;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore save interrupted for code=" + pairingCode.getCode(), e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Firestore save failed for code=" + pairingCode.getCode(), e.getCause());
        }
    }

    @Override
    public Optional<PairingCode> findByCode(String code) {
        log.debug("FirestorePairingCodeRepository.findByCode() — code={}", code);
        try {
            var snapshot = firestore.collection(collectionName).document(code).get().get();
            if (!snapshot.exists()) {
                return Optional.empty();
            }
            return Optional.of(fromFirestoreMap(code, snapshot.getData()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore findByCode interrupted for code=" + code, e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Firestore findByCode failed for code=" + code, e.getCause());
        }
    }

    @Override
    public void deleteByCode(String code) {
        log.debug("FirestorePairingCodeRepository.deleteByCode() — code={}", code);
        try {
            firestore.collection(collectionName).document(code).delete().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore delete interrupted for code=" + code, e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Firestore delete failed for code=" + code, e.getCause());
        }
    }

    private Map<String, Object> toFirestoreMap(PairingCode p) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", p.getCode());
        map.put("userId", p.getUserId());
        map.put("expiresAt", p.getExpiresAt() != null ? p.getExpiresAt().toString() : null);
        return map;
    }

    private PairingCode fromFirestoreMap(String code, Map<String, Object> data) {
        if (data == null) {
            return PairingCode.builder().code(code).build();
        }
        return PairingCode.builder()
                .code(code)
                .userId((String) data.get("userId"))
                .expiresAt(parseInstant((String) data.get("expiresAt")))
                .build();
    }

    private Instant parseInstant(String value) {
        if (value == null) return null;
        try { return Instant.parse(value); } catch (Exception e) { return null; }
    }
}
