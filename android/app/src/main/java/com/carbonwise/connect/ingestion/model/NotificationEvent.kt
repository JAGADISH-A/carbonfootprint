package com.carbonwise.connect.ingestion.model

/**
 * Notification-specific event produced by NotificationParser.
 */
data class NotificationEvent(
    override val id: String = UUID.randomUUID().toString(),
    override val source: DataSource = DataSource.NOTIFICATION,
    override val timestamp: Long,
    override val rawData: RawPayload,
    val packageName: String,
    val priority: Int,
    val isGroupSummary: Boolean,
    val category: String?
) : IngestionEvent
