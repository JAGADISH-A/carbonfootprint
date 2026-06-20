package com.carbonwise.connect.ingestion.extraction

import com.carbonwise.connect.ingestion.model.NotificationEvent
import com.carbonwise.connect.ingestion.model.TransactionCandidate
import com.carbonwise.connect.ingestion.notification.rules.AmountDetector
import com.carbonwise.connect.ingestion.notification.rules.MerchantDetector

/**
 * Extracts structured transaction information from notifications.
 * It uses deterministic parsing to extract fields like amount and merchant.
 * Independent of Android framework classes, making it reusable.
 */
class TransactionExtractor(
    private val amountDetector: AmountDetector = AmountDetector(),
    private val merchantDetector: MerchantDetector = MerchantDetector()
) {

    /**
     * Extracts structured information from a NotificationEvent.
     * It never rejects notifications, it just extracts what it can.
     * Values that cannot be extracted are left null.
     *
     * @param event The notification event to process.
     * @return A TransactionCandidate containing the extracted fields.
     */
    fun extract(event: NotificationEvent): TransactionCandidate {
        val title = event.rawData.title
        val body = event.rawData.body
        val fullText = "$title $body"

        // Detect amounts
        val detectedAmounts = amountDetector.detectFromFields(title, body)
        // For simplicity, take the amount with the highest confidence
        val bestAmount = detectedAmounts.maxByOrNull { it.confidence }

        // Detect merchants
        val detectedMerchants = merchantDetector.detectFromFields(title, body)
        // For simplicity, take the merchant with the highest confidence
        val bestMerchant = detectedMerchants.maxByOrNull { it.confidence }

        // Determine basic confidence based on extracted values
        var confidence = 0.0
        if (bestAmount != null) confidence += 0.5
        if (bestMerchant != null) confidence += 0.5

        return TransactionCandidate(
            merchant = bestMerchant?.merchantName,
            amount = bestAmount?.amount,
            currency = bestAmount?.currency?.name,
            transactionType = null, // Deterministic parsing doesn't currently detect transactionType, left null as per instructions
            sourceApp = event.packageName,
            timestamp = event.timestamp,
            rawNotification = fullText,
            confidence = confidence
        )
    }
}
