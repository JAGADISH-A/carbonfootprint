package com.carbonwise.connect.sms

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class SmsFilterTest {

    private val filter = SmsFilter()

    @Test
    fun `test SMS filter accepts transaction messages`() {
        val acceptList = listOf(
            // Food ordering
            RawSms("Swiggy", "Swiggy order delivered: Rs.450", 1717424194327L, 1, 1),
            RawSms("Zomato", "Your Zomato order has been placed. Paid Rs. 350", 1717424194327L, 1, 2),
            // Shopping
            RawSms("Amazon", "Amazon order shipped: Rs.2,399", 1717424194327L, 1, 3),
            RawSms("Flipkart", "Thank you for shopping at Flipkart. Your order for Rs. 1200 is confirmed", 1717424194327L, 1, 4),
            // Transport
            RawSms("Uber", "Uber: Your ride is confirmed. Paid Rs. 500", 1717424194327L, 1, 5),
            RawSms("Ola", "Ola: Booking success. Paid Rs. 400", 1717424194327L, 1, 6),
            // Fuel
            RawSms("HPCL", "HPCL: You have purchased 35.5L of petrol for Rs.3200", 1717424194327L, 1, 7),
            RawSms("BPCL", "BPCL: Fuel purchase of Rs. 2000 successful", 1717424194327L, 1, 8),
            // Utility bills
            RawSms("BESCOM", "BESCOM: Your bill of Rs.850 is due on 15-Jan", 1717424194327L, 1, 9),
            // Bank transactions / Payment confirmations
            RawSms("HDFCBK", "Spent Rs. 500 at Swiggy using HDFC Bank credit card", 1717424194327L, 1, 10),
            RawSms("AXISBK", "A/C X6909 Debit Rs.160.00 for UPI to pushpalatha", 1717424194327L, 1, 11)
        )

        for (sms in acceptList) {
            assertThat(filter.isUseful(sms)).isTrue()
        }
    }

    @Test
    fun `test SMS filter rejects OTPs, login alerts, promotional, and noise`() {
        val rejectList = listOf(
            // OTPs / Verification Codes
            RawSms("IPPB", "724709 is your OTP for MB-login/MPIN-reset", 1717424194327L, 1, 1),
            RawSms("WhatsApp", "Your WhatsApp code: 784-335", 1717424194327L, 1, 2),
            // Login Alerts
            RawSms("IPPB", "An attempt to login to your IPPB mobile app failed due to incorrect MPIN", 1717424194327L, 1, 3),
            // Promotional
            RawSms("Airtel", "Get 3GB/day data, for 3 days, in Rs39. Click i.airtel.in/rc39sil", 1717424194327L, 1, 4),
            RawSms("Vi", "Play now! Top 10 winners get FREE 2GB data!", 1717424194327L, 1, 5),
            // Generic Shipping without purchase evidence
            RawSms("Courier", "Your shipment with AWB 123456 is in transit", 1717424194327L, 1, 6),
            // Personal/Generic Noise
            RawSms("Mom", "Hi honey, what do you want for dinner?", 1717424194327L, 1, 7)
        )

        for (sms in rejectList) {
            assertThat(filter.isUseful(sms)).isFalse()
        }
    }
}
