package com.carbonfootprint.platform.activity.model;

/**
 * Identifies the origin of an ingested activity.
 *
 * <p>New sources can be added here without changing any downstream service.
 * The pipeline is designed around this enum — every adapter declares which
 * source it handles via {@code getSource()}.
 */
public enum ActivitySource {

    /** Physical or digital receipt uploaded by the user (Phase 1). */
    RECEIPT,

    /** Parsed from a Gmail email thread (Phase 2). */
    GMAIL,

    /** Parsed from an SMS message via Android Companion App (Future). */
    SMS,

    /** Pre-enriched transaction submitted directly from the Android Companion App (Phase 4). */
    MOBILE,

    /** Parsed from a bank or credit card statement (Future). */
    BANK_STATEMENT,

    /** Data from a smart meter or IoT sensor feed (Future). */
    IOT,

    /** Entered directly by the user through the UI (always available). */
    MANUAL,

    /** Source type is unknown or could not be determined. */
    UNKNOWN
}
