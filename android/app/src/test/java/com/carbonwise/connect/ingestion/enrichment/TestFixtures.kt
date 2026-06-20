package com.carbonwise.connect.ingestion.enrichment

import com.carbonwise.connect.ingestion.model.TransactionCandidate

object TestFixtures {
    fun createCandidate(
        merchant: String? = "Test Merchant",
        amount: Double? = 100.0,
        currency: String? = "INR",
        transactionType: String? = null,
        sourceApp: String? = "com.bank.app",
        timestamp: Long = 1622505600000L,
        rawNotification: String = "Raw notification text",
        confidence: Double = 1.0
    ): TransactionCandidate {
        return TransactionCandidate(
            merchant = merchant,
            amount = amount,
            currency = currency,
            transactionType = transactionType,
            sourceApp = sourceApp,
            timestamp = timestamp,
            rawNotification = rawNotification,
            confidence = confidence
        )
    }
}
