package com.carbonfootprint.platform.ingestion.model;

import com.carbonfootprint.platform.activity.model.Activity;
import com.carbonfootprint.platform.document.model.RawDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Result returned by the ingestion pipeline after processing an {@link IngestionRequest}.
 *
 * <p>Contains both the persisted {@link RawDocument} and the resulting
 * {@link Activity} (after parsing, validation, and normalisation). Callers
 * can inspect {@code success} to determine whether the full pipeline completed.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionResult {

    /** Whether the full pipeline completed without errors. */
    private boolean success;

    /** The persisted raw document produced by the ingestion source. */
    private RawDocument rawDocument;

    /**
     * The normalised, persisted activity.
     * Null if parsing or validation failed.
     */
    private Activity activity;

    /**
     * Human-readable summary of the pipeline outcome.
     * Populated for both success and failure cases.
     */
    private String message;

    /** Timestamp when the pipeline completed. */
    private Instant processedAt;

    // ── Convenience factory methods ──────────────────────────────────────────

    public static IngestionResult success(RawDocument rawDocument, Activity activity) {
        return IngestionResult.builder()
                .success(true)
                .rawDocument(rawDocument)
                .activity(activity)
                .message("Ingestion completed successfully.")
                .processedAt(Instant.now())
                .build();
    }

    public static IngestionResult failure(RawDocument rawDocument, String reason) {
        return IngestionResult.builder()
                .success(false)
                .rawDocument(rawDocument)
                .message(reason)
                .processedAt(Instant.now())
                .build();
    }
}
