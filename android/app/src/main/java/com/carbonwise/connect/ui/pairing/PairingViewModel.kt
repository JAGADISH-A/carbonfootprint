package com.carbonwise.connect.ui.pairing

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

data class PairingUiState(
    val code: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class PairingViewModel @Inject constructor(
    private val authRepository: AuthenticationRepository,
    private val settingsStore: SettingsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(PairingUiState())
    val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()

    fun updateCode(newCode: String) {
        // Remove dashes and any non-alphanumeric chars
        val cleaned = newCode.replace(Regex("[^A-Za-z0-9]"), "").uppercase()
        // Take at most 8 chars
        val truncated = cleaned.take(8)
        
        // Add dash in middle
        val formatted = if (truncated.length > 4) {
            "${truncated.substring(0, 4)}-${truncated.substring(4)}"
        } else {
            truncated
        }

        _uiState.update {
            it.copy(code = formatted, error = null)
        }
    }

    fun pairDevice() {
        val currentCode = _uiState.value.code
        if (currentCode.length != 9) return // 8 chars + 1 dash

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val result = authRepository.pairDevice(
                pairingCode = currentCode,
                deviceName = android.os.Build.MODEL ?: "Android Device",
                manufacturer = android.os.Build.MANUFACTURER ?: "Unknown",
                model = android.os.Build.MODEL ?: "Unknown",
                androidVersion = android.os.Build.VERSION.RELEASE ?: "Unknown",
                appVersion = "1.0.0"
            )
            if (result.isSuccess) {
                settingsStore.setConnected(true)
                settingsStore.setOnboardingComplete(true)
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } else {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = result.exceptionOrNull()?.message ?: "Failed to pair device. Please try again."
                    ) 
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
