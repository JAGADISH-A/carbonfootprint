package com.carbonwise.connect.sms

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsFilter @Inject constructor() {

    fun isUseful(sms: RawSms): Boolean {
        val lowerBody = sms.body.lowercase()
        
        // 1. Ignore OTPs and verification codes
        if (lowerBody.contains("otp") || 
            lowerBody.contains("verification code") || 
            lowerBody.contains("do not share")) {
            return false
        }
        
        // 2. Ignore obvious spam or personal (though sender checks usually filter personal)
        // Here we rely on MerchantPatterns to decide if it's a known merchant
        val merchant = MerchantPatterns.getMatchedMerchant(sms.sender, sms.body)
        
        return merchant != null
    }

    fun getMerchant(sms: RawSms): String {
        return MerchantPatterns.getMatchedMerchant(sms.sender, sms.body) ?: "Unknown"
    }
}
