package com.carbonfootprint.platform.carbon.coach.service;

import com.carbonfootprint.platform.carbon.analytics.model.CarbonAnalyticsResponse;
import com.carbonfootprint.platform.carbon.coach.config.CoachCacheProperties;
import com.carbonfootprint.platform.carbon.coach.model.AICarbonCoachResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Thread-safe in-memory cache for AI Coach responses.
 *
 * <h3>Cache key</h3>
 * {@code userId:analyticsHash} where analyticsHash is a SHA-256 digest of the
 * canonical JSON serialization of {@link CarbonAnalyticsResponse}.
 *
 * <h3>Behaviour</h3>
 * <ul>
 *   <li>Only successful AI responses are cached (never fallbacks or errors)</li>
 *   <li>Expired entries are lazily evicted during {@code get()} and {@code put()}</li>
 *   <li>No background scheduler — cleanup happens on access</li>
 * </ul>
 *
 * <h3>Observability</h3>
 * Runtime statistics are tracked via {@link CoachCacheStatistics} and logged
 * every 100 operations for operational visibility.
 *
 * <h3>Thread safety</h3>
 * Backed by {@link ConcurrentHashMap}. Counters use {@link LongAdder} for
 * high-throughput concurrent updates.
 */
@Slf4j
public class CoachCacheService {

    private static final ObjectMapper SORTED_MAPPER = new ObjectMapper()
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    private static final long STATS_LOG_INTERVAL = 100;

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final CoachCacheProperties properties;

    // ── Counters ──────────────────────────────────────────────────────────
    private final LongAdder hits = new LongAdder();
    private final LongAdder misses = new LongAdder();
    private final LongAdder stores = new LongAdder();
    private final LongAdder expiredRemovals = new LongAdder();
    private final AtomicLong operationCounter = new AtomicLong(0);

    public CoachCacheService(CoachCacheProperties properties) {
        this.properties = properties;
    }

    /**
     * Retrieves a cached coach response if present and not expired.
     *
     * @param userId           the user identifier
     * @param analyticsResponse the current analytics data (used to compute hash)
     * @return the cached response, or {@code null} on miss/expiry
     */
    public AICarbonCoachResponse get(String userId, CarbonAnalyticsResponse analyticsResponse) {
        if (!properties.isEnabled() || analyticsResponse == null) {
            return null;
        }

        String hash = computeHash(analyticsResponse);
        String key = userId + ":" + hash;
        String hashPrefix = hash.substring(0, Math.min(8, hash.length()));

        CacheEntry entry = cache.get(key);
        if (entry == null) {
            misses.increment();
            log.debug("Cache MISS — userId={} hash={} key={}", userId, hashPrefix, key);
            maybeLogStats();
            return null;
        }

        if (Instant.now().isAfter(entry.expiresAt)) {
            long expiredSecondsAgo = Duration.between(entry.expiresAt, Instant.now()).getSeconds();
            cache.remove(key);
            expiredRemovals.increment();
            misses.increment();
            log.debug("Cache EXPIRED — userId={} hash={} expired={}s ago",
                    userId, hashPrefix, expiredSecondsAgo);
            maybeLogStats();
            return null;
        }

        long ttlRemainingSeconds = Duration.between(Instant.now(), entry.expiresAt).getSeconds();
        hits.increment();
        log.debug("Cache HIT — userId={} hash={} ttlRemaining={}s",
                userId, hashPrefix, ttlRemainingSeconds);
        maybeLogStats();
        return entry.response;
    }

    /**
     * Stores a coach response in the cache.
     * Only successful AI responses should be stored — callers must verify
     * {@code response.isAiGenerated()} before calling this method.
     *
     * @param userId           the user identifier
     * @param analyticsResponse the analytics data (used to compute hash)
     * @param response         the successful AI-generated response to cache
     */
    public void put(String userId, CarbonAnalyticsResponse analyticsResponse, AICarbonCoachResponse response) {
        if (!properties.isEnabled() || analyticsResponse == null || response == null) {
            return;
        }

        String hash = computeHash(analyticsResponse);
        String key = userId + ":" + hash;
        String hashPrefix = hash.substring(0, Math.min(8, hash.length()));

        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(properties.getTtlMinutes()));
        cache.put(key, new CacheEntry(response, expiresAt));
        stores.increment();

        log.debug("Cache STORED — userId={} hash={} ttlMinutes={}",
                userId, hashPrefix, properties.getTtlMinutes());
        maybeLogStats();
    }

    /**
     * Evicts all cached entries for the given user.
     *
     * @param userId the user whose entries should be removed
     */
    public void evict(String userId) {
        var it = cache.entrySet().iterator();
        int removed = 0;
        while (it.hasNext()) {
            Map.Entry<String, CacheEntry> entry = it.next();
            if (entry.getKey().startsWith(userId + ":")) {
                it.remove();
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("Cache EVICT — userId={} entriesRemoved={}", userId, removed);
        }
    }

    /**
     * Returns the current cache size (for monitoring/debugging).
     */
    public int size() {
        return cache.size();
    }

    // ── Statistics ────────────────────────────────────────────────────────

    /**
     * Returns a snapshot of runtime cache statistics.
     *
     * @return immutable statistics record
     */
    public CoachCacheStatistics getStatistics() {
        long h = hits.sum();
        long m = misses.sum();
        long total = h + m;
        double hitRatio = total > 0 ? (double) h / total : 0.0;
        double missRatio = total > 0 ? (double) m / total : 0.0;

        return new CoachCacheStatistics(
                h,
                m,
                stores.sum(),
                expiredRemovals.sum(),
                cache.size(),
                hitRatio,
                missRatio,
                h  // each hit prevents one Groq call
        );
    }

    // ── Lazy cleanup ──────────────────────────────────────────────────────

    /**
     * Removes expired entries lazily. Called during get()/put() operations.
     * Iterates the cache and removes entries that have passed their TTL.
     * Tolerates concurrent modification — skipped entries are retried next time.
     */
    void evictExpired() {
        Instant now = Instant.now();
        List<String> expiredKeys = new ArrayList<>();

        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            if (now.isAfter(entry.getValue().expiresAt)) {
                expiredKeys.add(entry.getKey());
            }
        }

        for (String key : expiredKeys) {
            CacheEntry removed = cache.remove(key);
            if (removed != null) {
                String userId = key.substring(0, key.indexOf(':'));
                String hashPrefix = key.substring(key.indexOf(':') + 1);
                hashPrefix = hashPrefix.substring(0, Math.min(8, hashPrefix.length()));
                long expiredSecondsAgo = Duration.between(removed.expiresAt, now).getSeconds();
                expiredRemovals.increment();
                log.debug("Cache EXPIRED — userId={} hash={} expired={}s ago",
                        userId, hashPrefix, expiredSecondsAgo);
            }
        }
    }

    // ── Periodic stats logging ────────────────────────────────────────────

    private void maybeLogStats() {
        long opCount = operationCounter.incrementAndGet();
        if (opCount % STATS_LOG_INTERVAL == 0) {
            CoachCacheStatistics stats = getStatistics();
            log.info("{}", stats.toSummaryString());
        }
    }

    // ── Hash computation ──────────────────────────────────────────────────

    /**
     * Computes a SHA-256 hash of the canonical JSON serialization of the analytics response.
     *
     * <p>Canonical form is ensured by:
     * <ul>
     *   <li>Jackson's {@code SORT_PROPERTIES_ALPHABETICALLY} (via default ObjectMapper with Lombok getters)</li>
     *   <li>Deterministic {@code BigDecimal} and {@code Instant} serialization</li>
     *   <li>Sorted list iteration order from the data model</li>
     * </ul>
     *
     * @param analytics the analytics response to hash
     * @return Base64-encoded SHA-256 hash string
     */
    String computeHash(CarbonAnalyticsResponse analytics) {
        try {
            CarbonAnalyticsResponse canonical = canonicalize(analytics);
            String json = SORTED_MAPPER.writeValueAsString(canonical);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException | JsonProcessingException e) {
            throw new IllegalStateException("Failed to compute analytics hash", e);
        }
    }

    /**
     * Creates a canonical copy of the analytics response with sorted lists
     * to ensure deterministic serialization.
     */
    private CarbonAnalyticsResponse canonicalize(CarbonAnalyticsResponse analytics) {
        CarbonAnalyticsResponse canonical = CarbonAnalyticsResponse.builder()
                .totalCarbonKg(analytics.getTotalCarbonKg())
                .averageDailyKg(analytics.getAverageDailyKg())
                .activityCount(analytics.getActivityCount())
                .periodStart(analytics.getPeriodStart())
                .periodEnd(analytics.getPeriodEnd())
                .build();

        if (analytics.getCategoryTotals() != null) {
            List<com.carbonfootprint.platform.carbon.analytics.model.CategoryEmissionSummary> sorted =
                    analytics.getCategoryTotals().stream()
                            .sorted(java.util.Comparator
                                    .comparing(c -> c.getCategory() != null ? c.getCategory() : ""))
                            .toList();
            canonical = CarbonAnalyticsResponse.builder()
                    .totalCarbonKg(canonical.getTotalCarbonKg())
                    .averageDailyKg(canonical.getAverageDailyKg())
                    .activityCount(canonical.getActivityCount())
                    .periodStart(canonical.getPeriodStart())
                    .periodEnd(canonical.getPeriodEnd())
                    .categoryTotals(sorted)
                    .build();
        }

        if (analytics.getMonthlyTrend() != null) {
            List<com.carbonfootprint.platform.carbon.analytics.model.MonthlyEmissionTrend> sorted =
                    analytics.getMonthlyTrend().stream()
                            .sorted(java.util.Comparator
                                    .comparing(t -> t.getMonth() != null ? t.getMonth() : ""))
                            .toList();
            canonical = CarbonAnalyticsResponse.builder()
                    .totalCarbonKg(canonical.getTotalCarbonKg())
                    .averageDailyKg(canonical.getAverageDailyKg())
                    .activityCount(canonical.getActivityCount())
                    .periodStart(canonical.getPeriodStart())
                    .periodEnd(canonical.getPeriodEnd())
                    .categoryTotals(canonical.getCategoryTotals())
                    .monthlyTrend(sorted)
                    .build();
        }

        if (analytics.getTopActivities() != null) {
            List<com.carbonfootprint.platform.carbon.analytics.model.TopEmissionActivity> sorted =
                    analytics.getTopActivities().stream()
                            .sorted(java.util.Comparator
                                    .comparing(a -> a.getActivityId() != null ? a.getActivityId() : ""))
                            .toList();
            canonical = CarbonAnalyticsResponse.builder()
                    .totalCarbonKg(canonical.getTotalCarbonKg())
                    .averageDailyKg(canonical.getAverageDailyKg())
                    .activityCount(canonical.getActivityCount())
                    .periodStart(canonical.getPeriodStart())
                    .periodEnd(canonical.getPeriodEnd())
                    .categoryTotals(canonical.getCategoryTotals())
                    .monthlyTrend(canonical.getMonthlyTrend())
                    .topActivities(sorted)
                    .build();
        }

        return canonical;
    }

    // ── Internal entry ────────────────────────────────────────────────────

    record CacheEntry(AICarbonCoachResponse response, Instant expiresAt) {
    }
}
