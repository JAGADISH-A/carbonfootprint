package com.carbonwise.connect.ingestion.model

/**
 * Structured log entry for a single notification filtering decision.
 *
 * Captures every stage of the filtering pipeline for debugging, analytics,
 * and audit trails. Each [FilterLog] represents one complete evaluation
 * of a notification through the filter engine.
 *
 * Immutable value object. All fields are set at construction time.
 *
 * # Usage
 *
 * The filter engine creates a [FilterLog] for every notification it processes.
 * Logs can be stored locally, transmitted to analytics, or printed for debugging.
 *
 * # Fields
 *
 * @property packageName The Android package name of the notification source
 * @property title Notification title (truncated for storage)
 * @property body Notification body (truncated for storage)
 * @property timestamp When the notification was posted (from StatusBarNotification)
 * @property filterTimestamp When the filter engine evaluated this notification
 * @property packageVerdict Result of package name evaluation
 * @property keywords Positive keywords detected
 * @property negativeKeywords Negative keywords detected
 * @property amounts Monetary amounts detected
 * @property merchants Known merchants detected
 * @property confidence Final confidence score
 * @property accepted Whether the notification passed filtering
 * @property matchedRules All rules that were triggered
 * @property reason Human-readable explanation of the decision
 * @property durationMs Time taken to evaluate (for performance monitoring)
 */
data class FilterLog(
    val packageName: String,
    val title: String,
    val body: String,
    val timestamp: Long,
    val filterTimestamp: Long,
    val packageVerdict: PackageVerdict,
    val keywords: Set<String>,
    val negativeKeywords: Set<String>,
    val amounts: List<AmountLogEntry>,
    val merchants: List<MerchantLogEntry>,
    val confidence: Double,
    val accepted: Boolean,
    val matchedRules: List<String>,
    val reason: String,
    val durationMs: Long
) {
    /**
     * Compact string representation for logging output.
     */
    fun toLogString(): String {
        return buildString {
            append("[FILTER] ")
            append("pkg=$packageName ")
            append("verdict=${packageVerdict.name} ")
            append("confidence=${"%.2f".format(confidence)} ")
            append("accepted=$accepted ")
            if (keywords.isNotEmpty()) append("keywords=$keywords ")
            if (negativeKeywords.isNotEmpty()) append("negative=${negativeKeywords} ")
            if (amounts.isNotEmpty()) append("amounts=${amounts.size} ")
            if (merchants.isNotEmpty()) append("merchants=${merchants.map { it.name }} ")
            append("rules=$matchedRules ")
            append("duration=${durationMs}ms")
        }
    }

    companion object {
        /** Maximum length for title in logs. */
        const val MAX_TITLE_LENGTH = 100

        /** Maximum length for body in logs. */
        const val MAX_BODY_LENGTH = 200
    }
}

/**
 * Compact amount entry for logging.
 *
 * @property currency Currency code
 * @property amount Parsed numeric value
 * @property matchedText Original text that was matched
 */
data class AmountLogEntry(
    val currency: String,
    val amount: Double,
    val matchedText: String
)

/**
 * Compact merchant entry for logging.
 *
 * @property name Canonical merchant name
 * @property category Merchant category
 * @property matchedKeyword The specific keyword that was matched
 * @property confidence Detection confidence
 */
data class MerchantLogEntry(
    val name: String,
    val category: String,
    val matchedKeyword: String,
    val confidence: Double
)
