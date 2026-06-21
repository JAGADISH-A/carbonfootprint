package com.carbonwise.connect.notification

import com.carbonwise.connect.data.model.ActivitySource
import com.carbonwise.connect.data.model.PendingActivity
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationNormalizer @Inject constructor(
    private val filter: NotificationFilter
) {
    fun normalize(notification: RawNotification): PendingActivity {
        val merchant = filter.getMerchant(notification)
        
        val rawInput = "${notification.packageName}|${notification.title.orEmpty()}|${notification.text.orEmpty()}|${notification.timestamp}"
        val rawHash = hashString(rawInput)

        val messageBody = listOfNotNull(notification.title, notification.text).joinToString(" - ")

        return PendingActivity(
            id = rawHash,
            sender = notification.packageName,
            messageBody = messageBody,
            receivedTimestamp = notification.timestamp,
            normalizedMerchant = merchant,
            category = null,
            source = ActivitySource.NOTIFICATION,
            syncStatus = "PENDING",
            rawHash = rawHash,
            ingestionVersion = 1
        )
    }

    private fun hashString(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
