package com.carbonwise.connect.ingestion.pipeline

import com.carbonwise.connect.ingestion.model.ClassificationResult
import com.carbonwise.connect.ingestion.model.QueuedEvent
import com.carbonwise.connect.ingestion.model.UploadResult

/**
 * Stage 1: Converts raw platform data into a clean, typed model.
 * SMS parser converts SmsMessage, Email parser converts MimeMessage, etc.
 */
interface DataParser<RawInput, out ParsedOutput> {
    fun parse(input: RawInput): ParsedOutput?
}

/**
 * Stage 2: Decides whether parsed data is relevant for carbon tracking.
 * Returns true if the event should proceed to classification.
 */
interface DataFilter<in T> {
    fun isRelevant(data: T): Boolean
}

/**
 * Stage 3: Determines the event category and confidence score.
 * Used to route events to appropriate backend endpoints.
 */
interface DataClassifier<in T> {
    fun classify(data: T): ClassificationResult
}

/**
 * Stage 4: Uploads classified events to the backend.
 * Handles retries, batching, and network errors.
 */
interface UploadManager {
    suspend fun upload(events: List<QueuedEvent>): UploadResult
    suspend fun uploadSingle(event: QueuedEvent): UploadResult
}

/**
 * Stage 5: Manages the persistent queue of events awaiting upload.
 * Handles storage, ordering, retry logic, and cleanup.
 */
interface QueueRepository<T : QueuedEvent> {
    suspend fun enqueue(event: T)
    suspend fun enqueueAll(events: List<T>)
    suspend fun getPending(): List<T>
    suspend fun getPendingCount(): Int
    suspend fun markSynced(ids: List<String>)
    suspend fun markFailed(id: String, error: String)
    suspend fun retry(id: String): T?
    suspend fun cleanup(maxAge: Long)
    suspend fun clear()
}
