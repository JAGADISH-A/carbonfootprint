package com.carbonwise.connect.ingestion.model

/**
 * SMS-specific event produced by SmsParser (future).
 */
data class SmsEvent(
    override val id: String = java.util.UUID.randomUUID().toString(),
    override val source: DataSource = DataSource.SMS,
    override val timestamp: Long,
    override val rawData: RawPayload,
    val address: String,
    val body: String
) : IngestionEvent
