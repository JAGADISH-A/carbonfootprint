package com.carbonwise.connect.ui.permissions

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carbonwise.connect.data.local.SettingsStore
import com.carbonwise.connect.service.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PermissionStep(
    val id: String,
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val isOptional: Boolean = false
)

data class PermissionsUiState(
    val currentStep: Int = 0,
    val steps: List<PermissionStep> = emptyList(),
    val allComplete: Boolean = false
)

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsStore: SettingsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionsUiState())
    val uiState: StateFlow<PermissionsUiState> = _uiState.asStateFlow()

    init {
        checkPermissions()
    }

    private fun checkPermissions() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationAccess = notificationManager.isNotificationListenerAccessGranted(
            ComponentName(context, "com.carbonwise.connect.service.NotificationListenerServiceImpl")
        )

        val steps = listOf(
            PermissionStep(
                id = "notification",
                title = "Notification Listener",
                description = "Allow CarbonWise to read notifications for carbon data extraction",
                isGranted = notificationAccess
            ),
            PermissionStep(
                id = "sms",
                title = "SMS Access",
                description = "Optional: Read SMS for purchase receipt detection",
                isGranted = false,
                isOptional = true
            ),
            PermissionStep(
                id = "camera",
                title = "Camera",
                description = "Future: Scan receipts for carbon footprint data",
                isGranted = false,
                isOptional = true
            )
        )

        val firstIncomplete = steps.indexOfFirst { !it.isGranted && !it.isOptional }

        _uiState.value = PermissionsUiState(
            currentStep = if (firstIncomplete >= 0) firstIncomplete else steps.size - 1,
            steps = steps,
            allComplete = steps.filter { !it.isOptional }.all { it.isGranted }
        )
    }

    fun requestNotificationAccess() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun requestSmsPermission() {
        // Placeholder for future SMS permission request
        checkPermissions()
    }

    fun skipOptional() {
        val state = _uiState.value
        val nextStep = state.currentStep + 1
        if (nextStep < state.steps.size) {
            _uiState.value = state.copy(currentStep = nextStep)
        } else {
            _uiState.value = state.copy(allComplete = true)
        }
    }

    fun nextStep() {
        val state = _uiState.value
        val nextStep = state.currentStep + 1
        if (nextStep < state.steps.size) {
            _uiState.value = state.copy(currentStep = nextStep)
        } else {
            _uiState.value = state.copy(allComplete = true)
            SyncWorker.schedule(context)
        }
    }

    fun finish() {
        viewModelScope.launch {
            settingsStore.setOnboardingComplete(true)
        }
    }

    fun refreshPermissions() {
        checkPermissions()
    }
}
