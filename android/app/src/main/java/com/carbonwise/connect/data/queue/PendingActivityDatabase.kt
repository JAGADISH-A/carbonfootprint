package com.carbonwise.connect.data.queue

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PendingActivityEntity::class], version = 1, exportSchema = false)
abstract class PendingActivityDatabase : RoomDatabase() {
    abstract fun pendingActivityDao(): PendingActivityDao
}
