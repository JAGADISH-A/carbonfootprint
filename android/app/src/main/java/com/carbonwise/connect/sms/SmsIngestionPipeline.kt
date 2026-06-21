package com.carbonwise.connect.sms

import android.util.Log
import com.carbonwise.connect.data.queue.PendingActivityRepository
import com.carbonwise.connect.data.local.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class SmsScanResult(
    val found: Int,
    val relevant: Int,
    val newActivitiesSaved: Int
) {
    val duplicates: Int get() = relevant - newActivitiesSaved
}

@Singleton
class SmsIngestionPipeline @Inject constructor(
    private val collector: SmsCollector,
    private val filter: SmsFilter,
    private val normalizer: SmsNormalizer,
    private val repository: PendingActivityRepository,
    private val settingsStore: SettingsStore
) {
    companion object {
        private const val TAG = "SMSPipeline"
    }

    suspend fun runPipeline(sinceTimestamp: Long = 0L): SmsScanResult = withContext(Dispatchers.IO) {
        var found = 0
        var relevant = 0
        var newActivitiesSaved = 0
        
        // 1. Read SMS (Stages 1-3 are logged inside SmsCollector)
        val rawMessages = collector.collectSms(sinceTimestamp)
        found = rawMessages.size

        // ── Stage 4: Before SmsFilter ──
        Log.d(TAG, "═══════════════════════════════════════════════")
        Log.d(TAG, "Stage 4: Total messages retrieved from SmsCollector = $found")
        Log.d(TAG, "═══════════════════════════════════════════════")

        var newestProcessedTimestamp = 0L
        var hasException = false

        for ((index, rawSms) in rawMessages.withIndex()) {
            // ── Stage 5: SmsFilter ──
            val isAccepted = filter.isUseful(rawSms)
            if (isAccepted) {
                Log.d(TAG, "Stage 5: SMS[${index + 1}] ACCEPTED sender=${rawSms.sender}, body=${rawSms.body.take(60)}...")
                relevant++

                // ── Stage 6: SmsNormalizer ──
                val pendingActivity = normalizer.normalize(rawSms)
                Log.d(TAG, "Stage 6: Normalized → merchant=${pendingActivity.normalizedMerchant}, amount=N/A(not-extracted), category=${pendingActivity.category}, generatedId=${pendingActivity.id.take(16)}...")
                
                try {
                    // ── Stage 7: PendingActivityRepository.insertActivity() ──
                    val wasInserted = repository.insertActivity(pendingActivity)

                    // ── Stage 8: Immediately after insert - query Room row count ──
                    try {
                        val roomRowCount = repository.getTotalRowCount()
                        Log.d(TAG, "Stage 8: Room SELECT COUNT(*) FROM pending_activities = $roomRowCount")
                    } catch (e: Exception) {
                        Log.e(TAG, "Stage 8: Failed to query Room row count", e)
                    }

                    if (wasInserted) {
                        newActivitiesSaved++
                        Log.d(TAG, "Stage 7→8: Insert result = NEW ROW (not duplicate)")
                    } else {
                        Log.d(TAG, "Stage 7→8: Insert result = DUPLICATE (already existed, IGNORE strategy)")
                    }

                    // Log successfully processed (either newly saved or existing duplicate)
                    newestProcessedTimestamp = maxOf(newestProcessedTimestamp, rawSms.receivedTimestamp)

                } catch (e: Exception) {
                    Log.e(TAG, "Stage 7/8 failed with exception for SMS[${index + 1}]", e)
                    hasException = true
                }
            } else {
                // Determine rejection reason
                val rejectionReason = getFilterRejectionReason(rawSms)
                Log.d(TAG, "Stage 5: SMS[${index + 1}] REJECTED sender=${rawSms.sender}, reason=$rejectionReason")
            }
        }

        // ── Stage 9: Return summary ──
        val finalRoomCount = try {
            repository.getTotalRowCount()
        } catch (e: Exception) {
            Log.e(TAG, "Stage 9: Failed to query final Room row count", e)
            -1
        }

        val previousTimestamp = sinceTimestamp
        var newStoredTimestamp = previousTimestamp

        // Condition 3, 4, 5: Update settings store lastSmsScanTimestamp
        if (newActivitiesSaved > 0 && !hasException) {
            newStoredTimestamp = newestProcessedTimestamp
            settingsStore.setLastSmsScanTimestamp(newStoredTimestamp)
        }

        // Log Verification details
        android.util.Log.e("SMS_TIMESTAMP_FIX", "previous timestamp: $previousTimestamp")
        android.util.Log.e("SMS_TIMESTAMP_FIX", "newest processed SMS timestamp: $newestProcessedTimestamp")
        android.util.Log.e("SMS_TIMESTAMP_FIX", "new stored timestamp: $newStoredTimestamp")

        Log.d(TAG, "═══════════════════════════════════════════════")
        Log.d(TAG, "Stage 9: ═══ SMS PIPELINE SUMMARY ═══")
        Log.d(TAG, "Stage 9: SMS scanned:            $found")
        Log.d(TAG, "Stage 9: SMS accepted:           $relevant")
        Log.d(TAG, "Stage 9: PendingActivities created: $newActivitiesSaved")
        Log.d(TAG, "Stage 9: Room rows (total):      $finalRoomCount")
        Log.d(TAG, "Stage 9: Duplicates skipped:     ${relevant - newActivitiesSaved}")
        Log.d(TAG, "Stage 9: Newest processed SMS:   $newestProcessedTimestamp")
        Log.d(TAG, "Stage 9: New stored timestamp:   $newStoredTimestamp")
        Log.d(TAG, "═══════════════════════════════════════════════")
        
        return@withContext SmsScanResult(
            found = found,
            relevant = relevant,
            newActivitiesSaved = newActivitiesSaved
        )
    }

    /**
     * Replicate the SmsFilter logic to determine the exact rejection reason.
     * This does NOT change filter behavior — it only diagnoses why a message was rejected.
     */
    private fun getFilterRejectionReason(sms: RawSms): String {
        val lowerBody = sms.body.lowercase()
        if (lowerBody.contains("otp")) return "Contains 'otp'"
        if (lowerBody.contains("verification code")) return "Contains 'verification code'"
        if (lowerBody.contains("do not share")) return "Contains 'do not share'"
        
        val merchant = MerchantPatterns.getMatchedMerchant(sms.sender, sms.body)
        if (merchant == null) return "No known merchant matched in sender='${sms.sender}' or body"
        
        return "Unknown rejection reason"
    }
}

