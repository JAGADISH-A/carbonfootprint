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
        Log.d(TAG, "Notification received from package: ${rawNotification.packageName}")

        if (filter.isUseful(rawNotification)) {
            Log.d(TAG, "Notification accepted from package: ${rawNotification.packageName}")
            
            val pendingActivity = normalizer.normalize(rawNotification)
            val wasInserted = repository.insertActivity(pendingActivity)
            
            if (wasInserted) {
                Log.d(TAG, "Notification queued successfully")
            } else {
                Log.d(TAG, "Notification ignored (duplicate)")
            }
        } else {
            Log.d(TAG, "Notification ignored (filtered out)")
        }
    }
}
