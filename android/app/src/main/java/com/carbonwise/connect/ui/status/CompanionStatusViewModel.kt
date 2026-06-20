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
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkFlow("carbonwise_periodic_sync")
                .collect { workInfos ->
                    val status = if (workInfos.isNotEmpty()) {
                        val info = workInfos.first()
                        when (info.state) {
                            WorkInfo.State.ENQUEUED -> "Waiting for Network"
                            WorkInfo.State.RUNNING -> {
                                if (info.runAttemptCount > 0) "Retrying" else "Syncing"
                            }
                            WorkInfo.State.FAILED -> "Failed"
                            else -> "Idle"
                        }
                    } else {
                        "Idle"
                    }
                    _uiState.update { it.copy(syncStatus = status, isSyncing = status == "Syncing" || status == "Retrying") }
                }
        }
    }

    fun triggerLocalSync() {
        _uiState.update { it.copy(syncResult = null) }
        viewModelScope.launch {
            try {
                // Enqueue WorkManager sync instead of local
                com.carbonwise.connect.service.SyncWorker.schedule(context)
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
