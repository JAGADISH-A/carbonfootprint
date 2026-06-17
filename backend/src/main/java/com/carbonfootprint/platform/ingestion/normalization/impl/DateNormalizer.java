package com.carbonfootprint.platform.ingestion.normalization.impl;

import com.carbonfootprint.platform.activity.model.Activity;
import com.carbonfootprint.platform.ingestion.normalization.ActivityNormalizer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Normalizes the {@code occurredAt} timestamp to UTC.
 *
 * <p>Timestamps from OCR or email sources may arrive as local-time strings.
 * This normalizer ensures all timestamps stored in the activity are UTC Instants.
 *
 * <p>If {@code occurredAt} is null (parser could not determine the date),
 * this normalizer falls back to the document ingestion time ({@code createdAt}).
 *
 * <p>TODO (Phase 2): Accept timezone hint from {@code Activity.metadata}
 * (e.g., user's configured timezone) for more accurate local → UTC conversion.
 */
@Component
@Order(3)
public class DateNormalizer implements ActivityNormalizer {

    @Override
    public Activity normalize(Activity activity) {
        if (activity.getOccurredAt() != null) {
            // Already an Instant (UTC) — no conversion needed
            return activity;
        }

        // Fallback: use createdAt as the occurrence time
        Instant fallback = activity.getCreatedAt() != null
                ? activity.getCreatedAt()
                : Instant.now().atZone(ZoneOffset.UTC).toInstant();

        return activity.withOccurredAt(fallback);
    }
}
