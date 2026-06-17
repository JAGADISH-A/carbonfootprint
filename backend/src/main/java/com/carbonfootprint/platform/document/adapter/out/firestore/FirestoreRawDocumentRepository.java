package com.carbonfootprint.platform.document.adapter.out.firestore;

import com.carbonfootprint.platform.activity.model.ActivitySource;
import com.carbonfootprint.platform.document.model.RawDocument;
import com.carbonfootprint.platform.document.port.out.RawDocumentRepository;
import com.google.cloud.firestore.Firestore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Profile;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Firestore implementation of {@link RawDocumentRepository}.
 *
 * <h3>Collection path</h3>
 * {@code carbon.firestore.raw-documents-collection} (default: "raw_documents")
 *
 * <h3>Security note</h3>
 * Raw documents may contain sensitive OCR text. Firestore security rules
 * should restrict access to service accounts only — no direct client access.
 */
@Slf4j
@Repository
@Profile("!stub")
public class FirestoreRawDocumentRepository implements RawDocumentRepository {

    private final Firestore firestore;
    private final String collectionName;

    public FirestoreRawDocumentRepository(
            Firestore firestore,
            @Value("${carbon.firestore.raw-documents-collection:raw_documents}") String collectionName
    ) {
        this.firestore = firestore;
        this.collectionName = collectionName;
        log.info("FirestoreRawDocumentRepository initialised. collection='{}'", collectionName);
    }

    @Override
    public RawDocument save(RawDocument document) {
        log.debug("FirestoreRawDocumentRepository.save() — id={}", document.getId());
        try {
            firestore.collection(collectionName)
                     .document(document.getId())
                     .set(toFirestoreMap(document))
                     .get();
            return document;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore RawDocument save interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Firestore RawDocument save failed", e.getCause());
        }
    }

    @Override
    public Optional<RawDocument> findById(String id) {
        log.debug("FirestoreRawDocumentRepository.findById() — id={}", id);
        try {
            var snapshot = firestore.collection(collectionName).document(id).get().get();
            if (!snapshot.exists()) return Optional.empty();
            return Optional.of(fromFirestoreMap(id, snapshot.getData()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firestore RawDocument findById interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Firestore RawDocument findById failed", e.getCause());
        }
    }

    // ── Mapping helpers ────────────────────────────────────────────────────

    private Map<String, Object> toFirestoreMap(RawDocument d) {
        Map<String, Object> map = new HashMap<>();
        map.put("id",        d.getId());
        map.put("source",    d.getSource() != null ? d.getSource().name() : null);
        map.put("mimeType",  d.getMimeType());
        map.put("rawText",   d.getRawText());
        map.put("language",  d.getLanguage());
        map.put("confidence",d.getConfidence());
        map.put("userId",    d.getUserId());
        map.put("metadata",  d.getMetadata());
        map.put("createdAt", d.getCreatedAt() != null ? d.getCreatedAt().toString() : null);
        return map;
    }

    @SuppressWarnings("unchecked")
    private RawDocument fromFirestoreMap(String id, Map<String, Object> data) {
        if (data == null) return RawDocument.builder().id(id).build();
        return RawDocument.builder()
                .id(id)
                .source(parseSource((String) data.get("source")))
                .mimeType((String) data.get("mimeType"))
                .rawText((String) data.get("rawText"))
                .language((String) data.get("language"))
                .confidence(data.get("confidence") instanceof Number n ? n.doubleValue() : null)
                .userId((String) data.get("userId"))
                .metadata(data.get("metadata") instanceof Map
                        ? (Map<String, Object>) data.get("metadata")
                        : new HashMap<>())
                .createdAt(parseInstant((String) data.get("createdAt")))
                .build();
    }

    private ActivitySource parseSource(String value) {
        if (value == null) return null;
        try { return ActivitySource.valueOf(value); } catch (IllegalArgumentException e) { return null; }
    }

    private Instant parseInstant(String value) {
        if (value == null) return null;
        try { return Instant.parse(value); } catch (Exception e) { return null; }
    }
}
