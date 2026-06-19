package com.carbonwise.connect.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PendingDataDao {
    @Insert
    suspend fun insert(data: PendingDataEntity): Long

    @Query("SELECT * FROM pending_data WHERE synced = 0 ORDER BY timestamp ASC")
    suspend fun getPending(): List<PendingDataEntity>

    @Query("SELECT COUNT(*) FROM pending_data WHERE synced = 0")
    suspend fun getPendingCount(): Int

    @Query("UPDATE pending_data SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)

    @Query("DELETE FROM pending_data WHERE synced = 1")
    suspend fun cleanupSynced()

    @Query("DELETE FROM pending_data")
    suspend fun clearAll()
}
