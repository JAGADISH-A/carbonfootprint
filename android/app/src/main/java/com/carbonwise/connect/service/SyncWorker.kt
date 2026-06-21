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

    init {
        try {
            android.util.Log.d("SYNC_DEBUG", "Stage 1: Worker instantiated")
        } catch (e: Exception) {
            android.util.Log.e("SYNC_DEBUG", "Stage 1 failed", e)
        }
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
        android.util.Log.e("SYNC_TRACE", "1 entered doWork")
        try {
            android.util.Log.e("SYNC_TRACE", "2 before generating syncSessionId")
            val syncSessionId = try {
                val uuid = UUID.randomUUID().toString()
                android.util.Log.e("SYNC_TRACE", "3 syncSessionId generated: $uuid")
                uuid
            } catch (e: Exception) {
                android.util.Log.e("SYNC_TRACE", "EXCEPTION during UUID.randomUUID()", e)
                throw e
            }

            android.util.Log.e("SYNC_TRACE", "4 before reading lastSmsScanTimestamp from settingsStore")
            val lastSmsScan = try {
                val ts = settingsStore.lastSmsScanTimestamp.first()
                android.util.Log.e("SYNC_TRACE", "5 lastSmsScanTimestamp fetched: $ts")
                ts
            } catch (e: Exception) {
                android.util.Log.e("SYNC_TRACE", "EXCEPTION during lastSmsScanTimestamp.first()", e)
                throw e
            }

            android.util.Log.e("SYNC_TRACE", "6 before SmsIngestionPipeline.runPipeline")
            val smsResult = try {
                val result = smsIngestionPipeline.runPipeline(lastSmsScan)
                android.util.Log.e("SYNC_TRACE", "7 SmsIngestionPipeline.runPipeline finished. Found: ${result.found}, Relevant: ${result.relevant}")
                result
            } catch (e: Exception) {
                android.util.Log.e("SYNC_TRACE", "EXCEPTION during runPipeline()", e)
                throw e
            }



            val notifFound = 0
            val notifRelevant = 0

            android.util.Log.e("SYNC_TRACE", "10 before fetching pendingQueueCount")
            val pendingQueueCount = try {
                val count = pendingActivityRepository.getPendingCount().first()
                android.util.Log.e("SYNC_TRACE", "11 pendingQueueCount fetched: $count")
                count
            } catch (e: Exception) {
                android.util.Log.e("SYNC_TRACE", "EXCEPTION during getPendingCount().first()", e)
                throw e
            }

            var uploadSuccess = 0
            var uploadFailed = 0

            android.util.Log.e("SYNC_TRACE", "12 before checking pendingQueueCount > 0 ($pendingQueueCount)")
            if (pendingQueueCount > 0) {
                android.util.Log.e("SYNC_TRACE", "13 before uploadRepository.syncBatch()")
                try {
                    val syncResult = uploadRepository.syncBatch(syncSessionId)
                    android.util.Log.e("SYNC_TRACE", "14 after uploadRepository.syncBatch() returned: $syncResult")
                    when (syncResult) {
                        is ApiResult.Success -> {
                            android.util.Log.e("SYNC_TRACE", "15 syncResult is Success")
                            uploadSuccess = syncResult.data?.successCount ?: 0
                            uploadFailed = syncResult.data?.failedCount ?: 0
                        }
                        is ApiResult.Error -> {
                            android.util.Log.e("SYNC_TRACE", "16 syncResult is Error: ${syncResult.message}")
                            uploadFailed = pendingQueueCount
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SYNC_TRACE", "EXCEPTION during syncBatch()", e)
                    throw e
                }
            } else {
                android.util.Log.e("SYNC_TRACE", "17 pendingQueueCount is 0, skipping uploadRepository.syncBatch()")
            }

            android.util.Log.e("SYNC_TRACE", "18 before fetching remainingQueueCount")
            val remainingQueueCount = try {
                val count = pendingActivityRepository.getPendingCount().first()
                android.util.Log.e("SYNC_TRACE", "19 remainingQueueCount fetched: $count")
                count
            } catch (e: Exception) {
                android.util.Log.e("SYNC_TRACE", "EXCEPTION during remainingQueueCount.first()", e)
                throw e
            }

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

            android.util.Log.e("SYNC_TRACE", "20 before checking uploadFailed > 0 ($uploadFailed)")
            if (uploadFailed > 0) {
                android.util.Log.e("SYNC_TRACE", "21 returning Result.retry()")
                return Result.retry()
            } else {
                android.util.Log.e("SYNC_TRACE", "22 returning Result.success()")
                return Result.success()
            }
        } catch (exception: Exception) {
            android.util.Log.e("SYNC_TRACE", "23 caught exception in outer block of doWork()", exception)
            android.util.Log.e("SYNC_TRACE", "24 returning Result.retry()")
            return Result.retry()
        }
    }
}
