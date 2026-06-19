package com.carbonwise.connect.ingestion.queue

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface IngestionQueueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: QueuedEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<QueuedEventEntity>): List<Long>

    @Query("SELECT * FROM ingestion_queue WHERE status = 'pending' ORDER BY timestamp ASC")
    suspend fun getPending(): List<QueuedEventEntity>

    @Query("SELECT * FROM ingestion_queue WHERE status = 'pending' ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getPendingBatch(limit: Int): List<QueuedEventEntity>

    @Query("SELECT COUNT(*) FROM ingestion_queue WHERE status = 'pending'")
    suspend fun getPendingCount(): Int

    @Query("SELECT * FROM ingestion_queue WHERE id = :id")
    suspend fun getById(id: String): QueuedEventEntity?

    @Query("UPDATE ingestion_queue SET status = 'synced' WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("UPDATE ingestion_queue SET status = 'failed', lastError = :error, attemptCount = attemptCount + 1 WHERE id = :id")
    suspend fun markFailed(id: String, error: String)

    @Query("UPDATE ingestion_queue SET status = 'uploading' WHERE id IN (:ids)")
    suspend fun markUploading(ids: List<String>)

    @Query("UPDATE ingestion_queue SET status = 'pending', lastError = NULL WHERE id = :id")
    suspend fun resetToPending(id: String)

    @Query("DELETE FROM ingestion_queue WHERE status = 'synced' AND createdAt < :maxAge")
    suspend fun cleanupSynced(maxAge: Long)

    @Query("DELETE FROM ingestion_queue")
    suspend fun clearAll()

    @Query("DELETE FROM ingestion_queue WHERE status = 'failed' AND attemptCount >= :maxAttempts")
    suspend fun purgeExhausted(maxAttempts: Int)
}
