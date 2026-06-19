package com.carbonwise.connect.ingestion.notification.rules

/**
 * Detects monetary amounts in notification text.
 *
 * Extracts currency symbols, numeric values, and original matched text
 * from strings containing common Indian Rupee formats. This class performs
 * detection only — it does not score, filter, or make decisions.
 *
 * # Supported formats
 *
 * | Format          | Example        | Currency | Amount  |
 * |-----------------|----------------|----------|---------|
 * | ₹250            | ₹250           | INR      | 250.00  |
 * | ₹1,250          | ₹1,250         | INR      | 1250.00 |
 * | ₹1,250.50       | ₹1,250.50      | INR      | 1250.50 |
 * | Rs.250          | Rs.250         | INR      | 250.00  |
 * | Rs 250          | Rs 250         | INR      | 250.00  |
 * | Rs.1,250.50     | Rs.1,250.50    | INR      | 1250.50 |
 * | INR 250         | INR 250        | INR      | 250.00  |
 * | INR1,250        | INR1,250       | INR      | 1250.00 |
 * | INR 1,250.50    | INR 1,250.50   | INR      | 1250.50 |
 *
 * # Extending
 *
 * To support additional currencies, add new regex patterns to [patterns]
 * and update [CurrencyCode] with the corresponding currency identifier.
 */
class AmountDetector {

    /**
     * Scans text for monetary amount patterns.
     *
     * @param text The text to scan (notification title, body, or combined)
     * @return List of [DetectedAmount] objects, one per match, ordered by position
     */
    fun detect(text: String): List<DetectedAmount> {
        val results = mutableListOf<DetectedAmount>()

        for (pattern in patterns) {
            val matcher = pattern.regex.toRegex().findAll(text)
            for (match in matcher) {
                val amount = parseAmount(match.value, pattern.currency)
                if (amount != null) {
                    results.add(
                        DetectedAmount(
                            currency = pattern.currency,
                            amount = amount,
                            matchedText = match.value,
                            confidence = pattern.confidence
                        )
                    )
                }
            }
        }

        return results.distinctBy { it.matchedText }
    }

    /**
     * Scans title and body independently, combining results.
     *
     * @param title Notification title
     * @param body Notification body
     * @return Combined list of detected amounts from both fields
     */
    fun detectFromFields(title: String, body: String): List<DetectedAmount> {
        return detect(title) + detect(body)
    }

    /**
     * Parses the numeric value from a matched currency string.
     *
     * Strips currency symbols, removes commas, and parses as Double.
     *
     * @param matchedText The raw matched text
     * @param currency The currency code for this pattern
     * @return Parsed amount, or null if parsing fails
     */
    private fun parseAmount(matchedText: String, currency: CurrencyCode): Double? {
        return try {
            val cleaned = matchedText
                .replace(Regex("[₹$]"), "")
                .replace(Regex("Rs\\.?"), "")
                .replace(Regex("INR", RegexOption.IGNORE_CASE), "")
                .replace(",", "")
                .trim()

            cleaned.toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        /**
         * Ordered list of currency patterns.
         *
         * Order matters: more specific patterns (e.g., "₹1,250.50") should appear
         * before less specific ones (e.g., "₹250") to avoid partial matches.
         */
        private val patterns = listOf(
            // ₹1,250.50 (with decimal)
            CurrencyPattern(
                regex = Regex("₹\\d{1,3}(?:,\\d{2,3})*(?:\\.\\d{1,2})?"),
                currency = CurrencyCode.INR,
                confidence = 0.95
            ),
            // ₹1,250 (without decimal)
            CurrencyPattern(
                regex = Regex("₹\\d{1,3}(?:,\\d{2,3})*"),
                currency = CurrencyCode.INR,
                confidence = 0.90
            ),
            // ₹250 (simple)
            CurrencyPattern(
                regex = Regex("₹\\d+(?:\\.\\d{1,2})?"),
                currency = CurrencyCode.INR,
                confidence = 0.85
            ),
            // Rs.1,250.50 (with decimal)
            CurrencyPattern(
                regex = Regex("Rs\\.\\s?\\d{1,3}(?:,\\d{2,3})*(?:\\.\\d{1,2})?"),
                currency = CurrencyCode.INR,
                confidence = 0.90
            ),
            // Rs.1,250
            CurrencyPattern(
                regex = Regex("Rs\\.\\s?\\d{1,3}(?:,\\d{2,3})*"),
                currency = CurrencyCode.INR,
                confidence = 0.85
            ),
            // Rs 250.50
            CurrencyPattern(
                regex = Regex("Rs\\s\\d{1,3}(?:,\\d{2,3})*(?:\\.\\d{1,2})?"),
                currency = CurrencyCode.INR,
                confidence = 0.80
            ),
            // Rs 250
            CurrencyPattern(
                regex = Regex("Rs\\s\\d+(?:\\.\\d{1,2})?"),
                currency = CurrencyCode.INR,
                confidence = 0.75
            ),
            // INR 1,250.50 (with decimal)
            CurrencyPattern(
                regex = Regex("INR\\s?\\d{1,3}(?:,\\d{2,3})*(?:\\.\\d{1,2})?", RegexOption.IGNORE_CASE),
                currency = CurrencyCode.INR,
                confidence = 0.90
            ),
            // INR 1,250
            CurrencyPattern(
                regex = Regex("INR\\s?\\d{1,3}(?:,\\d{2,3})*", RegexOption.IGNORE_CASE),
                currency = CurrencyCode.INR,
                confidence = 0.85
            ),
            // INR 250 (simple)
            CurrencyPattern(
                regex = Regex("INR\\s?\\d+(?:\\.\\d{1,2})?", RegexOption.IGNORE_CASE),
                currency = CurrencyCode.INR,
                confidence = 0.80
            ),
            // $1,250.50 (USD — for future extensibility)
            CurrencyPattern(
                regex = Regex("\\$\\d{1,3}(?:,\\d{3})*(?:\\.\\d{1,2})?"),
                currency = CurrencyCode.USD,
                confidence = 0.85
            ),
        )
    }
}

/**
 * Identifies a currency by its ISO-like code.
 *
 * Currently supports INR only. Additional currencies can be added
 * as new patterns are required.
 */
enum class CurrencyCode(val symbol: String, val displayName: String) {
    INR("₹", "Indian Rupee"),
    USD("$", "US Dollar")
}

/**
 * Internal representation of a currency regex pattern with its confidence.
 *
 * @property regex The pattern to match against text
 * @property currency The currency code for matches
 * @property confidence Base confidence for this pattern type
 */
private data class CurrencyPattern(
    val regex: Regex,
    val currency: CurrencyCode,
    val confidence: Double
)

/**
 * A single detected monetary amount in notification text.
 *
 * @property currency The detected currency code
 * @property amount The parsed numeric amount
 * @property matchedText The original text that was matched
 * @property confidence Confidence score for this detection (0.0 to 1.0)
 */
data class DetectedAmount(
    val currency: CurrencyCode,
    val amount: Double,
    val matchedText: String,
    val confidence: Double
) {
    /**
     * Formatted amount string with currency symbol.
     */
    val formatted: String
        get() = "${currency.symbol}${"%,.2f".format(amount)}"
}
