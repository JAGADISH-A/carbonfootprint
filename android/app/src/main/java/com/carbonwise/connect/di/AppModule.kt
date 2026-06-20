package com.carbonwise.connect.di

import android.content.Context
import androidx.room.Room
import com.carbonwise.connect.data.local.AppDatabase
import com.carbonwise.connect.data.local.PendingDataDao
import com.carbonwise.connect.data.local.SyncLogDao
import com.carbonwise.connect.data.remote.ApiClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "carbonwise_connect.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun providePendingDataDao(db: AppDatabase): PendingDataDao = db.pendingDataDao()

    @Provides
    fun provideSyncLogDao(db: AppDatabase): SyncLogDao = db.syncLogDao()

    @Provides
    @Singleton
    fun provideApiClient(authInterceptor: com.carbonwise.connect.data.network.AuthInterceptor): ApiClient = ApiClient(authInterceptor)

    @Provides
    @Named("base_url")
    fun provideBaseUrl(): String = "http://10.0.2.2:8080/" // Note: use localhost emulator alias for now

    @Provides
    @Singleton
    fun provideMobileApiService(apiClient: ApiClient): com.carbonwise.connect.data.network.MobileApiService = apiClient.mobileApiService
}
