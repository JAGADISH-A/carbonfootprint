package com.carbonwise.connect.ingestion.notification

import com.carbonwise.connect.ingestion.model.FilterRule
import com.carbonwise.connect.ingestion.model.PackageVerdict
import com.carbonwise.connect.ingestion.notification.rules.DetectedAmount
import com.carbonwise.connect.ingestion.notification.rules.DetectedMerchant
import com.carbonwise.connect.ingestion.notification.rules.KeywordDetection

/**
 * Calculates a confidence score for notification relevance based on
 * multiple signal inputs.
 *
 * Aggregates signals from package evaluation, keyword detection, amount
 * detection, and merchant detection into a single confidence score between
 * 0.0 and 1.0. This class does NOT accept or reject notifications — it
 * only computes a score for downstream decision-making.
 *
 * # Signal weights
 *
 * | Signal             | Weight | Description                                      |
 * |--------------------|--------|--------------------------------------------------|
 * | Package verdict    | 0.30   | ALLOWED > UNKNOWN > BLOCKED                      |
 * | Merchant match     | 0.30   | Known merchant = strong carbon signal            |
 * | Positive keywords  | 0.20   | Carbon-relevant keywords increase confidence     |
 * | Amount detection   | 0.15   | Currency presence = transaction signal           |
 * | Negative keywords  | 0.05   | Spam/marketing keywords decrease confidence      |
 *
 * # Threshold
 *
 * The [THRESHOLD] constant (0.65) defines the minimum confidence for a
 * notification to be considered relevant. Notifications scoring below
 * this threshold should be discarded by downstream stages.
 *
 * # Extending
 *
 * To adjust weights, edit the companion object constants.
 * To add new signals, add a new parameter and weight in [calculate].
 */
class NotificationConfidenceCalculator {

    /**
     * Calculates confidence from all available signals.
     *
     * @param packageEvaluation Result of package name filtering
     * @param keywordDetection Detected positive/negative keywords
     * @param amounts Detected monetary amounts
     * @param merchants Detected known merchants
     * @return [ConfidenceResult] with score, reason, and matched rules
     */
    fun calculate(
        packageEvaluation: PackageEvaluation,
        keywordDetection: KeywordDetection,
        amounts: List<DetectedAmount>,
        merchants: List<DetectedMerchant>
    ): ConfidenceResult {
        val matchedRules = mutableListOf<String>()
        var confidence = 0.0

        // Signal 1: Package verdict
        val packageScore = scorePackage(packageEvaluation.verdict)
        confidence += packageScore * WEIGHT_PACKAGE
        if (packageEvaluation.verdict == PackageVerdict.ALLOWED) {
            matchedRules.add(FilterRule.PACKAGE_ALLOWLIST.name)
        }

        // Signal 2: Merchant match
        val merchantScore = scoreMerchants(merchants)
        confidence += merchantScore * WEIGHT_MERCHANT
        if (merchants.isNotEmpty()) {
            matchedRules.add(FilterRule.MERCHANT_MATCH.name)
        }

        // Signal 3: Positive keywords
        val positiveScore = scorePositiveKeywords(keywordDetection)
        confidence += positiveScore * WEIGHT_POSITIVE_KEYWORDS
        if (keywordDetection.positiveKeywords.isNotEmpty()) {
            matchedRules.add(FilterRule.POSITIVE_KEYWORD_MATCH.name)
        }

        // Signal 4: Amount detection
        val amountScore = scoreAmounts(amounts)
        confidence += amountScore * WEIGHT_AMOUNT
        if (amounts.isNotEmpty()) {
            matchedRules.add(FilterRule.AMOUNT_DETECTED.name)
        }

        // Signal 5: Negative keywords (penalty)
        val negativeScore = scoreNegativeKeywords(keywordDetection)
        confidence += negativeScore * WEIGHT_NEGATIVE_KEYWORDS
        if (keywordDetection.negativeKeywords.isNotEmpty()) {
            matchedRules.add(FilterRule.NEGATIVE_KEYWORD_MATCH.name)
        }

        confidence = confidence.coerceIn(0.0, 1.0)

        val reason = buildReason(
            confidence = confidence,
            packageVerdict = packageEvaluation.verdict,
            merchantCount = merchants.size,
            positiveCount = keywordDetection.positiveKeywords.size,
            negativeCount = keywordDetection.negativeKeywords.size,
            amountCount = amounts.size
        )

        return ConfidenceResult(
            confidence = confidence,
            matchedRules = matchedRules,
            reason = reason
        )
    }

    /**
     * Scores the package verdict contribution.
     *
     * ALLOWED = 1.0 (full contribution)
     * UNKNOWN = 0.5 (partial contribution)
     * BLOCKED = 0.0 (no contribution)
     */
    private fun scorePackage(verdict: PackageVerdict): Double {
        return when (verdict) {
            PackageVerdict.ALLOWED -> 1.0
            PackageVerdict.UNKNOWN -> 0.5
            PackageVerdict.BLOCKED -> 0.0
        }
    }

    /**
     * Scores merchant detection contribution.
     *
     * Uses the highest merchant confidence found, scaled to 0.0–1.0.
     * Multiple merchants don't stack — only the best match matters.
     */
    private fun scoreMerchants(merchants: List<DetectedMerchant>): Double {
        if (merchants.isEmpty()) return 0.0
        return merchants.maxOf { it.confidence }
    }

    /**
     * Scores positive keyword contribution.
     *
     * Each positive keyword adds a fractional amount, capped at 1.0.
     * More keywords = higher confidence, with diminishing returns.
     */
    private fun scorePositiveKeywords(detection: KeywordDetection): Double {
        if (detection.positiveKeywords.isEmpty()) return 0.0
        val raw = detection.positiveKeywords.size.toDouble() * KEYWORD_SCALE
        return raw.coerceAtMost(1.0)
    }

    /**
     * Scores amount detection contribution.
     *
     * Presence of any currency amount is a strong transaction signal.
     * Uses the highest amount confidence found.
     */
    private fun scoreAmounts(amounts: List<DetectedAmount>): Double {
        if (amounts.isEmpty()) return 0.0
        return amounts.maxOf { it.confidence }
    }

    /**
     * Scores negative keyword penalty.
     *
     * Each negative keyword applies a fractional penalty, floored at -1.0.
     * More negative keywords = lower confidence.
     */
    private fun scoreNegativeKeywords(detection: KeywordDetection): Double {
        if (detection.negativeKeywords.isEmpty()) return 0.0
        val raw = detection.negativeKeywords.size.toDouble() * NEGATIVE_KEYWORD_SCALE
        return raw.coerceAtLeast(-1.0)
    }

    /**
     * Builds a human-readable reason string.
     */
    private fun buildReason(
        confidence: Double,
        packageVerdict: PackageVerdict,
        merchantCount: Int,
        positiveCount: Int,
        negativeCount: Int,
        amountCount: Int
    ): String {
        val signals = mutableListOf<String>()

        signals.add("package=${packageVerdict.name.lowercase()}")
        if (merchantCount > 0) signals.add("merchants=$merchantCount")
        if (positiveCount > 0) signals.add("positive_keywords=$positiveCount")
        if (amountCount > 0) signals.add("amounts=$amountCount")
        if (negativeCount > 0) signals.add("negative_keywords=$negativeCount")

        return "confidence=${"%.2f".format(confidence)} [${signals.joinToString(", ")}]"
    }

    companion object {
        /** Minimum confidence threshold for relevance. */
        const val THRESHOLD = 0.65

        /** Weight for package verdict signal. */
        private const val WEIGHT_PACKAGE = 0.30

        /** Weight for merchant detection signal. */
        private const val WEIGHT_MERCHANT = 0.30

        /** Weight for positive keyword signal. */
        private const val WEIGHT_POSITIVE_KEYWORDS = 0.20

        /** Weight for amount detection signal. */
        private const val WEIGHT_AMOUNT = 0.15

        /** Weight for negative keyword penalty. */
        private const val WEIGHT_NEGATIVE_KEYWORDS = 0.05

        /** Scale factor for positive keywords (each keyword adds this amount). */
        private const val KEYWORD_SCALE = 0.15

        /** Scale factor for negative keywords (each keyword subtracts this amount). */
        private const val NEGATIVE_KEYWORD_SCALE = -0.10
    }
}

/**
 * Result of confidence calculation for a notification.
 *
 * @property confidence Final confidence score between 0.0 and 1.0
 * @property matchedRules Rule names that contributed to the score
 * @property reason Human-readable explanation of the score breakdown
 */
data class ConfidenceResult(
    val confidence: Double,
    val matchedRules: List<String>,
    val reason: String
) {
    /**
     * Whether the confidence meets or exceeds the threshold.
     */
    val meetsThreshold: Boolean
        get() = confidence >= NotificationConfidenceCalculator.THRESHOLD
}
