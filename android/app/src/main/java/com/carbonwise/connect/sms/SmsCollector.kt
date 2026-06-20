package com.carbonwise.connect.sms

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsCollector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun collectSms(sinceTimestamp: Long = 0L): List<RawSms> = withContext(Dispatchers.IO) {
        val result = mutableListOf<RawSms>()
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("_id", "thread_id", "address", "body", "date")
        
        // We only want SMS received after sinceTimestamp
        val selection = if (sinceTimestamp > 0) "date > ?" else null
        val selectionArgs = if (sinceTimestamp > 0) arrayOf(sinceTimestamp.toString()) else null
        val sortOrder = "date DESC"

        try {
            context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex("_id")
                val threadIdIdx = cursor.getColumnIndex("thread_id")
                val addressIdx = cursor.getColumnIndex("address")
                val bodyIdx = cursor.getColumnIndex("body")
                val dateIdx = cursor.getColumnIndex("date")

                while (cursor.moveToNext()) {
                    val id = if (idIdx >= 0) cursor.getLong(idIdx) else 0L
                    val threadId = if (threadIdIdx >= 0) cursor.getLong(threadIdIdx) else 0L
                    val address = if (addressIdx >= 0) cursor.getString(addressIdx) else ""
                    val body = if (bodyIdx >= 0) cursor.getString(bodyIdx) else ""
                    val date = if (dateIdx >= 0) cursor.getLong(dateIdx) else 0L

                    if (address.isNotBlank() && body.isNotBlank()) {
                        result.add(
                            RawSms(
                                sender = address,
                                body = body,
                                receivedTimestamp = date,
                                threadId = threadId,
                                messageId = id
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Ignore for now. Log in a real app.
        }

        return@withContext result
    }
}
