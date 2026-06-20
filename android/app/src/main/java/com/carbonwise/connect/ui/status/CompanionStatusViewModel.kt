package com.carbonwise.connect.ui.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carbonwise.connect.data.local.SettingsStore
import com.carbonwise.connect.data.model.CompanionHealthState
import com.carbonwise.connect.data.model.HealthStatus
import com.carbonwise.connect.data.permission.CompanionHealthManager
import com.carbonwise.connect.data.queue.PendingActivityRepository
import com.carbonwise.connect.data.repository.AuthenticationRepository
import com.carbonwise.connect.sms.SmsIngestionPipeline
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.work.WorkManager
import androidx.work.WorkInfo

data class CompanionUiState(
    val connectionStatus: String = "Connected",
    val pairedAccount: String = "Loading...",
    val lastSync: String = "Never",
    val pendingUploads: Int = 0, // Total pending
    val smsQueueCount: Int = 0,
    val notificationQueueCount: Int = 0,
    val failedUploads: Int = 0,
    val syncStatus: String = "Idle",
    val isUnpairing: Boolean = false,
    val unpairError: String? = null,
    val isUnpaired: Boolean = false,
    val isSyncing: Boolean = false,
    val syncResult: String? = null
)



@HiltViewModel
class CompanionStatusViewModel @Inject constructor(
    private val authRepository: AuthenticationRepository,
    private val settingsStore: SettingsStore,
    private val companionHealthManager: CompanionHealthManager,
    private val pendingActivityRepository: PendingActivityRepository,
    private val smsIngestionPipeline: SmsIngestionPipeline,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompanionUiState())
    val uiState: StateFlow<CompanionUiState> = _uiState.asStateFlow()
    
    val healthState: StateFlow<CompanionHealthState> = companionHealthManager.healthState

    init {
        loadStatus()
        observePendingCount()
        observeLastSync()

        viewModelScope.launch {
            healthState.collect {
                android.util.Log.d("PermissionCheck", "ViewModel healthState collected: $it")
            }
        }
    }

    fun refreshPermissions() {
        android.util.Log.d("PermissionCheck", "Refresh started in ViewModel")
        companionHealthManager.refresh()
    }

    private fun loadStatus() {
        _uiState.update { 
            it.copy(
                pairedAccount = "Carbon User" // Placeholder until fetched from backend
            ) 
        }
    }

    private fun observePendingCount() {
        viewModelScope.launch {
            pendingActivityRepository.getPendingCount().collect { count ->
                _uiState.update { it.copy(pendingUploads = count) }
            }
        }
        viewModelScope.launch {
            pendingActivityRepository.getPendingCountBySource(com.carbonwise.connect.data.model.ActivitySource.SMS.name).collect { count ->
                _uiState.update { it.copy(smsQueueCount = count) }
            }
        }
        viewModelScope.launch {
            pendingActivityRepository.getPendingCountBySource(com.carbonwise.connect.data.model.ActivitySource.NOTIFICATION.name).collect { count ->
                _uiState.update { it.copy(notificationQueueCount = count) }
            }
        }
        viewModelScope.launch {
            pendingActivityRepository.getFailedCount().collect { count ->
                _uiState.update { it.copy(failedUploads = count) }
            }
        }
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow("carbonwise_periodic_sync"),
                WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow("carbonwise_manual_sync")
            ) { periodicInfos, manualInfos ->
                val isManualRunning = manualInfos.any { it.state == WorkInfo.State.RUNNING }
                val isPeriodicRunning = periodicInfos.any { it.state == WorkInfo.State.RUNNING }
                val isManualEnqueued = manualInfos.any { it.state == WorkInfo.State.ENQUEUED }

                if (isManualRunning || isPeriodicRunning) {
                    val info = manualInfos.firstOrNull { it.state == WorkInfo.State.RUNNING } 
                        ?: periodicInfos.first { it.state == WorkInfo.State.RUNNING }
                    if (info.runAttemptCount > 0) "Retrying" else "Syncing"
                } else if (isManualEnqueued) {
                    "Waiting for Network"
                } else {
                    "Connected"
                }
            }.collect { status ->
                _uiState.update { it.copy(syncStatus = status, isSyncing = status == "Syncing" || status == "Retrying") }
            }
        }
    }

    private fun observeLastSync() {
        viewModelScope.launch {
            settingsStore.lastSyncTime.collect { time ->
                if (time == 0L) {
                    _uiState.update { it.copy(lastSync = "Never") }
                } else {
                    val displayTime = if (System.currentTimeMillis() - time < 60_000) {
                        "Just now"
                    } else {
                        android.text.format.DateUtils.getRelativeTimeSpanString(
                            time,
                            System.currentTimeMillis(),
                            android.text.format.DateUtils.MINUTE_IN_MILLIS
                        ).toString()
                    }
                    _uiState.update { it.copy(lastSync = displayTime) }
                }
            }
        }
    }

    fun triggerLocalSync() {
        android.util.Log.d("ManualSync", "ViewModel.triggerLocalSync() called")
        _uiState.update { it.copy(syncResult = null) }
        viewModelScope.launch {
            try {
                // Enqueue WorkManager sync instead of local
                android.util.Log.d("ManualSync", "Enqueuing WorkManager request")
                com.carbonwise.connect.service.SyncWorker.syncNow(context)
                _uiState.update { 
                    it.copy(
                        syncResult = "Sync initiated."
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(syncResult = "Error starting sync: ${e.message}") 
                }
            }
        }
    }

    fun clearSyncResult() {
        _uiState.update { it.copy(syncResult = null) }
    }

    fun unpairDevice(force: Boolean = false) {
        _uiState.update { it.copy(isUnpairing = true, unpairError = null) }
        viewModelScope.launch {
            if (force) {
                performLocalUnpair()
            } else {
                val result = authRepository.logout()
                if (result.isSuccess) {
                    performLocalUnpair()
                } else {
                    _uiState.update { 
                        it.copy(
                            isUnpairing = false,
                            unpairError = "Failed to reach backend. You can force remove, but the device may still appear on the web dashboard."
                        ) 
                    }
                }
            }
        }
    }

    private suspend fun performLocalUnpair() {
        authRepository.clearTokens()
        settingsStore.setConnected(false)
        settingsStore.setOnboardingComplete(false)
        _uiState.update { it.copy(isUnpairing = false, isUnpaired = true) }
    }

    fun clearError() {
        _uiState.update { it.copy(unpairError = null) }
    }
}
