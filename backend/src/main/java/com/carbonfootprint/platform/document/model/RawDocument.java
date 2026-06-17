package com.carbonfootprint.platform.document.model;

import com.carbonfootprint.platform.activity.model.ActivitySource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Domain object representing an unprocessed artefact produced by an ingestion source.
 *
 * <h3>Purpose</h3>
 * {@code RawDocument} sits between the ingestion source and the parsing layer.
 * It captures the raw extracted content before any semantic interpretation.
 * Every {@link com.carbonfootprint.platform.ingestion.port.out.IngestionSource}
 * must produce exactly one {@code RawDocument}.
 *
 * <h3>Ownership of raw content</h3>
 * All raw text, OCR output, and original bytes live here — NEVER inside
 * {@link com.carbonfootprint.platform.activity.model.Activity}.
 *
 * <h3>Extensibility</h3>
 * The {@code metadata} field absorbs source-specific attributes without
 * polluting the core schema. OCR-specific fields (DPI, page count),
 * Gmail-specific fields (threadId, subject), and SMS-specific fields
 * (sender, carrier) all live in metadata.
 */
@Getter
@Builder
@With
@NoArgsConstructor
@AllArgsConstructor
public class RawDocument {

    /**
     * Globally unique identifier for this raw document (UUID v4).
     */
    private String id;

    /**
     * Document schema version for forward-compatible Firestore migrations.
     * Current version: {@code 1}
     */
    @Builder.Default
    private int schemaVersion = 1;

    /**
     * The ingestion channel that produced this document.
     */
    private ActivitySource source;

    /**
     * MIME type of the original input.
     * Examples: "image/jpeg", "image/png", "application/pdf",
     *           "text/plain", "message/rfc822" (email).
     */
    private String mimeType;

    /**
     * Full extracted text content.
     * For images: result of OCR. For emails: HTML/plain body.
     * For SMS: raw message text. For PDFs: extracted text layer.
     */
    private String rawText;

    /**
     * BCP 47 language tag of the detected text language.
     * Example: "en", "hi", "fr". Null if detection was not performed.
     */
    private String language;

    /**
     * Extraction confidence score in range [0.0, 1.0].
     * For OCR: character-level confidence average.
     * For email parsing: 1.0 (deterministic).
     * Null when confidence is not applicable.
     */
    private Double confidence;

    /**
     * Flexible metadata store for source-specific attributes.
     *
     * <p>OCR examples: {@code {"dpi": 300, "pageCount": 1, "engine": "paddle"}}
     * <p>Gmail examples: {@code {"threadId": "...", "subject": "Your electricity bill"}}
     * <p>SMS examples: {@code {"sender": "+919876543210", "carrier": "Jio"}}
     * <p>Receipt examples: {@code {"fileName": "bill.jpg", "fileSizeBytes": 204800}}
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * Identifier of the authenticated user who submitted this document.
     */
    private String userId;

    /**
     * Timestamp when this document was received and stored by the platform.
     */
    private Instant createdAt;
}
