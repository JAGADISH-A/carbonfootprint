package com.carbonwise.connect.ui.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carbonwise.connect.data.local.SettingsStore
import com.carbonwise.connect.data.remote.ApiClient
import com.carbonwise.connect.data.repository.ApiResult
import com.carbonwise.connect.data.repository.ConnectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class WelcomeUiState(
    val isLoading: Boolean = false,
    val backendReachable: Boolean? = null,
    val error: String? = null,
    val navigateToPermissions: Boolean = false
)

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val repository: ConnectionRepository,
    private val settingsStore: SettingsStore,
    private val apiClient: ApiClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    init {
        checkBackend()
    }

    private fun checkBackend() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = repository.healthCheck()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        backendReachable = true
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        backendReachable = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun connectToCarbonWise() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val deviceId = UUID.randomUUID().toString()
            settingsStore.setDeviceId(deviceId)

            // Generate a demo auth token for now
            // In production, this would come from OAuth or account linking
            val authToken = "cw_${deviceId}_${System.currentTimeMillis()}"
            settingsStore.setAuthToken(authToken)

            when (val result = repository.connect(authToken)) {
                is ApiResult.Success -> {
                    if (result.data.success) {
                        settingsStore.setOnboardingComplete(true)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            navigateToPermissions = true
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.data.error ?: "Connection failed"
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun retryBackendCheck() {
        _uiState.value = WelcomeUiState()
        checkBackend()
    }
}
