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
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            for (smsMessage in messages) {
                if (smsMessage != null) {
                    scope.launch {
                        try {
                            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                            val activeNetwork = cm.activeNetwork
                            val capabilities = cm.getNetworkCapabilities(activeNetwork)
                            val isConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                            
                            if (isConnected) {
                                SyncWorker.syncNow(context)
                            }
                        } catch (e: Exception) {
                            Log.e("SMSPipeline", "Failed to trigger sync after SMS", e)
                        }
                    }
                }
            }
        }
    }
}
