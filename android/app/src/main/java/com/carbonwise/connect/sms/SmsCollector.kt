package com.carbonwise.connect.sms

import android.content.Context
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsCollector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "SMSPipeline"
        const val INITIAL_SYNC_WINDOW_DAYS = 14
    }

    suspend fun collectSms(sinceTimestamp: Long = 0L): List<RawSms> = withContext(Dispatchers.IO) {
        val result = mutableListOf<RawSms>()
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("_id", "thread_id", "address", "body", "date")
        
        val now = System.currentTimeMillis()
        val lookbackMs = INITIAL_SYNC_WINDOW_DAYS * 24 * 60 * 60 * 1000L
        val cutoffTime = now - lookbackMs

        val selection: String?
        val selectionArgs: Array<String>?

        if (sinceTimestamp > 0L) {
            selection = "date > ?"
            selectionArgs = arrayOf(sinceTimestamp.toString())
        } else {
            selection = "date >= ?"
            selectionArgs = arrayOf(cutoffTime.toString())
        }
        val sortOrder = "date DESC"

        try {
            val cursor = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use { c ->
                val idIdx = c.getColumnIndex("_id")
                val threadIdIdx = c.getColumnIndex("thread_id")
                val addressIdx = c.getColumnIndex("address")
                val bodyIdx = c.getColumnIndex("body")
                val dateIdx = c.getColumnIndex("date")

                while (c.moveToNext()) {
                    val id = if (idIdx >= 0) c.getLong(idIdx) else 0L
                    val threadId = if (threadIdIdx >= 0) c.getLong(threadIdIdx) else 0L
                    val address = if (addressIdx >= 0) c.getString(addressIdx) else ""
                    val body = if (bodyIdx >= 0) c.getString(bodyIdx) else ""
                    val date = if (dateIdx >= 0) c.getLong(dateIdx) else 0L

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
            Log.d(TAG, "Collected ${result.size} SMS messages (since=$sinceTimestamp)")
        } catch (e: SecurityException) {
            Log.e(TAG, "SMS permission not granted", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error during SMS collection", e)
        }

        return@withContext result
    }

    fun getTotalInboxCount(): Int {
        val uri = Uri.parse("content://sms/inbox")
        return try {
            context.contentResolver.query(uri, arrayOf("COUNT(*)"), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }
}
