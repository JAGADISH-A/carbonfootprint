package com.carbonwise.connect.data.queue

import com.carbonwise.connect.data.model.PendingActivity
import com.carbonwise.connect.data.model.toDomain
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingActivityRepository @Inject constructor(
    private val dao: PendingActivityDao
) {
    suspend fun insertActivity(activity: PendingActivity): Boolean {
        val result = dao.insert(activity.toEntity())
        return result != -1L
    }

    fun getPendingCount(): Flow<Int> {
        return dao.getPendingCount()
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
