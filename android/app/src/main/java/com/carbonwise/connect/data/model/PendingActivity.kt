package com.carbonwise.connect.data.model

data class PendingActivity(
    val id: String,
    val sender: String,
    val messageBody: String,
    val receivedTimestamp: Long,
    val normalizedMerchant: String,
    val category: String? = null,
    val source: String = "SMS",
    val syncStatus: String = "PENDING",
    val rawHash: String,
    val ingestionVersion: Int = 1,
    val retryCount: Int = 0
)

fun PendingActivity.toEntity() = com.carbonwise.connect.data.queue.PendingActivityEntity(
    id = id,
    sender = sender,
    messageBody = messageBody,
    receivedTimestamp = receivedTimestamp,
    normalizedMerchant = normalizedMerchant,
    category = category,
    source = source,
    syncStatus = syncStatus,
    rawHash = rawHash,
    ingestionVersion = ingestionVersion,
    retryCount = retryCount
)

fun com.carbonwise.connect.data.queue.PendingActivityEntity.toDomain() = PendingActivity(
    id = id,
    sender = sender,
    messageBody = messageBody,
    receivedTimestamp = receivedTimestamp,
    normalizedMerchant = normalizedMerchant,
    category = category,
    source = source,
    syncStatus = syncStatus,
    rawHash = rawHash,
    ingestionVersion = ingestionVersion,
    retryCount = retryCount
)
