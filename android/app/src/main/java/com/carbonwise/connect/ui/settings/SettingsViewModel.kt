package com.carbonwise.connect.ui.settings

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val notificationSyncEnabled: Boolean = true,
    val smsSyncEnabled: Boolean = false,
    val isSyncing: Boolean = false,
    val isDisconnecting: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ConnectionRepository,
    private val settingsStore: SettingsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val notificationSync = settingsStore.notificationSyncEnabled.first()
            val smsSync = settingsStore.smsSyncEnabled.first()
            _uiState.value = _uiState.value.copy(
                notificationSyncEnabled = notificationSync,
                smsSyncEnabled = smsSync
            )
        }
    }

    fun toggleNotificationSync(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setNotificationSync(enabled)
            _uiState.value = _uiState.value.copy(notificationSyncEnabled = enabled)
        }
    }

    fun toggleSmsSync(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setSmsSync(enabled)
            _uiState.value = _uiState.value.copy(smsSyncEnabled = enabled)
        }
    }

    fun manualSync() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, message = null)
            when (val result = repository.syncPendingData()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        message = "Synced ${result.data} items"
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        message = "Sync failed: ${result.message}"
                    )
                }
            }
        }
    }

    fun openPrivacyPolicy(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://carbonwise.app/privacy"))
        context.startActivity(intent)
    }

    fun disconnect() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDisconnecting = true)
            repository.disconnect()
            SyncWorker.cancel(context)
            _uiState.value = _uiState.value.copy(
                isDisconnecting = false,
                message = "Device disconnected"
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            settingsStore.clearAll()
            SyncWorker.cancel(context)
            _uiState.value = _uiState.value.copy(message = "Signed out")
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
