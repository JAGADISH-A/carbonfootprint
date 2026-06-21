package com.carbonwise.connect.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.carbonwise.connect.data.local.PendingDataEntity
import com.carbonwise.connect.data.local.PendingDataDao
import com.carbonwise.connect.data.local.SettingsStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationListenerServiceImpl : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationListener"
    }

    @Inject
    lateinit var settingsStore: SettingsStore

    @Inject
    lateinit var pendingDataDao: PendingDataDao

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        scope.launch {
            try {
                val enabled = settingsStore.notificationSyncEnabled.first()
                val connected = settingsStore.isConnected.first()

                if (!enabled || !connected) return@launch

                val notification = sbn.notification ?: return@launch
                val extras = notification.extras ?: return@launch

                val title = extras.getCharSequence("android.title")?.toString() ?: return@launch
                val text = extras.getCharSequence("android.text")?.toString() ?: return@launch

                val entity = PendingDataEntity(
                    type = "notification",
                    sourcePackage = sbn.packageName,
                    title = title,
                    body = text,
                    timestamp = sbn.postTime
                )

                pendingDataDao.insert(entity)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // No-op
    }
}
