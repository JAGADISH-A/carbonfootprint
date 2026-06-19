package com.carbonwise.connect.ingestion.notification.rules

import com.carbonwise.connect.ingestion.model.FilterRule

/**
 * Detects positive and negative keywords in notification text.
 *
 * Performs case-insensitive substring matching against the centralized
 * [PositiveKeywords] and [NegativeKeywords] lists. This class extracts
 * raw keyword data only — it does not calculate confidence, scores,
 * or make accept/reject decisions.
 *
 * # Matching behavior
 *
 * - Matching is case-insensitive.
 * - Exact substring matching: "order" matches "orders" and "ordered".
 * - Multi-word phrases match as complete substrings: "ride completed"
 *   matches "Your ride completed!" but "ride" alone does not.
 * - Both title and body are searched independently.
 * - Duplicate matches across title/body are deduplicated.
 *
 * # Extending
 *
 * To add new keywords, edit [PositiveKeywords] or [NegativeKeywords].
 * Do not add keywords directly to this class.
 */
class KeywordDetector {

    /**
     * Scans notification text for keyword matches.
     *
     * @param title The notification title
     * @param body The notification body text
     * @return [KeywordDetection] containing all detected keywords and rules
     */
    fun detect(title: String, body: String): KeywordDetection {
        val searchText = buildSearchText(title, body)

        val detectedPositive = detectKeywords(
            text = searchText,
            keywords = PositiveKeywords.values
        )

        val detectedNegative = detectKeywords(
            text = searchText,
            keywords = NegativeKeywords.values
        )

        val matchedRules = buildList {
            if (detectedPositive.isNotEmpty()) {
                add(FilterRule.POSITIVE_KEYWORD_MATCH.name)
            }
            if (detectedNegative.isNotEmpty()) {
                add(FilterRule.NEGATIVE_KEYWORD_MATCH.name)
            }
        }

        return KeywordDetection(
            positiveKeywords = detectedPositive,
            negativeKeywords = detectedNegative,
            matchedRules = matchedRules
        )
    }

    /**
     * Detects which keywords from the set appear in the text.
     *
     * @param text Lowercased search text
     * @param keywords Set of keywords to search for
     * @return Set of matched keywords (preserving original casing from the keyword list)
     */
    private fun detectKeywords(text: String, keywords: Set<String>): Set<String> {
        return keywords.filter { keyword ->
            text.contains(keyword.lowercase())
        }.toSet()
    }

    /**
     * Combines title and body into a single lowercase search string.
     */
    private fun buildSearchText(title: String, body: String): String {
        return "$title $body".lowercase()
    }
}

/**
 * Result of keyword detection in notification text.
 *
 * Contains raw extracted keywords with no scoring or decision applied.
 * This is an intermediate data product for downstream pipeline stages.
 *
 * @property positiveKeywords Keywords matched from [PositiveKeywords]
 * @property negativeKeywords Keywords matched from [NegativeKeywords]
 * @property matchedRules Rule names that were triggered by detections
 */
data class KeywordDetection(
    val positiveKeywords: Set<String>,
    val negativeKeywords: Set<String>,
    val matchedRules: List<String>
) {
    /**
     * Whether any keywords were detected (positive or negative).
     */
    val hasMatches: Boolean
        get() = positiveKeywords.isNotEmpty() || negativeKeywords.isNotEmpty()

    /**
     * Total number of keyword matches.
     */
    val totalMatches: Int
        get() = positiveKeywords.size + negativeKeywords.size
}
