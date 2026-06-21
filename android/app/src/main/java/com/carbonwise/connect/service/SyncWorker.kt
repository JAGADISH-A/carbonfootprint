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

            val notifFound = 0
            val notifRelevant = 0

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
                            android.util.Log.d("MOBILE_SYNC", "Uploading: $uploadSuccess")
                        }
                        is ApiResult.Error -> {
                            uploadFailed = pendingQueueCount
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SYNC_DEBUG", "syncBatch failed", e)
                    throw e
                }
            } else {
                android.util.Log.d("MOBILE_SYNC", "Uploading: 0")
            }

            val remainingQueueCount = pendingActivityRepository.countPending()

            val logMessage = """
                ========== Sync Cycle ==========
                SMS Scan
                Found: ${smsResult.found}
                Relevant: ${smsResult.relevant}
                Duplicates: ${smsResult.duplicates}
                Notification Scan
                Found: $notifFound
                Relevant: $notifRelevant
                Pending Queue:
                $pendingQueueCount activities
                Uploading...
                Backend
                Success: $uploadSuccess
                Failed: $uploadFailed
                Room Queue Remaining:
                $remainingQueueCount
                ========== Sync Complete ==========
            """.trimIndent()
            
            android.util.Log.i("SyncWorker", "\n$logMessage")

            if (uploadFailed > 0) {
                return Result.retry()
            } else {
                return Result.success()
            }
        } catch (exception: Exception) {
            android.util.Log.e("SYNC_DEBUG", "Stage 1 failed", exception)
            return Result.retry()
        }
    }
}
