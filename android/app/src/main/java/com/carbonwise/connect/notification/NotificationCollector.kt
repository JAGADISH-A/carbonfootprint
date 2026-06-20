package com.carbonwise.connect.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationCollector : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationCollector"
    }

    @Inject
    lateinit var pipeline: NotificationIngestionPipeline

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onCreate() {
        super.onCreate()
        Log.d("PermissionDebug", "NotificationCollector created")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("PermissionDebug", "NotificationCollector onListenerConnected()")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d("PermissionDebug", "NotificationCollector onListenerDisconnected()")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        try {
            Log.d("NotificationPipeline", "Stage 1: Enter onNotificationPosted()")
            super.onNotificationPosted(sbn)
            sbn?.let {
                val notification = it.notification ?: return
                val extras = notification.extras
                
                // Extract application name using PackageManager
                val pm = packageManager
                val appName = try {
                    val info = pm.getApplicationInfo(it.packageName, 0)
                    pm.getApplicationLabel(info).toString()
                } catch (e: Exception) {
                    null
                }

                val rawNotification = RawNotification(
                    appName = appName,
                    packageName = it.packageName,
                    title = extras.getString(Notification.EXTRA_TITLE),
                    text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
                    subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
                    timestamp = it.postTime,
                    notificationId = it.id,
                    isOngoing = notification.flags and Notification.FLAG_ONGOING_EVENT != 0
                )
                Log.d("NotificationPipeline", "Stage 2: RawNotification created")

                scope.launch {
                    pipeline.processNotification(rawNotification)
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationPipeline", "Stage 1/2 failed", e)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        Log.d("PermissionDebug", "NotificationCollector onNotificationRemoved()")
        // Ignored for now as per requirements
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
