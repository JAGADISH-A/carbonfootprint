package com.carbonwise.connect.ingestion.queue

import com.carbonwise.connect.ingestion.model.ClassificationResult
import com.carbonwise.connect.ingestion.model.EventCategory
import com.carbonwise.connect.ingestion.model.QueuedEvent
import com.carbonwise.connect.ingestion.pipeline.QueueRepository
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the persistent queue for notification events awaiting upload.
 * Single responsibility: store, retrieve, and manage queued notification events.
 */
@Singleton
class NotificationQueueRepository @Inject constructor(
    private val dao: IngestionQueueDao,
    private val gson: Gson
) : QueueRepository<QueuedEvent> {

    override suspend fun enqueue(event: QueuedEvent) {
        val entity = event.toEntity()
        dao.insert(entity)
    }

    override suspend fun enqueueAll(events: List<QueuedEvent>) {
        val entities = events.map { it.toEntity() }
        dao.insertAll(entities)
    }

    override suspend fun getPending(): List<QueuedEvent> {
        return dao.getPending().map { it.toQueuedEvent() }
    }

    override suspend fun getPendingCount(): Int {
        return dao.getPendingCount()
    }

    override suspend fun markSynced(ids: List<String>) {
        dao.markSynced(ids)
    }

    override suspend fun markFailed(id: String, error: String) {
        dao.markFailed(id, error)
    }

    override suspend fun retry(id: String): QueuedEvent? {
        dao.resetToPending(id)
        return dao.getById(id)?.toQueuedEvent()
    }

    override suspend fun cleanup(maxAge: Long) {
        dao.cleanupSynced(maxAge)
    }

    override suspend fun clear() {
        dao.clearAll()
    }

    suspend fun getBatch(limit: Int): List<QueuedEvent> {
        return dao.getPendingBatch(limit).map { it.toQueuedEvent() }
    }

    suspend fun markUploading(ids: List<String>) {
        dao.markUploading(ids)
    }

    suspend fun purgeExhausted(maxAttempts: Int) {
        dao.purgeExhausted(maxAttempts)
    }

    private fun QueuedEvent.toEntity(): QueuedEventEntity {
        return QueuedEventEntity(
            id = id,
            source = source.name,
            category = classification.category.name,
            confidence = classification.confidence,
            packageName = packageName,
            title = title,
            body = body,
            extras = gson.toJson(classification.labels),
            timestamp = timestamp,
            createdAt = createdAt,
            attemptCount = attemptCount,
            lastError = lastError,
            status = QueuedEventEntity.STATUS_PENDING
        )
    }

    private fun QueuedEventEntity.toQueuedEvent(): QueuedEvent {
        return QueuedEvent(
            id = id,
            packageName = packageName,
            title = title,
            body = body,
            timestamp = timestamp,
            createdAt = createdAt,
            attemptCount = attemptCount,
            lastError = lastError,
            classification = ClassificationResult(
                category = try { EventCategory.valueOf(category) } catch (_: Exception) { EventCategory.UNKNOWN },
                confidence = confidence,
                labels = try { gson.fromJson(extras, List::class.java) as? List<String> ?: emptyList() } catch (_: Exception) { emptyList() }
            )
        )
    }
}
