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

        android.util.Log.d("PermissionCheck", "PermissionManager state before update: ${_permissionState.value}")

        _permissionState.update {
            val newState = it.copy(
                smsGranted = smsGranted,
                notificationGranted = notificationGranted,
                batteryOptimizationIgnored = batteryIgnored,
                backgroundSyncReady = syncReady
            )
            android.util.Log.d("PermissionCheck", "PermissionManager state updated: $newState")
            newState
        }
    }

    private fun checkSmsPermission(): Boolean {
        // Step 1 - Package info
        android.util.Log.d("PermissionDebug", "context.packageName: ${context.packageName}")
        android.util.Log.d("PermissionDebug", "BuildConfig.APPLICATION_ID: ${com.carbonwise.connect.BuildConfig.APPLICATION_ID}")
        android.util.Log.d("PermissionDebug", "Process.myUid(): ${android.os.Process.myUid()}")
        android.util.Log.d("PermissionDebug", "UserHandle.myUserId(): ${android.os.Process.myUserHandle().hashCode()}") // fallback for myUserId if api is restricted
        android.util.Log.d("PermissionDebug", "Context hashCode: ${context.hashCode()}")

        // Step 5 - SMS Permission & Manifest check
        val readSms = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        val receiveSms = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        val postNotif = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        
        android.util.Log.d("PermissionDebug", "READ_SMS = $readSms")
        android.util.Log.d("PermissionDebug", "RECEIVE_SMS = $receiveSms")
        android.util.Log.d("PermissionDebug", "POST_NOTIFICATIONS = $postNotif")

        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            val requestedPermissions = packageInfo.requestedPermissions?.joinToString() ?: "None"
            android.util.Log.d("PermissionDebug", "Manifest permissions installed: $requestedPermissions")
        } catch (e: Exception) {
            android.util.Log.d("PermissionDebug", "Failed to get package info: ${e.message}")
        }

        return receiveSms // Currently using receiveSms based on previous fix
    }

    private fun checkNotificationListener(): Boolean {
        val packageName = context.packageName
        val component = android.content.ComponentName(context, com.carbonwise.connect.notification.NotificationCollector::class.java)
        
        // Step 2 - Verify Component
        android.util.Log.d("PermissionDebug", "flattenToString: ${component.flattenToString()}")
        android.util.Log.d("PermissionDebug", "className: ${component.className}")
        android.util.Log.d("PermissionDebug", "packageName: ${component.packageName}")

        // Step 4 - Compare Android APIs
        // API A
        val apiA = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        // API B
        val apiB = NotificationManagerCompat.getEnabledListenerPackages(context)
        // API C
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val apiC = nm.isNotificationListenerAccessGranted(component)

        android.util.Log.d("PermissionDebug", "API A (Settings.Secure): $apiA")
        android.util.Log.d("PermissionDebug", "API B (NotificationManagerCompat): $apiB")
        android.util.Log.d("PermissionDebug", "API C (isNotificationListenerAccessGranted): $apiC")

        val result = apiB.contains(packageName)
        android.util.Log.d("PermissionDebug", "Current calculated result (API B contains package): $result")

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
