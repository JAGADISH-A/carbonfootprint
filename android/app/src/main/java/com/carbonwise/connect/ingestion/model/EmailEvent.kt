package com.carbonwise.connect.ingestion.model

/**
 * Email-specific event produced by EmailParser (future).
 */
data class EmailEvent(
    override val id: String = java.util.UUID.randomUUID().toString(),
    override val source: DataSource = DataSource.EMAIL,
    override val timestamp: Long,
    override val rawData: RawPayload,
    val sender: String,
    val subject: String
) : IngestionEvent
