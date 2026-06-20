package com.carbonwise.connect.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.carbonwise.connect.service.SyncWorker

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var pipeline: SmsIngestionPipeline

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onReceive(context: Context, intent: Intent) {
        val tag = "SMSPipeline"
        Log.d(tag, "onReceive() entered")
        Log.d(tag, "intent.action: ${intent.action}")

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            for (smsMessage in messages) {
                if (smsMessage != null) {
                    val sender = smsMessage.displayOriginatingAddress ?: ""
                    val body = smsMessage.displayMessageBody ?: ""
                    val timestamp = smsMessage.timestampMillis
                    
                    Log.d(tag, "Stage 1: SMS Broadcast received. sender=$sender, body=$body, timestamp=$timestamp")

                    // Forward to pipeline by creating RawSms
                    val rawSms = RawSms(
                        sender = sender,
                        body = body,
                        receivedTimestamp = timestamp,
                        threadId = 0L, // ThreadID is not available in real-time broadcast
                        messageId = 0L // MessageID is not generated until saved to provider
                    )
                    Log.d(tag, "Stage 2: RawSms object created.")

                    scope.launch {
                        try {
                            // Forward directly to pipeline
                            pipeline.processRealtimeSms(rawSms)
                            
                            // Stage 10: Trigger immediate SyncWorker
                            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                            val activeNetwork = cm.activeNetwork
                            val capabilities = cm.getNetworkCapabilities(activeNetwork)
                            val isConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                            
                            if (isConnected) {
                                Log.d(tag, "Stage 10: Trigger immediate SyncWorker (only if network available).")
                                SyncWorker.syncNow(context)
                            } else {
                                Log.d(tag, "Stage 10 check: Network unavailable, skipped SyncWorker.")
                            }
                        } catch (e: Exception) {
                            Log.e(tag, "Pipeline execution failed", e)
                        }
                    }
                }
            }
        }
    }
}
