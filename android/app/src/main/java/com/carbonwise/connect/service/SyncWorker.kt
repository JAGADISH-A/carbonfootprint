package com.carbonwise.connect.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
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
import kotlinx.coroutines.flow.first
import java.util.UUID
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val uploadRepository: UploadRepository,
    private val settingsStore: com.carbonwise.connect.data.local.SettingsStore,
    private val smsIngestionPipeline: com.carbonwise.connect.sms.SmsIngestionPipeline,
    private val pendingActivityRepository: com.carbonwise.connect.data.queue.PendingActivityRepository
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SyncWorker"
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
                    15, TimeUnit.MINUTES
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
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

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
        try {
            val syncSessionId = UUID.randomUUID().toString()

            val lastSuccessfulUpload = settingsStore.lastSuccessfulUploadTimestamp.first()

            val smsResult = smsIngestionPipeline.runPipeline(lastSuccessfulUpload)

            val pendingQueueCount = pendingActivityRepository.countPending()

            var uploadSuccess = 0
            var uploadFailed = 0

            if (pendingQueueCount > 0) {
                try {
                    val syncResult = uploadRepository.syncBatch(syncSessionId)
                    when (syncResult) {
                        is ApiResult.Success -> {
                            uploadSuccess = syncResult.data?.successCount ?: 0
                            uploadFailed = syncResult.data?.failedCount ?: 0
                        }
                        is ApiResult.Error -> {
                            uploadFailed = pendingQueueCount
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "syncBatch failed", e)
                    throw e
                }
            }

            val remainingQueueCount = pendingActivityRepository.countPending()

            Log.i(TAG, "Sync complete: SMS(found=${smsResult.found}, relevant=${smsResult.relevant}, saved=${smsResult.newActivitiesSaved}), Upload(success=$uploadSuccess, failed=$uploadFailed), Queue(remaining=$remainingQueueCount)")

            if (uploadFailed > 0) {
                return Result.retry()
            } else {
                return Result.success()
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Sync cycle failed", exception)
            return Result.retry()
        }
    }
}
