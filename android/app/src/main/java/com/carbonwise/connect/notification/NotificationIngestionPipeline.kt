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
            if (filter.isUseful(rawNotification)) {
                val pendingActivity = normalizer.normalize(rawNotification)
                repository.insertActivity(pendingActivity)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process notification", e)
        }
    }
}
