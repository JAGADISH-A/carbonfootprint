package com.carbonwise.connect.ingestion.model

/**
 * Camera/image event produced by CameraParser (future).
 */
data class CameraEvent(
    override val id: String = java.util.UUID.randomUUID().toString(),
    override val source: DataSource = DataSource.CAMERA,
    override val timestamp: Long,
    override val rawData: RawPayload,
    val imagePath: String
) : IngestionEvent
