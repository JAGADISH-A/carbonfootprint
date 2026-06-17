package com.carbonfootprint.platform.integration.gmail;

import com.carbonfootprint.platform.activity.model.ActivitySource;
import com.carbonfootprint.platform.document.model.RawDocument;
import com.carbonfootprint.platform.ingestion.model.IngestionRequest;
import com.carbonfootprint.platform.ingestion.port.out.IngestionSource;
import com.carbonfootprint.platform.platform.exception.IngestionException;
import com.carbonfootprint.platform.shared.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Ingestion source for Gmail-based bill and receipt emails (Phase 2).
 *
 * <h3>Responsibility</h3>
 * Uses the Gmail API (via OAuth 2.0) to fetch relevant transactional emails
 * and wrap their content in a {@link RawDocument}.
 *
 * <h3>Email filtering</h3>
 * Only relevant emails are fetched (electricity bills, flight confirmations,
 * food delivery receipts, shopping invoices). Personal emails are excluded.
 * The Gmail search query is loaded from application configuration — it is
 * NEVER hardcoded in this class.
 *
 * <p>TODO (Phase 2): Implement Gmail API client, OAuth token refresh,
 * and email body extraction. Add {@code google-api-services-gmail} dependency.
 */
@Slf4j
@Component
public class GmailIngestionSource implements IngestionSource {

    @Override
    public ActivitySource getSource() {
        return ActivitySource.GMAIL;
    }

    @Override
    public boolean supports(IngestionRequest request) {
        return ActivitySource.GMAIL.equals(request.getSource());
    }

    @Override
    public RawDocument ingest(IngestionRequest request) throws IngestionException {
        log.info("GmailIngestionSource: Phase 2 — not yet implemented. userId={}",
                request.getUserId());

        // TODO (Phase 2): Implement Gmail API extraction
        // 1. Load OAuth token from request context
        // 2. Call Gmail API with configurable search query
        // 3. Extract email body (HTML → plain text)
        // 4. Populate rawText, subject, threadId in metadata

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("status", "PHASE_2_PENDING");

        return RawDocument.builder()
                .id(IdGenerator.generate())
                .source(ActivitySource.GMAIL)
                .mimeType("message/rfc822")
                .rawText("") // TODO (Phase 2): Replace with email body text
                .userId(request.getUserId())
                .metadata(metadata)
                .createdAt(Instant.now())
                .build();
    }
}
