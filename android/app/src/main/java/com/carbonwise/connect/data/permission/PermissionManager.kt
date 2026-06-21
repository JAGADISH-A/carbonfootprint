package com.carbonwise.connect.data.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.carbonwise.connect.data.model.PermissionState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _permissionState = MutableStateFlow(PermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val smsGranted = checkSmsPermission()
        val notificationGranted = checkNotificationListener()
        val batteryIgnored = checkBatteryOptimization()
        val syncReady = checkBackgroundSync()

        _permissionState.update {
            val newState = it.copy(
                smsGranted = smsGranted,
                notificationGranted = notificationGranted,
                batteryOptimizationIgnored = batteryIgnored,
                backgroundSyncReady = syncReady
            )
            newState
        }
    }

    private fun checkSmsPermission(): Boolean {
        val readSms = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        val receiveSms = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED

        return readSms && receiveSms
    }

    private fun checkNotificationListener(): Boolean {
        val packageName = context.packageName
        val component = android.content.ComponentName(context, com.carbonwise.connect.notification.NotificationCollector::class.java)
        
        // API B
        val apiB = NotificationManagerCompat.getEnabledListenerPackages(context)

        val result = apiB.contains(packageName)
        return result
    }

    private fun checkBatteryOptimization(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    private fun checkBackgroundSync(): Boolean {
        // Stubbed to true for now, will integrate with WorkManager constraints in future phases
        return true
    }
}
