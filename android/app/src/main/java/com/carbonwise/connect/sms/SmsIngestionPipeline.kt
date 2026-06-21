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
        
        val ignoredCounts = mutableMapOf<String, Int>(
            "OTP/Security" to 0,
            "Promotion" to 0,
            "Shipping Noise" to 0,
            "Noise" to 0
        )

        val rawMessages = collector.collectSms(sinceTimestamp)
        found = rawMessages.size

        var newestProcessedTimestamp = 0L
        var hasException = false

        for ((index, rawSms) in rawMessages.withIndex()) {
            val rejectionReason = filter.getRejectionReason(rawSms)
            if (rejectionReason == null) {
                relevant++

                val pendingActivity = normalizer.normalize(rawSms)
                
                try {
                    val wasInserted = repository.insertActivity(pendingActivity)

                    if (wasInserted) {
                        newActivitiesSaved++
                    }

                    newestProcessedTimestamp = maxOf(newestProcessedTimestamp, rawSms.receivedTimestamp)

                } catch (e: Exception) {
                    Log.e(TAG, "Failed to insert SMS activity[${index + 1}]", e)
                    hasException = true
                }
            } else {
                ignoredCounts[rejectionReason] = (ignoredCounts[rejectionReason] ?: 0) + 1
            }
        }

        val previousTimestamp = sinceTimestamp
        var newStoredTimestamp = previousTimestamp

        if (newActivitiesSaved > 0 && !hasException) {
            newStoredTimestamp = newestProcessedTimestamp
            settingsStore.setLastSmsScanTimestamp(newStoredTimestamp)
        }

        Log.d(TAG, "Pipeline complete: found=$found, relevant=$relevant, saved=$newActivitiesSaved, duplicates=${relevant - newActivitiesSaved}")
        
        return@withContext SmsScanResult(
            found = found,
            relevant = relevant,
            newActivitiesSaved = newActivitiesSaved
        )
    }
}
