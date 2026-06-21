package com.carbonwise.connect.notification

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationFilter @Inject constructor() {

    // Hardcoded rules for now, to be moved to configuration later
    private val allowedPackages = listOf(
        // Testing / Debugging
        "com.android.shell", "com.android.systemui",
        // Shopping
        "com.amazon.mShop.android.shopping", "com.flipkart.android", "com.myntra.android", 
        "com.grofers.customerapp", "com.bigbasket.mobileapp",
        // Food Delivery
        "in.swiggy.android", "com.application.zomato",
        // Transport
        "com.ubercab", "com.olacabs.customer", "com.rapido.passenger",
        // Travel
        "cris.org.in.prs.ima", "in.amazon.mShop.android.shopping", "in.goindigo.android", "com.airindia.mobe",
        // Finance
        "com.snapwork.hdfc", "com.csam.icici.bank.imobile", "com.sbi.SBIFreedomPlus", "com.axis.mobile", "com.msf.kbank.mobile"
    )

    private val ignoredPackages = listOf(
        "com.whatsapp", "org.telegram.messenger", "org.thoughtcrime.securesms",
        "com.google.android.apps.messaging", "com.samsung.android.messaging"
    )

    private val transactionKeywords = listOf(
        "order", "delivered", "shipped", "paid", "debited", "credited", "transaction", "booked", "trip", "payment"
    )

    private val ignoredKeywords = listOf(
        "otp", "one time password", "downloading", "system update"
    )

    fun isUseful(notification: RawNotification): Boolean {
        // Stage 1: Package-based matching
        if (notification.packageName in ignoredPackages) {
            return false
        }
        
        // Let's use substring matching for allowed packages to be robust against package name variations
        val isAllowedPackage = allowedPackages.any { notification.packageName.contains(it, ignoreCase = true) }
        
        if (!isAllowedPackage) {
            return false
        }
        
        // Stage 2: Content-based keyword matching
        val content = "${notification.title.orEmpty()} ${notification.text.orEmpty()} ${notification.subText.orEmpty()}".lowercase()
        
        // Ignore specific patterns (e.g., OTP)
        if (ignoredKeywords.any { content.contains(it) }) {
            return false
        }
        
        // Accept if it contains transactional keywords
        val hasTransactionKeyword = transactionKeywords.any { content.contains(it) }
        if (hasTransactionKeyword) {
            return true
        } else {
            return false
        }
    }
    
    fun getMerchant(notification: RawNotification): String {
        return notification.appName ?: notification.packageName.substringAfterLast(".")
    }
}
