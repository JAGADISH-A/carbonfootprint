package com.carbonwise.connect.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.carbonwise.connect.data.repository.ConnectionRepository
import com.carbonwise.connect.data.repository.ApiResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: ConnectionRepository
) : CoroutineWorker(context, params) {

    companion object {
        private const val CHANNEL_ID = "carbonwise_sync"
        private const val WORK_NAME = "carbonwise_periodic_sync"
        private const val NOTIFICATION_ID = 1001

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        val pendingCount = repository.getPendingCount()
        if (pendingCount == 0) return Result.success()

        return when (val syncResult = repository.syncPendingData()) {
            is ApiResult.Success -> Result.success()
            is ApiResult.Error -> Result.retry()
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Background Sync",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "CarbonWise background data synchronization"
        }

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }
}
