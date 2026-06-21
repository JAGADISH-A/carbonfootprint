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
    }

    suspend fun collectSms(sinceTimestamp: Long = 0L): List<RawSms> = withContext(Dispatchers.IO) {
        val result = mutableListOf<RawSms>()
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("_id", "thread_id", "address", "body", "date")
        
        // We only want SMS received after sinceTimestamp
        val selection = if (sinceTimestamp > 0) "date > ?" else null
        val selectionArgs = if (sinceTimestamp > 0) arrayOf(sinceTimestamp.toString()) else null
        val sortOrder = "date DESC"

        // ── Stage 1: Enter SmsCollector.collect() ──
        Log.d(TAG, "═══════════════════════════════════════════════")
        Log.d(TAG, "Stage 1: SmsCollector.collectSms() ENTERED")
        Log.d(TAG, "Stage 1: lastSmsScanTimestamp (sinceTimestamp) = $sinceTimestamp")
        Log.d(TAG, "Stage 1: query URI = $uri")
        Log.d(TAG, "Stage 1: projection = ${projection.joinToString()}")
        Log.d(TAG, "Stage 1: selection = $selection")
        Log.d(TAG, "Stage 1: selectionArgs = ${selectionArgs?.joinToString()}")
        Log.d(TAG, "═══════════════════════════════════════════════")

        // ── Task 3: Instrument SmsCollector SQL query ──
        android.util.Log.e("SMS_SQL_TRACE", "URI: $uri")
        android.util.Log.e("SMS_SQL_TRACE", "Selection: $selection")
        android.util.Log.e("SMS_SQL_TRACE", "SelectionArgs: ${selectionArgs?.joinToString() ?: "null"}")
        android.util.Log.e("SMS_SQL_TRACE", "SortOrder: $sortOrder")

        // ── Task 4: Query the device SMS database ──
        try {
            val allUri = Uri.parse("content://sms/inbox")
            val dateProjection = arrayOf("date")
            var newestSmsTimestamp: Long = 0L
            var oldestSmsTimestamp: Long = 0L

            // Get newest
            context.contentResolver.query(allUri, dateProjection, null, null, "date DESC")?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val dateIdx = cursor.getColumnIndex("date")
                    if (dateIdx >= 0) {
                        newestSmsTimestamp = cursor.getLong(dateIdx)
                    }
                }
            }

            // Get oldest
            context.contentResolver.query(allUri, dateProjection, null, null, "date ASC")?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val dateIdx = cursor.getColumnIndex("date")
                    if (dateIdx >= 0) {
                        oldestSmsTimestamp = cursor.getLong(dateIdx)
                    }
                }
            }

            val currentTime = System.currentTimeMillis()
            val difference = currentTime - newestSmsTimestamp
            android.util.Log.e("SMS_DEVICE_DB", "Newest SMS timestamp: $newestSmsTimestamp")
            android.util.Log.e("SMS_DEVICE_DB", "Oldest SMS timestamp: $oldestSmsTimestamp")
            android.util.Log.e("SMS_DEVICE_DB", "Current System.currentTimeMillis(): $currentTime")
            android.util.Log.e("SMS_DEVICE_DB", "currentTime - newestSmsTimestamp: $difference ms")

            if (newestSmsTimestamp < sinceTimestamp) {
                android.util.Log.e("SMS_DEVICE_DB", "All SMS are older than lastSmsScanTimestamp.")
            }
        } catch (e: Exception) {
            android.util.Log.e("SMS_DEVICE_DB", "Error querying device SMS DB", e)
        }

        try {
            // ── Stage 2: Execute ContentResolver.query() ──
            val cursor = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )
            Log.d(TAG, "Stage 2: ContentResolver.query() returned")
            Log.d(TAG, "Stage 2: cursor == null? ${cursor == null}")
            Log.d(TAG, "Stage 2: cursor.count = ${cursor?.count ?: "N/A (cursor is null)"}")
            Log.d(TAG, "Stage 2: SecurityException? No (query succeeded)")

            cursor?.use { c ->
                val idIdx = c.getColumnIndex("_id")
                val threadIdIdx = c.getColumnIndex("thread_id")
                val addressIdx = c.getColumnIndex("address")
                val bodyIdx = c.getColumnIndex("body")
                val dateIdx = c.getColumnIndex("date")

                // ── Stage 3: Iterate cursor ──
                Log.d(TAG, "Stage 3: Beginning cursor iteration (count=${c.count})")
                var rowNum = 0

                while (c.moveToNext()) {
                    rowNum++
                    val id = if (idIdx >= 0) c.getLong(idIdx) else 0L
                    val threadId = if (threadIdIdx >= 0) c.getLong(threadIdIdx) else 0L
                    val address = if (addressIdx >= 0) c.getString(addressIdx) else ""
                    val body = if (bodyIdx >= 0) c.getString(bodyIdx) else ""
                    val date = if (dateIdx >= 0) c.getLong(dateIdx) else 0L

                    Log.d(TAG, "Stage 3: SMS[$rowNum] id=$id, address=$address, body=${body.take(80)}..., date=$date, type=INBOX")

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
                    } else {
                        Log.w(TAG, "Stage 3: SMS[$rowNum] SKIPPED (blank address or body)")
                    }
                }
                Log.d(TAG, "Stage 3: Cursor iteration complete. Rows iterated=$rowNum, Valid SMS added=${result.size}")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Stage 2: SecurityException! SMS permission likely not granted", e)
        } catch (e: Exception) {
            Log.e(TAG, "Stage 2/3: Unexpected exception during SMS collection", e)
            e.printStackTrace()
            // Ignore for now. Log in a real app.
        }

        return@withContext result
    }
}

