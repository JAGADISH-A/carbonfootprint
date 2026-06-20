package com.carbonwise.connect.notification

import android.util.Log
import com.carbonwise.connect.data.queue.PendingActivityRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationIngestionPipeline @Inject constructor(
    private val filter: NotificationFilter,
    private val normalizer: NotificationNormalizer,
    private val repository: PendingActivityRepository
) {
    companion object {
        private const val TAG = "NotificationPipeline"
    }

    suspend fun processNotification(rawNotification: RawNotification) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Stage 3: pipeline.processNotification() entered")
            Log.d(TAG, "Notification received from package: ${rawNotification.packageName}")

            if (filter.isUseful(rawNotification)) {
                Log.d(TAG, "Notification accepted from package: ${rawNotification.packageName}")
                
                Log.d(TAG, "Stage 4: Notification normalization started")
                val pendingActivity = normalizer.normalize(rawNotification)
                Log.d(TAG, "Stage 6: Duplicate detection (inserting to repository)")
                val wasInserted = repository.insertActivity(pendingActivity)
                
                if (wasInserted) {
                    Log.d(TAG, "Stage 9: Queue insert success (wasInserted=true)")
                } else {
                    Log.d(TAG, "Stage 6: Duplicate detection (ignored duplicate)")
                }
            } else {
                Log.d(TAG, "Notification ignored (filtered out)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Stage 3/4/6/9 failed", e)
        }
    }
}
