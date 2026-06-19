package com.carbonwise.connect.ingestion.model

import java.util.UUID

/**
 * Base event type produced by all data sources (Notification, SMS, Email, Camera).
 * Each source provides its own subtype with source-specific fields.
 */
interface IngestionEvent {
    val id: String
    val source: DataSource
    val timestamp: Long
    val rawData: RawPayload
}

/**
 * Identifies which data source produced an event.
 */
enum class DataSource {
    NOTIFICATION,
    SMS,
    EMAIL,
    CAMERA
}

/**
 * Carries the original parsed data before classification.
 * Typed generically so each source can define its own payload structure.
 */
data class RawPayload(
    val packageName: String,
    val title: String,
    val body: String,
    val extras: Map<String, String> = emptyMap()
)

/**
 * Result of classification: which category this event belongs to
 * and how confident the classifier is.
 */
data class ClassificationResult(
    val category: EventCategory,
    val confidence: Float,
    val labels: List<String> = emptyList()
)

/**
 * Categories of carbon-related events that the system can detect.
 */
enum class EventCategory {
    TRANSPORT_RIDE,
    TRANSPORT_DELIVERY,
    FOOD_DELIVERY,
    FOOD_GROCERY,
    ENERGY_BILL,
    SHOPPING_ONLINE,
    SHOPPING_IN_STORE,
    SUBSCRIPTION,
    TRAVEL_BOOKING,
    UNKNOWN
}

/**
 * Outcome of an upload attempt.
 */
sealed class UploadResult {
    data class Success(val syncedCount: Int) : UploadResult()
    data class PartialSuccess(val syncedCount: Int, val failedIds: List<String>) : UploadResult()
    data class Failure(val error: String, val retryable: Boolean = true) : UploadResult()
}
