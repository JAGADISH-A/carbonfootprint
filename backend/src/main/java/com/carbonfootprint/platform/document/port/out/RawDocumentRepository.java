package com.carbonfootprint.platform.document.port.out;

import com.carbonfootprint.platform.document.model.RawDocument;

import java.util.Optional;

/**
 * Outbound port: persistence contract for {@link RawDocument} domain objects.
 *
 * <p>Raw documents are persisted separately from activities to maintain a
 * clean audit trail and to support reprocessing (e.g., when a new
 * {@link com.carbonfootprint.platform.ingestion.port.out.DocumentParser}
 * implementation is deployed).
 *
 * <p>Implementations:
 * {@link com.carbonfootprint.platform.document.adapter.out.firestore.FirestoreRawDocumentRepository}
 */
public interface RawDocumentRepository {

    /**
     * Persists a new raw document.
     *
     * @param document the document to save
     * @return the saved document
     */
    RawDocument save(RawDocument document);

    /**
     * Retrieves a raw document by its unique identifier.
     *
     * @param id the document ID
     * @return an {@link Optional} containing the document, or empty if not found
     */
    Optional<RawDocument> findById(String id);
}
