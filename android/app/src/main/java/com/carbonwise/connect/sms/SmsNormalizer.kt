package com.carbonwise.connect.sms

import com.carbonwise.connect.data.model.PendingActivity
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsNormalizer @Inject constructor(
    private val filter: SmsFilter
) {
    fun normalize(sms: RawSms): PendingActivity {
        val merchant = filter.getMerchant(sms)
        
        // Deterministic hash: sender + message body + timestamp
        val rawInput = "${sms.sender}|${sms.body}|${sms.receivedTimestamp}"
        val rawHash = hashString(rawInput)

        return PendingActivity(
            id = rawHash,
            sender = sms.sender,
            messageBody = sms.body,
            receivedTimestamp = sms.receivedTimestamp,
            normalizedMerchant = merchant,
            category = null, // Store as null per requirements
            source = "SMS",
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
