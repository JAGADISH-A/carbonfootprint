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
        try {
            android.util.Log.d(tag, "Stage 7: PendingActivityRepository.insert() started for id: ${activity.id}")
            val result = dao.insert(activity.toEntity())
            if (result != -1L) {
                android.util.Log.d(tag, "Stage 8: Room insert success (row: $result, table: pending_activities, pk: ${activity.id})")
            }
            return result != -1L
        } catch (e: Exception) {
            android.util.Log.e(tag, "Stage 7/8 failed", e)
            return false
        }
    }

    fun getPendingCount(): Flow<Int> {
        return dao.getPendingCount()
    }

    fun getPendingCountBySource(source: String): Flow<Int> {
        return dao.getPendingCountBySource(source)
    }

    fun getFailedCount(): Flow<Int> {
        return dao.getFailedCount()
    }

    suspend fun getEligibleForSync(maxRetries: Int = 5): List<PendingActivity> {
        return dao.getEligibleForSync(maxRetries).map { it.toDomain() }
    }

    suspend fun updateSyncStatus(ids: List<String>, status: String) {
        dao.updateSyncStatus(ids, status)
    }

    suspend fun incrementRetryCountAndFail(ids: List<String>) {
        dao.incrementRetryCountAndFail(ids)
    }

    suspend fun markPermanentFailure(ids: List<String>) {
        dao.markPermanentFailure(ids)
    }

    suspend fun cleanupSynced(olderThanMillis: Long) {
        dao.cleanupSynced(olderThanMillis)
    }
}
