package com.carbonwise.connect.data.queue

import com.carbonwise.connect.data.model.PendingActivity
import com.carbonwise.connect.data.model.toDomain
import com.carbonwise.connect.data.model.toEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingActivityRepository @Inject constructor(
    private val dao: PendingActivityDao
) {
    suspend fun insertActivity(activity: PendingActivity): Boolean {
        val tag = if (activity.source.name == "SMS") "SMSPipeline" else "NotificationPipeline"
        android.util.Log.d(tag, "Stage 7: PendingActivityRepository.insert() started for id: ${activity.id}")
        val result = dao.insert(activity.toEntity())
        if (result != -1L) {
            android.util.Log.d(tag, "Stage 8: Room insert success (row: $result, table: pending_activities, pk: ${activity.id})")
        }
        return result != -1L
    }

    fun getPendingCount(): Flow<Int> {
        return dao.getPendingCount()
    }

    suspend fun countPending(): Int {
        return dao.countPending()
    }

    suspend fun getPendingActivities(): List<PendingActivity> {
        return dao.getPendingActivities().map { it.toDomain() }
    }

    fun getPendingCountBySource(source: String): Flow<Int> {
        return dao.getPendingCountBySource(source)
    }

    fun getFailedCount(): Flow<Int> {
        return dao.getFailedCount()
    }

    suspend fun getEligibleForSync(maxRetries: Int = 5): List<PendingActivity> {
        android.util.Log.d("UploadPipeline", "PendingActivityRepository.getEligibleForSync(maxRetries=$maxRetries) called")
        val entities = dao.getEligibleForSync(maxRetries)
        android.util.Log.d("UploadPipeline", "PendingActivityRepository.getEligibleForSync() returning ${entities.size} items")
        return entities.map { it.toDomain() }
    }

    suspend fun updateSyncStatus(ids: List<String>, status: String) {
        android.util.Log.d("UploadPipeline", "PendingActivityRepository.updateSyncStatus(idsCount=${ids.size}, status=$status) called. IDs: $ids")
        dao.updateSyncStatus(ids, status)
        android.util.Log.d("UploadPipeline", "PendingActivityRepository.updateSyncStatus() completed")
    }

    suspend fun incrementRetryCountAndFail(ids: List<String>) {
        android.util.Log.d("UploadPipeline", "PendingActivityRepository.incrementRetryCountAndFail(idsCount=${ids.size}) called. IDs: $ids")
        dao.incrementRetryCountAndFail(ids)
        android.util.Log.d("UploadPipeline", "PendingActivityRepository.incrementRetryCountAndFail() completed")
    }

    suspend fun markPermanentFailure(ids: List<String>) {
        android.util.Log.d("UploadPipeline", "PendingActivityRepository.markPermanentFailure(idsCount=${ids.size}) called. IDs: $ids")
        dao.markPermanentFailure(ids)
        android.util.Log.d("UploadPipeline", "PendingActivityRepository.markPermanentFailure() completed")
    }

    suspend fun cleanupSynced(olderThanMillis: Long) {
        dao.cleanupSynced(olderThanMillis)
    }

    // Diagnostic: total row count for pipeline instrumentation
    suspend fun getTotalRowCount(): Int {
        return dao.getTotalRowCount()
    }
}
