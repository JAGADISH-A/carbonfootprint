package com.carbonwise.connect.ui.status

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carbonwise.connect.data.local.SettingsStore
import com.carbonwise.connect.data.repository.ApiResult
import com.carbonwise.connect.data.repository.ConnectionRepository
import com.carbonwise.connect.service.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatusUiState(
    val isConnecting: Boolean = true,
    val accountConnected: Boolean = false,
    val backendReachable: Boolean = false,
    val notificationAccess: Boolean = false,
    val smsAccess: Boolean = false,
    val backgroundSync: Boolean = false,
    val lastSyncTime: Long = 0L,
    val pendingUploadCount: Int = 0,
    val syncStatus: String = "Idle",
    val accountName: String = "",
    val accountEmail: String = ""
)

@HiltViewModel
class StatusViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ConnectionRepository,
    private val settingsStore: SettingsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatusUiState())
    val uiState: StateFlow<StatusUiState> = _uiState.asStateFlow()

    init {
        refreshStatus()
        startPeriodicRefresh()
    }

    private fun startPeriodicRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                refreshStatus()
            }
        }
    }

    fun refreshStatus() {
        viewModelScope.launch {
            val isConnected = settingsStore.isConnected.first()
            val accountName = settingsStore.accountName.first()
            val accountEmail = settingsStore.accountEmail.first()
            val lastSync = settingsStore.lastSyncTime.first()
            val notificationSync = settingsStore.notificationSyncEnabled.first()
            val pendingCount = repository.getPendingCount()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationAccess = notificationManager.isNotificationListenerAccessGranted(
                ComponentName(context, "com.carbonwise.connect.service.NotificationListenerServiceImpl")
            )

            _uiState.value = StatusUiState(
                isConnecting = false,
                accountConnected = isConnected,
                backendReachable = _uiState.value.backendReachable,
                notificationAccess = notificationAccess,
                smsAccess = false,
                backgroundSync = notificationSync,
                lastSyncTime = lastSync,
                pendingUploadCount = pendingCount,
                syncStatus = if (pendingCount > 0) "Pending" else "Idle",
                accountName = accountName,
                accountEmail = accountEmail
            )

            when (val result = repository.healthCheck()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(backendReachable = true)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(backendReachable = false)
                }
            }
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(syncStatus = "Syncing...")
            when (val result = repository.syncPendingData()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        syncStatus = "Synced",
                        pendingUploadCount = 0
                    )
                    refreshStatus()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(syncStatus = "Error: ${result.message}")
                }
            }
        }
    }

    fun openWebDashboard(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://web.carbonwise.app"))
        context.startActivity(intent)
    }
}
