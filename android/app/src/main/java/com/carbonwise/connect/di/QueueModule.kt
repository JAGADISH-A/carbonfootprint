package com.carbonwise.connect.di

import android.content.Context
import androidx.room.Room
import com.carbonwise.connect.data.queue.PendingActivityDao
import com.carbonwise.connect.data.queue.PendingActivityDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object QueueModule {

    @Provides
    @Singleton
    fun providePendingActivityDatabase(@ApplicationContext context: Context): PendingActivityDatabase {
        return Room.databaseBuilder(
            context,
            PendingActivityDatabase::class.java,
            "pending_activities.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun providePendingActivityDao(db: PendingActivityDatabase): PendingActivityDao = db.pendingActivityDao()
}
