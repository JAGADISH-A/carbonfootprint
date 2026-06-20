package com.carbonwise.connect.sms

import com.carbonwise.connect.data.queue.PendingActivityRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsIngestionPipeline @Inject constructor(
    private val collector: SmsCollector,
    private val filter: SmsFilter,
    private val normalizer: SmsNormalizer,
    private val repository: PendingActivityRepository
) {
    suspend fun runPipeline(sinceTimestamp: Long = 0L): Int = withContext(Dispatchers.IO) {
        var newActivitiesSaved = 0
        
        // 1. Read SMS
        val rawMessages = collector.collectSms(sinceTimestamp)
        
        for (rawSms in rawMessages) {
            // 2. Filter
            if (filter.isUseful(rawSms)) {
                // 3. Normalize
                val pendingActivity = normalizer.normalize(rawSms)
                
                // 4. Store in Room (IGNORE duplicate strategy handles existing)
                val wasInserted = repository.insertActivity(pendingActivity)
                if (wasInserted) {
                    newActivitiesSaved++
                }
            }
        }
        
        return@withContext newActivitiesSaved
    }

    suspend fun processRealtimeSms(rawSms: RawSms) = withContext(Dispatchers.IO) {
        val tag = "SMSPipeline"
        android.util.Log.d(tag, "Stage 3: SmsIngestionPipeline.processRealtimeSms() entered.")
        
        try {
            if (filter.isUseful(rawSms)) {
                android.util.Log.d(tag, "Stage 4: Normalization started.")
                val pendingActivity = normalizer.normalize(rawSms)
                
                android.util.Log.d(tag, "Stage 6: Duplicate detection / insertion started.")
                val wasInserted = repository.insertActivity(pendingActivity)
                if (wasInserted) {
                    val count = repository.getPendingCount().first()
                    android.util.Log.d(tag, "Stage 9: Pending count after insert: $count")
                } else {
                    android.util.Log.d(tag, "Stage 6: Duplicate detected, ignored")
                }
            } else {
                android.util.Log.d(tag, "SMS ignored (filtered out)")
            }
        } catch (e: Exception) {
            android.util.Log.e(tag, "Exception in processRealtimeSms", e)
            throw e
        }
    }
}
