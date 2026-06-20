package com.carbonwise.connect.sms

import com.carbonwise.connect.data.model.toEntity
import com.carbonwise.connect.data.queue.PendingActivityRepository
import kotlinx.coroutines.Dispatchers
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
                val wasInserted = repository.insertActivity(pendingActivity.toEntity())
                if (wasInserted) {
                    newActivitiesSaved++
                }
            }
        }
        
        return@withContext newActivitiesSaved
    }
}
