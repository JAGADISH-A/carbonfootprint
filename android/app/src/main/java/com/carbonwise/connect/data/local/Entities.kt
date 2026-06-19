package com.carbonwise.connect.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_data")
data class PendingDataEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val sourcePackage: String,
    val title: String,
    val body: String,
    val timestamp: Long,
    val synced: Boolean = false
)

@Entity(tableName = "sync_log")
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val success: Boolean,
    val itemsSynced: Int,
    val error: String? = null
)
