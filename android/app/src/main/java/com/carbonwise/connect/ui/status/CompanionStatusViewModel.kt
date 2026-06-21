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

        viewModelScope.launch {
            healthState.collect { state ->
                val displayTime = formatLastSyncTime(state.lastSyncTime)
                _uiState.update { it.copy(lastSync = displayTime) }
            }
        }
    }

    fun refreshPermissions() {
        companionHealthManager.refresh()
        val time = healthState.value.lastSyncTime
        val displayTime = formatLastSyncTime(time)
        _uiState.update { it.copy(lastSync = displayTime) }
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
                WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow("carbonwise_manual_sync"),
                healthState
            ) { periodicInfos, manualInfos, hState ->
                val isManualRunning = manualInfos.any { it.state == WorkInfo.State.RUNNING }
                val isPeriodicRunning = periodicInfos.any { it.state == WorkInfo.State.RUNNING }
                val isManualEnqueued = manualInfos.any { it.state == WorkInfo.State.ENQUEUED }

                val status = if (isManualRunning || isPeriodicRunning) {
                    val info = manualInfos.firstOrNull { it.state == WorkInfo.State.RUNNING } 
                        ?: periodicInfos.first { it.state == WorkInfo.State.RUNNING }
                    if (info.runAttemptCount > 0) "Retrying" else "Syncing"
                } else if (isManualEnqueued) {
                    "Waiting for Network"
                } else {
                    "Connected"
                }

                val displayStatus = if (status == "Syncing" && hState.lastSyncTime == 0L) {
                    "Scanning recent activity messages..."
                } else {
                    status
                }

                Pair(status, displayStatus)
            }.collect { (status, displayStatus) ->
                _uiState.update { it.copy(syncStatus = displayStatus, isSyncing = status == "Syncing" || status == "Retrying") }
            }
        }
    }

    private fun formatLastSyncTime(time: Long): String {
        if (time == 0L) return "Ready for first sync"
        val currentTime = System.currentTimeMillis()
        val diff = currentTime - time
        if (diff < 60_000) {
            return "Last synced just now"
        }
        if (diff < 3_600_000) {
            val minutes = (diff / 60_000).toInt()
            val suffix = if (minutes == 1) "minute" else "minutes"
            return "Last synced $minutes $suffix ago"
        }
        
        val timeFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        val formattedTime = timeFormat.format(java.util.Date(time))
        
        val calToday = java.util.Calendar.getInstance().apply { timeInMillis = currentTime }
        val calTime = java.util.Calendar.getInstance().apply { timeInMillis = time }
        
        val isSameDay = calToday.get(java.util.Calendar.YEAR) == calTime.get(java.util.Calendar.YEAR) &&
                        calToday.get(java.util.Calendar.DAY_OF_YEAR) == calTime.get(java.util.Calendar.DAY_OF_YEAR)
                        
        if (isSameDay) {
            return "Today • $formattedTime"
        }
        
        calToday.add(java.util.Calendar.DAY_OF_YEAR, -1)
        val isYesterday = calToday.get(java.util.Calendar.YEAR) == calTime.get(java.util.Calendar.YEAR) &&
                          calToday.get(java.util.Calendar.DAY_OF_YEAR) == calTime.get(java.util.Calendar.DAY_OF_YEAR)
                          
        if (isYesterday) {
            return "Yesterday • $formattedTime"
        }
        
        val fullFormat = java.text.SimpleDateFormat("MMM d, yyyy • h:mm a", java.util.Locale.getDefault())
        return fullFormat.format(java.util.Date(time))
    }

    fun triggerLocalSync() {
        _uiState.update { it.copy(syncResult = null) }
        viewModelScope.launch {
            try {
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
