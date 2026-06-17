package com.carbonfootprint.platform.ingestion.model;

import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.activity.model.ActivitySource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates all input data required to start an ingestion pipeline run.
 *
 * <p>This is the single entry point passed to every
 * {@link com.carbonfootprint.platform.ingestion.port.out.IngestionSource}.
 * It carries the raw bytes (for file uploads), optional plain text (for
 * email/SMS), and contextual hints that help the parser.
 *
 * <p>Design: immutable value object built via Lombok {@code @Builder}.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionRequest {

    /**
     * The authenticated user's identifier.
     */
    private String userId;

    /**
     * Declared source channel. Must be set by the calling controller.
     */
    private ActivitySource source;

    /**
     * Optional category hint supplied by the user (e.g., via UI dropdown).
     * The parser may override this if it infers a different category.
     */
    private ActivityCategory categoryHint;

    /**
     * Raw binary content of the uploaded document (e.g., JPEG, PDF bytes).
     * Null when the source is text-based (email, SMS).
     */
    private byte[] fileBytes;

    /**
     * MIME type of {@code fileBytes}.
     * Examples: "image/jpeg", "image/png", "application/pdf".
     */
    private String mimeType;

    /**
     * Original filename as uploaded by the user.
     */
    private String originalFilename;

    /**
     * Pre-extracted plain text content.
     * Used when the source is email or SMS (no binary attachment).
     */
    private String rawText;

    /**
     * Additional context passed from the ingestion source adapter.
     * Examples: email subject, SMS sender number, bank statement account info.
     */
    @Builder.Default
    private Map<String, Object> context = new HashMap<>();
}
