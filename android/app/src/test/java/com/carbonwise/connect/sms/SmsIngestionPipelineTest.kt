package com.carbonwise.connect.sms

import com.carbonwise.connect.data.local.SettingsStore
import com.carbonwise.connect.data.model.PendingActivity
import com.carbonwise.connect.data.queue.PendingActivityRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class SmsIngestionPipelineTest {

    private val mockCollector = mockk<SmsCollector>()
    private val mockFilter = SmsFilter()
    private val mockNormalizer = SmsNormalizer(mockFilter)
    private val mockRepository = mockk<PendingActivityRepository>()
    private val mockSettingsStore = mockk<SettingsStore>(relaxed = true)

    private val pipeline = SmsIngestionPipeline(
        mockCollector,
        mockFilter,
        mockNormalizer,
        mockRepository,
        mockSettingsStore
    )

    @Test
    fun `test pipeline processes messages and skips duplicates`() = runBlocking {
        val now = System.currentTimeMillis()
        val sms1 = RawSms("Swiggy", "Swiggy order delivered: Rs.450", now, 1, 1)
        val sms2 = RawSms("Amazon", "Amazon order shipped: Rs.2,399", now - 1000, 1, 2)
        val otpSms = RawSms("Airtel", "Your OTP is 123456", now - 2000, 1, 3)

        coEvery { mockCollector.collectSms(any()) } returns listOf(sms1, sms2, otpSms)
        coEvery { mockCollector.getTotalInboxCount() } returns 10
        coEvery { mockRepository.insertActivity(any()) } answers {
            val act = firstArg<PendingActivity>()
            // Let's pretend sms1 is inserted successfully, and sms2 is a duplicate (existed)
            act.sender == "Swiggy"
        }
        coEvery { mockRepository.getTotalRowCount() } returns 5

        val result = pipeline.runPipeline(0L)

        // otpSms rejected -> 2 relevant
        // sms1 inserted, sms2 duplicate -> 1 new activity saved, 1 duplicate
        assertThat(result.found).isEqualTo(3)
        assertThat(result.relevant).isEqualTo(2)
        assertThat(result.newActivitiesSaved).isEqualTo(1)
        assertThat(result.duplicates).isEqualTo(1)

        // Verify only relevant messages are normalized and inserted
        coVerify(exactly = 1) { mockRepository.insertActivity(match { it.sender == "Swiggy" }) }
        coVerify(exactly = 1) { mockRepository.insertActivity(match { it.sender == "Amazon" }) }
        coVerify(exactly = 0) { mockRepository.insertActivity(match { it.sender == "Airtel" }) }
    }
}
