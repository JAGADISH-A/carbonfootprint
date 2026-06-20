package com.carbonwise.connect.ui.permissions

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import com.carbonwise.connect.data.model.PermissionState
import com.carbonwise.connect.data.permission.PermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionManager: PermissionManager
) : ViewModel() {

    val permissionState: StateFlow<PermissionState> = permissionManager.permissionState

    fun refreshPermissions() {
        permissionManager.refresh()
    }

    fun requestNotificationAccess() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun requestBatteryOptimization() {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
