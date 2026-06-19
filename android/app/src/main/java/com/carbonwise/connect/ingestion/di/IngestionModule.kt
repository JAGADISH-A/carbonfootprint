package com.carbonwise.connect.ingestion.di

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.carbonwise.connect.ingestion.queue.IngestionQueueDao
import com.carbonwise.connect.ingestion.queue.QueuedEventEntity
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object IngestionModule {

    @Provides
    @Singleton
    fun provideIngestionDatabase(@ApplicationContext context: android.content.Context): IngestionDatabase {
        return Room.databaseBuilder(
            context,
            IngestionDatabase::class.java,
            "carbonwise_ingestion.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideIngestionQueueDao(db: IngestionDatabase): IngestionQueueDao {
        return db.ingestionQueueDao()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()
}

@Database(
    entities = [QueuedEventEntity::class],
    version = 1,
    exportSchema = false
)
abstract class IngestionDatabase : RoomDatabase() {
    abstract fun ingestionQueueDao(): IngestionQueueDao
}
