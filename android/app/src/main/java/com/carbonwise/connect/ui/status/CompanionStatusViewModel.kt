package com.carbonwise.connect.ui.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carbonwise.connect.data.local.SettingsStore
import com.carbonwise.connect.data.repository.AuthenticationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CompanionUiState(
    val connectionStatus: String = "Connected",
    val pairedAccount: String = "Loading...",
    val lastSync: String = "Never",
    val pendingUploads: Int = 0,
    val smsPermission: Boolean = false,
    val notificationPermission: Boolean = false,
    val backgroundSyncEnabled: Boolean = true,
    val isUnpairing: Boolean = false,
    val unpairError: String? = null,
    val isUnpaired: Boolean = false
)

@HiltViewModel
class CompanionStatusViewModel @Inject constructor(
    private val authRepository: AuthenticationRepository,
    private val settingsStore: SettingsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompanionUiState())
    val uiState: StateFlow<CompanionUiState> = _uiState.asStateFlow()

    init {
        loadStatus()
    }

    private fun loadStatus() {
        // Stubbed data for future reactive sync flows
        // Here we could listen to WorkManager, PermissionManager, etc.
        _uiState.update { 
            it.copy(
                pairedAccount = "Carbon User", // Placeholder until fetched from backend
                smsPermission = false,
                notificationPermission = false
            ) 
        }
    }

    fun unpairDevice(force: Boolean = false) {
        _uiState.update { it.copy(isUnpairing = true, unpairError = null) }
        viewModelScope.launch {
            if (force) {
                // Skip backend, just clear local
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
