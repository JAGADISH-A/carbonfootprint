package com.carbonwise.connect.ingestion.model

/**
 * Result produced by the filtering stage of the ingestion pipeline.
 *
 * Returned by any [DataFilter] implementation to indicate whether a parsed event
 * should proceed to classification or be discarded.
 *
 * Immutable value object. All fields are set at construction time.
 */
data class FilterResult(
    /**
     * Whether the event passed filtering and should proceed to classification.
     */
    val accepted: Boolean,

    /**
     * Confidence score for the filter decision, ranging from 0.0 to 1.0.
     *
     * - 1.0 means the filter is certain about its decision.
     * - 0.0 means the filter has no confidence (should not occur in practice).
     * - Values between 0.0 and 1.0 indicate varying degrees of certainty.
     *
     * Useful when combining multiple filter signals or logging filter quality.
     */
    val confidence: Double,

    /**
     * Human-readable explanation of why the filter made its decision.
     *
     * Examples:
     * - "Accepted: package is in allowlist"
     * - "Rejected: notification is a group summary"
     * - "Rejected: package is blocked (com.android.systemui)"
     * - "Rejected: notification body is empty"
     */
    val reason: String,

    /**
     * List of rule identifiers that were evaluated during filtering.
     *
     * Provides traceability for debugging and analytics. Each rule ID corresponds
     * to a specific check performed by the filter engine.
     *
     * Example: ["package_allowlist", "group_summary_check", "priority_check"]
     */
    val matchedRules: List<String>
) {
    companion object {
        /**
         * Creates a positive filter result indicating the event was accepted.
         *
         * @param reason Human-readable explanation for acceptance
         * @param matchedRules List of rule IDs that matched
         * @param confidence Confidence score (defaults to 1.0)
         */
        fun accept(
            reason: String,
            matchedRules: List<String>,
            confidence: Double = 1.0
        ): FilterResult {
            return FilterResult(
                accepted = true,
                confidence = confidence.coerceIn(0.0, 1.0),
                reason = reason,
                matchedRules = matchedRules
            )
        }

        /**
         * Creates a negative filter result indicating the event was rejected.
         *
         * @param reason Human-readable explanation for rejection
         * @param matchedRules List of rule IDs that were evaluated
         * @param confidence Confidence score (defaults to 1.0)
         */
        fun reject(
            reason: String,
            matchedRules: List<String>,
            confidence: Double = 1.0
        ): FilterResult {
            return FilterResult(
                accepted = false,
                confidence = confidence.coerceIn(0.0, 1.0),
                reason = reason,
                matchedRules = matchedRules
            )
        }
    }
}

/**
 * Verdict produced by package name evaluation.
 *
 * Represents the three possible states when checking a package against
 * the allowlist and blocklist rules.
 */
enum class PackageVerdict(val description: String) {
    /**
     * Package is in the allowlist. Notifications from this package
     * should proceed to further filtering stages.
     */
    ALLOWED("Package is in allowlist"),

    /**
     * Package is in the blocklist. Notifications from this package
     * are always rejected regardless of other rules.
     */
    BLOCKED("Package is in blocklist"),

    /**
     * Package is neither explicitly allowed nor blocked.
     * The allowlist may be empty (allowlist mode disabled), or the
     * package simply hasn't been categorized yet.
     */
    UNKNOWN("Package is not in allowlist or blocklist")
}

/**
 * Identifiers for standard filter rules applied during notification filtering.
 *
 * Each constant represents a single check that the filter engine may evaluate.
 * Rule IDs appear in [FilterResult.matchedRules] for traceability.
 */
enum class FilterRule(val description: String) {
    /** Notification is a group summary (contains no real content). */
    GROUP_SUMMARY_CHECK("Notification is a group summary"),

    /** Notification body or title is blank. */
    CONTENT_EMPTY_CHECK("Notification content is empty"),

    /** Package is in the user-defined allowlist. */
    PACKAGE_ALLOWLIST("Package is in allowlist"),

    /** Package is in the user-defined blocklist. */
    PACKAGE_BLOCKLIST("Package is in blocklist"),

    /** Package belongs to Android system UI. */
    SYSTEM_PACKAGE_CHECK("Package is a system UI package"),

    /** Notification priority is below threshold (e.g., silent/low priority). */
    PRIORITY_CHECK("Notification priority below threshold"),

    /** Package is recognized as a known carbon-relevant app. */
    KNOWN_PACKAGE_CHECK("Package is a known carbon-relevant app"),

    /** Notification category matches a tracked category. */
    CATEGORY_MATCH_CHECK("Notification category matches tracked categories"),

    /** Positive keywords were detected in the notification text. */
    POSITIVE_KEYWORD_MATCH("Positive keywords detected in title or body"),

    /** Negative keywords were detected in the notification text. */
    NEGATIVE_KEYWORD_MATCH("Negative keywords detected in title or body"),

    /** A known merchant was detected in the notification text. */
    MERCHANT_MATCH("Known merchant detected in title or body"),

    /** A monetary amount was detected in the notification text. */
    AMOUNT_DETECTED("Monetary amount detected in title or body"),

    /** No specific rule matched; default acceptance. */
    DEFAULT_ACCEPT("Default accept (no rejection rules triggered)")
}
