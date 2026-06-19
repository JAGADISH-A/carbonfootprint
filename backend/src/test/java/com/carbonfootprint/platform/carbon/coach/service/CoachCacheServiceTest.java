package com.carbonfootprint.platform.carbon.coach.service;

import com.carbonfootprint.platform.carbon.analytics.model.CarbonAnalyticsResponse;
import com.carbonfootprint.platform.carbon.analytics.model.CategoryEmissionSummary;
import com.carbonfootprint.platform.carbon.analytics.model.TopEmissionActivity;
import com.carbonfootprint.platform.carbon.coach.config.CoachCacheProperties;
import com.carbonfootprint.platform.carbon.coach.model.AICarbonCoachResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CoachCacheServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private CoachCacheProperties properties;
    private CoachCacheService cache;

    @BeforeEach
    void setUp() {
        properties = new CoachCacheProperties();
        properties.setEnabled(true);
        properties.setTtlMinutes(60);
        cache = new CoachCacheService(properties, OBJECT_MAPPER);
    }

    // ── Cache hit/miss ────────────────────────────────────────────────────

    @Test
    void get_identicalAnalytics_returnsCachedResponse() {
        CarbonAnalyticsResponse analytics = sampleAnalytics();
        AICarbonCoachResponse response = sampleResponse();

        cache.put("user-1", analytics, response);

        AICarbonCoachResponse cached = cache.get("user-1", analytics);

        assertThat(cached).isNotNull();
        assertThat(cached.getSummary()).isEqualTo("AI summary");
        assertThat(cached.isAiGenerated()).isTrue();
    }

    @Test
    void get_differentAnalytics_returnsNull() {
        CarbonAnalyticsResponse analytics1 = sampleAnalytics();
        CarbonAnalyticsResponse analytics2 = CarbonAnalyticsResponse.builder()
                .activityCount(10)
                .totalCarbonKg(new BigDecimal("100.0"))
                .build();

        cache.put("user-1", analytics1, sampleResponse());

        AICarbonCoachResponse cached = cache.get("user-1", analytics2);

        assertThat(cached).isNull();
    }

    @Test
    void get_differentUser_returnsNull() {
        CarbonAnalyticsResponse analytics = sampleAnalytics();

        cache.put("user-1", analytics, sampleResponse());

        AICarbonCoachResponse cached = cache.get("user-2", analytics);

        assertThat(cached).isNull();
    }

    @Test
    void get_cacheDisabled_returnsNull() {
        properties.setEnabled(false);

        CarbonAnalyticsResponse analytics = sampleAnalytics();
        cache.put("user-1", analytics, sampleResponse());

        AICarbonCoachResponse cached = cache.get("user-1", analytics);

        assertThat(cached).isNull();
    }

    @Test
    void get_nullAnalytics_returnsNull() {
        cache.put("user-1", sampleAnalytics(), sampleResponse());

        AICarbonCoachResponse cached = cache.get("user-1", null);

        assertThat(cached).isNull();
    }

    // ── Expiry ────────────────────────────────────────────────────────────

    @Test
    void get_expiredEntry_returnsNull() {
        properties.setTtlMinutes(0); // expires immediately

        CarbonAnalyticsResponse analytics = sampleAnalytics();
        cache.put("user-1", analytics, sampleResponse());

        // Wait for expiry
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}

        AICarbonCoachResponse cached = cache.get("user-1", analytics);

        assertThat(cached).isNull();
    }

    @Test
    void evictExpired_removesExpiredEntries() {
        properties.setTtlMinutes(0);

        CarbonAnalyticsResponse analytics = sampleAnalytics();
        cache.put("user-1", analytics, sampleResponse());
        cache.put("user-2", analytics, sampleResponse());

        try { Thread.sleep(50); } catch (InterruptedException ignored) {}

        cache.evictExpired();

        assertThat(cache.size()).isZero();
    }

    @Test
    void evictExpired_keepsValidEntries() {
        properties.setTtlMinutes(60);

        CarbonAnalyticsResponse analytics = sampleAnalytics();
        cache.put("user-1", analytics, sampleResponse());

        cache.evictExpired();

        assertThat(cache.size()).isEqualTo(1);
    }

    // ── Eviction ──────────────────────────────────────────────────────────

    @Test
    void evict_removesAllEntriesForUser() {
        CarbonAnalyticsResponse analytics = sampleAnalytics();
        cache.put("user-1", analytics, sampleResponse());
        cache.put("user-1", analytics, sampleResponse()); // same key
        cache.put("user-2", analytics, sampleResponse());

        cache.evict("user-1");

        assertThat(cache.size()).isEqualTo(1);
    }

    // ── Hash determinism ──────────────────────────────────────────────────

    @Test
    void computeHash_sameInputSameHash() {
        CarbonAnalyticsResponse analytics = sampleAnalytics();

        String hash1 = cache.computeHash(analytics);
        String hash2 = cache.computeHash(analytics);

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void computeHash_differentInputDifferentHash() {
        CarbonAnalyticsResponse analytics1 = sampleAnalytics();
        CarbonAnalyticsResponse analytics2 = CarbonAnalyticsResponse.builder()
                .activityCount(10)
                .totalCarbonKg(new BigDecimal("100.0"))
                .build();

        String hash1 = cache.computeHash(analytics1);
        String hash2 = cache.computeHash(analytics2);

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void computeHash_withInstantFields_serializesSuccessfully() {
        CarbonAnalyticsResponse analytics = CarbonAnalyticsResponse.builder()
                .activityCount(2)
                .totalCarbonKg(new BigDecimal("10.0"))
                .topActivities(List.of(
                        TopEmissionActivity.builder()
                                .activityId("act-1")
                                .merchant("Test")
                                .category("FUEL")
                                .carbonKg(new BigDecimal("10.0"))
                                .occurredAt(Instant.parse("2026-01-15T10:30:00Z"))
                                .build()))
                .build();

        String hash = cache.computeHash(analytics);

        assertThat(hash).isNotBlank();
    }

    @Test
    void get_withInstantFields_roundTripsSuccessfully() {
        CarbonAnalyticsResponse analytics = CarbonAnalyticsResponse.builder()
                .activityCount(1)
                .totalCarbonKg(new BigDecimal("5.0"))
                .topActivities(List.of(
                        TopEmissionActivity.builder()
                                .activityId("act-1")
                                .merchant("Test")
                                .category("FUEL")
                                .carbonKg(new BigDecimal("5.0"))
                                .occurredAt(Instant.parse("2026-03-01T08:00:00Z"))
                                .build()))
                .build();
        AICarbonCoachResponse response = sampleResponse();

        cache.put("user-1", analytics, response);
        AICarbonCoachResponse cached = cache.get("user-1", analytics);

        assertThat(cached).isNotNull();
        assertThat(cached.getSummary()).isEqualTo("AI summary");
    }

    @Test
    void computeHash_sortedCategories_producesSameHash() {
        CarbonAnalyticsResponse unsorted = CarbonAnalyticsResponse.builder()
                .activityCount(5)
                .totalCarbonKg(new BigDecimal("45.0"))
                .categoryTotals(List.of(
                        CategoryEmissionSummary.builder().category("SHOPPING").carbonKg(new BigDecimal("20.0")).build(),
                        CategoryEmissionSummary.builder().category("FUEL").carbonKg(new BigDecimal("25.0")).build()))
                .build();

        CarbonAnalyticsResponse sorted = CarbonAnalyticsResponse.builder()
                .activityCount(5)
                .totalCarbonKg(new BigDecimal("45.0"))
                .categoryTotals(List.of(
                        CategoryEmissionSummary.builder().category("FUEL").carbonKg(new BigDecimal("25.0")).build(),
                        CategoryEmissionSummary.builder().category("SHOPPING").carbonKg(new BigDecimal("20.0")).build()))
                .build();

        String hash1 = cache.computeHash(unsorted);
        String hash2 = cache.computeHash(sorted);

        assertThat(hash1).isEqualTo(hash2);
    }

    // ── Only AI responses cached ──────────────────────────────────────────

    @Test
    void put_nullResponse_notStored() {
        CarbonAnalyticsResponse analytics = sampleAnalytics();

        cache.put("user-1", analytics, null);

        assertThat(cache.size()).isZero();
    }

    @Test
    void put_nullAnalytics_notStored() {
        cache.put("user-1", null, sampleResponse());

        assertThat(cache.size()).isZero();
    }

    @Test
    void put_disabled_notStored() {
        properties.setEnabled(false);

        cache.put("user-1", sampleAnalytics(), sampleResponse());

        assertThat(cache.size()).isZero();
    }

    // ── Statistics ────────────────────────────────────────────────────────

    @Test
    void getStatistics_initialState_allZeros() {
        CoachCacheStatistics stats = cache.getStatistics();

        assertThat(stats.hits()).isZero();
        assertThat(stats.misses()).isZero();
        assertThat(stats.stores()).isZero();
        assertThat(stats.expiredRemovals()).isZero();
        assertThat(stats.currentSize()).isZero();
        assertThat(stats.hitRatio()).isZero();
        assertThat(stats.missRatio()).isZero();
        assertThat(stats.groqCallsAvoided()).isZero();
    }

    @Test
    void getStatistics_afterHit_tracksHitCount() {
        CarbonAnalyticsResponse analytics = sampleAnalytics();
        cache.put("user-1", analytics, sampleResponse());

        cache.get("user-1", analytics);

        CoachCacheStatistics stats = cache.getStatistics();
        assertThat(stats.hits()).isEqualTo(1);
        assertThat(stats.misses()).isZero();
        assertThat(stats.groqCallsAvoided()).isEqualTo(1);
    }

    @Test
    void getStatistics_afterMiss_tracksMissCount() {
        CarbonAnalyticsResponse analytics = sampleAnalytics();

        cache.get("user-1", analytics);

        CoachCacheStatistics stats = cache.getStatistics();
        assertThat(stats.misses()).isEqualTo(1);
        assertThat(stats.hits()).isZero();
        assertThat(stats.groqCallsAvoided()).isZero();
    }

    @Test
    void getStatistics_afterStore_tracksStoreCount() {
        CarbonAnalyticsResponse analytics = sampleAnalytics();

        cache.put("user-1", analytics, sampleResponse());

        CoachCacheStatistics stats = cache.getStatistics();
        assertThat(stats.stores()).isEqualTo(1);
        assertThat(stats.currentSize()).isEqualTo(1);
    }

    @Test
    void getStatistics_afterExpiredRemoval_tracksExpiredCount() {
        properties.setTtlMinutes(0);

        CarbonAnalyticsResponse analytics = sampleAnalytics();
        cache.put("user-1", analytics, sampleResponse());

        try { Thread.sleep(50); } catch (InterruptedException ignored) {}

        cache.get("user-1", analytics);

        CoachCacheStatistics stats = cache.getStatistics();
        assertThat(stats.expiredRemovals()).isEqualTo(1);
        assertThat(stats.misses()).isEqualTo(1);
    }

    @Test
    void getStatistics_hitAndMissRatio_computedCorrectly() {
        CarbonAnalyticsResponse analytics = sampleAnalytics();
        cache.put("user-1", analytics, sampleResponse());

        cache.get("user-1", analytics);  // hit
        cache.get("user-1", analytics);  // hit
        cache.get("user-1", analytics);  // hit
        cache.get("user-1", CarbonAnalyticsResponse.builder()
                .activityCount(10)
                .totalCarbonKg(new BigDecimal("100.0"))
                .build());  // miss (different analytics)

        CoachCacheStatistics stats = cache.getStatistics();
        assertThat(stats.hits()).isEqualTo(3);
        assertThat(stats.misses()).isEqualTo(1);
        assertThat(stats.hitRatio()).isCloseTo(0.75, org.assertj.core.data.Offset.offset(0.01));
        assertThat(stats.missRatio()).isCloseTo(0.25, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void getStatistics_toSummaryString_containsKeyMetrics() {
        CarbonAnalyticsResponse analytics = sampleAnalytics();
        cache.put("user-1", analytics, sampleResponse());
        cache.get("user-1", analytics);

        CoachCacheStatistics stats = cache.getStatistics();
        String summary = stats.toSummaryString();

        assertThat(summary).contains("Hit Rate");
        assertThat(summary).contains("Miss Rate");
        assertThat(summary).contains("Entries");
        assertThat(summary).contains("Groq Calls Prevented");
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private CarbonAnalyticsResponse sampleAnalytics() {
        return CarbonAnalyticsResponse.builder()
                .activityCount(5)
                .totalCarbonKg(new BigDecimal("45.0"))
                .averageDailyKg(new BigDecimal("6.4"))
                .categoryTotals(List.of(
                        CategoryEmissionSummary.builder()
                                .category("SHOPPING")
                                .carbonKg(new BigDecimal("27.0"))
                                .percentageOfTotal(new BigDecimal("60.0"))
                                .build()))
                .topActivities(List.of(
                        TopEmissionActivity.builder()
                                .activityId("act-1")
                                .merchant("Ashoka")
                                .category("SHOPPING")
                                .carbonKg(new BigDecimal("27.0"))
                                .build()))
                .build();
    }

    private AICarbonCoachResponse sampleResponse() {
        return AICarbonCoachResponse.builder()
                .summary("AI summary")
                .strengths(List.of("Strength 1"))
                .concerns(List.of("Concern 1"))
                .recommendations(List.of("Rec 1"))
                .weeklyChallenge("Challenge")
                .motivation("Motivation")
                .aiGenerated(true)
                .build();
    }
}
