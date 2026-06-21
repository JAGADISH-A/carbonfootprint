package com.carbonwise.connect.sms

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsFilter @Inject constructor() {

    fun isUseful(sms: RawSms): Boolean {
        return getRejectionReason(sms) == null
    }

    fun getRejectionReason(sms: RawSms): String? {
        val lowerBody = sms.body.lowercase()
        
        // 1. Check OTP/Security/Verification
        if (lowerBody.contains("otp") || 
            lowerBody.contains("verification code") || 
            lowerBody.contains("do not share") ||
            lowerBody.contains("login alert") ||
            lowerBody.contains("security alert") ||
            lowerBody.contains("password")) {
            return "OTP/Security"
        }
        
        // 2. Check generic shipping/tracking updates (without delivery confirmation or purchase details)
        if (lowerBody.contains("shipped") ||
            lowerBody.contains("tracking number") ||
            lowerBody.contains("awb") ||
            lowerBody.contains("consignment") ||
            lowerBody.contains("courier tracking")) {
            // Confirming delivery or purchase should NOT be discarded
            if (!lowerBody.contains("delivered") && !lowerBody.contains("order")) {
                return "Shipping Noise"
            }
        }
        
        // 3. Check promotional/marketing spam
        if (lowerBody.contains("promo") ||
            lowerBody.contains("offer") ||
            lowerBody.contains("discount") ||
            lowerBody.contains("coupon") ||
            lowerBody.contains("marketing") ||
            lowerBody.contains("advertisement") ||
            lowerBody.contains("apply code") ||
            lowerBody.contains("scratch card") ||
            lowerBody.contains("click") ||
            lowerBody.contains("free ") ||
            lowerBody.contains("winners get") ||
            lowerBody.contains("play now") ||
            lowerBody.contains("gb/day") ||
            lowerBody.contains("gb data")) {
            // Keep transaction details even if they mention promo codes/offers
            if (!isTransactionPattern(sms)) {
                return "Promotion"
            }
        }
        
        // 4. Layered candidate check (any match makes it accepted)
        if (isCandidate(sms)) {
            return null // Accepted!
        }
        
        return "Noise"
    }

    private fun isTransactionPattern(sms: RawSms): Boolean {
        val lowerBody = sms.body.lowercase()
        val financialKeywords = listOf("spent", "debited", "paid", "sent", "transferred", "charged", "received", "credited")
        return financialKeywords.any { lowerBody.contains(it) }
    }

    private fun isCandidate(sms: RawSms): Boolean {
        val lowerBody = sms.body.lowercase()
        
        // A. Known merchant (from MerchantPatterns)
        val merchant = MerchantPatterns.getMatchedMerchant(sms.sender, sms.body)
        if (merchant != null) return true
        
        // B. Financial/payment keywords (inclusive check)
        val financialKeywords = listOf(
            "spent", "debited", "paid", "sent", "transferred", 
            "charged", "received", "credited", "bought", "purchased",
            "order status", "delivered", "booking status", "ticket", "boarding pass"
        )
        if (financialKeywords.any { lowerBody.contains(it) }) return true
        
        // C. Currency + transaction pattern (Rs, INR, $, USD followed by digits)
        val currencyPattern = Regex("""(?i)(rs\.?|inr|usd|\$)\s?\d+""")
        if (currencyPattern.containsMatchIn(sms.body)) return true
        
        return false
    }

    fun getMerchant(sms: RawSms): String {
        return MerchantPatterns.getMatchedMerchant(sms.sender, sms.body) ?: "Unknown"
    }
}
