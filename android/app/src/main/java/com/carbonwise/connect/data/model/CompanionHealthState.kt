package com.carbonwise.connect.data.model

enum class HealthStatus {
    HEALTHY,
    WARNING,
    CRITICAL
}

data class HealthComponent(
    val id: String,
    val title: String,
    val description: String,
    val status: HealthStatus,
    val actionLabel: String? = null,
    val actionId: String? = null
)

data class CompanionHealthState(
    val overallHealth: HealthStatus = HealthStatus.CRITICAL,
    val warnings: List<String> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val components: List<HealthComponent> = emptyList(),
    val lastSyncTime: Long = 0L
)
