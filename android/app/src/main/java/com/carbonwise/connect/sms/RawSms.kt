package com.carbonwise.connect.sms

data class RawSms(
    val sender: String,
    val body: String,
    val receivedTimestamp: Long,
    val threadId: Long,
    val messageId: Long
)
