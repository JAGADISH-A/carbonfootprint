package com.carbonwise.connect.ingestion.notification

import android.service.notification.StatusBarNotification
import com.carbonwise.connect.ingestion.model.NotificationEvent
import com.carbonwise.connect.ingestion.model.RawPayload
import com.carbonwise.connect.ingestion.pipeline.DataParser
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Converts Android StatusBarNotification into a clean NotificationEvent.
 * Single responsibility: extract and normalize notification data.
 */
@Singleton
class NotificationParser @Inject constructor() : DataParser<StatusBarNotification, NotificationEvent> {

    override fun parse(input: StatusBarNotification): NotificationEvent? {
        val notification = input.notification ?: return null
        val extras = notification.extras ?: return null

        val title = extras.getCharSequence(EXTRA_TITLE)?.toString() ?: return null
        val text = extras.getCharSequence(EXTRA_TEXT)?.toString() ?: return null

        // Skip group summary notifications (they contain no real content)
        if (input.isGroup) {
            val groupKey = input.groupKey ?: ""
            if (groupKey.endsWith(":group_summary")) {
                return null
            }
        }

        val extrasMap = mutableMapOf<String, String>()
        extras.keySet().forEach { key ->
            extras.getCharSequence(key)?.toString()?.let { value ->
                extrasMap[key] = value
            }
        }

        return NotificationEvent(
            timestamp = input.postTime,
            rawData = RawPayload(
                packageName = input.packageName,
                title = title,
                body = text,
                extras = extrasMap
            ),
            packageName = input.packageName,
            priority = notification.priority,
            isGroupSummary = input.isGroup,
            category = notification.category
        )
    }

    companion object {
        private const val EXTRA_TITLE = "android.title"
        private const val EXTRA_TEXT = "android.text"
        private const val EXTRA_BIG_TEXT = "android.bigText"
        private const val EXTRA_SUB_TEXT = "android.subText"
        private const val EXTRA_INFO_TEXT = "android.infoText"
        private const val EXTRA_SUMMARY_TEXT = "android.summaryText"
    }
}
