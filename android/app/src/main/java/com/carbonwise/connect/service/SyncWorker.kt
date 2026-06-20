package com.carbonwise.connect.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.carbonwise.connect.data.repository.ApiResult
import com.carbonwise.connect.data.repository.UploadRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.UUID
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val uploadRepository: UploadRepository
) : CoroutineWorker(context, params) {

    init {
        android.util.Log.d("SyncWorker", "Worker instantiated")
    }

    companion object {
        private const val CHANNEL_ID = "carbonwise_sync"
        private const val WORK_NAME = "carbonwise_periodic_sync"
        private const val NOTIFICATION_ID = 1001

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15, TimeUnit.MINUTES // Retry 1 -> 15m, Retry 2 -> 30m, etc.
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }

        fun syncNow(context: Context) {
            android.util.Log.d("ManualSync", "SyncWorker.syncNow() called")
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            android.util.Log.d("ManualSync", "WorkRequest ID: ${request.id}")
            
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "carbonwise_manual_sync",
                    ExistingWorkPolicy.REPLACE,
                    request
                )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        android.util.Log.d("ManualSync", "doWork() called")
        android.util.Log.d("SyncWorker", "Starting sync")
        val syncSessionId = UUID.randomUUID().toString()
        val startTime = System.currentTimeMillis()

        return try {
            android.util.Log.d("ManualSync", "UploadRepository.syncBatch()")
            when (val syncResult = uploadRepository.syncBatch(syncSessionId)) {
                is ApiResult.Success -> {
                    // Log success: syncSessionId, duration = System.currentTimeMillis() - startTime, uploaded count
                    android.util.Log.d("SyncWorker", "Upload success")
                    Result.success()
                }
                is ApiResult.Error -> {
                    // Log failure
                    android.util.Log.e("SyncWorker", "Sync failed: ${syncResult.message}")
                    Result.retry()
                }
            }
        } catch (exception: Exception) {
            android.util.Log.e("SyncWorker", "Sync failed", exception)
            Result.retry()
        }
    }
}
