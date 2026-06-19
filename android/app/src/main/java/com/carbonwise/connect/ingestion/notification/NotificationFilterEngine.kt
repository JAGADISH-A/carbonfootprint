package com.carbonwise.connect.ingestion.notification

import android.util.Log
import com.carbonwise.connect.ingestion.model.AmountLogEntry
import com.carbonwise.connect.ingestion.model.FilterLog
import com.carbonwise.connect.ingestion.model.FilterResult
import com.carbonwise.connect.ingestion.model.FilterRule
import com.carbonwise.connect.ingestion.model.MerchantLogEntry
import com.carbonwise.connect.ingestion.model.NotificationEvent
import com.carbonwise.connect.ingestion.model.PackageVerdict
import com.carbonwise.connect.ingestion.notification.rules.AllowedPackages
import com.carbonwise.connect.ingestion.notification.rules.AmountDetector
import com.carbonwise.connect.ingestion.notification.rules.BlockedPackages
import com.carbonwise.connect.ingestion.notification.rules.DetectedAmount
import com.carbonwise.connect.ingestion.notification.rules.DetectedMerchant
import com.carbonwise.connect.ingestion.notification.rules.KeywordDetection
import com.carbonwise.connect.ingestion.notification.rules.KeywordDetector
import com.carbonwise.connect.ingestion.notification.rules.MerchantDetector
import com.carbonwise.connect.ingestion.pipeline.DataFilter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Full notification filtering engine that evaluates carbon relevance.
 *
 * Orchestrates the complete filtering pipeline:
 * 1. Package evaluation (blocklist/allowlist)
 * 2. Keyword detection (positive/negative)
 * 3. Amount detection (currency patterns)
 * 4. Merchant detection (known merchants)
 * 5. Confidence calculation (weighted signal aggregation)
 * 6. Decision (threshold-based accept/reject)
 *
 * # Architecture
 *
 * This engine is a pure orchestrator. Each step is delegated to a focused,
 * single-responsibility component. The engine itself contains no business
 * logic beyond coordination and logging.
 *
 * # Determinism
 *
 * Given the same inputs, this engine always produces the same output.
 * All detectors read from immutable configuration. No external state
 * is mutated during filtering.
 *
 * # Testability
 *
 * All dependencies are injected via constructor. In tests, provide mock
 * or stub implementations of each detector. The engine can also be tested
 * end-to-end by providing known inputs and asserting on FilterResult.
 *
 * # Logging
 *
 * Every filtering decision produces a [FilterLog] entry with full
 * traceability. Logs are printed via Android Log for development and
 * can be captured by analytics systems in production.
 *
 * # Extending
 *
 * To add new signals:
 * 1. Create a detector in [rules]
 * 2. Add it as a constructor parameter
 * 3. Call it in [filter]
 * 4. Add its output to [NotificationConfidenceCalculator.calculate]
 */
@Singleton
class NotificationFilterEngine @Inject constructor(
    private val keywordDetector: KeywordDetector,
    private val amountDetector: AmountDetector,
    private val merchantDetector: MerchantDetector,
    private val confidenceCalculator: NotificationConfidenceCalculator
) : DataFilter<NotificationEvent> {

    /**
     * Filters a notification event through the complete pipeline.
     *
     * This is the primary entry point. It runs all detection stages,
     * calculates confidence, and returns a final [FilterResult].
     *
     * @param data The notification event to filter
     * @return true if the notification is relevant for carbon tracking
     */
    override fun isRelevant(data: NotificationEvent): Boolean {
        val result = filter(data)
        return result.accepted
    }

    /**
     * Runs the full filtering pipeline and returns a detailed result.
     *
     * Use this method when you need access to the complete [FilterResult]
     * with confidence, matched rules, and reason — not just the boolean.
     *
     * @param data The notification event to filter
     * @return [FilterResult] with full decision details
     */
    fun filter(data: NotificationEvent): FilterResult {
        val startTime = System.nanoTime()

        // Stage 1: Package evaluation
        val packageEval = evaluatePackage(data.packageName)

        // Short-circuit: blocked packages are always rejected
        if (packageEval.verdict == PackageVerdict.BLOCKED) {
            val log = buildLog(
                data = data,
                packageEval = packageEval,
                keywords = emptyKeywordDetection(),
                amounts = emptyList(),
                merchants = emptyList(),
                confidence = 0.0,
                accepted = false,
                matchedRules = packageEval.filterResult.matchedRules,
                reason = packageEval.filterResult.reason,
                startTime = startTime
            )
            logFilterDecision(log)
            return FilterResult.reject(
                reason = log.reason,
                matchedRules = log.matchedRules,
                confidence = 0.0
            )
        }

        // Stage 2: Keyword detection
        val keywordResult = keywordDetector.detect(data.rawData.title, data.rawData.body)

        // Stage 3: Amount detection
        val searchText = "${data.rawData.title} ${data.rawData.body}"
        val amounts = amountDetector.detect(searchText)

        // Stage 4: Merchant detection
        val merchants = merchantDetector.detect(searchText)

        // Stage 5: Confidence calculation
        val confidenceResult = confidenceCalculator.calculate(
            packageEvaluation = packageEval,
            keywordDetection = keywordResult,
            amounts = amounts,
            merchants = merchants
        )

        // Stage 6: Decision
        val accepted = confidenceResult.meetsThreshold
        val reason = if (accepted) {
            "Accepted: ${confidenceResult.reason}"
        } else {
            "Rejected: ${confidenceResult.reason}"
        }

        val allRules = (packageEval.filterResult.matchedRules + confidenceResult.matchedRules).distinct()

        // Build and log
        val log = buildLog(
            data = data,
            packageEval = packageEval,
            keywords = keywordResult,
            amounts = amounts,
            merchants = merchants,
            confidence = confidenceResult.confidence,
            accepted = accepted,
            matchedRules = allRules,
            reason = reason,
            startTime = startTime
        )
        logFilterDecision(log)

        return if (accepted) {
            FilterResult.accept(
                reason = reason,
                matchedRules = allRules,
                confidence = confidenceResult.confidence
            )
        } else {
            FilterResult.reject(
                reason = reason,
                matchedRules = allRules,
                confidence = confidenceResult.confidence
            )
        }
    }

    /**
     * Evaluates a package name against blocklist and allowlist.
     *
     * @param packageName The Android package name
     * @return [PackageEvaluation] with verdict and partial filter result
     */
    fun evaluatePackage(packageName: String): PackageEvaluation {
        // Blocklist always wins
        if (packageName in BlockedPackages.values) {
            return PackageEvaluation(
                verdict = PackageVerdict.BLOCKED,
                filterResult = FilterResult.reject(
                    reason = "Package is blocked: $packageName",
                    matchedRules = listOf(FilterRule.PACKAGE_BLOCKLIST.name)
                )
            )
        }

        // Allowlist mode
        if (AllowedPackages.values.isNotEmpty()) {
            return if (packageName in AllowedPackages.values) {
                PackageEvaluation(
                    verdict = PackageVerdict.ALLOWED,
                    filterResult = FilterResult.accept(
                        reason = "Package is in allowlist: $packageName",
                        matchedRules = listOf(FilterRule.PACKAGE_ALLOWLIST.name)
                    )
                )
            } else {
                PackageEvaluation(
                    verdict = PackageVerdict.UNKNOWN,
                    filterResult = FilterResult.reject(
                        reason = "Package is not in allowlist: $packageName",
                        matchedRules = listOf(FilterRule.PACKAGE_ALLOWLIST.name)
                    )
                )
            }
        }

        // No allowlist active
        return PackageEvaluation(
            verdict = PackageVerdict.UNKNOWN,
            filterResult = FilterResult.accept(
                reason = "Package not in blocklist, no allowlist active: $packageName",
                matchedRules = listOf(FilterRule.DEFAULT_ACCEPT.name)
            )
        )
    }

    /**
     * Builds a structured log entry for the filtering decision.
     */
    private fun buildLog(
        data: NotificationEvent,
        packageEval: PackageEvaluation,
        keywords: KeywordDetection,
        amounts: List<DetectedAmount>,
        merchants: List<DetectedMerchant>,
        confidence: Double,
        accepted: Boolean,
        matchedRules: List<String>,
        reason: String,
        startTime: Long
    ): FilterLog {
        val durationMs = (System.nanoTime() - startTime) / 1_000_000

        return FilterLog(
            packageName = data.packageName,
            title = data.rawData.title.take(FilterLog.MAX_TITLE_LENGTH),
            body = data.rawData.body.take(FilterLog.MAX_BODY_LENGTH),
            timestamp = data.timestamp,
            filterTimestamp = System.currentTimeMillis(),
            packageVerdict = packageEval.verdict,
            keywords = keywords.positiveKeywords,
            negativeKeywords = keywords.negativeKeywords,
            amounts = amounts.map {
                AmountLogEntry(
                    currency = it.currency.name,
                    amount = it.amount,
                    matchedText = it.matchedText
                )
            },
            merchants = merchants.map {
                MerchantLogEntry(
                    name = it.merchantName,
                    category = it.category.name,
                    matchedKeyword = it.matchedKeyword,
                    confidence = it.confidence
                )
            },
            confidence = confidence,
            accepted = accepted,
            matchedRules = matchedRules,
            reason = reason,
            durationMs = durationMs
        )
    }

    /**
     * Outputs the filter log via Android Log.
     *
     * In production, this could also send to analytics, crash reporting,
     * or a local database for debugging.
     */
    private fun logFilterDecision(log: FilterLog) {
        if (log.accepted) {
            Log.i(TAG, log.toLogString())
        } else {
            Log.d(TAG, log.toLogString())
        }
    }

    private fun emptyKeywordDetection() = KeywordDetection(
        positiveKeywords = emptySet(),
        negativeKeywords = emptySet(),
        matchedRules = emptyList()
    )

    companion object {
        private const val TAG = "NotificationFilter"
    }
}

/**
 * Result of evaluating a single package name against the filter rules.
 *
 * @property verdict Whether the package is allowed, blocked, or unknown
 * @property filterResult The partial filter result with reason and matched rules
 */
data class PackageEvaluation(
    val verdict: PackageVerdict,
    val filterResult: FilterResult
)
