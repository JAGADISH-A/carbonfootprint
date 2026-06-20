package com.carbonwise.connect.sms

object MerchantPatterns {
    val KNOWN_MERCHANTS = listOf(
        "Amazon",
        "Flipkart",
        "Swiggy",
        "Zomato",
        "Blinkit",
        "BigBasket",
        "IRCTC",
        "AIRTEL",
        "Jio",
        "HDFC",
        "ICICI",
        "SBI",
        "Axis",
        "IndianOil",
        "HPCL",
        "BPCL",
        "Uber",
        "Ola",
        "Air India"
    ).map { it.lowercase() }

    // Sometimes sender IDs are like "AD-HDFCBK" or "VK-SWIGGY"
    // We can just check if any known merchant string is present in sender (case insensitive)
    
    fun getMatchedMerchant(sender: String, body: String): String? {
        val lowerSender = sender.lowercase()
        for (merchant in KNOWN_MERCHANTS) {
            if (lowerSender.contains(merchant)) {
                return merchant.replaceFirstChar { it.uppercase() }
            }
        }
        
        // As a fallback, sometimes the sender is a generic bank number,
        // and the merchant is in the body.
        // For this phase, we'll keep it simple and check if the body contains a merchant 
        // if it looks like a transactional message.
        // E.g., "Spent Rs. 500 at Swiggy"
        val lowerBody = body.lowercase()
        if (lowerBody.contains("spent") || lowerBody.contains("debited") || lowerBody.contains("paid")) {
            for (merchant in KNOWN_MERCHANTS) {
                if (lowerBody.contains(merchant)) {
                    return merchant.replaceFirstChar { it.uppercase() }
                }
            }
        }
        
        return null
    }
}
