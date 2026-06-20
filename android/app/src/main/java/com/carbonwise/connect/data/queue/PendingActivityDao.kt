package com.carbonwise.connect.data.queue

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingActivityDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(activity: PendingActivityEntity): Long

    @Query("SELECT COUNT(*) FROM pending_activities WHERE syncStatus = 'PENDING'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT * FROM pending_activities WHERE syncStatus = 'PENDING' OR (syncStatus = 'FAILED' AND retryCount < :maxRetries)")
    suspend fun getEligibleForSync(maxRetries: Int = 5): List<PendingActivityEntity>

    @Query("UPDATE pending_activities SET syncStatus = :status WHERE id IN (:ids)")
    suspend fun updateSyncStatus(ids: List<String>, status: String)

    @Query("UPDATE pending_activities SET syncStatus = 'FAILED', retryCount = retryCount + 1 WHERE id IN (:ids)")
    suspend fun incrementRetryCountAndFail(ids: List<String>)

    @Query("UPDATE pending_activities SET syncStatus = 'FAILED_PERMANENT' WHERE id IN (:ids)")
    suspend fun markPermanentFailure(ids: List<String>)

    @Query("SELECT COUNT(*) FROM pending_activities WHERE syncStatus = 'FAILED' OR syncStatus = 'FAILED_PERMANENT'")
    fun getFailedCount(): Flow<Int>

    @Query("DELETE FROM pending_activities WHERE syncStatus = 'SYNCED' AND receivedTimestamp < :olderThanMillis")
    suspend fun cleanupSynced(olderThanMillis: Long)
}
