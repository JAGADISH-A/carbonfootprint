package com.carbonwise.connect.data.permission

import com.carbonwise.connect.data.model.CompanionHealthState
import com.carbonwise.connect.data.model.HealthComponent
import com.carbonwise.connect.data.model.HealthStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompanionHealthManager @Inject constructor(
    private val permissionManager: PermissionManager
    // In the future:
    // private val authManager: AuthenticationManager
    // private val syncManager: SyncManager
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    fun refresh() {
        permissionManager.refresh()
    }

    // Aggregate health information from all managers
    // Future expansion: use combine(permissionState, authState, syncState) { perm, auth, sync -> ... }
    val healthState: StateFlow<CompanionHealthState> = permissionManager.permissionState.map { permState ->
        val components = mutableListOf<HealthComponent>()
        val warnings = mutableListOf<String>()
        val recommendations = mutableListOf<String>()

        // 1. SMS Permission
        val smsStatus = if (permState.smsGranted) HealthStatus.HEALTHY else HealthStatus.CRITICAL
        components.add(
            HealthComponent(
                id = "sms",
                title = "SMS Access",
                description = if (permState.smsGranted) "Granted" else "Missing SMS permission required for data collection.",
                status = smsStatus,
                actionLabel = if (!permState.smsGranted) "Grant Access" else null,
                actionId = "manage_permissions"
            )
        )
        if (!permState.smsGranted) {
            warnings.add("SMS permission is required.")
            recommendations.add("Please grant SMS permission to allow the Companion service to collect data.")
        }

        // 2. Notification Access
        val notifStatus = if (permState.notificationGranted) HealthStatus.HEALTHY else HealthStatus.WARNING
        components.add(
            HealthComponent(
                id = "notification",
                title = "Notification Access",
                description = if (permState.notificationGranted) "Granted" else "Missing Notification listener access.",
                status = notifStatus,
                actionLabel = if (!permState.notificationGranted) "Grant Access" else null,
                actionId = "manage_permissions"
            )
        )
        if (!permState.notificationGranted) {
            warnings.add("Notification access is limited.")
            recommendations.add("Enable Notification Access to collect purchase notifications.")
        }

        // 3. Battery Optimization
        val batteryStatus = if (permState.batteryOptimizationIgnored) HealthStatus.HEALTHY else HealthStatus.WARNING
        components.add(
            HealthComponent(
                id = "battery",
                title = "Battery Optimization",
                description = if (permState.batteryOptimizationIgnored) "Optimized" else "Ignoring Battery Optimization is recommended for reliable background sync.",
                status = batteryStatus,
                actionLabel = if (!permState.batteryOptimizationIgnored) "Disable Optimization" else null,
                actionId = "manage_permissions"
            )
        )
        if (!permState.batteryOptimizationIgnored) {
            warnings.add("Battery optimization is active.")
            recommendations.add("Disable battery optimization to ensure the background sync works reliably.")
        }

        // 4. Background Sync
        val syncStatus = if (permState.backgroundSyncReady) HealthStatus.HEALTHY else HealthStatus.WARNING
        components.add(
            HealthComponent(
                id = "sync",
                title = "Background Sync",
                description = if (permState.backgroundSyncReady) "Ready" else "Limited",
                status = syncStatus
            )
        )

        // Calculate Overall Health
        val overallHealth = when {
            components.any { it.status == HealthStatus.CRITICAL } -> HealthStatus.CRITICAL
            components.any { it.status == HealthStatus.WARNING } -> HealthStatus.WARNING
            else -> HealthStatus.HEALTHY
        }

        CompanionHealthState(
            overallHealth = overallHealth,
            warnings = warnings,
            recommendations = recommendations,
            components = components
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CompanionHealthState(overallHealth = HealthStatus.WARNING)
    )
}
