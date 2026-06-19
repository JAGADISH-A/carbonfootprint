package com.carbonwise.connect.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: SyncLogEntity): Long

    @Query("SELECT * FROM sync_log ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): SyncLogEntity?

    @Query("DELETE FROM sync_log")
    suspend fun clearAll()
}
