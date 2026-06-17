package com.carbonfootprint.platform.shared.constant;

/**
 * Platform-wide API constants.
 *
 * <p>All path prefixes, header names, and other string constants that appear
 * in more than one class must be defined here to prevent duplication.
 */
public final class ApiConstants {

    // ── API Versioning ──────────────────────────────────────────────────────

    public static final String API_V1 = "/api/v1";

    // ── Endpoint Paths ───────────────────────────────────────────────────────

    public static final String HEALTH_PATH     = API_V1 + "/health";
    public static final String INGESTION_PATH  = API_V1 + "/ingestion";
    public static final String ACTIVITIES_PATH = API_V1 + "/activities";
    public static final String CARBON_PATH     = API_V1 + "/carbon";

    // ── HTTP Header Names ────────────────────────────────────────────────────

    public static final String HEADER_REQUEST_ID    = "X-Request-Id";
    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";

    // ── Pagination Defaults ──────────────────────────────────────────────────

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE     = 100;

    // ── Firestore Collection Names (defaults — overridable via application.yml) ─

    public static final String COLLECTION_ACTIVITIES        = "activities";
    public static final String COLLECTION_RAW_DOCUMENTS     = "raw_documents";
    public static final String COLLECTION_CARBON_ASSESSMENTS = "carbon_assessments";

    private ApiConstants() {
        // Utility class — no instantiation
    }
}
