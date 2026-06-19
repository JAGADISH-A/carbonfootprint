package com.carbonwise.connect.ingestion.notification

import android.service.notification.StatusBarNotification
import com.carbonwise.connect.ingestion.model.NotificationEvent
import com.carbonwise.connect.ingestion.model.UploadResult
import com.carbonwise.connect.ingestion.queue.NotificationQueueRepository
import com.carbonwise.connect.ingestion.queue.QueuedEvent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates the full notification ingestion pipeline:
 * Parse -> Filter -> Classify -> Queue
 *
 * Does NOT handle uploads directly. That's the SyncWorker's job.
 * This keeps the pipeline purely about data transformation and storage.
 */
@Singleton
class NotificationIngestionPipeline @Inject constructor(
    private val parser: NotificationParser,
    private val filter: NotificationFilterEngine,
    private val classifier: NotificationClassifier,
    private val queue: NotificationQueueRepository
) {
    /**
     * Processes a raw StatusBarNotification through the pipeline.
     * Returns the queued event if it passed all stages, null otherwise.
     */
    suspend fun process(sbn: StatusBarNotification): QueuedEvent? {
        // Stage 1: Parse
        val parsed = parser.parse(sbn) ?: return null

        // Stage 2: Filter
        if (!filter.isRelevant(parsed)) return null

        // Stage 3: Classify
        val classification = classifier.classify(parsed)

        // Stage 4: Queue
        val queuedEvent = QueuedEvent(
            id = parsed.id,
            packageName = parsed.packageName,
            title = parsed.rawData.title,
            body = parsed.rawData.body,
            timestamp = parsed.timestamp,
            classification = classification
        )

        queue.enqueue(queuedEvent)
        return queuedEvent
    }

    /**
     * Processes multiple notifications in batch.
     */
    suspend fun processBatch(notifications: List<StatusBarNotification>): List<QueuedEvent> {
        return notifications.mapNotNull { process(it) }
    }

    /**
     * Returns the number of events waiting to be uploaded.
     */
    suspend fun getPendingCount(): Int = queue.getPendingCount()

    /**
     * Returns events pending upload (for SyncWorker to upload).
     */
    suspend fun getPendingEvents(): List<QueuedEvent> = queue.getPending()

    /**
     * Marks events as successfully synced.
     */
    suspend fun markSynced(ids: List<String>) = queue.markSynced(ids)

    /**
     * Marks an event as failed.
     */
    suspend fun markFailed(id: String, error: String) = queue.markFailed(id, error)
}
