package com.carbonwise.connect.ingestion.notification.rules

/**
 * Detects known merchant names in notification text.
 *
 * Searches notification title and body against the centralized [KnownMerchants]
 * list, supporting both primary names and aliases. This class performs detection
 * only — it does not filter, classify, or make accept/reject decisions.
 *
 * # Matching behavior
 *
 * - Matching is case-insensitive.
 * - Partial matching is supported: "uber" matches "Uber Ride" and "Uber Eats".
 * - Longer matches receive higher confidence (more specific = more reliable).
 * - Exact matches (full merchant name in text) receive maximum confidence.
 * - Both title and body are searched independently.
 * - Duplicate matches across title/body are deduplicated by merchant name.
 *
 * # Extending
 *
 * To add new merchants, edit [KnownMerchants]. Do not add merchants directly
 * to this class.
 */
class MerchantDetector {

    /**
     * Scans text for merchant name matches.
     *
     * @param text The text to scan (notification title, body, or combined)
     * @return List of [DetectedMerchant] objects, ordered by confidence descending
     */
    fun detect(text: String): List<DetectedMerchant> {
        val lowerText = text.lowercase()
        val results = mutableListOf<DetectedMerchant>()

        for (merchant in KnownMerchants.values) {
            val match = findBestMatch(lowerText, merchant) ?: continue
            results.add(match)
        }

        return results
            .distinctBy { it.merchantName }
            .sortedByDescending { it.confidence }
    }

    /**
     * Scans title and body independently, combining results.
     *
     * @param title Notification title
     * @param body Notification body
     * @return Combined list of detected merchants from both fields
     */
    fun detectFromFields(title: String, body: String): List<DetectedMerchant> {
        return detect(title) + detect(body)
    }

    /**
     * Finds the best matching keyword for a merchant in the text.
     *
     * Checks all names (primary + aliases) and returns the longest match
     * with the highest confidence.
     *
     * @param lowerText Lowercased search text
     * @param merchant The merchant entry to match against
     * @return Best [DetectedMerchant] or null if no match found
     */
    private fun findBestMatch(lowerText: String, merchant: MerchantEntry): DetectedMerchant? {
        var bestMatch: DetectedMerchant? = null

        for (name in merchant.allNames) {
            val lowerName = name.lowercase()
            if (!lowerText.contains(lowerName)) continue

            val confidence = calculateConfidence(
                merchantName = lowerName,
                searchText = lowerText,
                isPrimaryName = name == merchant.name
            )

            if (bestMatch == null || confidence > bestMatch.confidence) {
                bestMatch = DetectedMerchant(
                    merchantName = merchant.name,
                    category = merchant.category,
                    matchedKeyword = name,
                    confidence = confidence
                )
            }
        }

        return bestMatch
    }

    /**
     * Calculates confidence for a merchant match.
     *
     * Factors:
     * - Length of the merchant name relative to the search text
     * - Whether the match is exact (merchant name is a standalone word)
     * - Whether it's the primary name or an alias
     *
     * @param merchantName Lowercased merchant name that was matched
     * @param searchText Lowercased full text
     * @param isPrimaryName Whether the matched name is the primary merchant name
     * @return Confidence score between 0.0 and 1.0
     */
    private fun calculateConfidence(
        merchantName: String,
        searchText: String,
        isPrimaryName: Boolean
    ): Double {
        var confidence = BASE_CONFIDENCE

        // Longer matches are more specific
        val lengthRatio = merchantName.length.toDouble() / searchText.length.toDouble()
        confidence += lengthRatio * LENGTH_WEIGHT

        // Exact word boundary match is more reliable
        if (isExactWordMatch(searchText, merchantName)) {
            confidence += EXACT_MATCH_BONUS
        }

        // Primary name is more reliable than aliases
        if (isPrimaryName) {
            confidence += PRIMARY_NAME_BONUS
        }

        return confidence.coerceIn(0.0, 1.0)
    }

    /**
     * Checks if the merchant name appears as a standalone word/phrase.
     *
     * Looks for word boundaries before and after the match.
     *
     * @param text The full lowercased text
     * @param term The lowercased term to find
     * @return True if the term appears at a word boundary
     */
    private fun isExactWordMatch(text: String, term: String): Boolean {
        val regex = Regex("\\b${Regex.escape(term)}\\b")
        return regex.containsMatchIn(text)
    }

    companion object {
        /** Base confidence for any merchant match. */
        private const val BASE_CONFIDENCE = 0.60

        /** Weight for length ratio (longer merchant names = more specific). */
        private const val LENGTH_WEIGHT = 0.25

        /** Bonus for exact word boundary match. */
        private const val EXACT_MATCH_BONUS = 0.10

        /** Bonus for matching the primary merchant name vs an alias. */
        private const val PRIMARY_NAME_BONUS = 0.05
    }
}

/**
 * A single detected merchant in notification text.
 *
 * @property merchantName The canonical merchant name (from [KnownMerchants])
 * @property category The merchant's carbon-relevant category
 * @property matchedKeyword The specific name or alias that was matched
 * @property confidence Confidence score for this detection (0.0 to 1.0)
 */
data class DetectedMerchant(
    val merchantName: String,
    val category: MerchantCategory,
    val matchedKeyword: String,
    val confidence: Double
)
